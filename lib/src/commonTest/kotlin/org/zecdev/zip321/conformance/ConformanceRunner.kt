package org.zecdev.zip321.conformance

import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.ZIP321Error
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Conformance runner for the shared ZIP-321 vector corpus in the `test-vectors` git submodule
 * (oracle: librustzcash `zip321` crate).
 *
 * Valid vectors are parsed via [ZIP321.parse]`(...).getOrThrow()` and their payments compared
 * exactly (including [org.zecdev.zip321.model.NonNegativeAmount] amounts). Invalid vectors are asserted to be
 * rejected with the EXACT [ZIP321Error] discriminant named by the corpus (case-name comparison).
 * Known divergences live in [ExpectedFailures] and are asserted to currently fail.
 *
 * Deterministic: no network, no clocks; the corpus is embedded into the test sources at build time.
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
     * Valid vector must re-render to the reference canonical URI (or be an xfail). Vectors that
     * already fail to parse (documented in [ExpectedFailures.conformance]) cannot be re-rendered;
     * they are excluded from the render check instead of being double-counted as render gaps.
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

    /** Invalid vector must be rejected by [ZIP321.parse] (or be an xfail). */
    fun checkInvalid(name: String) {
        val vector = invalidVector(name)
        expectDocumentedOutcome(vector.name, ExpectedFailures.conformance) {
            checkInvalidVector(vector)
        }
    }
}

/**
 * A parsed payment as observed from the v2 API, normalized for comparison against a
 * [VectorPayment].
 */
private data class ObservedPayment(
    val address: String,
    val amountZat: ULong?,
    val memoBase64: String?,
    val label: String?,
    val message: String?,
    val other: List<Pair<String, String?>>,
)

private fun Payment.toObserved(): ObservedPayment =
    ObservedPayment(
        address = recipientAddress.value,
        amountZat = amount?.value,
        memoBase64 = memo?.toBase64URL(),
        label = label,
        message = message,
        other = otherParams.map { it.name to it.value },
    )

/**
 * Parses a corpus vector through the SAME delegation path a production caller uses: the library
 * owns the URI grammar, and the test-only [ReferenceAddressValidator] for the vector's network is
 * the authority on every recipient address. This is what makes the corpus's checksum-corruption,
 * mixed-case, Sprout and wrong-network vectors executable — the library rejects exactly what the
 * VALIDATOR rejects.
 */
private fun parseVectorUri(
    uri: String,
    network: String,
): Result<PaymentRequest> {
    val expecting = networkOfVector(network)
    return ZIP321.parse(uri, expecting, ReferenceAddressValidator.of(expecting))
}

private fun parseVector(vector: ValidVector): PaymentRequest = parseVectorUri(vector.uri, vector.network).getOrThrow()

private fun PaymentRequest.toObservedPayments(): List<ObservedPayment> = payments.map { it.toObserved() }

private fun checkValidVector(vector: ValidVector) {
    val result =
        try {
            parseVector(vector)
        } catch (t: Throwable) {
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
    // The DEFAULT formatting options are the canonical reference form (K13): each payment renders
    // at its ACTUAL stored paramindex, and a single payment at the empty paramindex uses the
    // leading-address form, mirroring librustzcash to_uri().
    val rendered = ZIP321.uriString(from = parseVector(vector))
    assertEquals(
        canonical,
        rendered,
        "re-rendered URI for '${vector.name}' vs reference canonical form",
    )
}

private fun checkInvalidVector(vector: InvalidVector) {
    val result = parseVectorUri(vector.uri, vector.network)
    assertTrue(
        result.isFailure,
        "vector '${vector.name}' (reference error: ${vector.error}) must be rejected " +
            "but parsed successfully as: ${result.getOrNull()} (${vector.description})",
    )

    val error = result.exceptionOrNull()
    assertTrue(
        error is ZIP321Error,
        "vector '${vector.name}' must fail with a ZIP321Error, was ${error?.let { it::class.simpleName }}",
    )

    val expected = expectedDiscriminant(vector.error)
    assertEquals(
        expected,
        error.discriminantName(),
        "error discriminant for '${vector.name}' (${vector.description})",
    )
}

/**
 * Maps a corpus `error` string (camelCase cross-language discriminant) to the expected
 * [ZIP321Error] subclass simple name. Any unrecognized corpus discriminant fails loudly so a corpus
 * update cannot silently pass.
 */
private fun expectedDiscriminant(corpusError: String): String =
    when (corpusError) {
        "invalidBase64" -> "InvalidBase64"
        "memoBytesError" -> "MemoBytesError"
        "transparentMemo" -> "TransparentMemo"
        "zeroValuedTransparentOutput" -> "ZeroValuedTransparentOutput"
        "tooManyPayments" -> "TooManyPayments"
        "duplicateParameter" -> "DuplicateParameter"
        "recipientMissing" -> "RecipientMissing"
        "invalidAddress" -> "InvalidAddress"
        "unknownRequiredParameter" -> "UnknownRequiredParameter"
        "invalidParamIndex" -> "InvalidParamIndex"
        "amountExceededSupply" -> "AmountExceededSupply"
        "amountInvalid" -> "AmountInvalid"
        "invalidURI" -> "InvalidURI"
        "parseError" -> "ParseError"
        else -> error("unmapped corpus error discriminant '$corpusError'")
    }

private fun ZIP321Error?.discriminantName(): String? =
    when (this) {
        is ZIP321Error.InvalidBase64 -> "InvalidBase64"
        is ZIP321Error.MemoBytesError -> "MemoBytesError"
        is ZIP321Error.TransparentMemo -> "TransparentMemo"
        is ZIP321Error.ZeroValuedTransparentOutput -> "ZeroValuedTransparentOutput"
        is ZIP321Error.TooManyPayments -> "TooManyPayments"
        is ZIP321Error.DuplicateParameter -> "DuplicateParameter"
        is ZIP321Error.RecipientMissing -> "RecipientMissing"
        is ZIP321Error.InvalidAddress -> "InvalidAddress"
        is ZIP321Error.UnknownRequiredParameter -> "UnknownRequiredParameter"
        is ZIP321Error.InvalidParamIndex -> "InvalidParamIndex"
        is ZIP321Error.AmountExceededSupply -> "AmountExceededSupply"
        is ZIP321Error.AmountInvalid -> "AmountInvalid"
        is ZIP321Error.InvalidURI -> "InvalidURI"
        is ZIP321Error.ParseError -> "ParseError"
        null -> null
    }
