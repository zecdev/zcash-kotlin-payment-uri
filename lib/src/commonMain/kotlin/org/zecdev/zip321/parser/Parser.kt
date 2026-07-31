// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321.parser

import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.ZIP321.ParserResult
import org.zecdev.zip321.model.LegacyAmount
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.CharsetValidations.Companion.QcharCharacterSet

private const val ZCASH_SCHEME = "zcash:"

// A ZIP-321 paramindex is `nonzerodigit *3digit`, i.e. 1..4 decimal digits with
// no leading zero.
private const val MAX_INDEX_DIGITS = 4

/**
 * Hand-rolled ZIP-321 URI parser. This replaces the previous kudzu
 * parser-combinator implementation with a dependency-free, pure-Kotlin state
 * machine that preserves v1 accept/reject behavior and error types EXACTLY
 * (verified against the shared conformance corpus and the existing test suite).
 *
 * The grammar mirrors the old combinators:
 *   uri        = "zcash:" [ address ] [ "?" params ]
 *   params     = keyval *( "&" keyval )          (SeparatedBy; trailing text
 *                                                 after the last keyval that is
 *                                                 not "&" is ignored, matching
 *                                                 kudzu's SeparatedByParser)
 *   keyval     = paramname [ "." paramindex ] [ "=" *qchar ]
 *   paramname  = *( any char up to "&" / "." / "=" )   then validated
 */
class Parser(
    private val network: Network,
    private val validator: AddressValidator,
) {
    // Address validation is fully DELEGATED. This parser implements the
    // ZIP-321 URI grammar and nothing else: whether a recipient string is a
    // valid, payable Zcash address — and what it can receive — is decided
    // exclusively by [validator], which is a REQUIRED constructor argument.
    // There is no built-in check to fall back on, weaken or compose with.

    // -- leading address -----------------------------------------------------

    /**
     * Parses the `zcash:` scheme and an optional leading (empty-paramindex)
     * address. Returns the leading address (or null) and the remaining,
     * still-unparsed text.
     *
     * Mirrors kudzu's `maybeLeadingAddressParse`: the address is the maximal
     * run of ASCII letters/digits, accepted only when [validator] accepts it.
     * When it does not, nothing is consumed.
     */
    fun parseLeadingAddress(input: String): Pair<IndexedParameter?, String> {
        require(input.startsWith(ZCASH_SCHEME)) { "expected `zcash:` scheme" }
        val rest = input.substring(ZCASH_SCHEME.length)
        var end = 0
        while (end < rest.length && rest[end].isAsciiLetterOrDigit()) end++
        val run = rest.substring(0, end)
        val recipient = if (run.isEmpty()) null else recipient(run, network, validator)
        if (recipient != null) {
            val addr =
                IndexedParameter(
                    index = 0u,
                    param = Param.Address(recipient),
                )
            return Pair(addr, rest.substring(end))
        }
        return Pair(null, rest)
    }

    // -- paramindex ----------------------------------------------------------

    /**
     * Parses a bare paramindex token: `nonzerodigit *3digit`.
     *
     * A leading zero (or non-digit) fails with [IllegalArgumentException]
     * (surfaced as [ZIP321.Errors.ParseError] end-to-end); more than four
     * digits fails with [ZIP321.Errors.InvalidParamIndex]. This split matches
     * the kudzu behavior exactly.
     */
    @Throws(ZIP321.Errors::class)
    fun parseParameterIndex(digits: String): UInt = validateIndexDigits(digits)

    private fun validateIndexDigits(digits: String): UInt {
        require(digits.isNotEmpty() && digits[0] in '1'..'9') { "invalid paramindex `$digits`" }
        require(digits.all { it in '0'..'9' }) { "invalid paramindex `$digits`" }
        if (digits.length > MAX_INDEX_DIGITS) {
            throw ZIP321.Errors.InvalidParamIndex(digits)
        }
        return digits.toUInt()
    }

    // -- optionally-indexed paramname ---------------------------------------

    /** Parses a whole `paramname[.index]` token (no `=value`). */
    fun parseOptionallyIndexedParamName(input: String): Pair<String, UInt?> {
        return parseOptionallyIndexedParamNameAt(input, 0).first
    }

    private fun parseOptionallyIndexedParamNameAt(
        input: String,
        pos: Int,
    ): Pair<Pair<String, UInt?>, Int> {
        // paramname: any char up to the first of '&', '.', '=' (or end).
        var p = pos
        while (p < input.length && input[p] != '&' && input[p] != '.' && input[p] != '=') {
            p++
        }
        val paramName = input.substring(pos, p)
        if (!paramName.all { c -> CharsetValidations.isValidParamNameChar(c) }) {
            throw ZIP321.Errors.ParseError("Invalid paramname $paramName")
        }

        if (p < input.length && input[p] == '.') {
            // '.' is consumed; a valid paramindex MUST follow (kudzu does not
            // backtrack the consumed '.').
            var d = p + 1
            require(d < input.length && input[d] in '1'..'9') { "invalid paramindex after `.`" }
            val start = d
            while (d < input.length && input[d] in '0'..'9') {
                d++
            }
            val index = validateIndexDigits(input.substring(start, d))
            return Pair(Pair(paramName, index), d)
        }

        return Pair(Pair(paramName, null), p)
    }

    // -- key/value -----------------------------------------------------------

    /** Parses a whole `paramname[.index][=value]` token. */
    fun parseQueryKeyAndValue(input: String): Pair<Pair<String, UInt?>, String?> {
        return parseQueryKeyAndValueAt(input, 0).first
    }

    private fun parseQueryKeyAndValueAt(
        input: String,
        pos: Int,
    ): Pair<Pair<Pair<String, UInt?>, String?>, Int> {
        val (nameIndex, afterName) = parseOptionallyIndexedParamNameAt(input, pos)
        if (afterName < input.length && input[afterName] == '=') {
            // value: maximal run of qchars (may be empty); '=' is always
            // consumed once present.
            var v = afterName + 1
            val start = v
            while (v < input.length && input[v] in QcharCharacterSet.characters) {
                v++
            }
            return Pair(Pair(nameIndex, input.substring(start, v)), v)
        }
        return Pair(Pair(nameIndex, null), afterName)
    }

    /**
     * Parses a `?`-led sequence of `&`-separated query parameters. Text after
     * the final parameter that does not continue with `&` is ignored, matching
     * kudzu's `SeparatedByParser` semantics.
     */
    private fun parseQueryParams(input: String): List<Pair<Pair<String, UInt?>, String?>> {
        require(input.startsWith("?")) { "expected `?` query marker" }
        val result = ArrayList<Pair<Pair<String, UInt?>, String?>>()
        var pos = 1
        while (true) {
            val (keyValue, next) = parseQueryKeyAndValueAt(input, pos)
            result.add(keyValue)
            if (next < input.length && input[next] == '&') {
                pos = next + 1
            } else {
                break
            }
        }
        return result
    }

    /**
     * maps a parsed Query Parameter key and value into an `IndexedParameter`
     * providing validation of Query keys and values. An address validation can be provided.
     */
    @Throws(ZIP321.Errors.InvalidParamValue::class)
    fun zcashParameter(parsedQueryKeyValue: Pair<Pair<String, UInt?>, String?>): IndexedParameter {
        val queryKey = parsedQueryKeyValue.first.first
        val queryKeyIndex =
            parsedQueryKeyValue.first.second?.let {
                if (it == 0u) {
                    throw ZIP321.Errors.InvalidParamIndex("$queryKey.0")
                } else {
                    it
                }
            } ?: 0u
        val queryValue = parsedQueryKeyValue.second

        val param =
            Param.from(
                queryKey,
                queryValue,
                queryKeyIndex,
                network,
                validator,
            )

        return IndexedParameter(queryKeyIndex, param)
    }

    /**
     * Parses the rest of the URI after the `zcash:` and possible
     * leading address have been captured, validating the found addresses
     * if validation is provided
     */
    fun parseParameters(
        remainingString: String,
        leadingAddress: IndexedParameter?,
    ): List<IndexedParameter> {
        val list = ArrayList<IndexedParameter>()

        leadingAddress?.let { list.add(it) }

        list.addAll(
            parseQueryParams(remainingString)
                .map { zcashParameter(it) },
        )

        if (list.isEmpty()) {
            throw ZIP321.Errors.RecipientMissing(null)
        }

        return list
    }

    /**
     * Maps a list of `IndexedParameter` into a list of validated `Payment`
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
            val (leadingAddress, remainingText) = parseLeadingAddress(uriString)

            // no remaining text to parse and no address found. Not a valid URI
            if (remainingText.isEmpty() && leadingAddress == null) {
                throw ZIP321.Errors.InvalidURI
            }

            if (remainingText.isEmpty() && leadingAddress != null) {
                when (val param = leadingAddress.param) {
                    is Param.Address -> return ParserResult.SingleAddress(param.recipientAddress)
                    else ->
                        throw ZIP321.Errors.ParseError(
                            "leading parameter after `zcash:` that is not an address",
                        )
                }
            }

            // remaining text is not empty there's still work to do
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
 * @param value the raw address string as it appeared in the URI.
 * @param network the network the request is being parsed for.
 * @param validator the caller-supplied authority on addresses.
 */
internal fun recipient(
    value: String,
    @Suppress("UNUSED_PARAMETER") network: Network,
    validator: AddressValidator,
): RecipientAddress? = RecipientAddress.create(value, validator)
