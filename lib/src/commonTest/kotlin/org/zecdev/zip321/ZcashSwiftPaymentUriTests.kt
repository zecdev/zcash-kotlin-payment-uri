package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/*
 * NOTE (K1/v2): amounts previously built with `BigDecimal(...)`/`roundZec()`
 * (JVM-only setup sugar) are constructed via the equivalent common
 * `NonNegativeAmount(String)` constructor; the resulting zatoshi values and
 * every expected URI are unchanged.
 */
class ZcashSwiftPaymentUriTests {
    @Test
    fun `test that a single recipient payment request is generated`() {
        val expected =
            "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val recipient =
            RecipientAddress(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                ParserContext.TESTNET,
            )
        assertEquals(expected, ZIP321.request(recipient))
    }

    @Test
    fun `test that a single payment request is generated`() {
        val expected =
            "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            RecipientAddress(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                ParserContext.TESTNET,
            )
        val payment =
            Payment(
                recipientAddress = recipient,
                nonNegativeAmount = NonNegativeAmount("1"),
                memo = MemoBytes("This is a simple memo."),
                label = null,
                message = "Thank you for your purchase",
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(payments = listOf(payment))

        assertEquals(
            expected,
            ZIP321.uriString(
                paymentRequest,
                ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true),
            ),
        )

        assertEquals(
            expected,
            ZIP321.request(
                payment,
                ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true),
            ),
        )
    }

    @Test
    fun `test that multiple payments can be put in one request starting with no paramIndex`() {
        val expected =
            "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"

        val recipient0 = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount("123.456"),
                memo = null,
                label = null,
                message = null,
                otherParams = null,
            )

        val recipient1 =
            RecipientAddress(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                ParserContext.TESTNET,
            )
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount("0.789"),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(payments = listOf(payment0, payment1))

        assertEquals(
            expected,
            ZIP321.uriString(paymentRequest, ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = false)),
        )
    }

    @Test
    fun `test that multiple payments can be parsed`() {
        val validURI =
            "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"

        val recipient0 = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET)
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount("123.456"),
                memo = null,
                label = null,
                message = null,
                otherParams = null,
            )

        val recipient1 =
            RecipientAddress(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                ParserContext.TESTNET,
            )
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount("0.789"),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(payments = listOf(payment0, payment1))

        when (val parsedRequest = ZIP321.request(validURI, ParserContext.TESTNET, null)) {
            is ZIP321.ParserResult.SingleAddress -> fail("expected Request. got $parsedRequest")
            is ZIP321.ParserResult.Request -> {
                assertEquals(paymentRequest, parsedRequest.paymentRequest)
            }
        }
    }
}
