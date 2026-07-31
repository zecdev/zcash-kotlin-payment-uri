package org.zecdev.zip321.conformance

import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.assertEquals

/**
 * Conformance runner for the shared ZIP-321 vector corpus in the
 * `test-vectors` git submodule (oracle: librustzcash `zip321` crate).
 *
 * The runner measures and documents where the current v1 implementation
 * diverges from the reference semantics; it does not assert v1 error types
 * (the corpus `error` discriminants are reference documentation only).
 * Known divergences live in [ExpectedFailures] and are asserted to
 * currently fail, so this suite is green as committed and flags stale
 * entries (XPASS) once the library is fixed.
 *
 * K1: the kotest `FreeSpec` that previously hosted these checks is replaced
 * by `GeneratedZip321ConformanceTest` (one `kotlin.test` function per corpus
 * vector, generated at build time alongside the embedded vectors), which
 * dispatches into [ConformanceRunner] by vector name. Assertions and the
 * expected-failure semantics are unchanged.
 *
 * Deterministic: no network, no clocks; the corpus is embedded into the test
 * sources at build time.
 */
internal object ConformanceRunner {
    val validVectors: List<ValidVector> by lazy { CorpusLoader.loadValid() }
    val invalidVectors: List<InvalidVector> by lazy { CorpusLoader.loadInvalid() }

    private fun validVector(name: String): ValidVector =
        checkNotNull(validVectors.find { it.name == name }) {
            "generated test references unknown valid vector '$name'; re-run the " +
                "generateConformanceVectors task after updating the submodule"
        }

    private fun invalidVector(name: String): InvalidVector =
        checkNotNull(invalidVectors.find { it.name == name }) {
            "generated test references unknown invalid vector '$name'; re-run the " +
                "generateConformanceVectors task after updating the submodule"
        }

    /** Valid vector must parse to the reference payments (or be an xfail). */
    fun checkValid(name: String) {
        val vector = validVector(name)
        expectDocumentedOutcome(vector.name, ExpectedFailures.conformance) {
            checkValidVector(vector)
        }
    }

    /**
     * Valid vector must re-render to the reference canonical URI (or be an
     * xfail). Vectors that already fail to parse (documented in
     * [ExpectedFailures.conformance]) cannot be re-rendered; as in the
     * pre-K1 kotest runner they are excluded from the render check instead
     * of being double-counted as render gaps.
     */
    fun checkRender(name: String) {
        val vector = validVector(name)
        if (vector.name in ExpectedFailures.conformance) {
            return
        }
        expectDocumentedOutcome(vector.name, ExpectedFailures.renderMismatch) {
            checkRenderRoundTrip(vector)
        }
    }

    /** Invalid vector must be rejected with a [ZIP321.Errors] (or be an xfail). */
    fun checkInvalid(name: String) {
        val vector = invalidVector(name)
        expectDocumentedOutcome(vector.name, ExpectedFailures.conformance) {
            checkInvalidVector(vector)
        }
    }
}

/**
 * A parsed payment as observed from the v1 API, normalized for comparison
 * against a [VectorPayment]. Note v1's model does not retain the original
 * ZIP-321 paramindex, so payments are compared positionally.
 */
private data class ObservedPayment(
    val address: String,
    val amountZat: Long?,
    val memoBase64: String?,
    val label: String?,
    val message: String?,
    val other: List<Pair<String, String?>>,
)

private fun Payment.toObserved(): ObservedPayment =
    ObservedPayment(
        address = recipientAddress.value,
        // LegacyAmount stores Long zatoshis internally and toString() renders them.
        amountZat = nonNegativeAmount?.toString()?.toLong(),
        memoBase64 = memo?.toBase64URL(),
        label = label,
        message = message,
        other = otherParams.orEmpty().map { it.key.value to it.value },
    )

/**
 * Parses a corpus vector through the SAME delegation path a production caller
 * uses: the library owns the URI grammar, and the test-only
 * [ReferenceAddressValidator] for the vector's network is the authority on
 * every recipient address. This is what makes the corpus's
 * checksum-corruption, mixed-case, Sprout and wrong-network vectors
 * executable — the library rejects exactly what the VALIDATOR rejects.
 */
private fun parseVectorUri(
    uri: String,
    network: String,
): ZIP321.ParserResult {
    val expecting = networkOfVector(network)
    return ZIP321.request(uri, expecting, ReferenceAddressValidator.of(expecting))
}

private fun ZIP321.ParserResult.toObservedPayments(): List<ObservedPayment> =
    when (this) {
        is ZIP321.ParserResult.SingleAddress ->
            listOf(
                ObservedPayment(
                    address = singleRecipient.value,
                    amountZat = null,
                    memoBase64 = null,
                    label = null,
                    message = null,
                    other = emptyList(),
                ),
            )
        is ZIP321.ParserResult.Request -> paymentRequest.payments.map { it.toObserved() }
    }

private fun checkValidVector(vector: ValidVector) {
    val result =
        try {
            parseVectorUri(vector.uri, vector.network)
        } catch (t: Throwable) {
            // NOTE: common code has no AssertionError(message, cause) constructor;
            // the rejection is described in the message instead.
            throw AssertionError(
                "vector '${vector.name}' must parse but was rejected with " +
                    "${t::class.simpleName}: ${t.message} (${vector.description})",
            )
        }

    val observed = result.toObservedPayments()
    assertEquals(
        vector.payments.size,
        observed.size,
        "payment count for '${vector.name}' (${vector.description})",
    )
    vector.payments.zip(observed).forEach { (expected, actual) ->
        assertEquals(
            ObservedPayment(
                address = expected.address,
                amountZat = expected.amountZat,
                memoBase64 = expected.memoBase64,
                label = expected.label,
                message = expected.message,
                other = expected.other,
            ),
            actual,
            "payment at paramindex ${expected.index} of '${vector.name}'",
        )
    }
}

private fun checkRenderRoundTrip(vector: ValidVector) {
    val canonical = checkNotNull(vector.canonicalUri)
    val rendered =
        when (val result = parseVectorUri(vector.uri, vector.network)) {
            is ZIP321.ParserResult.SingleAddress ->
                // Reference renders an address-only request as `zcash:<address>`.
                ZIP321.request(result.singleRecipient)
            is ZIP321.ParserResult.Request ->
                ZIP321.uriString(
                    from = result.paymentRequest,
                    // Mirror librustzcash to_uri(): the first payment uses the empty
                    // paramindex (address in the hier-part when it is the only one),
                    // subsequent payments are enumerated from `.1`.
                    formattingOptions =
                        ZIP321.FormattingOptions.UseEmptyParamIndex(
                            omitAddressLabel = result.paymentRequest.payments.size == 1,
                        ),
                )
        }
    assertEquals(
        canonical,
        rendered,
        "re-rendered URI for '${vector.name}' vs reference canonical form",
    )
}

private fun checkInvalidVector(vector: InvalidVector) {
    val result =
        try {
            parseVectorUri(vector.uri, vector.network)
        } catch (expected: ZIP321.Errors) {
            return // correctly rejected; v1 error *types* are deliberately not asserted
        } catch (t: Throwable) {
            throw AssertionError(
                "vector '${vector.name}' (reference error: ${vector.error}) was rejected, " +
                    "but with ${t::class.qualifiedName} instead of a ZIP321.Errors: ${t.message}",
            )
        }
    throw AssertionError(
        "vector '${vector.name}' (reference error: ${vector.error}) must be rejected " +
            "but parsed successfully as: $result (${vector.description})",
    )
}
