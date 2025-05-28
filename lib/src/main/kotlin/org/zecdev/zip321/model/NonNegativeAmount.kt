package org.zecdev.zip321.model


import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


private const val maxFractionalDecimalDigits: Int = 8

/**
 * An non-negative decimal ZEC amount represented as specified in ZIP-321.
 * Amount can be from 1 zatoshi (0.00000001) to the maxSupply of 21M ZEC (21_000_000)
 *
 * @property value The decimal value of the ZEC amount.
 */
class NonNegativeAmount {
    private val value: Long

    /**
     * Initializes a a NonNegativeAmount from a value expressed in Zatoshis.
     *
     * @param zatoshis Integer representation of the Zcash amount in Zatoshis
     * considering 100_000_000 zatoshis per ZEC
     */
    @Throws(AmountError::class)
    constructor(value: Long) {
        require(value >= 0) { throw  AmountError.NegativeAmount }
        require(value <= maxZatoshiSupply) { throw  AmountError.GreaterThanSupply}
        this.value = value
    }

    /**
     * Initializes an Amount from a `BigDecimal` number.
     *
     * @param value Decimal representation of the desired amount. Important: `BigDecimal` values
     * with more than 8 fractional digits will be rounded using bankers rounding.
     * @throws AmountError if the provided value can't represent or can't be rounded to a
     * non-negative non-zero ZEC decimal amount.
     */
    @Throws(AmountError::class)
    constructor(value: BigDecimal) {
        validateDecimal(value)
        this.value = zecToZatoshi(value)
    }



    @Throws(AmountError::class)
    constructor(decimalString: String) {
        this.value = zecToZatoshi(BigDecimal(decimalString))
    }

    /**
     * Enum representing errors that can occur during Amount operations.
     */
    sealed class AmountError(message: String) : Exception(message) {
        object NegativeAmount : AmountError("Amount cannot be negative") {
            private fun readResolve(): Any = NegativeAmount
        }

        object GreaterThanSupply : AmountError("Amount cannot be greater than the maximum supply") {
            private fun readResolve(): Any = GreaterThanSupply
        }

        object TooManyFractionalDigits : AmountError("Amount has too many fractional digits") {
            private fun readResolve(): Any = TooManyFractionalDigits
        }

        object InvalidTextInput : AmountError("Invalid text input for amount") {
            private fun readResolve(): Any = InvalidTextInput
        }
    }

    companion object {
        private val maxZecSupply: BigDecimal = BigDecimal("21000000")
        private val zatoshiPerZec: Long = 100_000_000
        private val maxZatoshiSupply: Long = 21000000 * zatoshiPerZec
        private val mathContext = MathContext(maxFractionalDecimalDigits, RoundingMode.HALF_EVEN)
        /**
         * Convert a decimal amount of ZEC into Zatoshis.
         *
         * @param coins number of coins
         * @return number of Zatoshis
         * @throws ArithmeticException if value has too much precision or will not fit in a long
         */
        @Throws(AmountError::class)
        fun zecToZatoshi(coins: BigDecimal): Long {
            validateDecimal(coins)
            try {
                return coins.movePointRight(maxFractionalDecimalDigits).longValueExact()
            } catch(e: ArithmeticException){
                throw AmountError.GreaterThanSupply
            }
        }

        /**
         * Convert a long amount of Zatoshis into ZEC.
         *
         * @param zatoshis number of zatoshis
         * @return number of ZEC in decimal
         * @throws AmountError if value has too much precision or will not fit in a long
         */
        @Throws(AmountError::class)
        fun zatoshiToZEC(zatoshis: Long): BigDecimal {
            try {
                val zec = BigDecimal(zatoshis, mathContext).movePointLeft(maxFractionalDecimalDigits)
                validateDecimal(zec)
                return zec
            } catch (e: ArithmeticException) {
                throw AmountError.GreaterThanSupply
            }
        }
        @Throws(AmountError::class)
        private fun validateDecimal(value: BigDecimal) {
            require(value >= BigDecimal.ZERO) { throw AmountError.NegativeAmount }
            require(value <= maxZecSupply) { throw AmountError.GreaterThanSupply }
            requireFractionalDigits(value)
        }

        private fun requireFractionalDigits(value: BigDecimal) {
            require(value.scale() <= maxFractionalDecimalDigits) {
                throw AmountError.TooManyFractionalDigits
            }
        }
    }

    @Throws(AmountError::class)
    fun toZecValueString():String {
        return zatoshiToZEC(value)
            .setScale(maxFractionalDecimalDigits, RoundingMode.HALF_EVEN)
            .stripTrailingZeros()
            .toPlainString()
    }

    /**
     * Converts the amount to a string representation.
     *
     * @return The string representation of the amount.
     */
    override fun toString(): String {
        return value.toString()
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NonNegativeAmount

        // NOTE: comparing with == operator provides false negatives.
        // apparently this is a JDK issue.
        return this.value.compareTo(other.value) == 0
    }

    override fun hashCode(): Int {
        return 31 * this.value.hashCode()
    }
}

fun BigDecimal.roundZec(): BigDecimal {
    return this.setScale(maxFractionalDecimalDigits, RoundingMode.HALF_EVEN)
}


