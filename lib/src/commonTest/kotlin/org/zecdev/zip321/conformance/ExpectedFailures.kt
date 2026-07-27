package org.zecdev.zip321.conformance

/**
 * Registry of KNOWN divergences between this library (v1) and the ZIP-321
 * reference semantics captured by the shared conformance corpus
 * (`test-vectors` submodule, oracle: librustzcash `zip321` crate).
 *
 * This file documents gaps — it does not excuse them. Every entry maps a
 * vector name to a precise reason describing what v1 currently does instead
 * of the reference behavior. A vector listed here is asserted to CURRENTLY
 * FAIL its conformance check: if the library is later fixed and the vector
 * starts passing, the suite fails loudly with an XPASS error so the stale
 * entry gets removed. Vectors not listed here must pass.
 */
object ExpectedFailures {
    /**
     * Valid vectors that v1 cannot parse to the reference payments, and
     * invalid vectors that v1 fails to reject with a [org.zecdev.zip321.ZIP321.Errors].
     */
    val conformance: Map<String, String> = mapOf(
        // --- valid vectors v1 rejects ---------------------------------------
        "structure_empty_request" to
            "v1 throws Errors.InvalidURI for 'zcash:'; the reference parses it as an " +
            "empty (zero-payment) transaction request (Parser.parse rejects when there " +
            "is no leading address and no remaining text).",
        "structure_empty_request_query_marker" to
            "v1 throws Errors.InvalidParamName(\"paramName cannot be empty\") for 'zcash:?'; " +
            "the reference parses it as an empty (zero-payment) transaction request " +
            "(the '?' leads Param.from to see an empty paramname instead of zero params).",
        "structure_empty_memo_on_sapling" to
            "v1 rejects 'memo=' (empty value) with Errors.MemoBytesError(MemoEmpty) because " +
            "MemoBytes requires non-empty data; the reference accepts a 0-byte memo " +
            "(MemoBytes::from_bytes accepts an empty slice).",

        // --- invalid vectors v1 accepts -------------------------------------
        "invalid_address_sapling_bad_checksum" to
            "v1 heuristic address validation (prefix + length + charset only, no bech32 " +
            "checksum verification) accepts a Sapling address with a corrupted checksum.",
        "invalid_address_unified_mainnet_bad_checksum" to
            "v1 heuristic address validation performs no bech32m/F4Jumble verification and " +
            "accepts a mainnet unified address with a corrupted checksum.",
        "invalid_address_transparent_bad_checksum" to
            "v1 heuristic address validation performs no base58check verification and " +
            "accepts a transparent address with a corrupted checksum.",
        "invalid_address_sapling_mixed_case" to
            "v1 accepts a mixed-case Sapling address ('...0YY2ex5...'); bech32 forbids " +
            "mixed case, but v1 only checks prefix/length/alphanumeric so the corrupted " +
            "encoding passes.",
        "invalid_amount_trailing_decimal_point" to
            "v1 accepts amount '123.' (parsed via BigDecimal, which allows a bare trailing " +
            "decimal point); the ZIP-321 grammar requires at least one digit after '.'.",
        "invalid_amount_leading_decimal_point" to
            "v1 accepts amount '.5' (parsed via BigDecimal, which allows a missing integer " +
            "part); the ZIP-321 grammar requires the integer part.",
        "spec_invalid_zero_valued_transparent_output" to
            "v1 has no zero-valued-transparent-output consensus check: 'amount=0' to a " +
            "transparent recipient parses successfully (LegacyAmount permits 0)."
    )

    /**
     * Valid vectors whose re-rendered URI (via `ZIP321.uriString`) differs
     * from the reference `canonicalUri` (librustzcash `to_uri()` output).
     * Tracked separately from [conformance] because v1's default formatting
     * may legitimately differ from the Rust canonical form.
     */
    val renderMismatch: Map<String, String> = mapOf(
        "amount_just_below_max_money" to
            "v1 re-renders amount 20999999.99999999 ZEC as 21000000: " +
            "LegacyAmount.zatoshiToZEC builds BigDecimal(zatoshis, MathContext(8, " +
            "HALF_EVEN)), rounding to 8 SIGNIFICANT DIGITS and silently inflating the " +
            "amount to max supply. Amount-corrupting round-trip bug, not just formatting.",
        "amount_parse_simple_large_decimal" to
            "v1 re-renders amount 3768769.02796286 ZEC as 3768769 (fractional part lost): " +
            "same 8-significant-digit MathContext rounding bug in zatoshiToZEC.",
        "structure_index_gap_only_address_5" to
            "v1's Payment model discards the original paramindex, so a request parsed from " +
            "'address.5='/'amount.5=' re-renders as 'zcash:<addr>?amount=1' instead of " +
            "preserving/enumerating indices like the reference canonical form."
    )
}

/**
 * Runs [block] as a conformance check for the vector [name].
 *
 * - If [name] is NOT in [xfails], the block must succeed (failures propagate).
 * - If [name] IS in [xfails], the block must fail; when it unexpectedly
 *   succeeds an XPASS [AssertionError] is thrown so stale expected-failure
 *   entries are flagged as soon as the library behavior changes.
 */
internal fun expectDocumentedOutcome(name: String, xfails: Map<String, String>, block: () -> Unit) {
    val reason = xfails[name]
    if (reason == null) {
        block()
        return
    }
    val failure = try {
        block()
        null
    } catch (expected: Throwable) {
        expected
    }
    if (failure == null) {
        throw AssertionError(
            "XPASS: vector '$name' unexpectedly PASSED but is registered as an expected failure " +
                "(reason: \"$reason\"). The library behavior has changed; remove this entry " +
                "from the expected-failures map in ExpectedFailures.kt."
        )
    }
    println("XFAIL (documented divergence) '$name': $reason")
    println("    observed: ${failure.message?.lineSequence()?.first()}")
}
