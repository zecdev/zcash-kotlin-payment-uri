package org.zecdev.zip321

/**
 * The public, sealed v2 error taxonomy for [ZIP-321](https://zips.z.cash/zip-0321) parsing. It
 * mirrors the cross-language conformance-corpus error discriminants rather than the internal v1
 * [ZIP321.Errors] grab-bag.
 *
 * DATA-LEAKAGE POLICY (enforced by construction): error payloads carry ONLY parameter names,
 * indices, counts, or fixed [StaticReason] values — never addresses, memo contents, amounts, or raw
 * URI slices. The single bounded exception is [InvalidParamIndex]'s raw token, which the grammar
 * caps at a handful of characters (`<= 5` digits) by construction. [StaticReason] is a closed enum
 * precisely so that raw input can never be smuggled into an error value.
 */
sealed class ZIP321Error : Exception() {
    /** A `memo` value was not valid unpadded base64url. */
    data class InvalidBase64(val index: UInt?) : ZIP321Error()

    /** A decoded memo exceeded 512 bytes or failed a required UTF-8 check. */
    data class MemoBytesError(val index: UInt?) : ZIP321Error()

    /**
     * A `memo` was supplied for a payment whose recipient cannot receive memos (a transparent
     * address).
     */
    data class TransparentMemo(val index: UInt?) : ZIP321Error()

    /**
     * A zero-valued `amount` was requested for a transparent recipient, which is disallowed by
     * consensus.
     */
    data class ZeroValuedTransparentOutput(val index: UInt?) : ZIP321Error()

    /** The request specified more payments than the format allows (> 9999). */
    data class TooManyPayments(val count: UInt) : ZIP321Error()

    /** The same parameter name occurred more than once at the same `paramindex`. */
    data class DuplicateParameter(val name: String, val index: UInt?) : ZIP321Error()

    /** A `paramindex` carried non-address parameters but no matching address. */
    data class RecipientMissing(val index: UInt?) : ZIP321Error()

    /**
     * An address string was not a valid encoding of a supported address type (bad checksum,
     * mixed-case bech32, wrong network, Sprout, ...).
     */
    data class InvalidAddress(val index: UInt?) : ZIP321Error()

    /** A `req-`-prefixed parameter the parser does not recognize was present. */
    data class UnknownRequiredParameter(val name: String) : ZIP321Error()

    /**
     * A `paramindex` was malformed (leading zero, or out of range). The raw token is bounded to a
     * handful of characters by the grammar.
     */
    data class InvalidParamIndex(val raw: String) : ZIP321Error()

    /** An `amount` parsed to a numeric value that exceeds `MAX_MONEY`. */
    data class AmountExceededSupply(val index: UInt?) : ZIP321Error()

    /**
     * An `amount` value was malformed or otherwise not a legal amount (negative, missing
     * whole/fractional part, arithmetic overflow).
     */
    data class AmountInvalid(val index: UInt?) : ZIP321Error()

    /** The URI violated the top-level ZIP-321 grammar itself. */
    data class InvalidURI(val reason: StaticReason) : ZIP321Error()

    /** A structural parse failure not covered by a more specific case. */
    data class ParseError(val reason: StaticReason) : ZIP321Error()

    /**
     * A fixed, input-independent reason for a top-level URI or structural parse failure. Every case
     * is a constant discriminant: no case carries a `String` or any other channel through which raw
     * user input could leak.
     */
    enum class StaticReason {
        /** The input was the empty string. */
        EMPTY_INPUT,

        /** The input exceeded the configured maximum byte length. */
        INPUT_TOO_LARGE,

        /** The input did not begin with the `zcash:` scheme. */
        NOT_ZCASH_SCHEME,

        /** The input included a `//` authority component, which ZIP-321 forbids. */
        INVALID_AUTHORITY,

        /**
         * The URI was structurally malformed (a catch-all for grammar failures not covered by a
         * more specific case).
         */
        MALFORMED_URI,

        /** A query parameter's name or value violated the ZIP-321 grammar. */
        INVALID_PARAMETER,
    }

    /**
     * Returns a copy of this error with [index] set on the index-bearing cases. Non-index cases are
     * returned unchanged. Used by the parser to tag payment-construction errors (produced
     * index-agnostically by [org.zecdev.zip321.model.Payment.create]) with the concrete payment
     * index once it is known.
     */
    fun withIndex(index: UInt?): ZIP321Error =
        when (this) {
            is InvalidBase64 -> InvalidBase64(index)
            is MemoBytesError -> MemoBytesError(index)
            is TransparentMemo -> TransparentMemo(index)
            is ZeroValuedTransparentOutput -> ZeroValuedTransparentOutput(index)
            is RecipientMissing -> RecipientMissing(index)
            is InvalidAddress -> InvalidAddress(index)
            is AmountExceededSupply -> AmountExceededSupply(index)
            is AmountInvalid -> AmountInvalid(index)
            is DuplicateParameter -> DuplicateParameter(name, index)
            is TooManyPayments,
            is UnknownRequiredParameter,
            is InvalidParamIndex,
            is InvalidURI,
            is ParseError,
            -> this
        }

    internal companion object {
        /**
         * Maps the internal v1 [ZIP321.Errors] taxonomy onto the public sealed v2 taxonomy. This is
         * the single exhaustive translation point; the throwing parse pipeline continues to raise
         * v1 errors internally and [ZIP321.parse] funnels them through here.
         *
         * Index convention: the v1 pipeline uses `nil`/`0` for the empty paramindex; the few v1
         * cases that carry a raw `UInt` (`0` for the empty index) are normalized to `null` here.
         */
        @Suppress("CyclomaticComplexMethod")
        fun from(legacy: ZIP321.Errors): ZIP321Error {
            fun norm(i: UInt): UInt? = if (i == 0u) null else i

            return when (legacy) {
                is ZIP321.Errors.AmountExceededSupply -> AmountExceededSupply(norm(legacy.value))
                // v1 `AmountTooSmall` covers negative / too-many-fractional-digits, both of which
                // are "malformed amount" in the corpus taxonomy.
                is ZIP321.Errors.AmountTooSmall -> AmountInvalid(norm(legacy.value))
                is ZIP321.Errors.DuplicateParameter -> DuplicateParameter(legacy.parameter, legacy.index)
                is ZIP321.Errors.InvalidAddress -> InvalidAddress(legacy.index)
                ZIP321.Errors.InvalidBase64 -> InvalidBase64(null)
                ZIP321.Errors.InvalidURI -> InvalidURI(StaticReason.MALFORMED_URI)
                is ZIP321.Errors.MemoBytesError -> MemoBytesError(legacy.index)
                is ZIP321.Errors.TooManyPayments -> TooManyPayments(legacy.value)
                is ZIP321.Errors.TransparentMemoNotAllowed -> TransparentMemo(legacy.index)
                is ZIP321.Errors.RecipientMissing -> RecipientMissing(legacy.index)
                is ZIP321.Errors.InvalidParamIndex -> InvalidParamIndex(legacy.value)
                is ZIP321.Errors.InvalidParamValue ->
                    // The only sub-grammar that reports `invalidParamValue` is `amount` (a malformed
                    // decimal / percent-escape / overflow); everything else is a structural failure.
                    if (legacy.param == ParamName.AMOUNT.value) {
                        AmountInvalid(legacy.index)
                    } else {
                        ParseError(StaticReason.INVALID_PARAMETER)
                    }
                is ZIP321.Errors.ParseError -> ParseError(StaticReason.MALFORMED_URI)
                is ZIP321.Errors.QcharDecodeFailed -> ParseError(StaticReason.INVALID_PARAMETER)
                is ZIP321.Errors.UnknownRequiredParameter -> UnknownRequiredParameter(legacy.value)
                is ZIP321.Errors.InvalidParamName -> ParseError(StaticReason.INVALID_PARAMETER)
            }
        }
    }
}
