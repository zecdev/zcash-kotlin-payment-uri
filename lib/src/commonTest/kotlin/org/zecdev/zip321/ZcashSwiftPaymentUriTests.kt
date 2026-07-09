package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * NOTE (K1/v2): amounts previously built with `BigDecimal(...)`/`roundZec()`
 * (JVM-only setup sugar) are constructed via the equivalent common
 * `NonNegativeAmount.zec(String)` factory; the resulting zatoshi values and
 * every expected URI are unchanged.
 */
class ZcashSwiftPaymentUriTests {
    @Test
    fun `test that a single recipient payment request is generated`() {
        val expected =
            "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        assertEquals(expected, ZIP321.request(recipient))
    }

    @Test
    fun `test that a single payment request is generated`() {
        val expected =
            "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        val payment =
            Payment(
                recipientAddress = recipient,
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = MemoBytes("This is a simple memo."),
                label = null,
                message = "Thank you for your purchase",
                otherParams = emptyList(),
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

        val recipient0 = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                amount = NonNegativeAmount.zec("123.456").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        val recipient1 =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                amount = NonNegativeAmount.zec("0.789").getOrThrow(),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = emptyList(),
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

        val recipient0 = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        val payment0 =
            Payment(
                recipientAddress = recipient0,
                amount = NonNegativeAmount.zec("123.456").getOrThrow(),
                memo = null,
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        val recipient1 =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        val payment1 =
            Payment(
                recipientAddress = recipient1,
                amount = NonNegativeAmount.zec("0.789").getOrThrow(),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = emptyList(),
            )

        val paymentRequest = PaymentRequest(payments = listOf(payment0, payment1))

        val parsedRequest = ZIP321.parse(validURI, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()
        assertEquals(paymentRequest, parsedRequest)
    }
}
