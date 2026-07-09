package org.zecdev.zip321.parser

import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Direct unit tests of [Param]'s internal contract: [Param.from]'s few paths not otherwise
 * reachable through [SubParserTests]/[ParserTests] (an empty query key, an invalid address at the
 * empty vs. a non-empty paramindex, and a `qchar`-decode failure); equality (each subtype's own
 * synthesized `data class` `equals`/`hashCode` — `Param` itself no longer hand-writes these, see
 * its K16 removal note) and [Param.partiallyEqual] exercised directly and exhaustively over every
 * subtype (these are mostly reached only indirectly — via [IndexedParameter] equality checks —
 * elsewhere in the suite, which doesn't exercise every subtype pairing).
 */
class ParamTests {
    private val recipient =
        validRecipient(
            "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
        )
    private val otherRecipient = validRecipient("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
    private val memo = MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg")
    private val otherMemo = MemoBytes("a different memo")

    // MARK: - Param.from

    @Test
    fun `Param from rejects an empty query key`() {
        val error =
            assertFailsWith<ZIP321.Errors.InvalidParamName> {
                Param.from("", "value", 0u, Network.TESTNET, ReferenceAddressValidator.TESTNET)
            }
        assertEquals("paramName cannot be empty", error.paramName)
    }

    @Test
    fun `Param from maps an invalid address at the empty paramindex to a null-index error`() {
        val error =
            assertFailsWith<ZIP321.Errors.InvalidAddress> {
                Param.from("address", "not-a-valid-address", 0u, Network.TESTNET, ReferenceAddressValidator.TESTNET)
            }
        assertEquals(null, error.index)
    }

    @Test
    fun `Param from maps an invalid address at a non-zero paramindex to that index`() {
        val error =
            assertFailsWith<ZIP321.Errors.InvalidAddress> {
                Param.from("address", "not-a-valid-address", 5u, Network.TESTNET, ReferenceAddressValidator.TESTNET)
            }
        assertEquals(5u, error.index)
    }

    @Test
    fun `Param from surfaces a qchar decode failure for a malformed label value`() {
        // A truncated percent-escape reaches the tokenizer (it's a valid qchar *value* byte
        // sequence — `%` is a legal raw value byte) but fails to qchar-decode.
        val error =
            assertFailsWith<ZIP321.Errors.QcharDecodeFailed> {
                Param.from("label", "100%", 0u, Network.TESTNET, ReferenceAddressValidator.TESTNET)
            }
        assertEquals("label", error.key)
        assertEquals("100%", error.value)
    }

    @Test
    fun `Param from surfaces a qchar decode failure for a malformed otherparam value`() {
        val error =
            assertFailsWith<ZIP321.Errors.QcharDecodeFailed> {
                Param.from("future-flag", "100%", 0u, Network.TESTNET, ReferenceAddressValidator.TESTNET)
            }
        assertEquals("future-flag", error.key)
    }

    // MARK: - Param.equals / hashCode

    @Test
    fun `equals and hashCode are consistent for every Param subtype`() {
        val pairs: List<Pair<Param, Param>> =
            listOf(
                Param.Address(recipient) to Param.Address(recipient),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()) to Param.Amount(NonNegativeAmount.zec("1").getOrThrow()),
                Param.Memo(memo) to Param.Memo(memo),
                Param.Label("payment") to Param.Label("payment"),
                Param.Message("thanks") to Param.Message("thanks"),
                Param.Other("future", "value") to Param.Other("future", "value"),
            )

        for ((a, b) in pairs) {
            assertEquals(a, b, "$a should equal $b")
            assertEquals(a.hashCode(), b.hashCode(), "$a and $b should hash equally")
        }
    }

    @Test
    fun `equals distinguishes values within the same Param subtype`() {
        val unequalPairs: List<Pair<Param, Param>> =
            listOf(
                Param.Address(recipient) to Param.Address(otherRecipient),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()) to Param.Amount(NonNegativeAmount.zec("2").getOrThrow()),
                Param.Memo(memo) to Param.Memo(otherMemo),
                Param.Label("payment") to Param.Label("other"),
                Param.Message("thanks") to Param.Message("other"),
                Param.Other("future", "value") to Param.Other("future", "other value"),
                // Other's synthesized equals compares BOTH fields; a differing `paramName` alone
                // (same `value`) must also be distinguished.
                Param.Other("a", "x") to Param.Other("b", "x"),
            )

        for ((a, b) in unequalPairs) {
            assertNotEquals(a, b, "$a should not equal $b")
        }
    }

    @Test
    fun `equals distinguishes different Param subtypes`() {
        val amount = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())
        val address = Param.Address(recipient)
        assertNotEquals<Param>(amount, address)
        assertNotEquals<Param>(address, amount)
        assertNotEquals<Param>(Param.Label("x"), Param.Message("x"))
    }

    @Test
    fun `equals rejects a non-Param other and reflects identity`() {
        val amount: Param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertFalse(amount.equals("not a Param"))
        assertTrue(amount == amount)
    }

    // MARK: - Param.partiallyEqual / hasDuplicateParam

    @Test
    fun `partiallyEqual matches same-kind Params regardless of value`() {
        assertTrue(Param.Address(recipient).partiallyEqual(Param.Address(otherRecipient)))
        assertTrue(Param.Amount(NonNegativeAmount.zec("1").getOrThrow()).partiallyEqual(Param.Amount(NonNegativeAmount.zec("2").getOrThrow())))
        assertTrue(Param.Memo(memo).partiallyEqual(Param.Memo(otherMemo)))
        assertTrue(Param.Label("a").partiallyEqual(Param.Label("b")))
        assertTrue(Param.Message("a").partiallyEqual(Param.Message("b")))
        assertTrue(Param.Other("future", "a").partiallyEqual(Param.Other("future", "b")))
    }

    @Test
    fun `partiallyEqual is reflexive by identity`() {
        val address = Param.Address(recipient)
        assertTrue(address.partiallyEqual(address))
    }

    @Test
    fun `partiallyEqual rejects differing kinds`() {
        val allKinds: List<Param> =
            listOf(
                Param.Address(recipient),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()),
                Param.Memo(memo),
                Param.Label("a"),
                Param.Message("a"),
                Param.Other("future", "a"),
            )

        for (kind in allKinds) {
            for (other in allKinds) {
                if (kind === other) continue
                assertFalse(kind.partiallyEqual(other), "$kind should not partially-equal $other")
            }
        }
    }

    @Test
    fun `partiallyEqual distinguishes different otherparam names`() {
        assertFalse(Param.Other("future", "a").partiallyEqual(Param.Other("other-future", "a")))
    }

    @Test
    fun `partiallyEqual for an Other whose name coincides with a reserved key still requires an Other on both sides`() {
        // `Param.Other`'s bare constructor (unlike `OtherParam.create`) does not reject reserved
        // names, so this coincidence is only reachable via direct construction. It matters because
        // the leading `name` guard compares the exact same STRING ("amount") for both sides here,
        // so it does NOT short-circuit before reaching `partiallyEqual`'s `is Other` branch — this
        // is the only way to exercise that branch's own `other is Other` check against a genuinely
        // different kind.
        val otherNamedAmount = Param.Other("amount", "value")
        val amount = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())
        assertEquals(otherNamedAmount.name, amount.name, "test premise: both compute name == \"amount\"")
        assertFalse(otherNamedAmount.partiallyEqual(amount))
    }
}
