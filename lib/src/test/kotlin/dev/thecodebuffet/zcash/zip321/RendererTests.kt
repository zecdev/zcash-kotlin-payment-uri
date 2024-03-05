package dev.thecodebuffet.zcash.zip321

import MemoBytes
import NonNegativeAmount
import Payment
import RecipientAddress
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

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

            val recipient0 = RecipientAddress(value = address0)
            Render.parameter(recipient0, null) shouldBe expected
        }

        "Address parameter is rendered with `paramIndex`" {
            val expected = "address.1=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
            val address0 = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"

            val recipient0 = RecipientAddress(value = address0)
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

            val recipient0 = RecipientAddress(value = address0)
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

            val recipient1 = RecipientAddress(value = address1)
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

            val recipient0 = RecipientAddress(value = address0)
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

            val recipient1 = RecipientAddress(value = address1)
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
    }
})
