package dev.thecodebuffet.zcash.zip321

import Amount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class AmountTests : FreeSpec({
    "Amount tests" - {
        "testAmountStringDecimals" {
            Amount(BigDecimal("123.456")).toString() shouldBe "123.456"
            "${Amount(BigDecimal("123.456"))}" shouldBe "123.456"
        }

        "testAmountTrailing" {
            Amount(BigDecimal("50.000")).toString() shouldBe "50"
        }

        "testAmountLeadingZeros" {
            Amount(BigDecimal("0000.5")).toString() shouldBe "0.5"
        }

        "testAmountMaxDecimals" {
            Amount(BigDecimal("0.12345678")).toString() shouldBe "0.12345678"
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<Amount.AmountError> {
                Amount(BigDecimal("21000000.00000001")).toString()
            }
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<Amount.AmountError> {
                Amount(BigDecimal("-1")).toString()
            }
        }

        // Text Conversion Tests

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<Amount.AmountError> {
                Amount("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            Amount("0.12345678").toString() shouldBe Amount(BigDecimal("0.12345678")).toString()
        }

        "testAmountParsesMaxAmount" {
            Amount("21000000").toString() shouldBe Amount(BigDecimal("21000000")).toString()
        }
    }
})
