package org.zecdev.zip321

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.ZIP321.FormattingOptions.EnumerateAllPayments
import org.zecdev.zip321.ZIP321.FormattingOptions.UseEmptyParamIndex
import org.zecdev.zip321.ZIP321.ParserResult.SingleAddress
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress

class ZIP321EncodingTests : FreeSpec({
    "ZIP321 Encoding Tests" - {
        "uriString(PaymentRequest, FormattingOptions) encodes multiple payments, with default formatting options" {
            val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            ZIP321.uriString(paymentRequest) shouldBe expected
        }

        "uriString(PaymentRequest, FormattingOptions) encodes multiple payments, with empty param index and address label omitted" {
            val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true)) shouldBe expected
        }

        "uriString(PaymentRequest, FormattingOptions) encodes multiple payments, with empty param index and address label not omitted" {
            val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false)) shouldBe expected
        }

        "request(RecipientAddress) encodes single address, with default formatting options" {
            val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

            ZIP321.request(recipientAddress) shouldBe expected
        }

        "request(RecipientAddress) encodes single address, with empty param index and address label not omitted" {
            val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

            ZIP321.request(recipientAddress, UseEmptyParamIndex(false)) shouldBe expected
        }

        "request(RecipientAddress) encodes single address, with all payments enumerated" {
            val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

            ZIP321.request(recipientAddress, EnumerateAllPayments) shouldBe expected
        }
    }
})
