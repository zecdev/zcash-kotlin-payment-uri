package org.zecdev.zip321.parser

import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// NOTE (K11/v2): the leading-address helpers are now `splitLeadingAddress` (pure split on the
// first `?`) and `leadingAddress` (validates a non-empty lead address). A rejected non-empty lead
// address now throws `ZIP321.Errors.InvalidAddress` — replacing v1's behavior of silently treating
// an unvalidated run as "no leading address" (and the ParseError it used to surface downstream).
class ParserTests {
    private val testnetAddress =
        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

    private fun parser(network: Network = Network.TESTNET) = Parser(network, ReferenceAddressValidator.of(network))

    @Test
    fun `splitLeadingAddress separates address and query`() {
        val (address, rest) = parser().splitLeadingAddress("zcash:$testnetAddress?amount=1.0001&message=lunch")
        assertEquals(testnetAddress, address)
        assertEquals("?amount=1.0001&message=lunch", rest)
    }

    @Test
    fun `splitLeadingAddress with no query has null rest`() {
        val (address, rest) = parser().splitLeadingAddress("zcash:$testnetAddress")
        assertEquals(testnetAddress, address)
        assertNull(rest)
    }

    @Test
    fun `splitLeadingAddress with no leading address`() {
        val (address, rest) = parser().splitLeadingAddress("zcash:?address.1=$testnetAddress&amount.1=1.0001")
        assertEquals("", address)
        assertEquals("?address.1=$testnetAddress&amount.1=1.0001", rest)
    }

    @Test
    fun `detects single recipient with leading address`() {
        val (rest, node) = parser().leadingAddress("zcash:$testnetAddress")

        val recipient = validRecipient(testnetAddress)
        assertNull(rest)
        assertEquals(IndexedParameter(0u, Param.Address(recipient)), node)
    }

    @Test
    fun `detects leading address with other params`() {
        val (rest, node) = parser().leadingAddress("zcash:$testnetAddress?amount=1.0001")

        val recipient = validRecipient(testnetAddress)
        assertEquals("?amount=1.0001", rest)
        assertEquals(IndexedParameter(0u, Param.Address(recipient)), node)
    }

    @Test
    fun `returns null node when no leading address is present`() {
        val (rest, node) = parser().leadingAddress("zcash:?amount=1.0001&address=$testnetAddress")
        assertEquals("?amount=1.0001&address=$testnetAddress", rest)
        assertNull(node)
    }

    @Test
    fun `detects single recipient with leading address that is very short`() {
        assertFails { parser().parse("zcash:z") }
    }

    @Test
    fun `rejects a very long leading run that is not a checksum-valid address`() {
        // v1 accepted this via the HRP-prefix + charset heuristic. v2 verifies the Bech32 checksum
        // (and the concatenation also exceeds the 1023-char Bech32 limit), so a non-empty lead
        // address that fails validation is now rejected with InvalidAddress rather than silently
        // treated as "no address".
        val longRun = testnetAddress.repeat(200)
        assertFailsWith<ZIP321.Errors.InvalidAddress> {
            parser().leadingAddress("zcash:$longRun")
        }
    }

    @Test
    fun `rejects single recipient with leading address that contains unicode characters`() {
        // This URI contains Unicode letters `ʔ`, `ꘌ`, `ꓸ` and `ẟ` that resemble the delimiters
        // `?`, `=`, `.` and `&`. With no ASCII `?`, the whole tail is the lead address, which
        // fails validation -> InvalidAddress.
        assertFailsWith<ZIP321.Errors.InvalidAddress> {
            parser().parse("zcash:tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpUʔamountꘌ1ꓸ234ẟmessageꘌThanks")
        }
    }

    @Test
    fun `zcashParameter creates a valid address`() {
        val value =
            "u1fl5mprj0t9p4jg92hjjy8q5myvwc60c9wv0xachauqpn3c3k4xwzlaueafq27dcg7tzzzaz5jl8tyj93wgs983y0jq0qfhzu6n4r8rakpv5f4gg2lrw4z6pyqqcrcqx04d38yunc6je"
        val recipient = validRecipient(value, Network.MAINNET)

        assertEquals(
            IndexedParameter(0u, Param.Address(recipient)),
            parser(Network.MAINNET).zcashParameter("address", null, value),
        )
    }

    @Test
    fun `parse resolves a full request with a leading address`() {
        val result = parser().parse("zcash:$testnetAddress?amount=1.0001&message=lunch")
        assertTrue(result is ZIP321.ParserResult.Request)
    }
}
