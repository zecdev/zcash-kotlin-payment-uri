package org.zecdev.zip321

import com.copperleaf.kudzu.parser.ParserException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.ZIP321.ParserResult.Request
import org.zecdev.zip321.ZIP321.ParserResult.SingleAddress
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParamNameString
import org.zecdev.zip321.parser.ParserContext

class ZIP321ParsingTests : FreeSpec({
    "ZIP321 Parsing Tests" - {
        "request(String, FormattingOptions) parses single address, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val url = "zcash:$address"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
        }

        "request(String, FormattingOptions) parses single address, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val url = "zcash:?address=$address"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
        }

        "request(String, FormattingOptions) parses single address, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val url = "zcash:?address.1=$address"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
        }

        "request(String, FormattingOptions) parses single payment with label but no amount, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val url = "zcash:$address?label=$label"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount shouldBe null
        }

        "request(String, FormattingOptions) parses single payment with label but no amount, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val url = "zcash:?address=$address&label=$label"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount shouldBe null
        }

        "request(String, FormattingOptions) parses single payment with label but no amount, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val label = "apple"
            val url = "zcash:?address.1=$address&label.1=$label"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount shouldBe null
        }

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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe label
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe null
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
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
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(key), value))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with empty param index and address label omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:$address?amount=$amount&$param"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(param), null))


        }

        "request(String, FormattingOptions) fails to parse payment URI with an amount param that has no value" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount&foo"

            shouldThrow<ZIP321.Errors.InvalidParamValue> { ZIP321.request(url, ParserContext.TESTNET) { _ -> true } }
        }

        "request(String, FormattingOptions) fails to parse payment URI with an address param that has no value" {
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:?address&amount=$amount&$param"

            shouldThrow<ZIP321.Errors.InvalidParamValue> { ZIP321.request(url, ParserContext.TESTNET) { _ -> true } }
        }

        "request(String, FormattingOptions) fails to parse payment URI with a memo param that has no value" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:$address?amount=$amount&$param&memo"

            shouldThrow<ZIP321.Errors.InvalidParamValue> { ZIP321.request(url, ParserContext.TESTNET) { _ -> true } }
        }

        "request(String, FormattingOptions) fails to parse payment URI with a label param that has no value" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:$address?amount=$amount&$param&label"

            shouldThrow<ZIP321.Errors.InvalidParamValue> { ZIP321.request(url, ParserContext.TESTNET) { _ -> true } }
        }

        "request(String, FormattingOptions) fails to parse payment URI with a message param that has no value" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:$address?amount=$amount&$param&message"

            shouldThrow<ZIP321.Errors.InvalidParamValue> { ZIP321.request(url, ParserContext.TESTNET) { _ -> true } }
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with empty param index and address label not omitted" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:?address=$address&amount=$amount&$param"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(param), null))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
        }

        "request(String, FormattingOptions) parses single payment with amount and unknown parameter with no value, with all payments enumerated" {
            val address = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val amount = "123.45"
            val param = "foo"
            val url = "zcash:?address.1=$address&amount.1=$amount&$param.1"

            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }

            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            paymentRequest.payments.size shouldBe 1
            val payment = paymentRequest.payments[0]
            payment.recipientAddress shouldBe RecipientAddress(address, ParserContext.TESTNET)
            payment.label shouldBe null
            payment.memo shouldBe null
            payment.message shouldBe null
            payment.otherParams shouldBe listOf(OtherParam(ParamNameString(param), null))
            payment.nonNegativeAmount?.toZecValueString() shouldBe amount
        }

        "request(String, FormattingOptions) fails to parse address with wrong characters" {
            val url =
                "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpUʔamount 1ꓸ234?message=Thanks%20for%20your%20payment%20for%20the%20correct%20&amount=20&Have=%20a%20nice%20day"

            shouldThrow<ZIP321.Errors.ParseError> {
                ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            }
        }

        "request(String, FormattingOptions) fails when empty string is provided" {
            shouldThrow<ZIP321.Errors.InvalidURI> {
                ZIP321.request("", ParserContext.TESTNET) { _ -> true }
            }
        }

        "request(String, FormattingOptions) fails when no URI Scheme string is detected" {
            shouldThrow<ZIP321.Errors.InvalidURI> {
                ZIP321.request("bitcoin:asdfasdfasdfasdfasdfasdfa", ParserContext.TESTNET) { _ -> true }
            }
        }
    }

})
