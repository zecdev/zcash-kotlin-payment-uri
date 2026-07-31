package org.zecdev.zip321.support

import org.zecdev.zip321.AddressDescriptor
import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network
import org.zecdev.zip321.model.RecipientAddress

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * An [AddressValidator] implemented on top of the test-only [Bech32] /
 * [Base58Check] reference checkers. It plays, for the test suite, the role a
 * wallet SDK plays in production: it is the authority on which address strings
 * are acceptable recipients, which network they belong to, and what they can
 * receive.
 *
 * It exists because the shared conformance corpus contains vectors whose whole
 * point is checksum corruption, mixed-case Bech32, wrong-network addresses and
 * Sprout recipients. Those vectors are only meaningful against a validator that
 * actually verifies encodings — and the library, by design, no longer contains
 * one. Putting it here is exactly the boundary the design intends: the corpus
 * proves the library rejects what the VALIDATOR rejects.
 *
 * Every constant below (HRPs, Base58Check version bytes) is taken from
 * librustzcash `zcash_protocol/src/constants/{mainnet,testnet,regtest}.rs`,
 * [ZIP-316](https://zips.z.cash/zip-0316) (Unified) and
 * [ZIP-320](https://zips.z.cash/zip-0320) (TEX).
 *
 * Accepts, for its [network]:
 *
 * - **Sapling** payment addresses: Bech32 (classic) checksum with the network's
 *   HRP (`zs` / `ztestsapling` / `zregtestsapling`). Shielded: can receive
 *   memos.
 * - **Unified** addresses: Bech32m checksum with the network's HRP
 *   (`u` / `utest` / `uregtest`). The payload is opaque here; no ZIP-316
 *   receiver decoding is performed, and a Revision 0 UA always carries a
 *   shielded receiver, so these are reported as memo-capable.
 * - **TEX** addresses (ZIP-320): Bech32m checksum with the network's HRP
 *   (`tex` / `textest` / `texregtest`). Transparent-source-only: transparent,
 *   no memos.
 * - **Transparent** P2PKH / P2SH: Base58Check (SHA-256d) whose decoded payload
 *   starts with the network's version bytes. Regtest reuses the testnet bytes.
 *
 * **Sprout addresses are always rejected** — even with a valid Base58Check
 * checksum — because ZIP-321 disallows Sprout recipients. Mixed-case Bech32
 * strings are rejected per BIP-173. A checksum-valid address of another network
 * is rejected: this validator only speaks for [network].
 */
@Suppress("MagicNumber")
internal class ReferenceAddressValidator(private val network: Network) : AddressValidator {
    override fun validate(address: String): AddressDescriptor? {
        // Sprout is checked by prefix BEFORE any generic Base58Check matching,
        // so that a checksum-valid Sprout address is still rejected.
        if (isSprout(address)) return null

        // Bech32/Bech32m kinds: one decode both verifies the checksum and
        // rejects mixed case; the (HRP, variant) pair must then match one of
        // this network's shielded/TEX encodings exactly.
        val decoded = Bech32.decode(address)
        if (decoded != null) {
            return when (decoded.hrp to decoded.variant) {
                saplingHRP to Bech32.Variant.BECH32,
                unifiedHRP to Bech32.Variant.BECH32M,
                -> AddressDescriptor(network, isTransparent = false, canReceiveMemos = true)
                texHRP to Bech32.Variant.BECH32M,
                -> AddressDescriptor(network, isTransparent = true, canReceiveMemos = false)
                // Checksum-valid but wrong HRP for this network, or the wrong
                // checksum variant for the kind its HRP claims.
                else -> null
            }
        }

        // Transparent kinds: Base58Check with this network's version bytes.
        if (!Base58Check.verify(address, listOf(p2pkhVersionBytes, p2shVersionBytes))) return null

        return AddressDescriptor(network, isTransparent = true, canReceiveMemos = false)
    }

    // -- Network constants ---------------------------------------------------

    /** Sprout HRPs. `zt` collides with `ztestsapling`, hence the exclusion. */
    private fun isSprout(address: String): Boolean =
        when (network) {
            Network.MAINNET -> address.startsWith("zc")
            Network.TESTNET, Network.REGTEST ->
                address.startsWith("zt") && !address.startsWith("ztestsapling")
        }

    /**
     * Bech32 human-readable part of Sapling payment addresses, per the Zcash
     * protocol specification (§5.6.4) and `HRP_SAPLING_PAYMENT_ADDRESS`.
     */
    private val saplingHRP: String
        get() =
            when (network) {
                Network.MAINNET -> "zs"
                Network.TESTNET -> "ztestsapling"
                Network.REGTEST -> "zregtestsapling"
            }

    /** Bech32m human-readable part of Unified Addresses, per ZIP-316. */
    private val unifiedHRP: String
        get() =
            when (network) {
                Network.MAINNET -> "u"
                Network.TESTNET -> "utest"
                Network.REGTEST -> "uregtest"
            }

    /** Bech32m human-readable part of TEX addresses, per ZIP-320. */
    private val texHRP: String
        get() =
            when (network) {
                Network.MAINNET -> "tex"
                Network.TESTNET -> "textest"
                Network.REGTEST -> "texregtest"
            }

    /**
     * `B58_PUBKEY_ADDRESS_PREFIX` version bytes (P2PKH). Regtest reuses
     * testnet's.
     */
    private val p2pkhVersionBytes: ByteArray
        get() =
            when (network) {
                Network.MAINNET -> byteArrayOf(0x1c, 0xb8.toByte()) // t1…
                Network.TESTNET, Network.REGTEST -> byteArrayOf(0x1d, 0x25) // tm…
            }

    /**
     * `B58_SCRIPT_ADDRESS_PREFIX` version bytes (P2SH). Regtest reuses
     * testnet's.
     */
    private val p2shVersionBytes: ByteArray
        get() =
            when (network) {
                Network.MAINNET -> byteArrayOf(0x1c, 0xbd.toByte()) // t3…
                Network.TESTNET, Network.REGTEST -> byteArrayOf(0x1c, 0xba.toByte()) // t2…
            }

    companion object {
        /** The mainnet reference validator. */
        val MAINNET = ReferenceAddressValidator(Network.MAINNET)

        /** The testnet reference validator. */
        val TESTNET = ReferenceAddressValidator(Network.TESTNET)

        /** The regtest reference validator. */
        val REGTEST = ReferenceAddressValidator(Network.REGTEST)

        /** The reference validator for [network]. */
        fun of(network: Network): ReferenceAddressValidator =
            when (network) {
                Network.MAINNET -> MAINNET
                Network.TESTNET -> TESTNET
                Network.REGTEST -> REGTEST
            }
    }
}

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * Wraps a checksum-valid fixture address as a [RecipientAddress], using the
 * [ReferenceAddressValidator] for [network] as the authority — the same
 * delegation path production callers use, applied to test fixtures.
 *
 * Fails loudly rather than returning `null`: every address a test names is
 * meant to be a real, checksum-valid address of [network], so a rejection is a
 * broken fixture, not an expected outcome.
 */
internal fun validRecipient(
    value: String,
    network: Network = Network.TESTNET,
): RecipientAddress =
    requireNotNull(RecipientAddress.create(value, ReferenceAddressValidator.of(network))) {
        "test fixture is not a checksum-valid $network address: $value"
    }
