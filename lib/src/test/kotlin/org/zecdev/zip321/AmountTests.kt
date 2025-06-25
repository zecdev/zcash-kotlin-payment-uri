package org.zecdev.zip321

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.NonNegativeAmount
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

class AmountTests : FreeSpec({
    "Amount tests" - {

        // BigDecimal Conversion Tests: Constructor

        "testAmountStringDecimals" {
            NonNegativeAmount(BigDecimal("123.456")).toZecValueString() shouldBe "123.456"
            NonNegativeAmount(BigDecimal("123.456")).toZecValueString() shouldBe "123.456"
        }

        "testAmountTrailing" {
            NonNegativeAmount(BigDecimal("50.000")).toZecValueString() shouldBe "50"
        }

        "testAmountLeadingZeros" {
            NonNegativeAmount(BigDecimal("0000.5")).toZecValueString() shouldBe "0.5"
        }

        "testAmountMaxDecimals" {
            NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString() shouldBe "0.12345678"
        }

        "testAmountThrowsIfMaxDecimalsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount(BigDecimal("0.123456780")).toZecValueString()
            }
        }

        "testAmountThrowsIfTooManyDecimals" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount(BigDecimal("0.123456789")).toZecValueString()
            }
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("21000000.00000001")).toZecValueString()
            }
        }

        "testAmountNotThrowsIfZeroAmount" {
            NonNegativeAmount(BigDecimal("0")).toZecValueString() shouldBe "0"
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("-1")).toZecValueString()
            }
        }

        // BigDecimal Conversion Tests: Factory Method

        "testAmountStringDecimalsCreateMethod" {
            NonNegativeAmount(BigDecimal("123.456")).toZecValueString() shouldBe "123.456"
        }

        "testAmountTrailing" {
            NonNegativeAmount(BigDecimal("50.000")).toZecValueString() shouldBe "50"
        }

        "testAmountLeadingZeros" {
            NonNegativeAmount(BigDecimal("0000.5")).toZecValueString() shouldBe "0.5"
        }

        "testAmountMaxDecimals" {
            NonNegativeAmount(BigDecimal("0.12345678")).toZecValueString() shouldBe "0.12345678"
        }

        // FIXME: Fails because input is rounded
        "testAmountThrowsIfMaxDecimalsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount(BigDecimal("0.123456780", MathContext(9, RoundingMode.HALF_EVEN))).toZecValueString()
            }
        }

        // FIXME: Fails because input is rounded
//        "testAmountThrowsIfTooManyDecimals" {
//            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
//                NonNegativeAmount(BigDecimal("0.123456789")).toZecValueString()
//            }
//        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("21000000.00000001")).toZecValueString()
            }
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("-1")).toZecValueString()
            }
        }

        "testAmountDoesNotThrowIfZeroAmount" {
            shouldNotThrowAny {
                NonNegativeAmount(BigDecimal("0")).toZecValueString()
            }
        }

        // Text Conversion Tests: Constructor

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            NonNegativeAmount("0.12345678").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("0.12345678")
            ).toZecValueString()
        }

        "testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.1234567890")
            }
        }

        "testAmountParsesMaxAmount" {
            NonNegativeAmount("21000000").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("21000000")
            ).toZecValueString()
        }

        "testAmountParsesMaxAmountWithTrailingZeroes" {
            NonNegativeAmount("21000000.00000000").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("21000000")
            ).toZecValueString()
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError.GreaterThanSupply> {
                NonNegativeAmount("21000000.00000001")
            }
        }

        // Text Conversion Tests: Factory Method

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            NonNegativeAmount("0.12345678").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("0.12345678")
            ).toZecValueString()
        }

        "testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.1234567890")
            }
        }

        "testAmountParsesMaxAmount" {
            NonNegativeAmount("21000000").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("21000000")
            ).toZecValueString()
        }

        "testAmountParsesMaxAmountWithTrailingZeroes" {
            NonNegativeAmount("21000000.00000000").toZecValueString() shouldBe NonNegativeAmount(
                BigDecimal("21000000")
            ).toZecValueString()
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError.GreaterThanSupply> {
                NonNegativeAmount("21000000.00000001")
            }
        }

        // Equality tests

        "testEquality" {
            NonNegativeAmount(BigDecimal("123.456789")).equals(NonNegativeAmount("123.456789")) shouldBe true
        }

        "testInEquality" {
            NonNegativeAmount(BigDecimal("123.456789")).equals(NonNegativeAmount("123.45678")) shouldBe false
        }
    }
})
