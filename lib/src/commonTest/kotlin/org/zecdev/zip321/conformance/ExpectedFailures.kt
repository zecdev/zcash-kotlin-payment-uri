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
    val conformance: Map<String, String> = emptyMap()

    /**
     * Valid vectors whose re-rendered URI (via `ZIP321.uriString`) differs
     * from the reference `canonicalUri` (librustzcash `to_uri()` output).
     * Tracked separately from [conformance] because v1's default formatting
     * may legitimately differ from the Rust canonical form.
     */
    val renderMismatch: Map<String, String> = mapOf(
        "structure_index_gap_only_address_5" to
            "the renderer still enumerates payments positionally from the empty/`.1` paramindex " +
            "rather than preserving the parsed paramindex, so a request parsed from " +
            "'address.5='/'amount.5=' re-renders as 'zcash:<addr>?amount=1' instead of " +
            "preserving/enumerating indices like the reference canonical form (renderer rewrite, K13)."
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
