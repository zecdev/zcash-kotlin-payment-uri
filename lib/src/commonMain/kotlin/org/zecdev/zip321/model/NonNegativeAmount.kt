package org.zecdev.zip321.model

private const val MAX_FRACTIONAL_DECIMAL_DIGITS: Int = 8
private const val ZATOSHI_PER_ZEC: Long = 100_000_000
private const val MAX_ZEC_SUPPLY_WHOLE: Long = 21_000_000
private const val MAX_ZATOSHI_SUPPLY: Long = MAX_ZEC_SUPPLY_WHOLE * ZATOSHI_PER_ZEC
private const val SIGNIFICANT_DIGITS: Int = 8

/**
 * A non-negative decimal ZEC amount represented as specified in ZIP-321.
 * Amount can be from 1 zatoshi (0.00000001) to the maxSupply of 21M ZEC (21_000_000).
 *
 * Internally the amount is stored as a checked [Long] number of zatoshis
 * (fixed-point, 8 fractional decimal digits), so this type is pure Kotlin and
 * carries no dependency on `java.math.BigDecimal`. A `BigDecimal`-based
 * constructor and helpers remain available to JVM consumers as extensions
 * (see the `jvmMain` source set).
 *
 * @property value The value of the ZEC amount in zatoshis.
 */
class NonNegativeAmount {
    internal val value: Long

    /**
     * Initializes a NonNegativeAmount from a value expressed in Zatoshis.
     *
     * @param value Integer representation of the Zcash amount in Zatoshis
     * considering 100_000_000 zatoshis per ZEC
     */
    @Throws(AmountError::class)
    constructor(value: Long) {
        require(value >= 0) { throw AmountError.NegativeAmount }
        require(value <= MAX_ZATOSHI_SUPPLY) { throw AmountError.GreaterThanSupply }
        this.value = value
    }

    /**
     * Initializes an Amount from a decimal `String` such as "1.2345".
     *
     * v1 behavior is preserved exactly: leading zeros in the whole part and
     * trailing zeros in the fractional part are ignored, a bare trailing
     * decimal point ("123.") and a missing integer part (".5") are accepted,
     * and a value with more than 8 fractional digits is rejected with
     * [AmountError.TooManyFractionalDigits]. Malformed input throws a
     * [NumberFormatException] (an [IllegalArgumentException]), matching the
     * exception the previous `java.math.BigDecimal(String)` constructor raised.
     */
    @Throws(AmountError::class)
    constructor(decimalString: String) {
        this.value = parseDecimalStringToZatoshi(decimalString)
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
        internal const val zatoshiPerZec: Long = ZATOSHI_PER_ZEC
        internal const val maxZatoshiSupply: Long = MAX_ZATOSHI_SUPPLY
        internal const val maxFractionalDecimalDigits: Int = MAX_FRACTIONAL_DECIMAL_DIGITS

        /**
         * Parses a decimal ZEC string into a checked number of zatoshis,
         * preserving the exact accept/reject semantics of the previous
         * `zecToZatoshi(BigDecimal(decimalString))` implementation.
         *
         * The validation order matches `BigDecimal`-era `validateDecimal`:
         * negative first, then greater-than-supply, then too-many-fractional
         * digits.
         */
        @Throws(AmountError::class)
        internal fun parseDecimalStringToZatoshi(decimalString: String): Long {
            val (negative, wholePart, fracPart) = lexDecimal(decimalString)

            val magnitudeIsZero = wholePart.all { it == '0' } && fracPart.all { it == '0' }

            // (1) negative
            if (negative && !magnitudeIsZero) throw AmountError.NegativeAmount
            // (2) greater than supply
            if (exceedsMaxSupply(wholePart, fracPart)) throw AmountError.GreaterThanSupply
            // (3) too many fractional digits
            if (fracPart.length > MAX_FRACTIONAL_DECIMAL_DIGITS) throw AmountError.TooManyFractionalDigits

            val wholeTrimmed = wholePart.trimStart('0').ifEmpty { "0" }
            val whole = wholeTrimmed.toLong()
            val fracPadded = (fracPart + "00000000").substring(0, MAX_FRACTIONAL_DECIMAL_DIGITS)
            return whole * ZATOSHI_PER_ZEC + fracPadded.toLong()
        }

        /**
         * Splits a decimal string into (negative, wholeDigits, fracDigits),
         * throwing [NumberFormatException] for malformed input exactly where
         * `java.math.BigDecimal(String)` would have thrown.
         *
         * Only plain decimal notation (optional sign, integer part, fraction)
         * is accepted. Scientific/exponent notation is intentionally NOT
         * supported here (it is unreachable from the ZIP-321 amount grammar and
         * untested); such input throws, whereas the old BigDecimal path would
         * have accepted it. See CHANGELOG.
         */
        private fun lexDecimal(s: String): Triple<Boolean, String, String> {
            if (s.isEmpty()) throw NumberFormatException("empty amount string")
            var i = 0
            var negative = false
            when (s[0]) {
                '+' -> i = 1
                '-' -> {
                    negative = true
                    i = 1
                }
            }
            val body = s.substring(i)
            if (body.isEmpty()) throw NumberFormatException("no digits in amount '$s'")

            val dot = body.indexOf('.')
            val wholePart: String
            val fracPart: String
            if (dot < 0) {
                wholePart = body
                fracPart = ""
            } else {
                wholePart = body.substring(0, dot)
                val rest = body.substring(dot + 1)
                if (rest.indexOf('.') >= 0) throw NumberFormatException("multiple decimal points in '$s'")
                fracPart = rest
            }
            // BigDecimal requires at least one digit overall ("." alone is invalid).
            if (wholePart.isEmpty() && fracPart.isEmpty()) throw NumberFormatException("no digits in '$s'")
            if (wholePart.any { !it.isDecimalDigit() }) throw NumberFormatException("invalid character in '$s'")
            if (fracPart.any { !it.isDecimalDigit() }) throw NumberFormatException("invalid character in '$s'")
            return Triple(negative, wholePart, fracPart)
        }

        private fun exceedsMaxSupply(wholePart: String, fracPart: String): Boolean {
            val w = wholePart.trimStart('0')
            if (w.length > MAX_SUPPLY_WHOLE_DIGITS) return true
            if (w.length == MAX_SUPPLY_WHOLE_DIGITS) {
                val cmp = w.compareTo(MAX_SUPPLY_WHOLE_STR)
                if (cmp > 0) return true
                if (cmp == 0) return fracPart.any { it != '0' }
            }
            return false
        }

        /**
         * Rounds a zatoshi amount to [SIGNIFICANT_DIGITS] significant decimal
         * digits with HALF_EVEN rounding.
         *
         * KNOWN v1 BUG, DELIBERATELY PRESERVED: this reproduces
         * `BigDecimal(zatoshis, MathContext(8, HALF_EVEN))` from the old
         * `zatoshiToZEC`, which rounds large zatoshi values to 8 significant
         * digits and thereby corrupts amounts on render (e.g.
         * 20999999.99999999 ZEC -> 21000000, 3768769.02796286 ZEC -> 3768769).
         * The conformance `renderMismatch` xfail entries depend on this exact
         * behavior; it is fixed in a later PR.
         */
        private fun roundToSignificantDigits(v: Long): Long {
            if (v == 0L) return 0L
            val digits = v.toString().length
            if (digits <= SIGNIFICANT_DIGITS) return v
            val drop = digits - SIGNIFICANT_DIGITS
            var divisor = 1L
            repeat(drop) { divisor *= 10 }
            val q = v / divisor
            val r = v % divisor
            val half = divisor / 2
            val roundedQ = when {
                r > half -> q + 1
                r < half -> q
                else -> if (q % 2L == 0L) q else q + 1
            }
            return roundedQ * divisor
        }

        internal fun renderZatoshiAsZec(v: Long): String {
            val rounded = roundToSignificantDigits(v)
            val whole = rounded / ZATOSHI_PER_ZEC
            val frac = rounded % ZATOSHI_PER_ZEC
            if (frac == 0L) return whole.toString()
            val fracStr = frac.toString().padStart(MAX_FRACTIONAL_DECIMAL_DIGITS, '0').trimEnd('0')
            return "$whole.$fracStr"
        }

        private const val MAX_SUPPLY_WHOLE_STR: String = "21000000"
        private const val MAX_SUPPLY_WHOLE_DIGITS: Int = 8
    }

    /**
     * Renders the amount as a canonical ZEC decimal string (e.g. "1.2345").
     *
     * NOTE: preserves the v1 8-significant-digit rounding bug for large amounts
     * (see [Companion.renderZatoshiAsZec]).
     */
    @Throws(AmountError::class)
    fun toZecValueString(): String = renderZatoshiAsZec(value)

    /**
     * Converts the amount to a string representation (the zatoshi value).
     */
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NonNegativeAmount

        // NOTE: comparing with == operator provides false negatives.
        // apparently this is a JDK issue.
        return this.value.compareTo(other.value) == 0
    }

    override fun hashCode(): Int {
        return 31 * this.value.hashCode()
    }
}

private fun Char.isDecimalDigit(): Boolean = this in '0'..'9'
