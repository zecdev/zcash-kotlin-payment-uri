// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/*
 * JVM-only `java.math.BigDecimal` interop for [LegacyAmount].
 *
 * The KMP `commonMain` core stores amounts as checked `Long` zatoshis and has
 * no `BigDecimal` dependency. To keep existing JVM consumers (and the JVM test
 * suite) source-compatible, the historical `BigDecimal` constructor and helper
 * functions are re-provided here, byte-for-byte identical to the pre-KMP
 * implementation. `LegacyAmount(BigDecimal)` resolves to the pseudo-
 * constructor function below (Kotlin overload resolution picks it because the
 * class itself has no `BigDecimal` constructor).
 */

private const val MAX_FRACTIONAL_DECIMAL_DIGITS: Int = 8
private val maxZecSupply: BigDecimal = BigDecimal("21000000")
private const val ZATOSHI_PER_ZEC: Long = 100_000_000
private val bigDecimalMathContext = MathContext(MAX_FRACTIONAL_DECIMAL_DIGITS, RoundingMode.HALF_EVEN)

/**
 * Initializes an [LegacyAmount] from a `BigDecimal` number.
 *
 * `BigDecimal` values with more than 8 fractional digits are rejected with
 * [LegacyAmount.AmountError.TooManyFractionalDigits]; behavior is
 * identical to the pre-KMP `BigDecimal` constructor.
 */
@Throws(LegacyAmount.AmountError::class)
fun LegacyAmount(value: BigDecimal): LegacyAmount =
    LegacyAmount(LegacyAmount.zecToZatoshi(value))

/**
 * Convert a decimal amount of ZEC into Zatoshis.
 *
 * @throws LegacyAmount.AmountError if value has too much precision or does
 * not fit in a long
 */
@Throws(LegacyAmount.AmountError::class)
fun LegacyAmount.Companion.zecToZatoshi(coins: BigDecimal): Long {
    validateDecimal(coins)
    return try {
        coins.movePointRight(MAX_FRACTIONAL_DECIMAL_DIGITS).longValueExact()
    } catch (e: ArithmeticException) {
        throw LegacyAmount.AmountError.GreaterThanSupply
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
@Throws(LegacyAmount.AmountError::class)
fun LegacyAmount.Companion.zatoshiToZEC(zatoshis: Long): BigDecimal {
    return try {
        val zec = BigDecimal(zatoshis, bigDecimalMathContext)
            .movePointLeft(MAX_FRACTIONAL_DECIMAL_DIGITS)
        validateDecimal(zec)
        zec
    } catch (e: ArithmeticException) {
        throw LegacyAmount.AmountError.GreaterThanSupply
    }
}

@Throws(LegacyAmount.AmountError::class)
private fun LegacyAmount.Companion.validateDecimal(value: BigDecimal) {
    require(value >= BigDecimal.ZERO) { throw LegacyAmount.AmountError.NegativeAmount }
    require(value <= maxZecSupply) { throw LegacyAmount.AmountError.GreaterThanSupply }
    require(value.scale() <= MAX_FRACTIONAL_DECIMAL_DIGITS) {
        throw LegacyAmount.AmountError.TooManyFractionalDigits
    }
}

fun BigDecimal.roundZec(): BigDecimal {
    return this.setScale(MAX_FRACTIONAL_DECIMAL_DIGITS, RoundingMode.HALF_EVEN)
}
