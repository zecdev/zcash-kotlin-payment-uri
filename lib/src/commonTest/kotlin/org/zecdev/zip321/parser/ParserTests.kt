package org.zecdev.zip321.parser

import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.model.NonNegativeAmount
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
        val (rest, recipient) = parser().leadingAddress("zcash:$testnetAddress")

        val expected = validRecipient(testnetAddress)
        assertNull(rest)
        assertEquals(expected, recipient)
    }

    @Test
    fun `detects leading address with other params`() {
        val (rest, recipient) = parser().leadingAddress("zcash:$testnetAddress?amount=1.0001")

        val expected = validRecipient(testnetAddress)
        assertEquals("?amount=1.0001", rest)
        assertEquals(expected, recipient)
    }

    @Test
    fun `returns null recipient when no leading address is present`() {
        val (rest, recipient) = parser().leadingAddress("zcash:?amount=1.0001&address=$testnetAddress")
        assertEquals("?amount=1.0001&address=$testnetAddress", rest)
        assertNull(recipient)
    }

    @Test
    fun `leadingAddress rejects an input that does not start with the zcash scheme`() {
        val error =
            assertFailsWith<ZIP321.Errors.ParseError> {
                parser().leadingAddress("bitcoin:notzcash")
            }
        assertEquals("Not `zcash:` uri", error.value)
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
        assertEquals(1, result.payments.size)
    }

    @Test
    fun `parse resolves the bare zcash scheme to the empty request`() {
        val result = parser().parse("zcash:")
        assertTrue(result.payments.isEmpty())
    }

    // MARK: - Parser.parse's own top-level guard (bypassing ZIP321.parse's identical guards)

    @Test
    fun `parse rejects an empty string when called directly`() {
        assertFailsWith<ZIP321.Errors.InvalidURI> { parser().parse("") }
    }

    @Test
    fun `parse rejects a non-zcash scheme when called directly`() {
        assertFailsWith<ZIP321.Errors.InvalidURI> { parser().parse("bitcoin:notzcash") }
    }

    // MARK: - scanIndexDigits / parseParameterIndex edge cases

    @Test
    fun `parseParameterIndex rejects a completely empty digit run`() {
        assertFailsWith<ZIP321.Errors.InvalidParamIndex> { parser().parseParameterIndex("") }
    }

    @Test
    fun `parseParameterIndex rejects trailing non-digit characters after a valid run`() {
        // `scanIndexDigits` itself accepts "123" (stopping at the first non-digit); the
        // full-consumption check in `parseParameterIndex` is what rejects the trailing "x".
        assertFailsWith<ZIP321.Errors.InvalidParamIndex> { parser().parseParameterIndex("123x") }
    }

    @Test
    fun `parseQueryToken rejects a completely empty query segment`() {
        // An empty segment has no first character for `scanName` to even inspect.
        assertFailsWith<ZIP321.Errors.ParseError> { parser().parseQueryToken("") }
    }

    // MARK: - parseParameters' own precondition (bypassing parse()'s guaranteed `?`-prefix)

    @Test
    fun `parseParameters requires its input to start with the query marker`() {
        assertFails { parser().parseParameters("amount=1", null) }
    }

    // MARK: - mapToIndexedPayments / mapToPayments

    @Test
    fun `mapToIndexedPayments rejects an empty parameter list`() {
        assertFailsWith<ZIP321.Errors.RecipientMissing> { parser().mapToIndexedPayments(emptyList()) }
    }

    @Test
    fun `mapToIndexedPayments rejects a non-empty index group that has no address`() {
        // Distinct from the empty-list case above: this index HAS parameters, just none of them
        // is an `address` — exercising `fromUniqueIndexedParameters`'s own "recipient not found"
        // fallback, tagged with the concrete (non-zero) index.
        val error =
            assertFailsWith<ZIP321.Errors.RecipientMissing> {
                parser().mapToIndexedPayments(listOf(IndexedParameter(5u, Param.Amount(NonNegativeAmount.zec("1").getOrThrow()))))
            }
        assertEquals(5u, error.index)
    }

    @Test
    fun `mapToPayments returns the mapped payments on success`() {
        val recipient = validRecipient(testnetAddress)
        val params =
            listOf(
                IndexedParameter(0u, Param.Address(recipient)),
                IndexedParameter(0u, Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
            )

        val payments = parser().mapToPayments(params)

        assertEquals(1, payments.size)
        assertEquals(recipient, payments[0].recipientAddress)
    }
}
