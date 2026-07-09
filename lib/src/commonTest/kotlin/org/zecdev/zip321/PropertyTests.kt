package org.zecdev.zip321

import org.zecdev.zip321.encodings.QCharCodec
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.Test
import kotlin.test.assertEquals

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
    // parse(uriString(from = r)) == success(r) for every generated PaymentRequest r.

    @Test
    fun `Law 1 - full round trip`() {
        for (seed in 0 until 300) {
            val r = rng(1, seed)
            val network = r.choice(Gen.allNetworks)
            val request = Gen.indexedPaymentRequest(r, network)

            val uri = ZIP321.uriString(from = request)
            val parsed = ZIP321.parse(uri, network, ReferenceAddressValidator.of(network))

            assertEquals(
                Result.success(request),
                parsed,
                "seed $seed ($network): round-trip law violated for uri: $uri",
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
            val network = r.choice(Gen.allNetworks)
            val request = Gen.indexedPaymentRequest(r, network)

            val uri = ZIP321.uriString(from = request)
            val reparsedRequest = ZIP321.parse(uri, network, ReferenceAddressValidator.of(network)).getOrThrow()

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

    // MARK: - Law 6: the parsed model never distinguishes the two spellings
    //
    // parse("zcash:<addr>") == parse("zcash:?address=<addr>") for every generated recipient. The
    // URI syntax is a rendering choice; it must not leak into the model.

    @Test
    fun `Law 6 - single-recipient spellings agree`() {
        for (seed in 0 until 200) {
            val r = rng(6, seed)
            val network = r.choice(Gen.allNetworks)
            val validator = ReferenceAddressValidator.of(network)
            val recipient = Gen.recipient(r, network)

            val leading = ZIP321.parse("zcash:${recipient.value}", network, validator)
            val labeled = ZIP321.parse("zcash:?address=${recipient.value}", network, validator)

            assertEquals(
                leading,
                labeled,
                "seed $seed ($network): the two single-recipient spellings parsed differently " +
                    "for ${recipient.value}",
            )
        }
    }

    // MARK: - Law 7: `otherParams` is always a list
    //
    // Every payment of every generated request reports a LIST of other params. There is no
    // absent/empty split left to observe, at construction or after a round trip.

    @Test
    fun `Law 7 - otherParams is always a list`() {
        for (seed in 0 until 200) {
            val r = rng(7, seed)
            val network = r.choice(Gen.allNetworks)
            val request = Gen.indexedPaymentRequest(r, network)

            val uri = ZIP321.uriString(from = request)
            val reparsed = ZIP321.parse(uri, network, ReferenceAddressValidator.of(network)).getOrThrow()

            for ((original, roundTripped) in request.payments.zip(reparsed.payments)) {
                // Same count, same order, same contents — and never "absent".
                assertEquals(
                    original.otherParams,
                    roundTripped.otherParams,
                    "seed $seed: other params changed across the round trip",
                )
            }
        }
    }

    // MARK: - Law 8 (adversarial): duplicate other-param names never construct
    //
    // A `Payment` whose other-param names repeat must be rejected, so that no `Payment` can exist
    // which renders to a URI the parser would reject as a duplicate parameter.

    @Test
    fun `Law 8 - duplicate other-param names never construct`() {
        for (seed in 0 until 200) {
            val r = rng(8, seed)
            val network = r.choice(Gen.allNetworks)
            val recipient = Gen.recipient(r, network)
            val (params, repeated) = Gen.duplicatedOtherParams(r)

            val result =
                Payment.create(
                    recipientAddress = recipient,
                    amount = null,
                    memo = null,
                    label = null,
                    message = null,
                    otherParams = params,
                )

            assertEquals(
                ZIP321Error.DuplicateParameter(repeated, null),
                result.exceptionOrNull(),
                "seed $seed: duplicate other-param '$repeated' was accepted " +
                    "(params: ${params.map { it.name }})",
            )

            // The builder path must agree: it feeds the same list to Payment.create.
            val builder = Payment.Builder(recipient)
            params.forEach { builder.otherParam(it.name, it.value) }
            assertEquals(
                ZIP321Error.DuplicateParameter(repeated, null),
                builder.build().exceptionOrNull(),
                "seed $seed: the builder accepted duplicate other-param '$repeated'",
            )
        }
    }
}
