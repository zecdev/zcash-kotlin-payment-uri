package org.zecdev.zip321

import org.zecdev.zip321.ZIP321.FormattingOptions.EnumerateAllPayments
import org.zecdev.zip321.ZIP321.FormattingOptions.UseEmptyParamIndex
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZIP321EncodingTests {
    // NOTE (K13): the DEFAULT formatting options changed to the canonical reference form
    // (UseEmptyParamIndex(omitAddressLabel = true)); payments render at their ACTUAL stored
    // paramindices (0 -> empty suffix, 1 -> `.1`) instead of being re-enumerated from `.1`.
    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with default formatting options`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                amount = NonNegativeAmount.zec("123.45").getOrThrow(),
                memo = null,
                label = "apple",
                message = null,
                otherParams = emptyList(),
            )

        val payment2 =
            Payment(
                recipientAddress =
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                amount = NonNegativeAmount.zec("1.2345").getOrThrow(),
                memo = null,
                label = "banana",
                message = null,
                otherParams = emptyList(),
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest))
    }

    // NOTE (K13): address-label omission only applies to a SINGLE payment at the empty
    // paramindex (matching the reference `to_uri`); a multi-payment request renders every
    // address with an explicit `address[.n]=` label.
    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with empty param index and address label omitted`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                amount = NonNegativeAmount.zec("123.45").getOrThrow(),
                memo = null,
                label = "apple",
                message = null,
                otherParams = emptyList(),
            )

        val payment2 =
            Payment(
                recipientAddress =
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                amount = NonNegativeAmount.zec("1.2345").getOrThrow(),
                memo = null,
                label = "banana",
                message = null,
                otherParams = emptyList(),
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true)))
    }

    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with empty param index and address label not omitted`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                amount = NonNegativeAmount.zec("123.45").getOrThrow(),
                memo = null,
                label = "apple",
                message = null,
                otherParams = emptyList(),
            )

        val payment2 =
            Payment(
                recipientAddress =
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                amount = NonNegativeAmount.zec("1.2345").getOrThrow(),
                memo = null,
                label = "banana",
                message = null,
                otherParams = emptyList(),
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false)))
    }

    @Test
    fun `request RecipientAddress encodes single address with default formatting options`() {
        val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        assertEquals(expected, ZIP321.request(recipientAddress))
    }

    @Test
    fun `request RecipientAddress encodes single address with empty param index and address label not omitted`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        assertEquals(expected, ZIP321.request(recipientAddress, UseEmptyParamIndex(false)))
    }

    @Test
    fun `request RecipientAddress encodes single address with all payments enumerated`() {
        val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        assertEquals(expected, ZIP321.request(recipientAddress, EnumerateAllPayments))
    }

    // MARK: - request(Payment, FormattingOptions) — the single-payment convenience overload
    // (distinct from `request(RecipientAddress, ...)` above), including its default argument.

    @Test
    fun `request Payment encodes a single payment with default formatting options`() {
        val payment =
            Payment(
                recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                amount = NonNegativeAmount.zec("1.5").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals("zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=1.5", ZIP321.request(payment))
    }

    @Test
    fun `request Payment encodes a single payment with all payments enumerated`() {
        val payment =
            Payment(
                recipientAddress = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                amount = NonNegativeAmount.zec("1.5").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals(
            "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=1.5",
            ZIP321.request(payment, EnumerateAllPayments),
        )
    }

    // MARK: - Payment's deprecated throwing `invoke` shim
    //
    // Within this module, `Payment(args)` resolves directly to the (internal) primary
    // constructor — the deprecated `operator fun invoke` companion shim exists for callers OUTSIDE
    // the module, who cannot see that constructor. Call it explicitly (`Payment.invoke(...)`,
    // rather than the `Payment(...)` call-site sugar) to actually exercise it.
    @Test
    fun `deprecated Payment invoke shim constructs an equivalent Payment`() {
        val recipient = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        val viaShim =
            Payment.invoke(
                recipientAddress = recipient,
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )
        val viaCreate =
            Payment.create(
                recipientAddress = recipient,
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            ).getOrThrow()

        assertEquals(viaCreate, viaShim)
    }

    @Test
    fun `deprecated Payment invoke shim throws the ZIP321Error a structural rule violates`() {
        val transparent = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        val error =
            assertFailsWith<ZIP321Error.TransparentMemo> {
                Payment.invoke(
                    recipientAddress = transparent,
                    amount = null,
                    memo = MemoBytes("memos not allowed to transparent"),
                    label = null,
                    message = null,
                    otherParams = emptyList(),
                )
            }
        assertEquals(null, error.index)
    }
}
