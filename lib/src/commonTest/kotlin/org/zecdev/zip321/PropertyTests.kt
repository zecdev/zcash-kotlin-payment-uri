package org.zecdev.zip321

import org.zecdev.zip321.encodings.QCharCodec
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Deterministic property-style round-trip tests (K16, mirroring Swift's S16). Each law is run over
 * a fixed range of PRNG seeds by looping inside a single `kotlin.test` `@Test` function — unlike
 * Swift's `@Test(arguments:)`, `kotlin.test` has no built-in parameterized-test mechanism, so the
 * loop (and its per-seed failure message) lives directly in the test body. The same seeds produce
 * the same generated values on every run and every platform (see `PropertyGenerators.kt`'s
 * `SplitMix64`) — there is no wall-clock/system randomness anywhere in this file.
 *
 * Reference inspiration: librustzcash `components/zip321/src/lib.rs` `pub mod testing`
 * (`arb_valid_memo`, `arb_zip321_payment`, `arb_zip321_request`, lines ~864-955), and the Swift
 * reference port `Tests/ZcashPaymentURITests/PropertyTests.swift` (S16).
 */
class PropertyTests {
    // A fixed, arbitrary offset per law so that distinct laws draw from non-overlapping
    // SplitMix64 streams even when their seed integers coincide.
    private fun rng(
        law: Int,
        seed: Int,
    ): SplitMix64 = SplitMix64((law.toULong() shl 52) + seed.toULong())

    // MARK: - Law 1: full round trip
    //
    // parse(uriString(from = r)) == success(Request(r)) for every generated PaymentRequest r.

    @Test
    fun `Law 1 - full round trip`() {
        for (seed in 0 until 300) {
            val r = rng(1, seed)
            val context = r.choice(Gen.allNetworks)
            val request = Gen.indexedPaymentRequest(r, context)

            val uri = ZIP321.uriString(from = request)
            val parsed = ZIP321.parse(uri, context)

            assertEquals(
                Result.success(ParsedRequest.Request(request) as ParsedRequest),
                parsed,
                "seed $seed ($context): round-trip law violated for uri: $uri",
            )
        }
    }

    // MARK: - Law 2: NonNegativeAmount decimal round trip
    //
    // NonNegativeAmount.zec(z.decimalString()) == z for every generated NonNegativeAmount z.

    @Test
    fun `Law 2 - NonNegativeAmount decimal round trip`() {
        for (seed in 0 until 300) {
            val r = rng(2, seed)
            val z = Gen.zatoshi(r)

            val reparsed = NonNegativeAmount.zec(z.decimalString()).getOrThrow()

            assertEquals(z, reparsed, "seed $seed: NonNegativeAmount round-trip failed for ${z.decimalString()}")
        }
    }

    // MARK: - Law 3: MemoBytes base64url round trip
    //
    // MemoBytes.fromBase64URL(m.toBase64URL()) == m for every generated MemoBytes m.

    @Test
    fun `Law 3 - MemoBytes base64url round trip`() {
        for (seed in 0 until 300) {
            val r = rng(3, seed)
            val memo = Gen.memoBytes(r)

            val reparsed = MemoBytes.fromBase64URL(memo.toBase64URL())

            assertEquals(memo, reparsed, "seed $seed: MemoBytes round-trip failed")
        }
    }

    // MARK: - Law 4: QCharCodec round trip
    //
    // QCharCodec.decode(QCharCodec.encode(s)) == s for arbitrary unicode strings.

    @Test
    fun `Law 4 - QCharCodec round trip`() {
        for (seed in 0 until 300) {
            val r = rng(4, seed)
            val s = Gen.unicodeString(r)

            val decoded = QCharCodec.decode(QCharCodec.encode(s))

            assertEquals(s, decoded, "seed $seed: qchar round-trip failed for ${s.encodeToByteArray().decodeToString()}")
        }
    }

    // MARK: - Law 5: paramindex preservation
    //
    // A request with sparse indices round-trips preserving indexedPayments exactly — both the set
    // of indices and, at each index, the payment.

    @Test
    fun `Law 5 - paramindex preservation`() {
        for (seed in 0 until 200) {
            val r = rng(5, seed)
            val context = r.choice(Gen.allNetworks)
            val request = Gen.indexedPaymentRequest(r, context)

            val uri = ZIP321.uriString(from = request)
            val parsed = ZIP321.parse(uri, context).getOrThrow()
            val reparsedRequest =
                when (parsed) {
                    is ParsedRequest.Request -> parsed.request
                    is ParsedRequest.SingleAddress ->
                        fail("seed $seed ($context): expected a Request, got a SingleAddress for uri: $uri")
                }

            val originalIndices = request.indexedPayments.map { it.index }
            val reparsedIndices = reparsedRequest.indexedPayments.map { it.index }
            assertEquals(originalIndices, reparsedIndices, "seed $seed: paramindex set/order not preserved")

            for ((original, reparsed) in request.indexedPayments.zip(reparsedRequest.indexedPayments)) {
                assertEquals(original.index, reparsed.index)
                assertEquals(
                    original.payment,
                    reparsed.payment,
                    "seed $seed: payment at index ${original.index} changed across round trip",
                )
            }
        }
    }
}
