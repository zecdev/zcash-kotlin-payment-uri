// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321

import org.zecdev.zip321.ZIP321.FormattingOptions.EnumerateAllPayments
import org.zecdev.zip321.ZIP321.FormattingOptions.UseEmptyParamIndex
import org.zecdev.zip321.model.LegacyAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext
import kotlin.test.Test
import kotlin.test.assertEquals

class ZIP321EncodingTests {
    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with default formatting options`() {
        val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = LegacyAmount("123.45"),
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
                nonNegativeAmount = LegacyAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest))
    }

    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with empty param index and address label omitted`() {
        val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = LegacyAmount("123.45"),
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
                nonNegativeAmount = LegacyAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true)))
    }

    @Test
    fun `uriString PaymentRequest FormattingOptions encodes multiple payments with empty param index and address label not omitted`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

        val payment1 =
            Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = LegacyAmount("123.45"),
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
                nonNegativeAmount = LegacyAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null,
            )

        val paymentRequest = PaymentRequest(listOf(payment1, payment2))

        assertEquals(expected, ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false)))
    }

    @Test
    fun `request RecipientAddress encodes single address with default formatting options`() {
        val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET)

        assertEquals(expected, ZIP321.request(recipientAddress))
    }

    @Test
    fun `request RecipientAddress encodes single address with empty param index and address label not omitted`() {
        val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET)

        assertEquals(expected, ZIP321.request(recipientAddress, UseEmptyParamIndex(false)))
    }

    @Test
    fun `request RecipientAddress encodes single address with all payments enumerated`() {
        val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET)

        assertEquals(expected, ZIP321.request(recipientAddress, EnumerateAllPayments))
    }
}
