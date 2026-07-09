package org.zecdev.zip321

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.nulls.shouldBeNull
import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.extensions.qcharEncoded
import org.zecdev.zip321.parser.QcharString

class QcharStringTests : StringSpec({

    "QcharString is initialized from a valid raw string" {
        val string = "valid QcharString"
        val result = QcharString.from(string)
        result.shouldNotBeNull()
    }

    "QcharString fails to initialize from an already qchar-encoded string in strict mode" {
        val encodedString = "Thank%20You!"
        val result = QcharString.from(encodedString, true)
        result.shouldBeNull()
    }

    "QcharString fails to initialize from empty string" {
        val result = QcharString.from("")
        result.shouldBeNull()
    }

    "qcharDecode returns same string for input with no encodings" {
        val input = "nospecialcharacters"
        input.qcharDecode() shouldBe "nospecialcharacters"
    }

    "qcharEncode returns same string for input with no special characters" {
        val input = "nospecialcharacters"
        input.qcharEncoded() shouldBe "nospecialcharacters"
    }
})
