package org.zecdev.zip321

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext

class RendererTests : FreeSpec({
    "Amount Tests" - {
        "Amount parameter is rendered with no `paramIndex`" {
            val expected = "amount=123.456"
            val nonNegativeAmount = NonNegativeAmount(123.456.toBigDecimal())
            Render.parameter(nonNegativeAmount, null) shouldBe expected
        }

        "Amount parameter is rendered with `paramIndex`" {
            val expected = "amount.1=123.456"
            val nonNegativeAmount = NonNegativeAmount(123.456.toBigDecimal())
            Render.parameter(nonNegativeAmount, 1u) shouldBe expected
        }

        "Address parameter is rendered with no `paramIndex`" {
            val expected = "address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

            val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
            Render.parameter(recipient0, null) shouldBe expected
        }

        "Address parameter is rendered with `paramIndex`" {
            val expected = "address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

            val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
            Render.parameter(recipient0, 1u) shouldBe expected
        }

        "Message parameter is rendered with no `paramIndex`" {
            val expected = "message=Thank%20you%20for%20your%20purchase"
            Render.parameterMessage(message = "Thank you for your purchase", index = null) shouldBe expected
        }

        "Message parameter is rendered with `paramIndex`" {
            val expected = "message.10=Thank%20you%20for%20your%20purchase"
            Render.parameterMessage(message = "Thank you for your purchase", index = 10u) shouldBe expected
        }

        "Label parameter is rendered with no `paramIndex`" {
            val expected = "label=Lunch%20Tab"
            Render.parameterLabel(label = "Lunch Tab", index = null) shouldBe expected
        }

        "Label parameter is rendered with `paramIndex`" {
            val expected = "label.1=Lunch%20Tab"
            Render.parameterLabel(label = "Lunch Tab", index = 1u) shouldBe expected
        }

        "required future parameter is rendered with no `paramIndex`" {
            val expected = "req-futureParam=Future%20is%20Z"
            Render.parameter(label = "req-futureParam", value = "Future is Z", index = null) shouldBe expected
        }

        "required future parameter is rendered with `paramIndex" {
            val expected = "req-futureParam.1=Future%20is%20Z"
            Render.parameter(label = "req-futureParam", value = "Future is Z", index = 1u) shouldBe expected
        }

        "Memo parameter is rendered with no `paramIndex`" {
            val expected = "memo=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
            val memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉")
            Render.parameter(memo, null) shouldBe expected
        }

        "Memo parameter is rendered with `paramIndex`" {
            val expected = "memo.10=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
            val memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉")
            Render.parameter(memo, 10u) shouldBe expected
        }

        // MARK: Payment

        "Payment is rendered with no `paramIndex`" {
            val expected = "address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.456"
            val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

            val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
            val payment0 = Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount(123.456.toBigDecimal()),
                memo = null,
                label = null,
                message = null,
                otherParams = null
            )

            Render.payment(payment0, null) shouldBe expected
        }

        "Payment is rendered with `paramIndex`" {
            val expected =
                "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
            val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

            val recipient1 = RecipientAddress(value = address1, ParserContext.TESTNET)
            val payment1 = Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount(0.789.toBigDecimal()),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null
            )

            Render.payment(payment1, 1u) shouldBe expected
        }

        "Payment renders with no `paramIndex` and no address label" {
            val expected = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.456"
            val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

            val recipient0 = RecipientAddress(value = address0, ParserContext.TESTNET)
            val payment0 = Payment(
                recipientAddress = recipient0,
                nonNegativeAmount = NonNegativeAmount(123.456.toBigDecimal()),
                memo = null,
                label = null,
                message = null,
                otherParams = null
            )

            Render.payment(payment0, null, omittingAddressLabel = true) shouldBe expected
        }

        "Payment renderer ignores label omission when index is provided" {
            val expected =
                "address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=0.789&memo.1=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
            val address1 = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

            val recipient1 = RecipientAddress(value = address1, ParserContext.TESTNET)
            val payment1 = Payment(
                recipientAddress = recipient1,
                nonNegativeAmount = NonNegativeAmount(0.789.toBigDecimal()),
                memo = MemoBytes("This is a unicode memo ✨🦄🏆🎉"),
                label = null,
                message = null,
                otherParams = null
            )

            Render.payment(payment1, 1u, omittingAddressLabel = true) shouldBe expected
        }

        "Payment request renderer increments index when start index is given" {
            val expected = "zcash:?address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount.1=123.45&label.1=apple&address.2=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.2=1.2345&label.2=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            Render.request(paymentRequest, 1u) shouldBe expected
        }

        "Payment request renderer increments index when start index is null" {
            val expected = "zcash:?address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU&amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            Render.request(paymentRequest, null) shouldBe expected
        }

        "Payment request renderer increments index when start index is null and address parameter is omitted" {
            val expected = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU?amount=123.45&label=apple&address.1=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount.1=1.2345&label.1=banana"

            val payment1 = Payment(
                recipientAddress = RecipientAddress("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("123.45"),
                memo = null,
                label = "apple",
                message = null,
                otherParams = null
            )

            val payment2 = Payment(
                recipientAddress = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET),
                nonNegativeAmount = NonNegativeAmount("1.2345"),
                memo = null,
                label = "banana",
                message = null,
                otherParams = null
            )

            val paymentRequest = PaymentRequest(listOf(payment1, payment2))

            Render.request(paymentRequest, null, true) shouldBe expected
        }
    }
})
