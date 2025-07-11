package org.zecdev.zip321

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.ZIP321.FormattingOptions.UseEmptyParamIndex
import org.zecdev.zip321.ZIP321.ParserResult.Request
import org.zecdev.zip321.ZIP321.ParserResult.SingleAddress
import org.zecdev.zip321.parser.ParserContext
import org.zecdev.zip321.ZIP321.FormattingOptions.EnumerateAllPayments

class RoundTripTests : FreeSpec({
    "Round-Trip Tests" - {
        "Round-trip parsing and encoding via request() of single address, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            val roundTrip = ZIP321.request(recipientAddress, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via request() of single address, with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            val roundTrip = ZIP321.request(recipientAddress, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via request() of single address, with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is SingleAddress) shouldBe true
            val recipientAddress = (parserResult as SingleAddress).singleRecipient
            val roundTrip = ZIP321.request(recipientAddress, EnumerateAllPayments)
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label and zero amount, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=0&label=rehearsal"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label and amount, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label and amount, with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label and amount, with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with amount and label containing delimiter, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple+banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with amount and label containing delimiter, with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple+banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with amount and label containing delimiter, with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple+banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
            roundTrip shouldBe url
        }


        "Round-trip parsing and encoding via uriString() of single payment with label but no amount, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?label=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label but no amount, with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&label=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with label but no amount, with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&label.1=apple"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
            roundTrip shouldBe url
        }


        "Round-trip parsing and encoding via uriString() of single payment with amount and unknown parameter, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&foo=bar"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with amount and many 'other' parameters, with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&foo=bar&bar=foo"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding via uriString() of single payment with amount and unknown parameter, with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&foo=bar"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

//
        "Round-trip parsing and encoding via uriString() of single payment with amount and unknown parameter, with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&foo.1=bar"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding of multiple payments with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding of multiple payments with empty param index and address label omitted" {
            val url = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?foo=bar&bar=foo&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(true))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding of multiple payments with empty param index and address label not omitted" {
            val url = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, UseEmptyParamIndex(false))
            roundTrip shouldBe url
        }

        "Round-trip parsing and encoding of multiple payments with all payments enumerated" {
            val url = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"
            val parserResult = ZIP321.request(url, ParserContext.TESTNET) { _ -> true }
            (parserResult is Request) shouldBe true
            val paymentRequest = (parserResult as Request).paymentRequest
            val roundTrip = ZIP321.uriString(paymentRequest, EnumerateAllPayments)
            roundTrip shouldBe url
        }
    }
})
