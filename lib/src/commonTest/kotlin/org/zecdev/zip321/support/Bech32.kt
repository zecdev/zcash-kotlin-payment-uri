package org.zecdev.zip321.support

/**
 * TEST SUPPORT — not part of the shipped library.
 *
 * A dependency-free Bech32 / Bech32m decoder-verifier, per BIP-173 and
 * BIP-350. Used to structurally validate Sapling (Bech32), TEX (Bech32m) and
 * Unified (Bech32m) Zcash addresses by confirming their human-readable
 * prefix, charset and BCH checksum.
 *
 * Length limit — why 1023 and not BIP-173's 90:
 * BIP-173 specifies an overall length cap of 90 characters, but that limit is
 * specific to the Bitcoin segwit use case; it is NOT a property of the
 * Bech32 construction itself. Zcash addresses (in particular Unified
 * Addresses) are routinely far longer than 90 characters, so librustzcash
 * does not apply the 90-char cap. `zcash_address` decodes Bech32/Bech32m via
 * the `bech32` crate (v0.11.0), whose per-`Checksum` code-length limit is:
 *
 *     bech32-0.11.0/src/primitives/mod.rs
 *       impl Checksum for Bech32  { const CODE_LENGTH: usize = 1023; ... }
 *       impl Checksum for Bech32m { const CODE_LENGTH: usize = 1023; ... }
 *
 * Sapling addresses (`Bech32`) and TEX addresses (`Bech32m`) inherit this
 * 1023-character limit. Unified Addresses use a custom checksum,
 * `Bech32mZip316` (zcash_address/src/kind/unified.rs), which raises the
 * limit to 4_194_368 (ZIP-316 l^MAX) but is otherwise Bech32m; real UAs are
 * well under 1023 characters in practice. We therefore mirror the standard
 * Bech32/Bech32m limit of 1023, which is the value that applies to every
 * address this verifier is intended to validate.
 *
 * Port of the Swift reference test-support `Bech32.swift`. It verifies
 * Sapling / Unified / TEX address checksums for the test suite's own
 * reference address validator, which is what makes the shared conformance
 * corpus's checksum-corruption vectors executable. The library itself
 * contains no address decoding of any kind.
 */
@Suppress("MagicNumber")
internal object Bech32 {
    /** The two BCH checksum variants and their target residues. */
    enum class Variant(val checksumConstant: Int) {
        BECH32(1),
        BECH32M(0x2bc830a3),
    }

    /** Maximum total encoded length accepted (see the object documentation). */
    const val MAX_LENGTH = 1023

    /** Bech32 data charset (BIP-173). */
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    /** Reverse lookup: ASCII code point -> 5-bit value, or -1 if not in the charset. */
    private val CHARSET_REVERSE: IntArray =
        IntArray(128) { -1 }.also { table ->
            CHARSET.forEachIndexed { value, char -> table[char.code] = value }
        }

    /**
     * The result of a successful [decode]: the lowercased human-readable
     * part, the decoded 5-bit data values (excluding the 6 checksum
     * characters), and which variant's checksum matched.
     *
     * [data] is compared/hashed by content (not reference identity).
     */
    class Decoded(val hrp: String, val data: ByteArray, val variant: Variant) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decoded) return false
            return hrp == other.hrp && variant == other.variant && data.contentEquals(other.data)
        }

        override fun hashCode(): Int = (hrp.hashCode() * 31 + variant.hashCode()) * 31 + data.contentHashCode()
    }

    /**
     * Decodes and checksum-verifies a Bech32 or Bech32m string.
     *
     * @return the lowercased human-readable part, the decoded 5-bit data
     *   values (excluding the 6 checksum characters), and which variant's
     *   checksum matched — or `null` if the string is not a well-formed,
     *   checksum-valid Bech32/Bech32m encoding.
     */
    @Suppress("ReturnCount", "CyclomaticComplexMethod")
    fun decode(s: String): Decoded? {
        // Must be ASCII and within the length limit.
        if (s.length > MAX_LENGTH) return null

        // Printable ASCII 33..126 only (excludes spaces and controls). Reject
        // mixed case BEFORE lowercasing (BIP-173).
        var hasLower = false
        var hasUpper = false
        for (ch in s) {
            val code = ch.code
            if (code < 33 || code > 126) return null
            if (ch in 'a'..'z') hasLower = true
            if (ch in 'A'..'Z') hasUpper = true
        }
        if (hasLower && hasUpper) return null

        val lowered = s.lowercase()

        // Separator is the LAST '1'.
        val sepIndex = lowered.lastIndexOf('1')
        if (sepIndex < 0) return null

        // HRP occupies everything before the separator: 1..83 chars. (Every
        // character was already confirmed ASCII 33..126 above.)
        val hrp = lowered.substring(0, sepIndex)
        if (hrp.isEmpty() || hrp.length > 83) return null

        // Data part follows the separator and must be >= 6 (checksum) chars.
        val dataPart = lowered.substring(sepIndex + 1)
        if (dataPart.length < 6) return null

        // Map each data character to its 5-bit value.
        val values = ByteArray(dataPart.length)
        for (i in dataPart.indices) {
            val code = dataPart[i].code
            if (code >= 128) return null
            val v = CHARSET_REVERSE[code]
            if (v < 0) return null
            values[i] = v.toByte()
        }

        // Verify the checksum and identify the variant.
        val residue = polymod(hrpExpand(hrp) + values)
        val variant =
            when (residue) {
                Variant.BECH32.checksumConstant -> Variant.BECH32
                Variant.BECH32M.checksumConstant -> Variant.BECH32M
                else -> return null
            }

        // Strip the 6-character checksum from the returned data.
        val data = values.copyOfRange(0, values.size - 6)
        return Decoded(hrp, data, variant)
    }

    /**
     * Convenience: verifies that [s] decodes as the expected [variant] with
     * the expected human-readable part [expectedHrp].
     */
    fun verify(
        s: String,
        expectedHrp: String,
        variant: Variant,
    ): Boolean {
        val decoded = decode(s) ?: return false
        return decoded.variant == variant && decoded.hrp == expectedHrp.lowercase()
    }

    // MARK: - BCH checksum primitives (BIP-173 / BIP-350)

    private fun polymod(values: ByteArray): Int {
        val generator = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
        var chk = 1
        for (value in values) {
            val top = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor value.toInt()
            for (i in 0 until 5) {
                if ((top ushr i) and 1 != 0) chk = chk xor generator[i]
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: String): ByteArray {
        val expanded = ByteArray(hrp.length * 2 + 1)
        var index = 0
        for (ch in hrp) expanded[index++] = (ch.code shr 5).toByte()
        expanded[index++] = 0
        for (ch in hrp) expanded[index++] = (ch.code and 0x1f).toByte()
        return expanded
    }
}
