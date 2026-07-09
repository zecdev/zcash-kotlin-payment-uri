package org.zecdev.zip321.parser

import org.zecdev.zip321.Network
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.support.ReferenceAddressValidator
import org.zecdev.zip321.support.validRecipient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// NOTE (K11/v2): rewritten onto the Scanner-based grammar. The paramindex/name/query-segment
// tokenizers are now `parseParameterIndex`/`parseNameAndIndex`/`parseQueryToken`, and
// `zcashParameter` takes `(name, index, value)` rather than a nested `Pair`. Accept/reject
// behavior is preserved (and tightened per the reference); the grouping/duplicate/payment tests
// are unchanged.
class SubParserTests {
    private fun parser() = Parser(Network.TESTNET, ReferenceAddressValidator.TESTNET)

    // paramindex subparser

    @Test
    fun `parses non-zero single digit`() {
        assertEquals(1u, parser().parseParameterIndex("1"))
        assertEquals(9u, parser().parseParameterIndex("9"))
    }

    @Test
    fun `fails on zero single digit`() {
        assertFails { parser().parseParameterIndex("0") }
    }

    @Test
    fun `parses many digits`() {
        assertEquals(12u, parser().parseParameterIndex("12"))
        assertEquals(123u, parser().parseParameterIndex("123"))
    }

    @Test
    fun `fails on leading zero many digits`() {
        assertFails { parser().parseParameterIndex("090") }
    }

    @Test
    fun `fails on too many digits`() {
        assertFails { parser().parseParameterIndex("19999") }
    }

    // Optionally indexed parameter name parsing

    @Test
    fun `parses a non-indexed parameter`() {
        assertEquals(Pair<String, UInt?>("address", null), parser().parseNameAndIndex("address"))
    }

    @Test
    fun `parses a indexed parameter`() {
        assertEquals(Pair<String, UInt?>("address", 123u), parser().parseNameAndIndex("address.123"))
    }

    @Test
    fun `parses a name without index containing dashes`() {
        assertEquals(Pair<String, UInt?>("asset-id", null), parser().parseNameAndIndex("asset-id"))
    }

    @Test
    fun `fails to parse a zero-index parameter`() {
        assertFails { parser().parseNameAndIndex("address.0") }
    }

    @Test
    fun `fails to parse leading zero parameter`() {
        assertFails { parser().parseNameAndIndex("address.023") }
    }

    @Test
    fun `fails to parse a parameter with an index greater than 9999`() {
        assertFails { parser().parseNameAndIndex("address.19999") }
    }

    @Test
    fun `fails to parse a paramname with invalid characters`() {
        assertFails { parser().parseNameAndIndex("add[ress[1].1") }
    }

    @Test
    fun `fails to parse a percent-escaped paramname`() {
        assertFails { parser().parseNameAndIndex("%61ddress") }
    }

    // Query segment parsing

    @Test
    fun `parses a query key with no index`() {
        val parsed = parser().parseQueryToken("address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        assertEquals("address", parsed.first)
        assertEquals(null, parsed.second)
        assertEquals("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", parsed.third)
    }

    @Test
    fun `parses a query key with a valid index`() {
        val parsed = parser().parseQueryToken("address.123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        assertEquals("address", parsed.first)
        assertEquals(123u, parsed.second)
        assertEquals("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", parsed.third)
    }

    @Test
    fun `fails to parse a query key with invalid index`() {
        assertFails {
            parser().parseQueryToken("address.00123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        }
    }

    @Test
    fun `parser catches query qcharencoded values`() {
        assertEquals(
            Triple("message", 1u, "Thank%20You%20For%20Your%20Purchase"),
            parser().parseQueryToken("message.1=Thank%20You%20For%20Your%20Purchase"),
        )
    }

    @Test
    fun `valueless parameter is accepted`() {
        // ZIP-321 `otherparam` grammar allows an absent `= *qchar`; v1 preserves this.
        assertEquals(Triple("future-flag", null, null), parser().parseQueryToken("future-flag"))
    }

    @Test
    fun `empty value is accepted`() {
        assertEquals(Triple("message", null, ""), parser().parseQueryToken("message="))
    }

    @Test
    fun `percent-escaped name is rejected`() {
        assertFails { parser().parseQueryToken("%61mount=1") }
    }

    @Test
    fun `non-qchar in value is rejected`() {
        // A raw non-qchar character (space) leaves trailing input.
        assertFails { parser().parseQueryToken("label=a b") }
    }

    @Test
    fun `leading-zero index in query segment is rejected`() {
        assertFails { parser().parseQueryToken("address.0=x") }
    }

    @Test
    fun `overlong index in query segment is rejected`() {
        assertFails { parser().parseQueryToken("amount.10000=1") }
    }

    @Test
    fun `tokenizer does not reject an unknown required param`() {
        // The tokenizer is name/value-agnostic; `req-` rejection happens in zcashParameter.
        parser().parseQueryToken("req-unknown-future-option=true")
    }

    @Test
    fun `zcashParameter fails on unknown required param`() {
        assertFails {
            parser().zcashParameter("req-unknown-future-option", null, "true")
        }
    }

    // zcashParameter — reserved query keys

    @Test
    fun `Zcash parameter creates valid amount`() {
        assertEquals(
            IndexedParameter(1u, Param.Amount(amount = NonNegativeAmount.zec("1.00020112").getOrThrow())),
            parser().zcashParameter("amount", 1u, "1.00020112"),
        )
    }

    @Test
    fun `Zcash parameter creates valid message`() {
        val value = "Thank%20You%20For%20Your%20Purchase"
        val qcharDecodedValue = value.qcharDecode()
        assertNotEquals("", qcharDecodedValue)

        assertEquals(
            IndexedParameter(1u, Param.Message(qcharDecodedValue)),
            parser().zcashParameter("message", 1u, value),
        )
    }

    @Test
    fun `Zcash parameter creates valid label`() {
        val value = "Thank%20You%20For%20Your%20Purchase"
        val qcharDecodedValue = value.qcharDecode()
        assertNotEquals("", qcharDecodedValue)

        assertEquals(
            IndexedParameter(99u, Param.Label(qcharDecodedValue)),
            parser().zcashParameter("label", 99u, value),
        )
    }

    @Test
    fun `Zcash parameter creates valid memo`() {
        val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        assertEquals(
            IndexedParameter(99u, Param.Memo(MemoBytes.fromBase64URL(value))),
            parser().zcashParameter("memo", 99u, value),
        )
    }

    @Test
    fun `Zcash parameter creates valid memo that contains UTF-8 characters`() {
        val value = "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        assertEquals(
            Triple("memo", 99u, value),
            parser().parseQueryToken("memo.99=$value"),
        )
    }

    @Test
    fun `Zcash parameter creates safely ignored other parameter`() {
        val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        assertEquals(
            IndexedParameter(0u, Param.Other("future-binary-format", value)),
            parser().zcashParameter("future-binary-format", null, value),
        )
    }

    @Test
    fun `Zcash parameter percent-decodes an other parameter value`() {
        val result = parser().zcashParameter("future-param", null, "hello%20world")
        val other = result.param
        assertTrue(other is Param.Other)
        assertEquals("hello world", (other as Param.Other).value)
    }

    // Parses many parameters in a row

    @Test
    fun `Index parameters are parsed with no leading address`() {
        val remainingString = "?address=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val expected =
            listOf(
                IndexedParameter(0u, Param.Address(recipient)),
                IndexedParameter(0u, Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(0u, Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(0u, Param.Message("Thank you for your purchase")),
            )

        assertEquals(expected, parser().parseParameters(remainingString, null))
    }

    @Test
    fun `Index parameters are parsed with leading address`() {
        val remainingString = "?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val expected =
            listOf(
                IndexedParameter(0u, Param.Address(recipient)),
                IndexedParameter(0u, Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(0u, Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(0u, Param.Message("Thank you for your purchase")),
            )

        val leadingAddress = IndexedParameter(0u, Param.Address(recipient))

        assertEquals(expected, parser().parseParameters(remainingString, leadingAddress))
    }

    // Duplicate params are caught

    @Test
    fun `Duplicate other params are detected`() {
        val params =
            listOf(
                Param.Address(
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                ),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other("future", "is awesome"),
            )

        assertTrue(params.hasDuplicateParam(Param.Other("future", "is dystopic")))
    }

    @Test
    fun `Duplicate address params are detected`() {
        val params =
            listOf(
                Param.Address(
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                ),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other("future", "is awesome"),
            )

        assertTrue(
            params.hasDuplicateParam(
                Param.Address(
                    validRecipient(
                        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
                    ),
                ),
            ),
        )
    }

    // Payment can be created from uniquely indexed params

    @Test
    fun `Payment is created from indexed parameters`() {
        val recipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val params =
            listOf(
                Param.Address(recipient),
                Param.Amount(NonNegativeAmount.zec("1").getOrThrow()),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other("future", "is awesome"),
            )

        val payment = Payment.fromUniqueIndexedParameters(index = 1u, parameters = params)

        assertEquals(
            Payment(
                recipientAddress = recipient,
                amount = NonNegativeAmount.zec("1").getOrThrow(),
                memo = null,
                label = "payment",
                message = "Thanks",
                otherParams = listOf(OtherParam("future", "is awesome")),
            ),
            payment,
        )
    }

    @Test
    fun `duplicate addresses are detected`() {
        val shieldedRecipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val duplicateAddressParams: List<IndexedParameter> =
            listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Other("future", "is awesome")),
            )

        val error =
            assertFailsWith<ZIP321.Errors> {
                parser().mapToPayments(duplicateAddressParams)
            }
        assertEquals(ZIP321.Errors.DuplicateParameter("address", null), error)
    }

    @Test
    fun `duplicate amounts are detected`() {
        val shieldedRecipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val duplicateAmountParams: List<IndexedParameter> =
            listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("2").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Other("future", "is awesome")),
            )

        val error =
            assertFailsWith<ZIP321.Errors> {
                parser().mapToPayments(duplicateAmountParams)
            }
        assertEquals(ZIP321.Errors.DuplicateParameter("amount", null), error)
    }

    @Test
    fun `duplicate message are detected`() {
        val shieldedRecipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val duplicateParams: List<IndexedParameter> =
            listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("2").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Other("future", "is awesome")),
            )

        val error =
            assertFailsWith<ZIP321.Errors> {
                parser().mapToPayments(duplicateParams)
            }
        assertEquals(ZIP321.Errors.DuplicateParameter("message", null), error)
    }

    @Test
    fun `duplicate memos are detected`() {
        val shieldedRecipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val duplicateParams: List<IndexedParameter> =
            listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Other("future", "is awesome")),
            )

        val error =
            assertFailsWith<ZIP321.Errors> {
                parser().mapToPayments(duplicateParams)
            }
        assertEquals(ZIP321.Errors.DuplicateParameter("memo", null), error)
    }

    @Test
    fun `duplicate other params are detected`() {
        val shieldedRecipient =
            validRecipient(
                "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez",
            )

        val duplicateParams: List<IndexedParameter> =
            listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount.zec("1").getOrThrow())),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Other("future", "is dystopian")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Other("future", "is awesome")),
            )

        val error =
            assertFailsWith<ZIP321.Errors> {
                parser().mapToPayments(duplicateParams)
            }
        assertEquals(ZIP321.Errors.DuplicateParameter("future", null), error)
    }
}
