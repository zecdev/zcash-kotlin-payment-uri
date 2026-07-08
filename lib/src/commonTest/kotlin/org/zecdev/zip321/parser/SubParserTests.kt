package org.zecdev.zip321.parser

import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.RecipientAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// NOTE (K0/v2): these were white-box tests of the kudzu parser-combinator
// objects. They now exercise the equivalent hand-rolled parser helpers, with
// every input and expected accept/reject/value preserved unchanged.
//
// NOTE (K1/v2): converted from kotest FreeSpec to kotlin.test. Amounts
// previously built with `BigDecimal(1)` / `BigDecimal(2)` (JVM-only setup
// sugar) use the equivalent common `NonNegativeAmount(String)` constructor;
// the resulting zatoshi values are unchanged. Two kotest *containers* that
// carried assertions directly ("fails on leading zero many digits" and
// "fails on too many digits") are now regular tests.
class SubParserTests {

    // paramindex subparser

    @Test
    fun `parses non-zero single digit`() {
        assertEquals(
            1u,
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("1")
        )

        assertEquals(
            9u,
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("9")
        )
    }

    @Test
    fun `fails on zero single digit`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("0")
        }
    }

    @Test
    fun `parses many digits`() {
        assertEquals(
            12u,
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("12")
        )
        assertEquals(
            123u,
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("123")
        )
    }

    @Test
    fun `fails on leading zero many digits`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("090")
        }
    }

    @Test
    fun `fails on too many digits`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("19999")
        }
    }

    // Optionally IndexedParameter Name parsing

    @Test
    fun `parses a non-indexed parameter`() {
        assertEquals(
            Pair<String, UInt?>("address", null),
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address")
        )
    }

    @Test
    fun `parses a indexed parameter`() {
        assertEquals(
            Pair<String, UInt?>("address", 123u),
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address.123")
        )
    }

    @Test
    fun `fails to parse a zero-index parameter`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address.0")
        }
    }

    @Test
    fun `fails to parse leading zero parameter`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address.023")
        }
    }

    @Test
    fun `fails to parse a parameter with an index greater than 9999`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address.19999")
        }
    }

    @Test
    fun `fails to parse a paramname with invalid characters`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("add[ress[1].1")
        }
    }

    // Query and Key parser

    @Test
    fun `parses a query key with no index`() {
        val parsedQueryParam = Parser(ParserContext.TESTNET, addressValidation = null)
            .parseQueryKeyAndValue("address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        assertEquals("address", parsedQueryParam.first.first)
        assertEquals(null, parsedQueryParam.first.second)
        assertEquals("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", parsedQueryParam.second)
    }

    @Test
    fun `parses a query key with a valid index`() {
        val parsedQueryParam = Parser(ParserContext.TESTNET, addressValidation = null)
            .parseQueryKeyAndValue("address.123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

        assertEquals("address", parsedQueryParam.first.first)
        assertEquals(123u, parsedQueryParam.first.second)
        assertEquals("tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU", parsedQueryParam.second)
    }

    @Test
    fun `fails to parse a query key with invalid index`() {
        assertFails {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue("address.00123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
        }
    }

    // query key parsing tests

    @Test
    fun `parser catches query qcharencoded values`() {
        assertEquals(
            Pair(Pair("message", 1u), "Thank%20You%20For%20Your%20Purchase"),
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue("message.1=Thank%20You%20For%20Your%20Purchase")
        )
    }

    @Test
    fun `Zcash parameter creates valid amount`() {
        val query = "amount"
        val value = "1.00020112"
        val index = 1u
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
        assertEquals(
            IndexedParameter(1u, Param.Amount(amount = NonNegativeAmount(value))),
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input)
        )
    }

    @Test
    fun `Zcash parameter creates valid message`() {
        val query = "message"
        val index = 1u
        val value = "Thank%20You%20For%20Your%20Purchase"
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
        val qcharDecodedValue = value.qcharDecode()
        assertNotEquals("", qcharDecodedValue)

        assertEquals(
            IndexedParameter(1u, Param.Message(qcharDecodedValue)),
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input)
        )
    }

    @Test
    fun `Zcash parameter creates valid label`() {
        val query = "label"
        val index = 1u
        val value = "Thank%20You%20For%20Your%20Purchase"
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
        val qcharDecodedValue = value.qcharDecode()
        assertNotEquals("", qcharDecodedValue)

        assertEquals(
            IndexedParameter(1u, Param.Label(qcharDecodedValue)),
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input)
        )
    }

    @Test
    fun `Zcash parameter creates valid memo`() {
        val query = "memo"
        val index = 99u
        val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
        val memo = MemoBytes.fromBase64URL(value)
        assertEquals(
            IndexedParameter(99u, Param.Memo(memo)),
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input)
        )
    }

    @Test
    fun `Zcash parameter creates valid memo that contains UTF-8 characters`() {
        val query = "memo"
        val index = 99u
        val value = "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
        assertEquals(
            input,
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue(
                    "memo.99=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
                )
        )
    }

    @Test
    fun `Zcash parameter creates safely ignored other parameter`() {
        val query = "future-binary-format"
        val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
        val input = Pair<Pair<String, UInt?>, String>(Pair(query, null), value)
        assertEquals(
            IndexedParameter(0u, Param.Other(ParamNameString(query), value)),
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input)
        )
    }

    // Parses many parameters in a row

    @Test
    fun `Index parameters are parsed with no leading address`() {
        val remainingString = "?address=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val expected = listOf(
            IndexedParameter(0u, Param.Address(recipient)),
            IndexedParameter(0u, Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(0u, Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(0u, Param.Message("Thank you for your purchase"))
        )

        assertEquals(
            expected,
            Parser(ParserContext.TESTNET, addressValidation = null).parseParameters(remainingString, null)
        )
    }

    @Test
    fun `Index parameters are parsed with leading address`() {
        val remainingString = "?amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

        val recipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val expected = listOf(
            IndexedParameter(0u, Param.Address(recipient)),
            IndexedParameter(0u, Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(0u, Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(0u, Param.Message("Thank you for your purchase"))
        )

        val leadingAddress = IndexedParameter(0u, Param.Address(recipient))

        assertEquals(
            expected,
            Parser(ParserContext.TESTNET, addressValidation = null).parseParameters(remainingString, leadingAddress)
        )
    }

    // Duplicate Params are caught

    @Test
    fun `Duplicate other params are detected`() {
        val params = listOf(
            Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)),
            Param.Amount(NonNegativeAmount("1")),
            Param.Message("Thanks"),
            Param.Label("payment"),
            Param.Other(ParamNameString("future"), "is awesome")
        )

        assertTrue(params.hasDuplicateParam(Param.Other(ParamNameString("future"), "is dystopic")))
    }

    @Test
    fun `Duplicate address params are detected`() {
        val params = listOf(
            Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)),
            Param.Amount(NonNegativeAmount("1")),
            Param.Message("Thanks"),
            Param.Label("payment"),
            Param.Other(ParamNameString("future"), "is awesome")
        )

        assertTrue(params.hasDuplicateParam(Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET))))
    }

    // Payment can be created from uniquely indexed Params

    @Test
    fun `Payment is created from indexed parameters`() {
        val recipient = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val params = listOf(
            Param.Address(recipient),
            Param.Amount(NonNegativeAmount("1")),
            Param.Message("Thanks"),
            Param.Label("payment"),
            Param.Other(ParamNameString("future"), "is awesome")
        )

        val payment = Payment.fromUniqueIndexedParameters(index = 1u, parameters = params)

        assertEquals(
            Payment(
                recipientAddress = recipient,
                nonNegativeAmount = NonNegativeAmount("1"),
                memo = null,
                label = "payment",
                message = "Thanks",
                otherParams = listOf(OtherParam(ParamNameString("future"), "is awesome"))
            ),
            payment
        )
    }

    @Test
    fun `duplicate addresses are detected`() {
        val shieldedRecipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val duplicateAddressParams: List<IndexedParameter> = listOf(
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Label("payment")),
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
        )

        val error = assertFailsWith<ZIP321.Errors> {
            Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateAddressParams)
        }
        assertEquals(ZIP321.Errors.DuplicateParameter("address", null), error)
    }

    @Test
    fun `duplicate amounts are detected`() {
        val shieldedRecipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val duplicateAmountParams: List<IndexedParameter> = listOf(
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Label("payment")),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("2"))),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
        )

        val error = assertFailsWith<ZIP321.Errors> {
            Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateAmountParams)
        }
        assertEquals(ZIP321.Errors.DuplicateParameter("amount", null), error)
    }

    @Test
    fun `duplicate message are detected`() {
        val shieldedRecipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val duplicateParams: List<IndexedParameter> = listOf(
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Label("payment")),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("2"))),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
        )

        val error = assertFailsWith<ZIP321.Errors> {
            Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
        }
        assertEquals(ZIP321.Errors.DuplicateParameter("message", null), error)
    }

    @Test
    fun `duplicate memos are detected`() {
        val shieldedRecipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val duplicateParams: List<IndexedParameter> = listOf(
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Label("payment")),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
        )

        val error = assertFailsWith<ZIP321.Errors> {
            Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
        }
        assertEquals(ZIP321.Errors.DuplicateParameter("memo", null), error)
    }

    @Test
    fun `duplicate other params are detected`() {
        val shieldedRecipient =
            RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

        val duplicateParams: List<IndexedParameter> = listOf(
            IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
            IndexedParameter(index = 0u, param = Param.Label("payment")),
            IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount("1"))),
            IndexedParameter(index = 0u, param = Param.Message("Thanks")),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is dystopian")),
            IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
            IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
        )

        val error = assertFailsWith<ZIP321.Errors> {
            Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
        }
        assertEquals(ZIP321.Errors.DuplicateParameter("future", null), error)
    }
}
