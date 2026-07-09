package org.zecdev.zip321.conformance

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.model.Payment

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
 * Deterministic: no network, no clocks; the corpus is read from the test
 * classpath.
 */
class Zip321ConformanceSpec : FreeSpec({

    val validVectors = CorpusLoader.loadValid()
    val invalidVectors = CorpusLoader.loadInvalid()
    val allNames = (validVectors.map { it.name } + invalidVectors.map { it.name })

    "corpus wiring sanity" - {
        "corpus contains the expected number of uniquely-named vectors" {
            withClue("valid vector count") { validVectors.size shouldBe 22 }
            withClue("invalid vector count") { invalidVectors.size shouldBe 28 }
            withClue("vector names must be unique corpus-wide") {
                allNames.toSet().size shouldBe allNames.size
            }
        }

        "expected-failure maps only reference vectors that exist in the corpus" {
            withClue("stale names in ExpectedFailures.conformance") {
                allNames shouldContainAll ExpectedFailures.conformance.keys
            }
            withClue("stale names in ExpectedFailures.renderMismatch") {
                validVectors.map { it.name } shouldContainAll ExpectedFailures.renderMismatch.keys
            }
        }
    }

    "valid vectors parse to the reference payments" - {
        validVectors.forEach { vector ->
            vector.name {
                expectDocumentedOutcome(vector.name, ExpectedFailures.conformance) {
                    checkValidVector(vector)
                }
            }
        }
    }

    "valid vectors re-render to the reference canonical URI" - {
        // Vectors that already fail to parse (documented in
        // ExpectedFailures.conformance) cannot be re-rendered; they are
        // excluded here instead of being double-counted as render gaps.
        validVectors
            .filter { it.name !in ExpectedFailures.conformance && it.canonicalUri != null }
            .forEach { vector ->
                vector.name {
                    expectDocumentedOutcome(vector.name, ExpectedFailures.renderMismatch) {
                        checkRenderRoundTrip(vector)
                    }
                }
            }
    }

    "invalid vectors are rejected with a ZIP321.Errors" - {
        invalidVectors.forEach { vector ->
            vector.name {
                expectDocumentedOutcome(vector.name, ExpectedFailures.conformance) {
                    checkInvalidVector(vector)
                }
            }
        }
    }
})

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
    val other: List<Pair<String, String?>>
)

private fun Payment.toObserved(): ObservedPayment = ObservedPayment(
    address = recipientAddress.value,
    // NonNegativeAmount stores Long zatoshis internally and toString() renders them.
    amountZat = nonNegativeAmount?.toString()?.toLong(),
    memoBase64 = memo?.toBase64URL(),
    label = label,
    message = message,
    other = otherParams.orEmpty().map { it.key.value to it.value }
)

private fun parseVectorUri(uri: String, network: String): ZIP321.ParserResult =
    ZIP321.request(uri, networkToParserContext(network), validatingRecipients = null)

private fun ZIP321.ParserResult.toObservedPayments(): List<ObservedPayment> = when (this) {
    is ZIP321.ParserResult.SingleAddress -> listOf(
        ObservedPayment(
            address = singleRecipient.value,
            amountZat = null,
            memoBase64 = null,
            label = null,
            message = null,
            other = emptyList()
        )
    )
    is ZIP321.ParserResult.Request -> paymentRequest.payments.map { it.toObserved() }
}

private fun checkValidVector(vector: ValidVector) {
    val result = try {
        parseVectorUri(vector.uri, vector.network)
    } catch (t: Throwable) {
        throw AssertionError(
            "vector '${vector.name}' must parse but was rejected with " +
                "${t::class.simpleName}: ${t.message} (${vector.description})",
            t
        )
    }

    val observed = result.toObservedPayments()
    withClue("payment count for '${vector.name}' (${vector.description})") {
        observed.size shouldBe vector.payments.size
    }
    vector.payments.zip(observed).forEach { (expected, actual) ->
        withClue("payment at paramindex ${expected.index} of '${vector.name}'") {
            actual shouldBe ObservedPayment(
                address = expected.address,
                amountZat = expected.amountZat,
                memoBase64 = expected.memoBase64,
                label = expected.label,
                message = expected.message,
                other = expected.other
            )
        }
    }
}

private fun checkRenderRoundTrip(vector: ValidVector) {
    val canonical = checkNotNull(vector.canonicalUri)
    val rendered = when (val result = parseVectorUri(vector.uri, vector.network)) {
        is ZIP321.ParserResult.SingleAddress ->
            // Reference renders an address-only request as `zcash:<address>`.
            ZIP321.request(result.singleRecipient)
        is ZIP321.ParserResult.Request -> ZIP321.uriString(
            from = result.paymentRequest,
            // Mirror librustzcash to_uri(): the first payment uses the empty
            // paramindex (address in the hier-part when it is the only one),
            // subsequent payments are enumerated from `.1`.
            formattingOptions = ZIP321.FormattingOptions.UseEmptyParamIndex(
                omitAddressLabel = result.paymentRequest.payments.size == 1
            )
        )
    }
    withClue("re-rendered URI for '${vector.name}' vs reference canonical form") {
        rendered shouldBe canonical
    }
}

private fun checkInvalidVector(vector: InvalidVector) {
    val result = try {
        parseVectorUri(vector.uri, vector.network)
    } catch (expected: ZIP321.Errors) {
        return // correctly rejected; v1 error *types* are deliberately not asserted
    } catch (t: Throwable) {
        throw AssertionError(
            "vector '${vector.name}' (reference error: ${vector.error}) was rejected, " +
                "but with ${t::class.qualifiedName} instead of a ZIP321.Errors: ${t.message}",
            t
        )
    }
    throw AssertionError(
        "vector '${vector.name}' (reference error: ${vector.error}) must be rejected " +
            "but parsed successfully as: $result (${vector.description})"
    )
}
