package org.zecdev.zip321

import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The address-validation DELEGATION contract, and the test-only
 * [ReferenceAddressValidator] that stands in for a wallet SDK.
 *
 * The library implements the ZIP-321 URI grammar and nothing else. Whether a
 * recipient string is a valid, payable Zcash address — and what that recipient
 * can receive — is answered exclusively by the caller-supplied
 * [AddressValidator], and the library takes that answer as final. These tests
 * pin both halves of that contract: what the library does with a validator's
 * verdict, and that the reference validator the rest of the suite injects is
 * itself correct.
 */
class AddressValidationTests {
    // -- Address fixtures ----------------------------------------------------

    companion object {
        const val SAPLING_MAINNET =
            "zs1z7rejlpsa98s2rrrfkwmaxu53e4ue0ulcrw0h4x5g8jl04tak0d3mm47vdtahatqrlkngh9slya"
        const val SAPLING_TESTNET =
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        const val SAPLING_REGTEST =
            "zregtestsapling1qqqqqqqqqqqqqqqqqqcguyvaw2vjk4sdyeg0lc970u659lvhqq7t0np6hlup5lusxle7505hlz3"

        const val UNIFIED_MAINNET =
            "u1l8xunezsvhq8fgzfl7404m450nwnd76zshscn6nfys7vyz2ywyh4cc5daaq0c7q2su5lqfh23sp7fkf3kt27ve59" +
                "48mzpfdvckzaect2jtte308mkwlycj2u0eac077wu70vqcetkxf"
        const val UNIFIED_TESTNET =
            "utest10c5kutapazdnf8ztl3pu43nkfsjx89fy3uuff8tsmxm6s86j37pe7uz94z5jhkl49pqe8yz75rlsaygexk6jpaxwx0esjr8wm5ut7d5s"
        const val UNIFIED_REGTEST =
            "uregtest15xk7vj4grjkay6mnfl93dhsflc2yeunhxwdh38rul0rq3dfhzzxgm5szjuvtqdha4t4p2q02ks0jgzrhjkrav70z9xlvq0plpcjkd5z3"

        const val TEX_MAINNET = "tex1s2rt77ggv6q989lr49rkgzmh5slsksa9khdgte"
        const val TEX_TESTNET = "textest1qyqszqgpqyqszqgpqyqszqgpqyqszqgpfcjgfy"
        const val TEX_REGTEST = "texregtest1s2rt77ggv6q989lr49rkgzmh5slsksa990zqpk"

        const val P2PKH_MAINNET = "t1Hsc1LR8yKnbbe3twRp88p6vFfC5t7DLbs"
        const val P2SH_MAINNET = "t3JZcvsuaXE6ygokL4XUiZSTrQBUoPYFnXJ"
        const val P2PKH_TESTNET = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        const val P2SH_TESTNET = "t26YoyZ1iPgiMEWL4zGUm74eVWfhyDMXzY2"

        const val SPROUT_MAINNET =
            "zcU1Cd6zYyZCd2VJF8yKgmzjxdiiU1rgTTjEwoN1CGUWCziPkUTXUjXmX7TMqdMNsTfuiGN1jQoVN4kGxUR4sAPN4XZ7pxb"
        const val SPROUT_TESTNET =
            "ztJ1EWLKcGwF2S4NA17pAJVdco8Sdkz4AQPxt1cLTEfNuyNswJJc2BbBqYrsRZsp31xbVZwhF7c7a2L9jsF3p3ZwRWpqqyS"

        val ALL_NETWORKS = listOf(Network.MAINNET, Network.TESTNET, Network.REGTEST)

        /**
         * Every checksum-valid address, the ONLY network on which it must
         * validate, and the capabilities it must be reported with.
         */
        val VALID_MATRIX: List<MatrixEntry> =
            listOf(
                MatrixEntry(SAPLING_MAINNET, Network.MAINNET, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(SAPLING_TESTNET, Network.TESTNET, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(SAPLING_REGTEST, Network.REGTEST, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(UNIFIED_MAINNET, Network.MAINNET, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(UNIFIED_TESTNET, Network.TESTNET, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(UNIFIED_REGTEST, Network.REGTEST, isTransparent = false, canReceiveMemos = true),
                MatrixEntry(TEX_MAINNET, Network.MAINNET, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(TEX_TESTNET, Network.TESTNET, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(TEX_REGTEST, Network.REGTEST, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(P2PKH_MAINNET, Network.MAINNET, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(P2SH_MAINNET, Network.MAINNET, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(P2PKH_TESTNET, Network.TESTNET, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(P2SH_TESTNET, Network.TESTNET, isTransparent = true, canReceiveMemos = false),
                // Regtest transparent addresses use the TESTNET version bytes
                // (librustzcash zcash_protocol/src/constants/regtest.rs).
                MatrixEntry(P2PKH_TESTNET, Network.REGTEST, isTransparent = true, canReceiveMemos = false),
                MatrixEntry(P2SH_TESTNET, Network.REGTEST, isTransparent = true, canReceiveMemos = false),
            )

        /**
         * Valid addresses with the last character substituted within their
         * charset: the structure and HRP/leading symbols remain plausible but
         * the checksum no longer verifies.
         */
        val CORRUPTED: List<Pair<String, Network>> =
            listOf(
                // last char z → q (corpus vector invalid_address_sapling_bad_checksum)
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2keq" to Network.TESTNET,
                SAPLING_MAINNET.dropLast(1).plus("q") to Network.MAINNET,
                SAPLING_REGTEST.dropLast(1).plus("q") to Network.REGTEST,
                // corpus vector invalid_address_unified_mainnet_bad_checksum
                UNIFIED_MAINNET.dropLast(1).plus("q") to Network.MAINNET,
                UNIFIED_TESTNET.dropLast(1).plus("q") to Network.TESTNET,
                UNIFIED_REGTEST.dropLast(1).plus("q") to Network.REGTEST,
                TEX_MAINNET.dropLast(1).plus("q") to Network.MAINNET,
                TEX_TESTNET.dropLast(1).plus("q") to Network.TESTNET,
                TEX_REGTEST.dropLast(1).plus("q") to Network.REGTEST,
                // corpus vector invalid_address_transparent_bad_checksum
                "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovp1" to Network.TESTNET,
                P2PKH_MAINNET.dropLast(1).plus("t") to Network.MAINNET,
                P2SH_MAINNET.dropLast(1).plus("K") to Network.MAINNET,
                P2SH_TESTNET.dropLast(1).plus("3") to Network.TESTNET,
            )

        val GARBAGE =
            listOf(
                "",
                "asdf",
                "zs1",
                "u1",
                "t1",
                "not an address at all",
                // valid bech32 checksum but an HRP no Zcash network uses (BIP-173 vector)
                "a12uel5l",
            )
    }

    /** One row of [VALID_MATRIX]. */
    data class MatrixEntry(
        val address: String,
        val network: Network,
        val isTransparent: Boolean,
        val canReceiveMemos: Boolean,
    )

    // -- The delegation contract ---------------------------------------------

    /**
     * The library owns the URI grammar and nothing else: a string that is not
     * remotely a Zcash address parses fine when the caller's validator says it
     * is a recipient. Nothing in the library gets a second opinion.
     */
    @Test
    fun `the validator can accept what is not even an address`() {
        val validator =
            AddressValidator {
                AddressDescriptor(Network.MAINNET, isTransparent = false, canReceiveMemos = true)
            }

        val request =
            ZIP321.parse("zcash:notanaddressatall?amount=1", Network.MAINNET, validator).getOrThrow()

        assertEquals("notanaddressatall", request.payments.first().recipientAddress.value)
    }

    /**
     * Conversely, a rejection is final: a perfectly well-formed, checksum-valid
     * address is invalid if the caller's validator says so.
     */
    @Test
    fun `the validator can reject a well-formed address`() {
        assertIs<ZIP321Error.InvalidAddress>(
            ZIP321.parse(
                "zcash:?address=$SAPLING_TESTNET&amount=1",
                Network.TESTNET,
                AddressValidator { null },
            ).exceptionOrNull(),
        )
    }

    /**
     * The capabilities the library enforces (here, memo support) are read off
     * the descriptor the validator returned, not derived from the address
     * string.
     */
    @Test
    fun `payment rules consume the descriptor not the address string`() {
        val memoURI = "zcash:$P2PKH_MAINNET?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"

        // A `t1…`-looking string the validator declares memo-capable: the memo
        // is accepted, because the descriptor is what counts.
        val asShielded =
            AddressValidator {
                AddressDescriptor(Network.MAINNET, isTransparent = false, canReceiveMemos = true)
            }
        ZIP321.parse(memoURI, Network.MAINNET, asShielded).getOrThrow()

        // The same URI with a validator that reports a recipient that cannot
        // receive memos is rejected by the ZIP-321 transparent-memo rule.
        val asTransparent =
            AddressValidator {
                AddressDescriptor(Network.MAINNET, isTransparent = true, canReceiveMemos = false)
            }
        assertIs<ZIP321Error.TransparentMemo>(
            ZIP321.parse(memoURI, Network.MAINNET, asTransparent).exceptionOrNull(),
        )
    }

    /**
     * Every address in the URI is offered to the validator, including the
     * leading (unlabeled) one.
     */
    @Test
    fun `every recipient is offered to the validator`() {
        val seen = mutableListOf<String>()
        val validator =
            AddressValidator { address ->
                seen.add(address)
                AddressDescriptor(Network.TESTNET, isTransparent = false, canReceiveMemos = true)
            }

        ZIP321.parse(
            "zcash:$SAPLING_TESTNET?amount=1&address.1=$P2PKH_TESTNET&amount.1=2",
            Network.TESTNET,
            validator,
        ).getOrThrow()

        assertEquals(listOf(SAPLING_TESTNET, P2PKH_TESTNET), seen)
    }

    // -- The expected network ------------------------------------------------

    /**
     * The library asks for ONE expected network. A validator that accepts an
     * address but places it on another network makes the request invalid: the
     * library compares `descriptor.network` against `expecting` and reports an
     * invalid address.
     */
    @Test
    fun `address on another network is rejected`() {
        // The validator accepts and reports testnet; the request expects mainnet.
        val testnetSayingValidator =
            AddressValidator {
                AddressDescriptor(Network.TESTNET, isTransparent = false, canReceiveMemos = true)
            }

        val uri = "zcash:?address=$SAPLING_TESTNET&amount=1"

        assertIs<ZIP321Error.InvalidAddress>(
            ZIP321.parse(uri, Network.MAINNET, testnetSayingValidator).exceptionOrNull(),
        )

        // The same URI and validator against the matching network parses.
        ZIP321.parse(uri, Network.TESTNET, testnetSayingValidator).getOrThrow()
    }

    /** The mismatch is reported for indexed recipients too, carrying the index. */
    @Test
    fun `address on another network is rejected at its paramindex`() {
        val mixedNetworkValidator =
            AddressValidator { address ->
                AddressDescriptor(
                    if (address == SAPLING_TESTNET) Network.TESTNET else Network.MAINNET,
                    isTransparent = false,
                    canReceiveMemos = true,
                )
            }

        val uri = "zcash:?address=$SAPLING_TESTNET&amount=1&address.1=$SAPLING_MAINNET&amount.1=2"

        val error =
            assertIs<ZIP321Error.InvalidAddress>(
                ZIP321.parse(uri, Network.TESTNET, mixedNetworkValidator).exceptionOrNull(),
            )
        assertEquals(1u, error.index)
    }

    // -- The reference (test-only) validator: valid matrix --------------------

    @Test
    fun `valid address accepted on its network with the expected capabilities`() {
        for (entry in VALID_MATRIX) {
            val descriptor =
                assertNotNull(
                    ReferenceAddressValidator.of(entry.network).validate(entry.address),
                    "${entry.address} should be valid on ${entry.network}",
                )
            assertEquals(entry.network, descriptor.network)
            assertEquals(entry.isTransparent, descriptor.isTransparent, entry.address)
            assertEquals(entry.canReceiveMemos, descriptor.canReceiveMemos, entry.address)
        }
    }

    /**
     * A checksum-valid address must be rejected by every network it does not
     * belong to. (Transparent testnet addresses belong to BOTH testnet and
     * regtest, which share version bytes.)
     */
    @Test
    fun `wrong network rejected`() {
        for (network in ALL_NETWORKS) {
            val allowed = VALID_MATRIX.filter { it.network == network }.map { it.address }.toSet()
            for (entry in VALID_MATRIX) {
                if (entry.address in allowed) continue
                assertNull(
                    ReferenceAddressValidator.of(network).validate(entry.address),
                    "${entry.address} must be invalid on $network",
                )
            }
        }
    }

    // -- The reference validator: rejections ---------------------------------

    @Test
    fun `corrupted checksum rejected`() {
        for ((address, network) in CORRUPTED) {
            assertNull(
                ReferenceAddressValidator.of(network).validate(address),
                "corrupted $address must be invalid on $network",
            )
        }
    }

    /**
     * Sprout addresses have VALID Base58Check checksums and must still be
     * rejected on every network: ZIP-321 disallows Sprout recipients.
     */
    @Test
    fun `sprout rejected even with a valid checksum`() {
        for (network in ALL_NETWORKS) {
            assertNull(ReferenceAddressValidator.of(network).validate(SPROUT_MAINNET))
            assertNull(ReferenceAddressValidator.of(network).validate(SPROUT_TESTNET))
        }
    }

    @Test
    fun `mixed case bech32 rejected`() {
        // Corpus vector invalid_address_sapling_mixed_case: '0yy' → '0YY'.
        val mixedSapling =
            "ztestsapling10YY2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        assertNull(ReferenceAddressValidator.TESTNET.validate(mixedSapling))

        assertNull(ReferenceAddressValidator.MAINNET.validate(UNIFIED_MAINNET.dropLast(1).plus("F")))

        // ALL-uppercase Bech32 is permitted by BIP-173 and by the reference
        // zcash_address parser.
        assertNotNull(ReferenceAddressValidator.MAINNET.validate(TEX_MAINNET.uppercase()))
    }

    @Test
    fun `garbage rejected on all networks`() {
        for (input in GARBAGE) {
            for (network in ALL_NETWORKS) {
                assertNull(
                    ReferenceAddressValidator.of(network).validate(input),
                    "'$input' must be invalid on $network",
                )
            }
        }
    }

    // -- RecipientAddress through the reference validator --------------------

    @Test
    fun `RecipientAddress rejects a bad checksum`() {
        val corrupted =
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2keq"
        assertNull(RecipientAddress.create(corrupted, ReferenceAddressValidator.TESTNET))
    }

    @Test
    fun `RecipientAddress accepts a checksum-valid address`() {
        assertNotNull(RecipientAddress.create(SAPLING_REGTEST, ReferenceAddressValidator.REGTEST))
        assertNotNull(RecipientAddress.create(TEX_TESTNET, ReferenceAddressValidator.TESTNET))
    }
}
