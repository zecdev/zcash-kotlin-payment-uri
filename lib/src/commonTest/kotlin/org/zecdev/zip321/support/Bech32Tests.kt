package org.zecdev.zip321.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BIP-173 (Bech32) and BIP-350 (Bech32m) known-answer vectors, plus real
 * Zcash Sapling / Unified / regtest addresses and their corpus-corrupted
 * variants (which must fail checksum verification). Ported 1:1 from the
 * Swift reference `Bech32Tests.swift`.
 */
class Bech32Tests {
    // MARK: BIP-173 valid Bech32

    private val validBech32Strings =
        listOf(
            "A12UEL5L",
            "a12uel5l",
            "an83characterlonghumanreadablepartthatcontainsthenumber1andtheexcludedcharactersbio1tt5tgs",
            "abcdef1qpzry9x8gf2tvdw0s3jn54khce6mua7lmqqqxw",
            "split1checkupstagehandshakeupstreamerranterredcaperred2y9e3w",
            "?1ezyfcl",
        )

    @Test
    fun validBech32() {
        for (s in validBech32Strings) {
            val decoded = assertNotNull(Bech32.decode(s), s)
            assertEquals(Bech32.Variant.BECH32, decoded.variant, s)
        }
    }

    // MARK: BIP-350 valid Bech32m

    private val validBech32mStrings =
        listOf(
            "A1LQFN3A",
            "a1lqfn3a",
            "abcdef1l7aum6echk45nj3s0wdvt2fg8x9yrzpqzd3ryx",
            "split1checkupstagehandshakeupstreamerranterredcaperredlc445v",
            "?1v759aa",
        )

    @Test
    fun validBech32m() {
        for (s in validBech32mStrings) {
            val decoded = assertNotNull(Bech32.decode(s), s)
            assertEquals(Bech32.Variant.BECH32M, decoded.variant, s)
        }
    }

    // MARK: Invalid encodings must decode to null

    private val invalidEncodings =
        listOf(
            // invalid checksum
            "A1G7SGD8",
            // invalid data character 'b'
            "x1b4n0q5v",
            // too short (data part < 6)
            "li1dgmt3",
            // empty HRP
            "1pzry9x0s0muk",
            // no separator
            "pzry9x0s0muk",
            // too short / empty HRP after last '1'
            "10a06t8",
            // empty HRP
            "1qzzfhee",
            // Mixed upper/lower case is rejected before lowercasing.
            "ztestsapling10YY2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
        )

    @Test
    fun invalidEncodings() {
        for (s in invalidEncodings) {
            assertNull(Bech32.decode(s), s)
        }
    }

    @Test
    fun mixedCaseRejectedBeforeLowercasing() {
        // Valid lowercase and valid uppercase decode; a mix of both does not.
        assertNotNull(Bech32.decode("a12uel5l"))
        assertNotNull(Bech32.decode("A12UEL5L"))
        assertNull(Bech32.decode("a12UEL5L"))
    }

    @Test
    fun nonPrintableAsciiRejected() {
        assertNull(Bech32.decode(" 1nwldj5")) // space (0x20) in HRP
        assertNull(Bech32.decode("\u007f1axkwrx")) // DEL (0x7f)
    }

    @Test
    fun allDigitStringNeverSetsHasLowerOrHasUpper() {
        // HRP "?" (BIP-173 permits a non-alphabetic HRP; see the "?1ezyfcl" vector above) plus an
        // all-digit data part: neither `hasLower` nor `hasUpper` is ever set `true`. The checksum
        // won't validate, but that's fine — this exercises the mixed-case guard's "all false"
        // path, not a successful decode.
        assertNull(Bech32.decode("?1982035468"))
    }

    @Test
    fun hrpLongerThan83CharsRejected() {
        // Well under the overall 1023-char limit, but the HRP alone exceeds BIP-173's 83-char cap.
        val overlongHrp = "a".repeat(84) + "1qqqqqq"
        assertTrue(overlongHrp.length <= Bech32.MAX_LENGTH)
        assertNull(Bech32.decode(overlongHrp))
    }

    @Test
    fun exceedingLengthLimitRejected() {
        // A syntactically char-valid string longer than the 1023-char limit.
        val tooLong = "a1" + "q".repeat(1023)
        assertTrue(tooLong.length > Bech32.MAX_LENGTH)
        assertNull(Bech32.decode(tooLong))
    }

    // MARK: Real Zcash addresses from the vector corpus

    @Test
    fun saplingTestnetAddress() {
        val addr = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val decoded = assertNotNull(Bech32.decode(addr))
        assertEquals(Bech32.Variant.BECH32, decoded.variant)
        assertEquals("ztestsapling", decoded.hrp)
        assertTrue(Bech32.verify(addr, "ztestsapling", Bech32.Variant.BECH32))
    }

    @Test
    fun unifiedMainnetAddress() {
        // Mainnet UA, zcash-test-vectors unified_address.json.
        val addr =
            "u1l8xunezsvhq8fgzfl7404m450nwnd76zshscn6nfys7vyz2ywyh4cc5daaq0c7q2su5lqfh23sp7fkf3kt27ve59" +
                "48mzpfdvckzaect2jtte308mkwlycj2u0eac077wu70vqcetkxf"
        val decoded = assertNotNull(Bech32.decode(addr))
        assertEquals(Bech32.Variant.BECH32M, decoded.variant)
        assertEquals("u", decoded.hrp)
        assertTrue(Bech32.verify(addr, "u", Bech32.Variant.BECH32M))
    }

    @Test
    fun saplingRegtestAddress() {
        val addr = "zregtestsapling1qqqqqqqqqqqqqqqqqqcguyvaw2vjk4sdyeg0lc970u659lvhqq7t0np6hlup5lusxle7505hlz3"
        val decoded = assertNotNull(Bech32.decode(addr))
        assertEquals(Bech32.Variant.BECH32, decoded.variant)
        assertEquals("zregtestsapling", decoded.hrp)
    }

    // MARK: Corpus corrupted variants must FAIL

    @Test
    fun corruptedSaplingFails() {
        // Last char changed within the charset ('z' -> 'q'): checksum breaks.
        val corrupted = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2keq"
        assertNull(Bech32.decode(corrupted))
    }

    @Test
    fun corruptedUnifiedFails() {
        // Last char changed within the charset ('f' -> 'q'): checksum breaks.
        val corrupted =
            "u1l8xunezsvhq8fgzfl7404m450nwnd76zshscn6nfys7vyz2ywyh4cc5daaq0c7q2su5lqfh23sp7fkf3kt27ve59" +
                "48mzpfdvckzaect2jtte308mkwlycj2u0eac077wu70vqcetkxq"
        assertNull(Bech32.decode(corrupted))
    }

    // MARK: verify() convenience

    @Test
    fun verifyRejectsWrongHrpOrVariant() {
        val sapling = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        assertTrue(Bech32.verify(sapling, "ztestsapling", Bech32.Variant.BECH32))
        assertFalse(Bech32.verify(sapling, "zs", Bech32.Variant.BECH32)) // wrong HRP
        assertFalse(Bech32.verify(sapling, "ztestsapling", Bech32.Variant.BECH32M)) // wrong variant
    }

    @Test
    fun verifyRejectsAStringThatDoesNotDecodeAtAll() {
        assertFalse(Bech32.verify("not-bech32-at-all", "zs", Bech32.Variant.BECH32))
    }

    // MARK: Decoded value semantics

    @Test
    fun decodedEqualsComparesByHrpVariantAndDataContent() {
        val addr = "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val first = assertNotNull(Bech32.decode(addr))
        val second = assertNotNull(Bech32.decode(addr))

        // Distinct `Decoded` instances (same input decoded twice) still compare equal by content.
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals(first, first)

        val differentHrp = assertNotNull(Bech32.decode("a12uel5l"))
        assertFalse(first.equals(differentHrp))
        assertFalse(first.equals("not a Decoded"))

        // Same HRP, same variant, but different data content: another distinct testnet Sapling
        // address.
        val sameHrpAndVariantDifferentData =
            assertNotNull(
                Bech32.decode("ztestsapling1n65uaftvs2g7075q2x2a04shfk066u3lldzxsrprfrqtzxnhc9ps73v4lhx4l9yfxj46sl0q90k"),
            )
        assertEquals(first.hrp, sameHrpAndVariantDifferentData.hrp)
        assertEquals(first.variant, sameHrpAndVariantDifferentData.variant)
        assertFalse(first.equals(sameHrpAndVariantDifferentData))
    }

    @Test
    fun decodedEqualsDistinguishesVariantAloneWithMatchingHrpAndData() {
        // No real address pair shares both HRP and data while differing only in checksum variant,
        // so this specific combination is only reachable by constructing `Decoded` directly
        // (accessible within the module) rather than through `decode()`.
        val data = byteArrayOf(1, 2, 3)
        val bech32 = Bech32.Decoded("same-hrp", data, Bech32.Variant.BECH32)
        val bech32m = Bech32.Decoded("same-hrp", data, Bech32.Variant.BECH32M)
        assertFalse(bech32.equals(bech32m))
    }
}
