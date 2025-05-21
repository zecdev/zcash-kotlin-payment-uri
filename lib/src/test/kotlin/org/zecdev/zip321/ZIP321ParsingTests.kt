package org.zecdev.zip321

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.ZIP321.ParserResult.Request
import org.zecdev.zip321.ZIP321.ParserResult.SingleAddress
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext

class ZIP321ParsingTests : FreeSpec({
    "ZIP321 Parsing Tests" - {
        "request(String, FormattingOptions) parses single address, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val url = "zcash:$address"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            recipientAddress shouldBe RecipientAddress(address)
        }

        // FIXME: Fails with spurious NegativeAmount error
//        "request(String, FormattingOptions) parses single address, with empty param index and address label not omitted" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val url = "zcash:?address=$address"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is SingleAddress) shouldBe true
//            val recipientAddress = (parserResult as SingleAddress).singleRecipient
//            recipientAddress shouldBe RecipientAddress(address)
//        }

        // FIXME: Fails with spurious NegativeAmount error
//        "request(String, FormattingOptions) parses single address, with all payments enumerated" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val url = "zcash:?address.1=$address"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is SingleAddress) shouldBe true
//            val recipientAddress = (parserResult as SingleAddress).singleRecipient
//            recipientAddress shouldBe RecipientAddress(address)
//        }

        // FIXME: Fails with spurious NegativeAmount error
//        "request(String, FormattingOptions) parses single payment with label but no amount, with empty param index and address label omitted" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val label = "apple"
//            val url = "zcash:$address?label=$label"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe label
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            payment.otherParams shouldBe null
//            // FIXME: payment.nonNegativeAmount shouldBe null
//        }

        // FIXME: Fails with spurious NegativeAmount error
//        "request(String, FormattingOptions) parses single payment with label but no amount, with empty param index and address label not omitted" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val label = "apple"
//            val url = "zcash:?address=$address&label=$label"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe label
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            payment.otherParams shouldBe null
//            // FIXME: payment.nonNegativeAmount shouldBe null
//        }

        // FIXME: Fails with spurious NegativeAmount error
//        "request(String, FormattingOptions) parses single payment with label but no amount, with all payments enumerated" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val label = "apple"
//            val url = "zcash:?address.1=$address&label.1=$label"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe label
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            payment.otherParams shouldBe null
//            // FIXME: payment.nonNegativeAmount shouldBe null
//        }

        "request(String, FormattingOptions) parses single payment with label and amount, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val amount = "123.45"
            val url = "zcash:$address?label=$label&amount=$amount"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with label and amount, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val amount = "123.45"
            val url = "zcash:?address=$address&label=$label&amount=$amount"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with label and amount, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val amount = "123.45"
            val url = "zcash:?address.1=$address&label.1=$label&amount.1=$amount"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = "bar"
            val url = "zcash:$address?amount=$amount&$key=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = "bar"
            val url = "zcash:?address=$address&amount=$amount&$key=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = "bar"
            val url = "zcash:?address.1=$address&amount.1=$amount&$key.1=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with empty value, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = ""
            val url = "zcash:$address?amount=$amount&$key=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with empty value, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = ""
            val url = "zcash:?address=$address&amount=$amount&$key=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with empty value, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val key = "foo"
            val value = ""
            val url = "zcash:?address.1=$address&amount.1=$amount&$key.1=$value"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(key, value))
            payment.nonNegativeAmount?.value.toString() shouldBe amount
        }

        // FIXME: Fails with ParserException
//        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with empty param index and address label omitted" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val amount = "123.45"
//            val param = "foo"
//            val url = "zcash:$address?amount=$amount&$param"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe null
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            // FIXME: payment.otherParams shouldBe listOf(OtherParam(param, null))
//            payment.nonNegativeAmount.value.toString() shouldBe amount
//        }

        // FIXME: Fails with ParserException
//        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with empty param index and address label not omitted" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val amount = "123.45"
//            val param = "foo"
//            val url = "zcash:?address=$address&amount=$amount&$param"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe null
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            // FIXME: payment.otherParams shouldBe listOf(OtherParam(param, null))
//            payment.nonNegativeAmount.value.toString() shouldBe amount
//        }

        // FIXME: Fails with ParserException
//        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with all payments enumerated" {
//            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
//            val amount = "123.45"
//            val param = "foo"
//            val url = "zcash:?address.1=$address&amount.1=$amount&$param.1"
//
//            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
//
//            (parserResult is Request) shouldBe true
//            val paymentRequest = (parserResult as Request).paymentRequest
//            paymentRequest.payments.size shouldBe 1
//            val payment = paymentRequest.payments[0]
//            payment.recipientAddress shouldBe RecipientAddress(address)
//            payment.label shouldBe null
//            payment.memo shouldBe null
//            payment.message shouldBe null
//            // FIXME: payment.otherParams shouldBe listOf(OtherParam(param, null))
//            payment.nonNegativeAmount.value.toString() shouldBe amount
//        }
    }
})
