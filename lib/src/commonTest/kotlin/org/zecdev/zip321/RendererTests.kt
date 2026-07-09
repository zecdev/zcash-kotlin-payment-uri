package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * NOTE (K1/v2): amounts previously built with `123.456.toBigDecimal()` (JVM
 * BigDecimal setup sugar) are constructed via the equivalent common
 * `NonNegativeAmount(String)` constructor; the resulting zatoshi values and
 * every expected rendered string are unchanged.
 */
class RendererTests {
    @Test
    fun `Amount parameter is rendered with no paramIndex`() {
        val expected = "amount=123.456"
        val nonNegativeAmount = NonNegativeAmount("123.456")
        assertEquals(expected, Render.parameter(nonNegativeAmount, null))
    }

    @Test
    fun `Amount parameter is rendered with paramIndex`() {
        val expected = "amount.1=123.456"
        val nonNegativeAmount = NonNegativeAmount("123.456")
        assertEquals(expected, Render.parameter(nonNegativeAmount, 1u))
    }

    @Test
    fun `Address parameter is rendered with no paramIndex`() {
        val expected = "address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
        assertEquals(expected, Render.parameter(recipient0, null))
    }

    @Test
    fun `Address parameter is rendered with paramIndex`() {
        val expected = "address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
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
        assertEquals(expected, Render.parameter(label = "req-futureParam", value = "Future is Z", index = null))
    }

    @Test
    fun `required future parameter is rendered with paramIndex`() {
        val expected = "req-futureParam.1=Future%20is%20Z"
        assertEquals(expected, Render.parameter(label = "req-futureParam", value = "Future is Z", index = 1u))
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

        val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount("123.456"),
                memo = null,
                label = null,
                message = null,
                otherParams = null,
            )

        assertEquals(expected, Render.payment(payment0, null))
    }

    @Test
    fun `Payment is rendered with paramIndex`() {
        val expected =
            "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

        val recipient1 = RecipientAddress(value = address1, ParserContext.TESTNET)
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount("0.789"),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null,
            )

        assertEquals(expected, Render.payment(payment1, 1u))
    }

    @Test
    fun `Payment renders with no paramIndex and no address label`() {
        val expected = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.456"
        val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

        val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount("123.456"),
                memo = null,
                label = null,
                message = null,
                otherParams = null,
            )

        assertEquals(expected, Render.payment(payment0, null, omittingAddressLabel = true))
    }

    @Test
    fun `Payment renderer ignores label omission when index is provided`() {
        val expected =
            "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

        val recipient1 = RecipientAddress(value = address1, ParserContext.TESTNET)
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount("0.789"),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null,
            )

        assertEquals(expected, Render.payment(payment1, 1u, omittingAddressLabel = true))
    }

    @Test
    fun `Payment request renderer increments index when start index is given`() {
        val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null,
            )

        val payment2 =
            Payment(
                recipientAddress =
                    RecipientAddress(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                        ParserContext.TESTNET,
                    ),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, Render.request(paymentRequest, 1u))
    }

    @Test
    fun `Payment request renderer increments index when start index is null`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null,
            )

        val payment2 =
            Payment(
                recipientAddress =
                    RecipientAddress(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                        ParserContext.TESTNET,
                    ),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, Render.request(paymentRequest, null))
    }

    @Test
    fun `Payment request renderer increments index when start index is null and address parameter is omitted`() {
        val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null,
            )

        val payment2 =
            Payment(
                recipientAddress =
                    RecipientAddress(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                        ParserContext.TESTNET,
                    ),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, Render.request(paymentRequest, null, true))
    }
}
