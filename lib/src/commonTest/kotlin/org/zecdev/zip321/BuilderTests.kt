package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * K14: fluent builders + DSL sugar, ported 1:1 from the Swift blueprint's S14 BuilderTests.
 *
 * Covers the "four scenarios" cross-language construction contract (simple single-address
 * request, amount+memo payment, multi-payment, and parse with an injected validator),
 * deferred-error surfacing at `build()`, and equivalence between the `paymentRequest { }` DSL
 * and the explicit `PaymentRequest.Builder`.
 */
class BuilderTests {
    private val saplingAddress =
        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
    private val saplingAddress2 =
        "ztestsapling1n65uaftvs2g7075q2x2a04shfk066u3lldzxsrprfrqtzxnhc9ps73v4lhx4l9yfxj46sl0q90k"
    private val transparentAddress = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

    private fun recipient(value: String): RecipientAddress = validRecipient(value)

    // MARK: - (a) simple single-address request

    @Test
    fun `scenario a - simple single-address request`() {
        val payment =
            Payment.Builder(recipient(saplingAddress))
                .build()
                .getOrThrow()

        val request =
            PaymentRequest.Builder()
                .add(payment)
                .build()
                .getOrThrow()

        assertEquals(listOf(0u), request.indexedPayments.map { it.index })
        assertEquals(saplingAddress, request.payments.first().recipientAddress.value)
        assertNull(request.payments.first().amount)

        // Renders to the canonical leading-address form.
        assertEquals("zcash:$saplingAddress", ZIP321.uriString(from = request))
    }

    // MARK: - (b) amount + memo payment

    @Test
    fun `scenario b - amount and memo payment`() {
        val payment =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "1.2345")
                .memo(utf8 = "Thanks!")
                .message("Invoice #42")
                .build()
                .getOrThrow()

        assertEquals(NonNegativeAmount.zec("1.2345").getOrThrow(), payment.amount)
        assertEquals(MemoBytes("Thanks!").toBase64URL(), payment.memo?.toBase64URL())
        assertEquals("Invoice #42", payment.message)
        assertNull(payment.label)

        // Equivalent to the direct factory.
        val direct =
            Payment.create(
                recipientAddress = recipient(saplingAddress),
                amount = NonNegativeAmount.zec("1.2345").getOrThrow(),
                memo = MemoBytes("Thanks!"),
                label = null,
                message = "Invoice #42",
                otherParams = emptyList(),
            ).getOrThrow()

        assertEquals(direct, payment)
    }

    // MARK: - (c) multi-payment request

    @Test
    fun `scenario c - multi-payment request`() {
        val alice =
            Payment.Builder(recipient(transparentAddress))
                .amount(zec = "123.456")
                .build()
                .getOrThrow()

        val bob =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "0.789")
                .memo(utf8 = "hi bob")
                .build()
                .getOrThrow()

        val request =
            PaymentRequest.Builder()
                .add(alice)
                .add(bob)
                .build()
                .getOrThrow()

        assertEquals(listOf(0u, 1u), request.indexedPayments.map { it.index })
        assertEquals(transparentAddress, request.payments[0].recipientAddress.value)
        assertEquals(saplingAddress, request.payments[1].recipientAddress.value)

        // Explicit non-sequential index is preserved.
        val pinned =
            PaymentRequest.Builder()
                .add(alice)
                .add(bob, at = 7u)
                .build()
                .getOrThrow()
        assertEquals(listOf(0u, 7u), pinned.indexedPayments.map { it.index })
    }

    // MARK: - (d) parse with injected validator

    @Test
    fun `scenario d - parse with injected validator`() {
        val uri = "zcash:$saplingAddress?amount=1"

        // The injected validator is the ONLY authority: rejecting everything makes even a
        // checksum-valid address fail.
        val rejectAll = ZIP321.parse(uri, Network.TESTNET, AddressValidator { null })
        assertEquals(ZIP321Error.InvalidAddress(null), rejectAll.exceptionOrNull())

        // A validator that accepts it reports the descriptor the library then trusts.
        val request = ZIP321.parse(uri, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertEquals(saplingAddress, request.payments.first().recipientAddress.value)
    }

    // MARK: - deferred-error surfacing

    @Test
    fun `deferred bad zec string surfaces as AmountInvalid`() {
        val result =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "not-a-number")
                .build()

        assertEquals(ZIP321Error.AmountInvalid(null), result.exceptionOrNull())
    }

    @Test
    fun `deferred above max money surfaces as AmountExceededSupply`() {
        val result =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "21000000.00000001")
                .build()

        assertEquals(ZIP321Error.AmountExceededSupply(null), result.exceptionOrNull())
    }

    @Test
    fun `deferred memo on transparent recipient surfaces as TransparentMemo`() {
        val result =
            Payment.Builder(recipient(transparentAddress))
                .amount(zec = "1")
                .memo(utf8 = "memos not allowed to transparent")
                .build()

        assertEquals(ZIP321Error.TransparentMemo(null), result.exceptionOrNull())
    }

    @Test
    fun `deferred oversized memo surfaces as MemoBytesError`() {
        val result =
            Payment.Builder(recipient(saplingAddress))
                .memo(utf8 = "x".repeat(513))
                .build()

        assertEquals(ZIP321Error.MemoBytesError(null), result.exceptionOrNull())
    }

    @Test
    fun `deferred reserved otherparam key surfaces at build`() {
        val result =
            Payment.Builder(recipient(saplingAddress))
                .otherParam(name = "amount", value = "2")
                .build()

        assertEquals(
            ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER),
            result.exceptionOrNull(),
        )
    }

    @Test
    fun `first deferred error wins in field order`() {
        // Both amount and other-param are invalid; amount (checked first) wins.
        val result =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "bogus")
                .otherParam(name = "", value = "x")
                .build()

        assertEquals(ZIP321Error.AmountInvalid(null), result.exceptionOrNull())
    }

    @Test
    fun `duplicate index surfaces as DuplicateParameter`() {
        val payment = Payment.Builder(recipient(saplingAddress)).build().getOrThrow()

        val result =
            PaymentRequest.Builder()
                .add(payment, at = 3u)
                .add(payment, at = 3u)
                .build()

        assertEquals(ZIP321Error.DuplicateParameter("address", 3u), result.exceptionOrNull())
    }

    @Test
    fun `above max index surfaces as TooManyPayments`() {
        val payment = Payment.Builder(recipient(saplingAddress)).build().getOrThrow()

        val result =
            PaymentRequest.Builder()
                .add(payment, at = 10_000u)
                .build()

        assertEquals(ZIP321Error.TooManyPayments(10_000u), result.exceptionOrNull())
    }

    // MARK: - OtherParam.create validation

    @Test
    fun `OtherParam create accepts a valid non-reserved name`() {
        val param = OtherParam.create("future-param", "hello world").getOrThrow()
        assertEquals("future-param", param.name)
        assertEquals("hello world", param.value)

        // A value-less parameter is valid too.
        assertNull(OtherParam.create("flag", null).getOrThrow().value)
    }

    @Test
    fun `OtherParam create rejects empty reserved and malformed names`() {
        val invalidParameter = ZIP321Error.ParseError(ZIP321Error.StaticReason.INVALID_PARAMETER)

        // Empty name.
        assertEquals(invalidParameter, OtherParam.create("", "x").exceptionOrNull())

        // Every reserved query key.
        for (reserved in listOf("address", "amount", "label", "memo", "message")) {
            assertEquals(
                invalidParameter,
                OtherParam.create(reserved, "x").exceptionOrNull(),
                "reserved name '$reserved' must be rejected",
            )
        }

        // Any req- prefixed name (a library cannot mint required parameters).
        assertEquals(invalidParameter, OtherParam.create("req-zip", "x").exceptionOrNull())

        // Invalid paramname charset: leading digit, leading dash, inner disallowed characters.
        for (malformed in listOf("1param", "-param", "pa ram", "pa%20ram", "param=", "päram")) {
            assertEquals(
                invalidParameter,
                OtherParam.create(malformed, "x").exceptionOrNull(),
                "malformed name '$malformed' must be rejected",
            )
        }
    }

    // MARK: - DSL sugar equivalence

    @Test
    fun `DSL matches explicit builder`() {
        val explicit =
            PaymentRequest.Builder()
                .add(
                    Payment.Builder(recipient(transparentAddress))
                        .amount(zec = "123.456")
                        .build()
                        .getOrThrow(),
                )
                .add(
                    Payment.Builder(recipient(saplingAddress))
                        .amount(zec = "0.789")
                        .memo(utf8 = "hi bob")
                        .build()
                        .getOrThrow(),
                )
                .build()
                .getOrThrow()

        val sugar =
            paymentRequest {
                payment(recipient(transparentAddress)) {
                    amount(zec = "123.456")
                }
                payment(recipient(saplingAddress)) {
                    amount(zec = "0.789")
                    memo(utf8 = "hi bob")
                }
            }.getOrThrow()

        assertEquals(explicit, sugar)
    }

    @Test
    fun `DSL accepts a prebuilt single payment`() {
        val alice =
            Payment.Builder(recipient(saplingAddress))
                .amount(zec = "1")
                .build()
                .getOrThrow()

        val sugar = paymentRequest { payment(alice) }.getOrThrow()
        val explicit = PaymentRequest.Builder().add(alice).build().getOrThrow()

        assertEquals(explicit, sugar)
    }

    @Test
    fun `DSL accepts a list of prebuilt payments`() {
        val prebuilt =
            listOf(saplingAddress, saplingAddress2, transparentAddress).map { addr ->
                Payment.Builder(recipient(addr)).amount(zec = "1").build().getOrThrow()
            }

        val sugar = paymentRequest { payments(prebuilt) }.getOrThrow()
        val explicit = PaymentRequest.Builder(prebuilt).build().getOrThrow()

        assertEquals(explicit, sugar)
        assertEquals(listOf(0u, 1u, 2u), sugar.indexedPayments.map { it.index })
    }

    @Test
    fun `DSL empty block produces the empty request`() {
        val empty = paymentRequest { }.getOrThrow()

        assertTrue(empty.payments.isEmpty())
        assertEquals(PaymentRequest(emptyList()), empty)
    }

    @Test
    fun `DSL surfaces the first payment construction error in source order`() {
        val result =
            paymentRequest {
                payment(recipient(saplingAddress)) {
                    amount(zec = "bogus")
                }
                payment(recipient(transparentAddress)) {
                    memo(utf8 = "also invalid: memo to transparent")
                }
            }

        assertEquals(ZIP321Error.AmountInvalid(null), result.exceptionOrNull())
    }

    @Test
    fun `DSL full scenario renders the canonical URI`() {
        val request =
            paymentRequest {
                payment(recipient(saplingAddress)) {
                    amount(NonNegativeAmount.zec("1").getOrThrow())
                    memo(MemoBytes("This is a simple memo."))
                    message("Thank you for your purchase")
                }
            }.getOrThrow()

        assertEquals(
            "zcash:$saplingAddress?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg" +
                "&message=Thank%20you%20for%20your%20purchase",
            ZIP321.uriString(from = request),
        )
    }
}
