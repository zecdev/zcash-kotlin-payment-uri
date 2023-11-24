package dev.thecodebuffet.zcash.zip321

import dev.thecodebuffet.zcash.zip321.extensions.qcharEncoded
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class EncodingTests : FunSpec({

    test("qcharEncoded string contains allowed characters only") {
        val message = "sk8:forever@!"
        message.qcharEncoded() shouldBe message
    }

    test("qcharEncoded string has percent-encoded disallowed characters") {
        mapOf(
            "Thank you for your purchase" to "Thank%20you%20for%20your%20purchase",
            "Use Coupon [ZEC4LIFE] to get a 20% discount on your next purchase!!" to
                "Use%20Coupon%20%5BZEC4LIFE%5D%20to%20get%20a%2020%25%20discount%20on%20your%20next%20purchase!!",
            "Order #321" to "Order%20%23321",
            "Your Ben & Jerry's Order" to "Your%20Ben%20%26%20Jerry's%20Order",
            " " to "%20",
            "\"" to "%22",
            "#" to "%23",
            "%" to "%25",
            "&" to "%26",
            "/" to "%2F",
            "<" to "%3C",
            "=" to "%3D",
            ">" to "%3E",
            "?" to "%3F",
            "[" to "%5B",
            "\\" to "%5C",
            "]" to "%5D",
            "^" to "%5E",
            "`" to "%60",
            "{" to "%7B",
            "|" to "%7C",
            "}" to "%7D"
        ).forEach { (input, expected) ->
            input.qcharEncoded() shouldBe expected
        }
    }

    test("unallowed characters are escaped") {
        val unallowedCharacters = listOf(
            " ", "\"", "#", "%", "&", "/", "<", "=", ">", "?", "[", "\\", "]", "^", "`", "{", "|", "}"
        )

        unallowedCharacters.forEach { unallowed ->
            val qcharEncoded = unallowed.qcharEncoded()
            qcharEncoded should {
                it != null && it.contains("%")
            }
        }

        (0x00..0x1F).map { it.toChar().toString() }.forEach { controlChar ->
            val qcharEncoded = controlChar.qcharEncoded()
            qcharEncoded should {
                it != null && it.contains("%")
            }
        }
    }
})
