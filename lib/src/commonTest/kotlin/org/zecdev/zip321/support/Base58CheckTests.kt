package org.zecdev.zip321.support

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Base58Check decode/verify tests for transparent Zcash addresses, plus edge
 * cases (leading-zero bytes, invalid alphabet characters, short input).
 * Ported 1:1 from the Swift reference `Base58CheckTests.swift`.
 */
class Base58CheckTests {
    // Transparent address version bytes, from librustzcash
    // zcash_protocol/src/constants/{mainnet,testnet}.rs.
    private val mainnetP2PKH = bytes(0x1c, 0xb8)
    private val testnetP2PKH = bytes(0x1d, 0x25)

    @Test
    fun testnetP2PKHDecodes() {
        // From spec/lib.rs; also the source of the corpus's corrupted variant.
        val addr = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val payload = assertNotNull(Base58Check.decode(addr))
        assertContentEquals(testnetP2PKH, payload.copyOfRange(0, 2))
        assertTrue(Base58Check.verify(addr, listOf(testnetP2PKH)))
    }

    @Test
    fun mainnetP2PKHDecodes() {
        // Mainnet t1 address from zcash-test-vectors (zip_0320.json).
        val addr = "t1V9mnyk5Z5cTNMCkLbaDwSskgJZucTLdgW"
        val payload = assertNotNull(Base58Check.decode(addr))
        assertContentEquals(mainnetP2PKH, payload.copyOfRange(0, 2))
        assertTrue(Base58Check.verify(addr, listOf(mainnetP2PKH)))
    }

    @Test
    fun corruptedTestnetAddressFails() {
        // Corpus invalid variant: last char changed ('U' -> '1'), checksum breaks.
        val corrupted = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovp1"
        assertNull(Base58Check.decode(corrupted))
        assertFalse(Base58Check.verify(corrupted, listOf(testnetP2PKH)))
    }

    @Test
    fun verifyRejectsWrongVersionBytes() {
        // A valid testnet address must not verify against mainnet prefixes.
        val addr = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        assertFalse(Base58Check.verify(addr, listOf(mainnetP2PKH)))
        // ...but does verify when the correct prefix is among several.
        assertTrue(Base58Check.verify(addr, listOf(mainnetP2PKH, testnetP2PKH)))
    }

    @Test
    fun verifyRejectsAPrefixLongerThanTheDecodedPayload() {
        // The (2-byte version + 20-byte hash) payload is far shorter than this contrived prefix;
        // `verify` must reject it (not throw) rather than assume the prefix always fits.
        val addr = "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        val tooLongPrefix = ByteArray(100)
        assertFalse(Base58Check.verify(addr, listOf(tooLongPrefix)))
    }

    @Test
    fun leadingOnePreservesLeadingZeroBytes() {
        // '1'-prefixed vector encoding payload 0x00 01 02 03 04 (checksum valid).
        val vector = "1An6UhWF92g"
        val payload = assertNotNull(Base58Check.decode(vector))
        assertContentEquals(bytes(0x00, 0x01, 0x02, 0x03, 0x04), payload)
    }

    @Test
    fun invalidAlphabetCharactersRejected() {
        // 0, O, I, l are not in the base58 alphabet.
        val badAddresses =
            listOf(
                "tmEZ0hbWHTpdKMw5it8YDspUXSMGQyFwovpU",
                "tmEZOhbWHTpdKMw5it8YDspUXSMGQyFwovpU",
                "tmEZIhbWHTpdKMw5it8YDspUXSMGQyFwovpU",
                "tmEZlhbWHTpdKMw5it8YDspUXSMGQyFwovpU",
            )
        for (bad in badAddresses) {
            assertNull(Base58Check.decode(bad), bad)
        }
    }

    @Test
    fun tooShortRejected() {
        // Fewer than 4 decoded bytes cannot carry a checksum.
        assertNull(Base58Check.decode(""))
        assertNull(Base58Check.decode("z")) // decodes to a single byte
        assertNull(Base58Check.decode("111")) // three zero bytes, no checksum
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
