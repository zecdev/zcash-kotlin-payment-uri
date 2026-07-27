package org.zecdev.zip321.model

import kotlin.jvm.JvmInline

/**
 * A non-negative integer count of zatoshi (1 ZEC == 100_000_000 zatoshi), the atomic unit in
 * which every ZEC amount is represented on the Zcash ledger.
 *
 * `NonNegativeAmount` is intentionally amount-agnostic: it only enforces the invariant that a
 * value is representable on-chain (`0..MAX_MONEY`). It has no opinion on whether zero is an
 * acceptable amount for a given payment — that policy (e.g. rejecting a zero-valued transparent
 * output) lives at the [Payment] level.
 *
 * Non-negativity is structural, not checked: the underlying representation is an **unsigned**
 * [ULong], mirroring the `u64`-backed `Zatoshis` type of the librustzcash `zip321` reference and
 * the `UInt64`-backed `NonNegativeAmount` of `zcash-swift-payment-uri`. A negative amount is
 * therefore not merely rejected, it is unrepresentable.
 *
 * ZEC decimal strings are parsed with the **strict** ZIP-321 `amountparam` grammar:
 * ```
 * amountparam = 1*DIGIT [ "." 1*8DIGIT ]
 * ```
 * i.e. the whole-number part must be present (leading zeros are permitted and ignored, e.g.
 * `"050"` is `50`), and if a decimal point is present it must be followed by 1 to 8 digits
 * (trailing zeros are permitted, e.g. `"00.500"` is `0.5`, but a bare trailing/leading point
 * such as `"123."` or `".5"` is rejected).
 *
 * Note: this type supersedes the v1 [LegacyAmount] (which carried the `NonNegativeAmount` name
 * before v2), whose parsing was lenient and whose rendering rounded to 8 significant digits.
 *
 * Java interop: because the backing type is `ULong`, the `value` accessor and the factories
 * carry Kotlin's unsigned-type name mangling on the JVM and are not intended to be called from
 * plain Java. See the CHANGELOG for the v2 note.
 *
 * @property value this amount, represented as an unsigned integer count of zatoshi.
 */
@JvmInline
value class NonNegativeAmount private constructor(val value: ULong) : Comparable<NonNegativeAmount> {
    /**
     * Errors produced by the [NonNegativeAmount.zatoshi] and [NonNegativeAmount.zec] factories.
     *
     * The taxonomy is kept aligned 1:1 with `zcash-swift-payment-uri`'s `AmountError`. Note that
     * [NegativeAmount] is **unreachable** from [NonNegativeAmount.zatoshi] — the parameter is
     * unsigned, so a negative zatoshi count cannot be expressed — and unreachable from
     * [NonNegativeAmount.zec] as well, because the strict grammar rejects a leading sign as
     * [InvalidDecimalString] before any sign interpretation happens. It is retained so the error
     * taxonomy of the string path stays complete and identical across the Swift and Kotlin
     * libraries.
     */
    sealed class AmountException(message: String) : Exception(message) {
        /** the provided value is negative; unreachable, see [AmountException]. */
        object NegativeAmount : AmountException("Amount cannot be negative")

        /** the provided value is greater than [NonNegativeAmount.MAX_MONEY]. */
        object ExceededSupply : AmountException("Amount cannot exceed the maximum supply")

        /** the decimal string has more than 8 digits after the decimal point. */
        object TooManyFractionalDigits : AmountException("Amount has too many fractional digits")

        /** the string does not conform to the strict ZIP-321 `amountparam` grammar. */
        object InvalidDecimalString : AmountException("Invalid ZIP-321 amount decimal string")
    }

    companion object {
        /** `MAX_MONEY`: the maximum number of zatoshi that can ever exist (21_000_000 ZEC). */
        const val MAX_MONEY: ULong = 2_100_000_000_000_000u

        /** number of zatoshi in 1 ZEC. */
        internal const val ZATOSHI_PER_ZEC: ULong = 100_000_000u

        /** the maximum number of digits allowed after the decimal point in a ZEC decimal string. */
        internal const val MAX_FRACTIONAL_DIGITS: Int = 8

        /**
         * Creates a [NonNegativeAmount] from a raw zatoshi count.
         * @param value an unsigned integer count of zatoshi.
         * @return success wrapping the [NonNegativeAmount] if `value` is in `0..MAX_MONEY`,
         * otherwise failure with [AmountException.ExceededSupply] — the only reachable failure,
         * since an unsigned count cannot be negative.
         *
         * Note: named after the zatoshi unit on purpose, mirroring the Swift reference factory
         * `NonNegativeAmount.zatoshi(_:)` so both libraries expose the same API.
         */
        fun zatoshi(value: ULong): Result<NonNegativeAmount> =
            if (value > MAX_MONEY) {
                Result.failure(AmountException.ExceededSupply)
            } else {
                Result.success(NonNegativeAmount(value))
            }

        /**
         * Creates a [NonNegativeAmount] by parsing a decimal ZEC amount string using the
         * **strict** ZIP-321 `amountparam` grammar: `1*DIGIT [ "." 1*8DIGIT ]`.
         * @param decimalString a plain (non-scientific), non-negative decimal ZEC string,
         * e.g. `"123.456"`, `"0.5"`, `"21000000"`.
         * @return success wrapping the parsed [NonNegativeAmount] or failure with the specific
         * [AmountException] describing why the string was rejected.
         *
         * Note: grammar-driven early exits, ported 1:1 from the Swift reference guard-style
         * parser; collapsing them into nested expressions would obscure the grammar.
         */
        @Suppress("ReturnCount")
        fun zec(decimalString: String): Result<NonNegativeAmount> {
            val length = decimalString.length
            var index = 0

            while (index < length && decimalString[index] in '0'..'9') {
                index++
            }
            if (index == 0) return Result.failure(AmountException.InvalidDecimalString)
            val wholeDigits = decimalString.substring(0, index)

            var fractionDigits = ""
            if (index < length && decimalString[index] == '.') {
                index++

                val fractionStart = index
                while (index < length && decimalString[index] in '0'..'9') {
                    index++
                }
                if (index == fractionStart) return Result.failure(AmountException.InvalidDecimalString)

                if (index - fractionStart > MAX_FRACTIONAL_DIGITS) {
                    return Result.failure(AmountException.TooManyFractionalDigits)
                }

                fractionDigits = decimalString.substring(fractionStart, index)
            }

            // any leftover input (extra characters, whitespace, a second '.', scientific
            // notation, a sign, etc.) makes the whole string invalid: the grammar requires
            // full consumption. A leading '-' or '+' never reaches a sign interpretation —
            // it fails the `1*DIGIT` prefix above and is reported as InvalidDecimalString.
            if (index != length) return Result.failure(AmountException.InvalidDecimalString)

            // the whole part having more digits than fit in a `ULong` (or otherwise
            // overflowing) is necessarily greater than `MAX_MONEY`.
            val whole =
                wholeDigits.toULongOrNull()
                    ?: return Result.failure(AmountException.ExceededSupply)

            // bounding `whole` here (rather than after scaling) guarantees the multiplication
            // below cannot overflow `ULong`.
            if (whole > MAX_MONEY / ZATOSHI_PER_ZEC) return Result.failure(AmountException.ExceededSupply)

            val paddedFraction = fractionDigits.padEnd(MAX_FRACTIONAL_DIGITS, '0')
            val fraction = paddedFraction.toULong()

            val total = whole * ZATOSHI_PER_ZEC + fraction

            if (total > MAX_MONEY) return Result.failure(AmountException.ExceededSupply)

            return Result.success(NonNegativeAmount(total))
        }
    }

    /**
     * Renders this amount as a plain decimal ZEC string, matching the reference `amount_str`
     * implementation (librustzcash `zip321`): the whole-number part is always present, the
     * fractional part is present only if nonzero, and trailing zeros in the fractional part
     * are trimmed.
     */
    fun decimalString(): String {
        val whole = value / ZATOSHI_PER_ZEC
        val fraction = value % ZATOSHI_PER_ZEC

        if (fraction == 0uL) return whole.toString()

        val fractionDigits =
            fraction.toString()
                .padStart(MAX_FRACTIONAL_DIGITS, '0')
                .trimEnd('0')

        return "$whole.$fractionDigits"
    }

    override fun compareTo(other: NonNegativeAmount): Int = value.compareTo(other.value)
}
