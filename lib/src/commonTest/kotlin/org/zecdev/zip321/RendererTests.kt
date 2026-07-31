package org.zecdev.zip321

import org.zecdev.zip321.model.IndexedPayment
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * NOTE (K1/v2): amounts previously built with `123.456.toBigDecimal()` (JVM
 * BigDecimal setup sugar) are constructed via the equivalent common
 * `NonNegativeAmount.zec(String)` factory; the resulting zatoshi values and
 * every expected rendered string are unchanged.
 */
class RendererTests {
    @Test
    fun `Amount parameter is rendered with no paramIndex`() {
        val expected = "amount=123.456"
        val amount = NonNegativeAmount.zec("123.456").getOrThrow()
        assertEquals(expected, Render.parameter(amount, null))
    }

    @Test
    fun `Amount parameter is rendered with paramIndex`() {
        val expected = "amount.1=123.456"
        val amount = NonNegativeAmount.zec("123.456").getOrThrow()
        assertEquals(expected, Render.parameter(amount, 1u))
    }

    @Test
    fun `Address parameter is rendered with no paramIndex`() {
        val expected = "address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = validRecipient(address0)
        assertEquals(expected, Render.parameter(recipient0, null))
    }

    @Test
    fun `Address parameter is rendered with paramIndex`() {
        val expected = "address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = validRecipient(address0)
        assertEquals(expected, Render.parameter(recipient0, 1u))
    }

    @Test
    fun `Message parameter is rendered with no paramIndex`() {
        val expected = "message=Thank%20you%20for%20your%20purchase"
        assertEquals(expected, Render.parameterMessage(message = "Thank you for your purchase", index = null))
    }

    @Test
    fun `Message parameter is rendered with paramIndex`() {
        val expected = "message.10=Thank%20you%20for%20your%20purchase"
        assertEquals(expected, Render.parameterMessage(message = "Thank you for your purchase", index = 10u))
    }

    @Test
    fun `Label parameter is rendered with no paramIndex`() {
        val expected = "label=Lunch%20Tab"
        assertEquals(expected, Render.parameterLabel(label = "Lunch Tab", index = null))
    }

    @Test
    fun `Label parameter is rendered with paramIndex`() {
        val expected = "label.1=Lunch%20Tab"
        assertEquals(expected, Render.parameterLabel(label = "Lunch Tab", index = 1u))
    }

    @Test
    fun `required future parameter is rendered with no paramIndex`() {
        val expected = "req-futureParam=Future%20is%20Z"
        assertEquals(expected, Render.parameter(name = "req-futureParam", decodedValue = "Future is Z", index = null))
    }

    @Test
    fun `required future parameter is rendered with paramIndex`() {
        val expected = "req-futureParam.1=Future%20is%20Z"
        assertEquals(expected, Render.parameter(name = "req-futureParam", decodedValue = "Future is Z", index = 1u))
    }

    @Test
    fun `Memo parameter is rendered with no paramIndex`() {
        val expected = "memo=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉")
        assertEquals(expected, Render.parameter(memo, null))
    }

    @Test
    fun `Memo parameter is rendered with paramIndex`() {
        val expected = "memo.10=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉")
        assertEquals(expected, Render.parameter(memo, 10u))
    }

    // MARK: Payment

    @Test
    fun `Payment is rendered with no paramIndex`() {
        val expected = "address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = validRecipient(address0)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                amount = NonNegativeAmount.zec("123.456").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals(expected, Render.payment(payment0, null))
    }

    @Test
    fun `Payment is rendered with paramIndex`() {
        val expected =
            "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

        val recipient1 = validRecipient(address1)
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                amount = NonNegativeAmount.zec("0.789").getOrThrow(),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals(expected, Render.payment(payment1, 1u))
    }

    @Test
    fun `Payment renders with no paramIndex and no address label`() {
        val expected = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.456"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = validRecipient(address0)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                amount = NonNegativeAmount.zec("123.456").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals(expected, Render.payment(payment0, null, omittingAddressLabel = true))
    }

    @Test
    fun `Payment renderer ignores label omission when index is provided`() {
        val expected =
            "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

        val recipient1 = validRecipient(address1)
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                amount = NonNegativeAmount.zec("0.789").getOrThrow(),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        assertEquals(expected, Render.payment(payment1, 1u, omittingAddressLabel = true))
    }

    @Test
    fun `Payment request renderer enumerates all payments from 1 in normalization mode`() {
        val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"

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

        assertEquals(expected, Render.request(paymentRequest, ZIP321.FormattingOptions.EnumerateAllPayments))
    }

    @Test
    fun `Payment request renderer preserves stored indices with empty param index`() {
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

        assertEquals(
            expected,
            Render.request(paymentRequest, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = false)),
        )
    }

    // NOTE (K13): label omission only applies to a SINGLE payment at the empty paramindex
    // (matching the reference `to_uri`); a multi-payment request always renders every address
    // with an explicit `address[.n]=` label.
    @Test
    fun `Payment request renderer ignores address label omission for multi-payment requests`() {
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

        assertEquals(
            expected,
            Render.request(paymentRequest, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true)),
        )
    }

    // MARK: K13 — canonical renderer semantics

    @Test
    fun `parameterIndex renders empty for null and the empty paramindex`() {
        assertEquals("", Render.parameterIndex(null))
        assertEquals("", Render.parameterIndex(0u))
        assertEquals(".1", Render.parameterIndex(1u))
        assertEquals(".9999", Render.parameterIndex(9999u))
    }

    @Test
    fun `empty request renders as the bare scheme in either mode`() {
        val empty = PaymentRequest(emptyList())
        assertEquals("zcash:", Render.request(empty, ZIP321.FormattingOptions.UseEmptyParamIndex(true)))
        assertEquals("zcash:", Render.request(empty, ZIP321.FormattingOptions.UseEmptyParamIndex(false)))
        assertEquals("zcash:", Render.request(empty, ZIP321.FormattingOptions.EnumerateAllPayments))
    }

    @Test
    fun `otherparam with a value renders the equals separator and a value-less one does not`() {
        val withValue = OtherParam(name = "future-param", value = "hello world")
        val valueless = OtherParam(name = "flag", value = null)
        val emptyValue = OtherParam(name = "empty", value = "")

        assertEquals("future-param=hello%20world", Render.parameter(withValue, null))
        assertEquals("flag.2", Render.parameter(valueless, 2u))
        assertEquals("empty=", Render.parameter(emptyValue, null))
    }

    @Test
    fun `a lone payment at a non-zero index renders at its ACTUAL stored index`() {
        val expected =
            "zcash:?address.5=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.5=1"

        val payment =
            Payment(
                recipientAddress =
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )
        val request = PaymentRequest.fromIndexedPayments(listOf(IndexedPayment(5u, payment)))

        // The stored index is preserved even when label omission is requested (the payment is
        // not at the empty paramindex, so the leading-address form does not apply).
        assertEquals(expected, Render.request(request, ZIP321.FormattingOptions.UseEmptyParamIndex(true)))
        assertEquals(expected, Render.request(request, ZIP321.FormattingOptions.UseEmptyParamIndex(false)))
    }

    @Test
    fun `EnumerateAllPayments re-numbers stored indices sequentially from 1`() {
        val payment =
            Payment(
                recipientAddress =
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )
        val request = PaymentRequest.fromIndexedPayments(listOf(IndexedPayment(5u, payment)))

        assertEquals(
            "zcash:?address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1",
            Render.request(request, ZIP321.FormattingOptions.EnumerateAllPayments),
        )
    }
}
