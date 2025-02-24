package org.zecdev.zip321

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.NonNegativeAmount
import java.math.BigDecimal

class AmountTests : FreeSpec({
    "Amount tests" - {
        "testAmountStringDecimals" {
            NonNegativeAmount(BigDecimal("123.456")).toString() shouldBe "123.456"
            "${NonNegativeAmount(BigDecimal("123.456"))}" shouldBe "123.456"
        }

        "testAmountTrailing" {
            NonNegativeAmount(BigDecimal("50.000")).toString() shouldBe "50"
        }

        "testAmountLeadingZeros" {
            NonNegativeAmount(BigDecimal("0000.5")).toString() shouldBe "0.5"
        }

        "testAmountMaxDecimals" {
            NonNegativeAmount(BigDecimal("0.12345678")).toString() shouldBe "0.12345678"
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("21000000.00000001")).toString()
            }
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("-1")).toString()
            }
        }

        // Text Conversion Tests

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            NonNegativeAmount("0.12345678").toString() shouldBe NonNegativeAmount(BigDecimal("0.12345678")).toString()
        }

        "testAmountParsesMaxAmount" {
            NonNegativeAmount("21000000").toString() shouldBe NonNegativeAmount(BigDecimal("21000000")).toString()
        }
    }
})
