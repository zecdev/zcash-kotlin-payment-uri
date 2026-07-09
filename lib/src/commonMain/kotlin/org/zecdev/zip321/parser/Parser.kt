// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321.parser

import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.ZIP321.ParserResult
import org.zecdev.zip321.encodings.QCharCodec
import org.zecdev.zip321.model.LegacyAmount
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress

private const val ZCASH_SCHEME = "zcash:"

// A ZIP-321 paramindex is `nonzerodigit *3digit`, i.e. 1..4 decimal digits with
// no leading zero.
private const val MAX_INDEX_DIGITS = 4

private fun isAlpha(c: Char): Boolean = c in 'A'..'Z' || c in 'a'..'z'

private fun isDigit(c: Char): Boolean = c in '0'..'9'

/**
 * A `paramname` continuation character: `ALPHA / DIGIT / "+" / "-"`. This matches the reference
 * `namechars` (`alphanum_or("+-")`), which — like `nom`'s `AsChar` — is ASCII-only.
 */
private fun isNameChar(c: Char): Boolean = isAlpha(c) || isDigit(c) || c == '+' || c == '-'

/**
 * ZIP-321 URI parser built on the single-pass [Scanner]. It follows the reference `nom` pipeline
 * in librustzcash `zip321`: the `zcash:` scheme, a `take_till('?')` lead address (empty allowed;
 * a non-empty lead address must validate), then `&`-separated query segments parsed as
 * `name [ "." index ] [ "=" value ]`.
 *
 * - Parameter names are `ALPHA *( ALPHA / DIGIT / "+" / "-" )`; a percent-escape in a name is
 *   rejected.
 * - Indices are `NONZERO 0*3DIGIT` (no leading zero, at most four digits).
 * - Raw values are restricted to `qchar`-permitted characters / percent-escapes.
 * - `label`/`message`/`other` values are percent-decoded via [QCharCodec]; `address`/`amount`/
 *   `memo` values are handed to their own grammars verbatim (a `%` in them is therefore rejected).
 */
class Parser(
    private val network: Network,
    private val validator: AddressValidator,
) {
    // Address validation is fully DELEGATED. This parser implements the
    // ZIP-321 URI grammar and nothing else: whether a recipient string is a
    // valid, payable Zcash address — and what it can receive — is decided
    // exclusively by [validator], which is a REQUIRED constructor argument.
    // There is no built-in check to fall back on, weaken or compose with. The
    // only rule applied on top of the validator's verdict is the
    // expected-network comparison in [recipient].

    // -- leading address -----------------------------------------------------

    /**
     * Splits a `zcash:` URI into its lead address and the remaining query part.
     *
     * Mirrors the reference `preceded(tag("zcash:"), take_till(|c| c == '?'))`: the lead address
     * is every character after `zcash:` up to (but not including) the first `?`. The returned
     * `address` is a (possibly empty) [String]; `rest` is the remainder starting at `?`, or `null`
     * when there is no `?` in the input.
     * @param input a URI beginning with `zcash:`.
     */
    fun splitLeadingAddress(input: String): Pair<String, String?> {
        val afterScheme = input.substring(ZCASH_SCHEME.length)
        val questionMark = afterScheme.indexOf('?')
        return if (questionMark < 0) {
            Pair(afterScheme, null)
        } else {
            Pair(afterScheme.substring(0, questionMark), afterScheme.substring(questionMark))
        }
    }

    /**
     * Parses the leading address and returns the rest of the input.
     *
     * A non-empty lead address MUST validate as a recipient address (payment index 0); an empty
     * lead address is allowed (the request's payments come entirely from query parameters, or the
     * request is a bare `zcash:`).
     *
     * @return a pair of the rest of the input (the `?`-prefixed query part, or `null`) and an
     * optional leading-address [IndexedParameter].
     * @throws ZIP321.Errors.InvalidAddress if a non-empty lead address is rejected by
     * [validator] or belongs to a network other than [network] (the unified mapping).
     */
    @Throws(ZIP321.Errors::class)
    fun leadingAddress(input: String): Pair<String?, IndexedParameter?> {
        if (!input.startsWith(ZCASH_SCHEME)) {
            throw ZIP321.Errors.ParseError("Not `zcash:` uri")
        }

        val (address, rest) = splitLeadingAddress(input)

        if (address.isNotEmpty()) {
            // Unified mapping: any non-empty lead address the validator rejects — or that it
            // places on another network — becomes InvalidAddress.
            val recipient =
                recipient(address, network, validator)
                    ?: throw ZIP321.Errors.InvalidAddress(null)
            return Pair(rest, IndexedParameter(index = 0u, param = Param.Address(recipient)))
        }

        return Pair(rest, null)
    }

    // -- query parameter grammar ---------------------------------------------

    /**
     * Scans a `paramname` (`ALPHA *( ALPHA / DIGIT / "+" / "-" )`) from the current position.
     * Returns `null` (consuming nothing) if the first character is not an `ALPHA`.
     */
    private fun scanName(scanner: Scanner): String? {
        val first = scanner.peek()
        if (first == null || !isAlpha(first)) {
            return null
        }

        scanner.advance()
        return first + scanner.takeWhile(::isNameChar)
    }

    /**
     * Scans a `paramindex` digit run (no leading `.`): `NONZERO 0*3DIGIT`, i.e. 1 to 4 digits with
     * no leading zero. Throws [ZIP321.Errors.InvalidParamIndex] on an empty run, a leading zero,
     * or more than four digits.
     */
    private fun scanIndexDigits(scanner: Scanner): UInt {
        val digits = scanner.takeWhile(::isDigit)
        if (digits.isEmpty() || digits.length > MAX_INDEX_DIGITS || digits[0] == '0') {
            throw ZIP321.Errors.InvalidParamIndex(digits)
        }
        return digits.toUInt()
    }

    /**
     * Scans an optional `"." paramindex`. Returns `null` (consuming nothing) when the next
     * character is not `.`; throws when a `.` is present but not followed by a valid index.
     */
    private fun scanIndex(scanner: Scanner): UInt? {
        if (scanner.peek() != '.') return null
        scanner.advance()
        return scanIndexDigits(scanner)
    }

    /**
     * Parses a single `&`-delimited query segment into its `(name, index, value)` components.
     *
     * The raw value is charset-restricted to `qchar`-permitted characters / percent-escapes here;
     * its interpretation (percent-decoding vs. sub-grammar parsing) happens later in [Param.from].
     * Throws [ZIP321.Errors.ParseError] if the name is missing/invalid (e.g. empty, or containing
     * a percent-escape) or if any character in the segment is left unconsumed.
     */
    @Throws(ZIP321.Errors::class)
    fun parseQueryToken(token: String): Triple<String, UInt?, String?> {
        val scanner = Scanner(token)

        val name =
            scanName(scanner)
                ?: throw ZIP321.Errors.ParseError("invalid or empty parameter name in query segment '$token'")

        val index = scanIndex(scanner)

        var value: String? = null
        if (scanner.expect('=')) {
            value = scanner.takeWhile { QCharCodec.isValueByte(it.code) }
        }

        if (!scanner.isAtEnd) {
            throw ZIP321.Errors.ParseError("unexpected characters in query segment '$token'")
        }

        return Triple(name, index, value)
    }

    /**
     * Parses a standalone `paramindex` digit run, requiring full consumption. Test-facing helper.
     */
    @Throws(ZIP321.Errors::class)
    fun parseParameterIndex(input: String): UInt {
        val scanner = Scanner(input)
        val value = scanIndexDigits(scanner)
        if (!scanner.isAtEnd) {
            throw ZIP321.Errors.InvalidParamIndex(input)
        }
        return value
    }

    /**
     * Parses a `paramname` with optional `.paramindex`, requiring full consumption. Test-facing
     * helper.
     */
    @Throws(ZIP321.Errors::class)
    fun parseNameAndIndex(input: String): Pair<String, UInt?> {
        val scanner = Scanner(input)
        val name =
            scanName(scanner)
                ?: throw ZIP321.Errors.ParseError("invalid parameter name '$input'")
        val index = scanIndex(scanner)
        if (!scanner.isAtEnd) {
            throw ZIP321.Errors.ParseError("unexpected characters in parameter name '$input'")
        }
        return Pair(name, index)
    }

    /**
     * Validates a parsed `(name, index, value)` triple and maps it into an [IndexedParameter].
     * `index == null` maps to the "no index" sentinel `0` (zero is not a valid explicit index; the
     * grammar's no-leading-zero rule already prevents it). Reserved-key dispatch, `req-` rejection
     * and per-type value validation happen in [Param.from].
     */
    @Throws(ZIP321.Errors::class)
    fun zcashParameter(
        name: String,
        index: UInt?,
        value: String?,
    ): IndexedParameter {
        val resolvedIndex = index ?: 0u
        val param = Param.from(name, value, resolvedIndex, network, validator)
        return IndexedParameter(resolvedIndex, param)
    }

    /**
     * Parses the `?`-led query parameters and checks that they are individually valid. Text is
     * split on `&` WITHOUT omitting empty segments, so a stray `&` or a lone `?` yields an empty
     * segment that is rejected by [parseQueryToken] (matching the reference's non-omitting split).
     * @param remainingString a string beginning with the `?` query separator.
     * @param leadingAddress an optional leading-address indexed parameter parsed earlier.
     */
    @Throws(ZIP321.Errors::class)
    fun parseParameters(
        remainingString: String,
        leadingAddress: IndexedParameter?,
    ): List<IndexedParameter> {
        require(remainingString.startsWith("?")) { "expected `?` query marker" }

        val list = ArrayList<IndexedParameter>()
        leadingAddress?.let { list.add(it) }

        val afterQuestionMark = remainingString.substring(1)
        for (token in afterQuestionMark.split("&")) {
            val (name, index, value) = parseQueryToken(token)
            list.add(zcashParameter(name, index, value))
        }

        if (list.isEmpty()) {
            throw ZIP321.Errors.RecipientMissing(null)
        }

        return list
    }

    /**
     * Maps a list of [IndexedParameter] into a list of validated [Payment], grouping by paramindex
     * and rejecting duplicate parameter kinds within an index.
     */
    @Throws(ZIP321.Errors::class)
    fun mapToPayments(indexedParameters: List<IndexedParameter>): List<Payment> {
        if (indexedParameters.isEmpty()) {
            throw ZIP321.Errors.RecipientMissing(null)
        }

        val paramsByIndex: MutableMap<UInt, MutableList<Param>> = mutableMapOf()

        for (idxParam in indexedParameters) {
            val paramVecByIndex = paramsByIndex[idxParam.index]
            if (paramVecByIndex != null) {
                if (paramVecByIndex.hasDuplicateParam(idxParam.param)) {
                    throw ZIP321.Errors.DuplicateParameter(
                        idxParam.param.name,
                        idxParam.index.mapToParamIndex(),
                    )
                } else {
                    paramVecByIndex.add(idxParam.param)
                }
            } else {
                paramsByIndex[idxParam.index] = mutableListOf(idxParam.param)
            }
        }

        return paramsByIndex
            .map { (index, parameters) ->
                Payment.fromUniqueIndexedParameters(index, parameters)
            }
    }

    @Throws(ZIP321.Errors::class)
    fun parse(uriString: String): ParserResult {
        if (uriString.isEmpty() || !uriString.startsWith(ZCASH_SCHEME)) {
            throw ZIP321.Errors.InvalidURI
        }

        try {
            val (remainingText, leadingAddress) = leadingAddress(uriString)

            // No query part (`zcash:` or a legacy `zcash:<address>`).
            if (remainingText == null) {
                return when (val param = leadingAddress?.param) {
                    is Param.Address -> ParserResult.SingleAddress(param.recipientAddress)
                    // No leading address and no query: a bare `zcash:`. v1 rejects the empty
                    // (zero-payment) request the reference accepts here.
                    null -> throw ZIP321.Errors.InvalidURI
                    else ->
                        throw ZIP321.Errors.ParseError(
                            "leading parameter after `zcash:` that is not an address",
                        )
                }
            }

            val payments =
                mapToPayments(
                    parseParameters(remainingText, leadingAddress),
                )

            val totalPayments = payments.size.toUInt()

            if (totalPayments > ZIP321.maxPaymentsAllowed) {
                throw ZIP321.Errors.TooManyPayments(totalPayments)
            }

            return if (payments.size == 1 && payments.first().isSingleAddress()) {
                ParserResult.SingleAddress(payments.first().recipientAddress)
            } else {
                ParserResult.Request(
                    PaymentRequest(
                        payments,
                    ),
                )
            }
        } catch (e: IllegalArgumentException) {
            val message = e.message ?: "parser failed with unknown error"
            throw ZIP321.Errors.ParseError(message)
        }
    }
}

@Suppress("detekt:CyclomaticComplexMethod")
fun Payment.Companion.fromUniqueIndexedParameters(
    index: UInt,
    parameters: List<Param>,
): Payment {
    val recipient =
        parameters.firstOrNull { param ->
            when (param) {
                is Param.Address -> true
                else -> false
            }
        }?.let { address ->
            when (address) {
                is Param.Address -> address.recipientAddress
                else -> null
            }
        } ?: throw ZIP321.Errors.RecipientMissing(index.mapToParamIndex())

    var amount: LegacyAmount? = null
    var memo: MemoBytes? = null
    var label: String? = null
    var message: String? = null
    val other = ArrayList<OtherParam>()

    for (param in parameters) {
        when (param) {
            is Param.Address -> continue
            is Param.Amount -> amount = param.amount
            is Param.Label -> label = param.label
            is Param.Memo -> {
                // The validator's descriptor is authoritative: a recipient it
                // reports as unable to receive memos may not carry one.
                if (!recipient.canReceiveMemos) {
                    throw ZIP321.Errors.TransparentMemoNotAllowed(index.mapToParamIndex())
                }

                memo = param.memoBytes
            }
            is Param.Message -> message = param.message
            is Param.Other -> other.add(OtherParam(param.paramName, param.value))
        }
    }

    return Payment(
        recipient,
        amount,
        memo,
        label,
        message,
        when (other.isEmpty()) {
            true -> null
            false -> other
        },
    )
}

fun UInt.mapToParamIndex(): UInt? {
    return when (this == 0u) {
        false -> this
        true -> null
    }
}

/**
 * Wraps [value] as a [RecipientAddress] when [validator] accepts it.
 *
 * The validator is AUTHORITATIVE: this library performs no address validation
 * of its own, so a `null` return here means the caller rejected the address and
 * the request is invalid.
 *
 * The one rule the library applies on top of the validator's verdict is a
 * COMPARISON, not a validation: the accepted address must belong to the network
 * the request is being parsed FOR. A request is parsed against one expected
 * network, so a recipient the validator places on another network makes the
 * request invalid — reported as an invalid address, since from the caller's
 * point of view that address cannot be paid in this context. ZIP-321 itself is
 * network-agnostic (the librustzcash reference parses addresses without a
 * network), so this enforcement is a consumer-library requirement, deliberately
 * made explicit through `expecting`.
 *
 * @param value the raw address string as it appeared in the URI.
 * @param network the network the request is being parsed for.
 * @param validator the caller-supplied authority on addresses.
 */
internal fun recipient(
    value: String,
    network: Network,
    validator: AddressValidator,
): RecipientAddress? =
    validator.validate(value)
        // The network comparison, and nothing else, is applied on top of the
        // validator's verdict.
        ?.takeIf { descriptor -> descriptor.network == network }
        ?.let { descriptor -> RecipientAddress(value, descriptor) }
