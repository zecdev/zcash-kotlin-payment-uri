package org.zecdev.zip321.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/*
 * JVM-only `java.math.BigDecimal` interop for [NonNegativeAmount].
 *
 * The KMP `commonMain` core stores amounts as checked `Long` zatoshis and has
 * no `BigDecimal` dependency. To keep existing JVM consumers (and the JVM test
 * suite) source-compatible, the historical `BigDecimal` constructor and helper
 * functions are re-provided here, byte-for-byte identical to the pre-KMP
 * implementation. `NonNegativeAmount(BigDecimal)` resolves to the pseudo-
 * constructor function below (Kotlin overload resolution picks it because the
 * class itself has no `BigDecimal` constructor).
 */

private const val MAX_FRACTIONAL_DECIMAL_DIGITS: Int = 8
private val maxZecSupply: BigDecimal = BigDecimal("21000000")
private const val ZATOSHI_PER_ZEC: Long = 100_000_000
private val bigDecimalMathContext = MathContext(MAX_FRACTIONAL_DECIMAL_DIGITS, RoundingMode.HALF_EVEN)

/**
 * Initializes an [NonNegativeAmount] from a `BigDecimal` number.
 *
 * `BigDecimal` values with more than 8 fractional digits are rejected with
 * [NonNegativeAmount.AmountError.TooManyFractionalDigits]; behavior is
 * identical to the pre-KMP `BigDecimal` constructor.
 */
@Throws(NonNegativeAmount.AmountError::class)
fun NonNegativeAmount(value: BigDecimal): NonNegativeAmount =
    NonNegativeAmount(NonNegativeAmount.zecToZatoshi(value))

/**
 * Convert a decimal amount of ZEC into Zatoshis.
 *
 * @throws NonNegativeAmount.AmountError if value has too much precision or does
 * not fit in a long
 */
@Throws(NonNegativeAmount.AmountError::class)
fun NonNegativeAmount.Companion.zecToZatoshi(coins: BigDecimal): Long {
    validateDecimal(coins)
    return try {
        coins.movePointRight(MAX_FRACTIONAL_DECIMAL_DIGITS).longValueExact()
    } catch (e: ArithmeticException) {
        throw NonNegativeAmount.AmountError.GreaterThanSupply
    }
}

/**
 * Convert a long amount of Zatoshis into ZEC.
 *
 * KNOWN v1 BUG, DELIBERATELY PRESERVED: `BigDecimal(zatoshis, MathContext(8,
 * HALF_EVEN))` rounds large zatoshi amounts to 8 significant digits, corrupting
 * the value. Kept for exact JVM API/behavior parity; the equivalent rendering
 * path in `commonMain` reproduces the same rounding.
 */
@Throws(NonNegativeAmount.AmountError::class)
fun NonNegativeAmount.Companion.zatoshiToZEC(zatoshis: Long): BigDecimal {
    return try {
        val zec = BigDecimal(zatoshis, bigDecimalMathContext)
            .movePointLeft(MAX_FRACTIONAL_DECIMAL_DIGITS)
        validateDecimal(zec)
        zec
    } catch (e: ArithmeticException) {
        throw NonNegativeAmount.AmountError.GreaterThanSupply
    }
}

@Throws(NonNegativeAmount.AmountError::class)
private fun NonNegativeAmount.Companion.validateDecimal(value: BigDecimal) {
    require(value >= BigDecimal.ZERO) { throw NonNegativeAmount.AmountError.NegativeAmount }
    require(value <= maxZecSupply) { throw NonNegativeAmount.AmountError.GreaterThanSupply }
    require(value.scale() <= MAX_FRACTIONAL_DECIMAL_DIGITS) {
        throw NonNegativeAmount.AmountError.TooManyFractionalDigits
    }
}

fun BigDecimal.roundZec(): BigDecimal {
    return this.setScale(MAX_FRACTIONAL_DECIMAL_DIGITS, RoundingMode.HALF_EVEN)
}
