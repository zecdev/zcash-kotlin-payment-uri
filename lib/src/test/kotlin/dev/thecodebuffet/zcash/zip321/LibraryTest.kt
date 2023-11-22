package dev.thecodebuffet.zcash.zip321
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe


class MyTests : ShouldSpec({
    should("return the length of the string") {
        "sammy".length shouldBe 5
        "".length shouldBe 0
    }
})