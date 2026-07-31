package org.zecdev.zip321.parser

import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParserTests {
    @Test
    fun `detects single recipient with leading address`() {
        val validURI = "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val (node, remainingText) =
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parseLeadingAddress(validURI)

        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        assertTrue(remainingText.isEmpty())
        assertEquals(IndexedParameter(0u, Param.Address(recipient)), node)
    }

    @Test
    fun `detects single recipient with leading address that is very short`() {
        val invalidURI = "zcash:z"
        assertFails {
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parse(invalidURI)
        }
    }

    @Test
    fun `rejects a very long leading run that is not a checksum-valid address`() {
        // v1 accepted this via the HRP-prefix + charset heuristic: a valid
        // testnet Sapling address repeated many times still "looked like" a
        // Sapling address (correct prefix, all-Bech32 charset) even though it
        // is NOT a checksum-valid Bech32 string. With validation delegated to
        // the reference validator (which verifies the Bech32 checksum, and for
        // which the concatenation also exceeds the 1023-char Bech32 limit) it
        // is correctly rejected: the scanner still walks the whole run, but
        // nothing is consumed as a leading address.
        val longRun =
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
                .repeat(200)
        val validURI = "zcash:$longRun"
        val (node, remainingText) =
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parseLeadingAddress(validURI)

        assertNull(node)
        assertEquals(longRun, remainingText)
    }

    @Test
    fun `rejects single recipient with leading address that contains unicode characters`() {
        assertFailsWith<ZIP321.Errors.ParseError> {
            // This URI contains Unicode letters `ʔ`, `ꘌ`, `ꓸ` and `ẟ` that resemble the delimiters `?`, `=`, `.` and `&`
            val invalidURI = "zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpUʔamountꘌ1ꓸ234ẟmessageꘌThanks"
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parse(
                invalidURI,
            )
        }
    }

    @Test
    fun `detects leading address with other params`() {
        val validURI = "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez?amount=1.0001"
        val (node, remainingText) =
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parseLeadingAddress(validURI)

        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )
        assertFalse(remainingText.isEmpty())
        assertEquals(IndexedParameter(0u, Param.Address(recipient)), node)
    }

    @Test
    fun `returns null when no leading address is present`() {
        val validURI = "zcash:?amount=1.0001&address=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
        val (node, remainingText) =
            Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET).parseLeadingAddress(validURI)
        assertFalse(remainingText.isEmpty())
        assertNull(node)
    }
}
