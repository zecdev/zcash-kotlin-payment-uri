package org.zecdev.zip321.support

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * A dependency-free Base58Check decoder-verifier used to structurally
 * validate transparent Zcash addresses (P2PKH / P2SH). Base58Check is the
 * Bitcoin/Zcash encoding of `version-bytes || payload || checksum`, where the
 * checksum is the first 4 bytes of SHA-256d over `version-bytes || payload`.
 *
 * Port of the Swift reference's test-support `Base58Check.swift`. It verifies
 * transparent-address checksums for the test suite's own reference address
 * validator; the library itself decodes no addresses.
 */
@Suppress("MagicNumber")
internal object Base58Check {
    /** Base58 alphabet (Bitcoin/Zcash ordering). Note the deliberately
     * omitted visually-ambiguous characters: `0`, `O`, `I`, `l`. */
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /** Reverse lookup: ASCII code point -> base58 digit value, or -1 if invalid. */
    private val ALPHABET_REVERSE: IntArray =
        IntArray(128) { -1 }.also { table ->
            ALPHABET.forEachIndexed { value, char -> table[char.code] = value }
        }

    /**
     * Decodes a Base58Check string and verifies its 4-byte SHA-256d checksum.
     *
     * @return the decoded payload (version bytes followed by data) with the
     *   4-byte checksum removed, or `null` if the string contains characters
     *   outside the base58 alphabet, is too short to contain a checksum, or
     *   fails checksum verification.
     */
    @Suppress("ReturnCount")
    fun decode(s: String): ByteArray? {
        val raw = base58Decode(s) ?: return null
        // Need at least the 4-byte checksum (a real address also has version
        // bytes + data, but 4 is the hard minimum to even split).
        if (raw.size < 4) return null

        val payload = raw.copyOfRange(0, raw.size - 4)
        val checksum = raw.copyOfRange(raw.size - 4, raw.size)
        val computed = Sha256.doubleHash(payload).copyOfRange(0, 4)
        if (!computed.contentEquals(checksum)) return null
        return payload
    }

    /**
     * Convenience: verifies that [s] is a valid Base58Check string whose
     * decoded payload begins with one of the accepted version-byte prefixes.
     *
     * Transparent-address version bytes (from librustzcash
     * `zcash_protocol/src/constants/{mainnet,testnet,regtest}.rs`):
     *
     * | network  | P2PKH (`B58_PUBKEY_ADDRESS_PREFIX`) | P2SH (`B58_SCRIPT_ADDRESS_PREFIX`) |
     * |----------|--------------------------------------|-------------------------------------|
     * | mainnet  | `[0x1c, 0xb8]` (`t1…`)                | `[0x1c, 0xbd]` (`t3…`)               |
     * | testnet  | `[0x1d, 0x25]` (`tm…`)                | `[0x1c, 0xba]` (`t2…`)               |
     * | regtest  | `[0x1d, 0x25]` (same as testnet)      | `[0x1c, 0xba]` (same as testnet)     |
     */
    fun verify(
        s: String,
        expectedVersionBytes: List<ByteArray>,
    ): Boolean {
        val payload = decode(s) ?: return false
        return expectedVersionBytes.any { prefix ->
            payload.size >= prefix.size && payload.copyOfRange(0, prefix.size).contentEquals(prefix)
        }
    }

    // MARK: - Base58 big-integer decode (no BigInt dependency)

    /**
     * Decodes a base58 string into its big-endian byte representation,
     * preserving leading-zero bytes (encoded as leading `'1'` characters).
     */
    @Suppress("ReturnCount")
    private fun base58Decode(s: String): ByteArray? {
        if (s.isEmpty()) return null

        // Big-endian base-256 accumulator built up digit by digit.
        val bytes = mutableListOf<Byte>()
        for (ch in s) {
            val code = ch.code
            if (code >= 128) return null
            val digit = ALPHABET_REVERSE[code]
            if (digit < 0) return null

            // bytes = bytes * 58 + digit
            var carry = digit
            var i = bytes.size - 1
            while (i >= 0) {
                carry += 58 * (bytes[i].toInt() and 0xff)
                bytes[i] = (carry and 0xff).toByte()
                carry = carry shr 8
                i--
            }
            while (carry > 0) {
                bytes.add(0, (carry and 0xff).toByte())
                carry = carry shr 8
            }
        }

        // Each leading '1' represents a leading zero byte.
        var leadingZeros = 0
        for (ch in s) {
            if (ch == '1') leadingZeros++ else break
        }

        return ByteArray(leadingZeros) + bytes.toByteArray()
    }
}
