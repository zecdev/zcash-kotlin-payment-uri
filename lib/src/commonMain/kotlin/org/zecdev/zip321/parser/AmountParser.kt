package org.zecdev.zip321.parser

import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.model.NonNegativeAmount

/**
 * Parses a ZIP-321 `amountparam` value using the **strict** grammar
 * `amountparam = 1*DIGIT [ "." 1*8DIGIT ]`, delegating to [NonNegativeAmount.zec] (which
 * implements the grammar with checked integer arithmetic) and mapping its
 * [NonNegativeAmount.AmountException] onto the closest v1 [ZIP321.Errors] case.
 *
 * Unlike the deprecated `LegacyAmount(decimalString)` path this rejects a leading or trailing
 * decimal point (`".5"`, `"123."`), a sign, whitespace, scientific notation, and any `%` escape
 * (amount values are never percent-decoded), matching the reference `parse_amount`.
 */
object AmountParser {
    /**
     * Parses [string] into a [NonNegativeAmount].
     *
     * @param string the raw `amount` parameter value.
     * @param index the ZIP-321 payment index (0 meaning "no index"), used to tag the thrown error.
     * @throws ZIP321.Errors describing why the amount was rejected.
     */
    @Throws(ZIP321.Errors::class)
    fun parse(
        string: String,
        index: UInt,
    ): NonNegativeAmount =
        NonNegativeAmount.zec(string).getOrElse { error ->
            throw mapError(error, index)
        }

    /**
     * Maps a strict [NonNegativeAmount.AmountException] onto the closest v1 `amount` error,
     * preserving the mapping used for the deprecated `LegacyAmount` path:
     *   - [NonNegativeAmount.AmountException.ExceededSupply] (including `ULong`-overflowing
     *     strings) -> [ZIP321.Errors.AmountExceededSupply]
     *   - [NonNegativeAmount.AmountException.InvalidDecimalString] (grammar-shape failure: empty
     *     whole or fraction part, sign, stray characters) -> [ZIP321.Errors.InvalidParamValue]
     *     for `amount`
     *   - [NonNegativeAmount.AmountException.TooManyFractionalDigits] ->
     *     [ZIP321.Errors.AmountTooSmall] (v1 parity)
     *   - [NonNegativeAmount.AmountException.NegativeAmount] -> [ZIP321.Errors.AmountTooSmall]
     *     (v1 parity; not reachable, since the amount type is unsigned and the grammar has no
     *     sign, but mapped for totality)
     */
    private fun mapError(
        error: Throwable,
        index: UInt,
    ): ZIP321.Errors =
        when (error) {
            is NonNegativeAmount.AmountException.ExceededSupply -> ZIP321.Errors.AmountExceededSupply(index)
            is NonNegativeAmount.AmountException.TooManyFractionalDigits -> ZIP321.Errors.AmountTooSmall(index)
            is NonNegativeAmount.AmountException.NegativeAmount -> ZIP321.Errors.AmountTooSmall(index)
            else -> ZIP321.Errors.InvalidParamValue("amount", if (index == 0u) null else index)
        }
}
