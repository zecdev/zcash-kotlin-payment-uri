package org.zecdev.zip321.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An non-negative decimal ZEC amount represented as specified in ZIP-321.
 * Amount can be from 1 zatoshi (0.00000001) to the maxSupply of 21M ZEC (21_000_000)
 *
 * @property value The decimal value of the ZEC amount.
 */
class NonNegativeAmount {
    val value: BigDecimal

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
        this.value = value
    }

    @Throws(AmountError::class)
    constructor(decimalString: String) {
        val decimal = decimalFromString(decimalString)
        validateDecimal(decimal)
        this.value = decimal
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
        private const val maxFractionalDecimalDigits: Int = 8
        private val maxSupply: BigDecimal = BigDecimal("21000000")

        @Throws(AmountError::class)
        private fun validateDecimal(value: BigDecimal) {
            require(value > BigDecimal.ZERO) { throw AmountError.NegativeAmount }
            require(value <= maxSupply) { throw AmountError.GreaterThanSupply }
            requireFractionalDigits(value)
        }

        /**
         * Rounds the given `BigDecimal` value according to bankers rounding.
         *
         * @return The rounded value.
         */
        private fun BigDecimal.round(): BigDecimal {
            return this.setScale(maxFractionalDecimalDigits, RoundingMode.HALF_EVEN)
        }

        /**
         * Creates an Amount from a `BigDecimal` value.
         *
         * @param value The decimal representation of the desired amount.
         * @return A valid ZEC amount.
         * @throws AmountError if the provided value cannot represent or cannot be rounded to a
         * non-negative non-zero ZEC decimal amount.
         */
        @Throws(AmountError::class)
        fun create(value: BigDecimal): NonNegativeAmount {
            return NonNegativeAmount(value.round())
        }

        /**
         * Creates an Amount from a string representation.
         *
         * @param string String representation of the ZEC amount.
         * @return A valid ZEC amount.
         * @throws AmountError if the string cannot be parsed or if the parsed value violates ZEC
         * amount constraints.
         */
        @Throws(AmountError::class)
        fun createFromString(string: String): NonNegativeAmount {
            return create(decimalFromString(string))
        }

        @Throws(AmountError::class)
        fun decimalFromString(string: String): BigDecimal {
            try {
                val decimal = BigDecimal(string)

                requireFractionalDigits(decimal)

                return decimal
            } catch (e: NumberFormatException) {
                throw AmountError.InvalidTextInput
            }
        }

        private fun requireFractionalDigits(value: BigDecimal) {
            require(value.scale() <= maxFractionalDecimalDigits) {
                throw AmountError.TooManyFractionalDigits
            }
        }
    }

    /**
     * Converts the amount to a string representation.
     *
     * @return The string representation of the amount.
     */
    override fun toString(): String {
        return value.setScale(maxFractionalDecimalDigits, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString()
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
