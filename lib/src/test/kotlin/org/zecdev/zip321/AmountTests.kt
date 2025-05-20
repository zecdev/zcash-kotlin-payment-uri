package org.zecdev.zip321

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.NonNegativeAmount
import java.math.BigDecimal

class AmountTests : FreeSpec({
    "Amount tests" - {

        // BigDecimal Conversion Tests: Constructor

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
// FIXME: this tests should not fail
//        "testAmountThrowsIfMaxDecimalsWithTrailingZeroes" {
//            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
//                NonNegativeAmount(BigDecimal("0.123456780")).toString()
//            }
//        }

        "testAmountThrowsIfTooManyDecimals" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount(BigDecimal("0.123456789")).toString()
            }
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("21000000.00000001")).toString()
            }
        }

        "testAmountNotThrowsIfZeroAmount" {
            NonNegativeAmount(BigDecimal("0")).toString() shouldBe "0"
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount(BigDecimal("-1")).toString()
            }
        }

        // BigDecimal Conversion Tests: Factory Method

        "testAmountStringDecimalsCreateMethod" {
            NonNegativeAmount.create(BigDecimal("123.456")).toString() shouldBe "123.456"
            "${NonNegativeAmount(BigDecimal("123.456"))}" shouldBe "123.456"
        }

        "testAmountTrailing" {
            NonNegativeAmount.create(BigDecimal("50.000")).toString() shouldBe "50"
        }

        "testAmountLeadingZeros" {
            NonNegativeAmount.create(BigDecimal("0000.5")).toString() shouldBe "0.5"
        }

        "testAmountMaxDecimals" {
            NonNegativeAmount.create(BigDecimal("0.12345678")).toString() shouldBe "0.12345678"
        }

        // FIXME: Fails because input is rounded
//        "testAmountThrowsIfMaxDecimalsWithTrailingZeroes" {
//            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
//                NonNegativeAmount.create(BigDecimal("0.123456780")).toString()
//            }
//        }

        // FIXME: Fails because input is rounded
//        "testAmountThrowsIfTooManyDecimals" {
//            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
//                NonNegativeAmount.create(BigDecimal("0.123456789")).toString()
//            }
//        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount.create(BigDecimal("21000000.00000001")).toString()
            }
        }

        "testAmountThrowsIfNegativeAmount" {
            shouldThrow<NonNegativeAmount.AmountError> {
                NonNegativeAmount.create(BigDecimal("-1")).toString()
            }
        }

        "testAmountDoesNotThrowIfZeroAmount" {
            shouldNotThrowAny {
                NonNegativeAmount.create(BigDecimal("0")).toString()
            }
        }

        // Text Conversion Tests: Constructor

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            NonNegativeAmount("0.12345678").toString() shouldBe NonNegativeAmount(BigDecimal("0.12345678")).toString()
        }

        "testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount("0.1234567890")
            }
        }

        "testAmountParsesMaxAmount" {
            NonNegativeAmount("21000000").toString() shouldBe NonNegativeAmount(BigDecimal("21000000")).toString()
        }

        "testAmountParsesMaxAmountWithTrailingZeroes" {
            NonNegativeAmount("21000000.00000000").toString() shouldBe NonNegativeAmount(BigDecimal("21000000")).toString()
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError.GreaterThanSupply> {
                NonNegativeAmount("21000000.00000001")
            }
        }

        // Text Conversion Tests: Factory Method

        "testAmountThrowsIfTooManyFractionalDigits" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount.createFromString("0.123456789")
            }
        }

        "testAmountParsesMaxFractionalDigits" {
            NonNegativeAmount.createFromString("0.12345678").toString() shouldBe NonNegativeAmount(BigDecimal("0.12345678")).toString()
        }

        "testAmountThrowsIfMaxFractionalDigitsWithTrailingZeroes" {
            shouldThrow<NonNegativeAmount.AmountError.TooManyFractionalDigits> {
                NonNegativeAmount.createFromString("0.1234567890")
            }
        }

        "testAmountParsesMaxAmount" {
            NonNegativeAmount.createFromString("21000000").toString() shouldBe NonNegativeAmount(BigDecimal("21000000")).toString()
        }

        "testAmountParsesMaxAmountWithTrailingZeroes" {
            NonNegativeAmount.createFromString("21000000.00000000").toString() shouldBe NonNegativeAmount(BigDecimal("21000000")).toString()
        }

        "testAmountThrowsIfMaxSupply" {
            shouldThrow<NonNegativeAmount.AmountError.GreaterThanSupply> {
                NonNegativeAmount.createFromString("21000000.00000001")
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
