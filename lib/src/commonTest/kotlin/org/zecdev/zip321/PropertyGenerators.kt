package org.zecdev.zip321

import org.zecdev.zip321.model.IndexedPayment
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.isAsciiLetter
import org.zecdev.zip321.support.ReferenceAddressValidator

// Deterministic generators for the property-style round-trip tests in `PropertyTests.kt`. Mirrors
// (in spirit) the proptest strategies in librustzcash `components/zip321/src/lib.rs` `pub mod
// testing` (`arb_valid_memo`, `arb_zip321_payment`, `arb_zip321_request`) and the Swift reference
// port (`Tests/ZcashPaymentURITests/PropertyGenerators.swift`, S16), but built on a tiny inline
// seeded PRNG so every run is byte-for-byte deterministic across platforms — no wall-clock/system
// random seeding anywhere.

/**
 * A minimal SplitMix64 PRNG. Deterministic, fast, and portable: the same seed always produces the
 * same stream on every platform.
 */
@Suppress("MagicNumber")
class SplitMix64(seed: ULong) {
    private var state: ULong = seed

    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }

    /** A uniform value in the closed range [range] (slight modulo bias is immaterial here). */
    fun nextULong(range: ULongRange): ULong {
        val span = range.last - range.first
        if (span == 0uL) return range.first
        return range.first + (next() % (span + 1uL))
    }

    fun nextInt(range: IntRange): Int =
        nextULong(range.first.toULong()..range.last.toULong()).toInt()

    /** Returns `true` with roughly the given probability (`0.0..1.0`). */
    fun nextBool(probability: Double = 0.5): Boolean {
        val scaled = (probability.coerceIn(0.0, 1.0) * 1_000_000).toULong()
        return nextULong(0uL..999_999uL) < scaled
    }

    /** Picks a uniformly random element of a non-empty list. */
    fun <T> choice(list: List<T>): T = list[nextInt(0..(list.size - 1))]
}

/**
 * The fixed pool of KNOWN-VALID (checksum-verified) addresses per network, one of each recipient
 * kind ZIP-321 allows: transparent P2PKH, transparent P2SH, Sapling, Unified, TEX. Lifted verbatim
 * from the literals already exercised by
 * `lib/src/commonTest/kotlin/org/zecdev/zip321/parser/NetworkValidationTests.kt`'s
 * `validMatrix` (see that file's header for provenance of each address).
 */
object AddressPool {
    val mainnet: List<String> =
        listOf(
            "t1Hsc1LR8yKnbbe3twRp88p6vFfC5t7DLbs",
            "t3JZcvsuaXE6ygokL4XUiZSTrQBUoPYFnXJ",
            "zs1z7rejlpsa98s2rrrfkwmaxu53e4ue0ulcrw0h4x5g8jl04tak0d3mm47vdtahatqrlkngh9slya",
            "u1l8xunezsvhq8fgzfl7404m450nwnd76zshscn6nfys7vyz2ywyh4cc5daaq0c7q2su5lqfh23sp7fkf3kt27ve5948mzpfdvckzaect2jtte308mkwlycj2u0eac077wu70vqcetkxf",
            "tex1s2rt77ggv6q989lr49rkgzmh5slsksa9khdgte",
        )

    val testnet: List<String> =
        listOf(
            "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU",
            "t26YoyZ1iPgiMEWL4zGUm74eVWfhyDMXzY2",
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            "utest10c5kutapazdnf8ztl3pu43nkfsjx89fy3uuff8tsmxm6s86j37pe7uz94z5jhkl49pqe8yz75rlsaygexk6jpaxwx0esjr8wm5ut7d5s",
            "textest1qyqszqgpqyqszqgpqyqszqgpqyqszqgpfcjgfy",
        )

    /** Transparent kinds reuse the testnet version bytes on regtest. */
    val regtest: List<String> =
        listOf(
            "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU",
            "t26YoyZ1iPgiMEWL4zGUm74eVWfhyDMXzY2",
            "zregtestsapling1qqqqqqqqqqqqqqqqqqcguyvaw2vjk4sdyeg0lc970u659lvhqq7t0np6hlup5lusxle7505hlz3",
            "uregtest15xk7vj4grjkay6mnfl93dhsflc2yeunhxwdh38rul0rq3dfhzzxgm5szjuvtqdha4t4p2q02ks0jgzrhjkrav70z9xlvq0plpcjkd5z3",
            "texregtest1s2rt77ggv6q989lr49rkgzmh5slsksa990zqpk",
        )

    fun addresses(network: Network): List<String> =
        when (network) {
            Network.MAINNET -> mainnet
            Network.TESTNET -> testnet
            Network.REGTEST -> regtest
        }
}

object Gen {
    val allNetworks: List<Network> =
        listOf(Network.MAINNET, Network.TESTNET, Network.REGTEST)

    /**
     * Reserved ZIP-321 query keys that `otherParam` names must never collide with (matches
     * `OtherParam.create`'s `isReservedName`).
     */
    private val reservedParamNames: Set<String> = setOf("address", "amount", "label", "memo", "message")

    /**
     * A pool of characters/graphemes chosen to exercise every corner of the `qchar` grammar: plain
     * `unreserved` bytes, every `allowed-delims` character, bytes that MUST be percent-encoded
     * (space, `%`, `&`, `/`, control-adjacent punctuation, …), and non-ASCII text (accented Latin,
     * CJK, emoji — including a multi-scalar skin-tone cluster and a combining mark) to stress
     * multi-byte UTF-8 percent-encoding.
     */
    private val unicodePool: List<String> =
        listOf(
            "a", "Z", "5", "-", ".", "_", "~",
            "!", "$", "'", "(", ")", "*", "+", ",", ";",
            ":", "@",
            " ", "\"", "#", "%", "&", "/", "<", "=", ">", "?", "[", "\\", "]", "^", "`", "{", "|", "}",
            "é", "ñ", "中", "星", "🎉", "😀", "👍🏽", "𐍈", "́",
        )

    /** Arbitrary valid memo bytes: 0..512 raw bytes (matches the reference `arb_valid_memo`). */
    fun memoBytes(rng: SplitMix64): MemoBytes {
        val length = rng.nextInt(0..MemoBytes.maxLength)
        val bytes = ByteArray(length) { rng.nextULong(0uL..255uL).toByte() }
        // Never fails: `length <= MemoBytes.maxLength` by construction.
        return MemoBytes(bytes)
    }

    /**
     * Arbitrary `NonNegativeAmount` in `0...MAX_MONEY`, biased to also hit the exact boundary
     * values (0, 1, `MAX_MONEY`, `MAX_MONEY - 1`) that a purely uniform draw would rarely land on.
     *
     * The amount type is unsigned, so the whole draw stays in `ULong`: there is no sign to mask
     * off and no negative half of the range to fold back in.
     */
    fun zatoshi(rng: SplitMix64): NonNegativeAmount {
        val value: ULong =
            when (rng.nextInt(0..9)) {
                0 -> 0uL
                1 -> NonNegativeAmount.MAX_MONEY
                2 -> 1uL
                3 -> NonNegativeAmount.MAX_MONEY - 1uL
                else -> rng.nextULong(0uL..NonNegativeAmount.MAX_MONEY)
            }
        // Never fails: `value` is always in `0..MAX_MONEY` by construction.
        return NonNegativeAmount.zatoshi(value).getOrThrow()
    }

    /**
     * An arbitrary label/message/otherParam-value string: unicode text (including emoji and
     * characters that require percent-encoding), possibly empty.
     */
    fun unicodeString(rng: SplitMix64): String {
        val length = rng.nextInt(0..12)
        val result = StringBuilder()
        repeat(length) { result.append(rng.choice(unicodePool)) }
        return result.toString()
    }

    /**
     * An arbitrary valid `paramname` (`ALPHA *(ALPHA / DIGIT / "+" / "-")`) that does not collide
     * with a reserved ZIP-321 query key and does not carry the `req-` prefix.
     */
    fun paramName(rng: SplitMix64): String {
        val letters = ('a'..'z') + ('A'..'Z')
        val nameChars = letters + ('0'..'9') + listOf('+', '-')

        while (true) {
            val length = rng.nextInt(1..8)
            val chars = StringBuilder()
            chars.append(rng.choice(letters))
            repeat(length - 1) { chars.append(rng.choice(nameChars)) }
            val name = chars.toString()
            if (name.startsWith("req-") || reservedParamNames.contains(name)) {
                continue
            }
            check(name.first().isAsciiLetter())
            return name
        }
    }

    /**
     * An arbitrary valid `Payment` to one of `AddressPool`'s known-valid recipients on [network].
     *
     * Note: an `amount` is ALWAYS attached (mirroring the reference `arb_zip321_payment`, which
     * sets `amount: Some(amount)` unconditionally): this sidesteps the single-payment/empty-query
     * "bare address" collapse (see `Render.request`'s `omitAddressLabel` special case) that would
     * otherwise make the full round-trip law ambiguous between `.Request` and `.SingleAddress`.
     */
    fun payment(
        rng: SplitMix64,
        network: Network,
    ): Payment {
        val addressString = rng.choice(AddressPool.addresses(network))
        // The generator goes through the same delegation path production callers use: the
        // test-only reference validator is the authority, and the descriptor it returns is what
        // the payment rules below consult.
        val recipient =
            RecipientAddress.create(addressString, ReferenceAddressValidator.of(network))
                ?: throw IllegalStateException(
                    "AddressPool entry '$addressString' failed to validate on $network — pool is stale.",
                )

        var amount = zatoshi(rng)
        if (recipient.isTransparent && amount.value == 0uL) {
            // Zero-valued transparent outputs are disallowed by consensus.
            amount = NonNegativeAmount.zatoshi(1uL).getOrThrow()
        }

        val memo: MemoBytes? =
            if (recipient.canReceiveMemos && rng.nextBool(0.5)) memoBytes(rng) else null

        val label: String? = if (rng.nextBool(0.4)) unicodeString(rng) else null
        val message: String? = if (rng.nextBool(0.4)) unicodeString(rng) else null

        val otherParams = mutableListOf<OtherParam>()
        val usedNames = mutableSetOf<String>()
        val otherCount = rng.nextInt(0..3)
        repeat(otherCount) {
            var name = paramName(rng)
            while (usedNames.contains(name)) {
                name = paramName(rng)
            }
            usedNames.add(name)
            val value: String? = if (rng.nextBool(0.5)) unicodeString(rng) else null
            // Never fails: `name` is always a valid, non-reserved paramname.
            otherParams.add(OtherParam.create(name, value).getOrThrow())
        }

        return Payment.create(
            recipientAddress = recipient,
            amount = amount,
            memo = memo,
            label = label,
            message = message,
            otherParams = otherParams,
        ).getOrThrow()
    }

    /**
     * An arbitrary `PaymentRequest` of 0...20 payments at sparse, unique `paramindex` values in
     * `0...9999` (mirroring the reference `arb_zip321_request`'s `btree_map(0usize..10000, …,
     * 1..10)`, extended down to 0 payments to also exercise the empty request).
     */
    fun indexedPaymentRequest(
        rng: SplitMix64,
        network: Network,
    ): PaymentRequest {
        val count = rng.nextInt(0..20)
        val usedIndices = mutableSetOf<UInt>()
        val indexed = mutableListOf<IndexedPayment>()

        repeat(count) {
            var index: UInt
            do {
                index = rng.nextInt(0..9999).toUInt()
            } while (usedIndices.contains(index))
            usedIndices.add(index)
            indexed.add(IndexedPayment(index, payment(rng, network)))
        }

        // Never fails: indices are unique by construction and `<= 9999`.
        return PaymentRequest.fromIndexedPayments(indexed)
    }

    /**
     * A DELIBERATELY INVALID other-param list: 1..4 distinct names with one of them repeated at an
     * arbitrary later position (so the duplicate is not always adjacent or trailing). Constructing
     * a [Payment] from this must fail.
     */
    fun duplicatedOtherParams(rng: SplitMix64): Pair<List<OtherParam>, String> {
        val names = mutableListOf<String>()
        val used = mutableSetOf<String>()
        val count = rng.nextInt(1..4)

        repeat(count) {
            var name = paramName(rng)
            while (used.contains(name)) {
                name = paramName(rng)
            }
            used.add(name)
            names.add(name)
        }

        val repeatedIndex = rng.nextInt(0..(names.size - 1))
        val repeated = names[repeatedIndex]
        names.add(rng.nextInt((repeatedIndex + 1)..names.size), repeated)

        val params =
            names.map { name ->
                val value: String? = if (rng.nextBool(0.5)) unicodeString(rng) else null
                // Never fails: `name` is always a valid, non-reserved paramname.
                OtherParam.create(name, value).getOrThrow()
            }

        return Pair(params, repeated)
    }

    /**
     * An arbitrary known-valid recipient on [network], resolved through the reference validator —
     * the same delegation path a production caller uses.
     */
    fun recipient(
        rng: SplitMix64,
        network: Network,
    ): RecipientAddress {
        val addressString = rng.choice(AddressPool.addresses(network))
        // Never fails: the pool holds only checksum-valid addresses.
        return requireNotNull(RecipientAddress.create(addressString, ReferenceAddressValidator.of(network)))
    }
}
