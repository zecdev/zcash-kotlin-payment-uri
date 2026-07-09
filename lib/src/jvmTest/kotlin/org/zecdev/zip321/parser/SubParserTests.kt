package org.zecdev.zip321.parser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.RecipientAddress
import java.math.BigDecimal

// NOTE (K0/v2): these were white-box tests of the kudzu parser-combinator
// objects. They now exercise the equivalent hand-rolled parser helpers, with
// every input and expected accept/reject/value preserved unchanged.
class SubParserTests : FreeSpec({
    "paramindex subparser" - {
        "parses non-zero single digit" {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("1") shouldBe 1u

            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("9") shouldBe 9u
        }

        "fails on zero single digit" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseParameterIndex("0")
            }
        }

        "parses many digits" {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("12") shouldBe 12u
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseParameterIndex("123") shouldBe 123u
        }

        "fails on leading zero many digits" - {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseParameterIndex("090")
            }
        }

        "fails on too many digits" - {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseParameterIndex("19999")
            }
        }
    }
    "Optionally IndexedParameter Name parsing" - {
        "parses a non-indexed parameter" {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address") shouldBe Pair<String, UInt?>("address", null)
        }
        "parses a indexed parameter" {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseOptionallyIndexedParamName("address.123") shouldBe Pair<String, UInt?>("address", 123u)
        }
        "fails to parse a zero-index parameter" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseOptionallyIndexedParamName("address.0")
            }
        }
        "fails to parse leading zero parameter" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseOptionallyIndexedParamName("address.023")
            }
        }

        "fails to parse a parameter with an index greater than 9999" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseOptionallyIndexedParamName("address.19999")
            }
        }

        "fails to parse a paramname with invalid characters" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseOptionallyIndexedParamName("add[ress[1].1")
            }
        }
    }
    "Query and Key parser" - {
        "parses a query key with no index" {
            val parsedQueryParam = Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue("address=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

            parsedQueryParam.first.first shouldBe "address"
            parsedQueryParam.first.second shouldBe null
            parsedQueryParam.second shouldBe "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        }

        "parses a query key with a valid index" {
            val parsedQueryParam = Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue("address.123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")

            parsedQueryParam.first.first shouldBe "address"
            parsedQueryParam.first.second shouldBe 123u
            parsedQueryParam.second shouldBe "tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU"
        }

        "fails to parse a query key with invalid index" {
            shouldThrowAny {
                Parser(ParserContext.TESTNET, addressValidation = null)
                    .parseQueryKeyAndValue("address.00123=tmEZhbWHTpdKMw5it8YDspUXSMGQyFwovpU")
            }
        }
    }

    "query key parsing tests" - {
        "parser catches query qcharencoded values" {
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue("message.1=Thank%20You%20For%20Your%20Purchase") shouldBe
                Pair(Pair("message", 1u), "Thank%20You%20For%20Your%20Purchase")
        }

        "Zcash parameter creates valid amount" {
            val query = "amount"
            val value = "1.00020112"
            val index = 1u
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input) shouldBe
                IndexedParameter(1u, Param.Amount(amount = NonNegativeAmount(value)))
        }

        "Zcash parameter creates valid message" {
            val query = "message"
            val index = 1u
            val value = "Thank%20You%20For%20Your%20Purchase"
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
            val qcharDecodedValue = value.qcharDecode()
            qcharDecodedValue shouldNotBe ""

            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input) shouldBe
                IndexedParameter(1u, Param.Message(qcharDecodedValue))
        }

        "Zcash parameter creates valid label" {
            val query = "label"
            val index = 1u
            val value = "Thank%20You%20For%20Your%20Purchase"
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
            val qcharDecodedValue = value.qcharDecode()
            qcharDecodedValue shouldNotBe ""

            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input) shouldBe
                IndexedParameter(1u, Param.Label(qcharDecodedValue))
        }

        "Zcash parameter creates valid memo" {
            val query = "memo"
            val index = 99u
            val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
            val memo = MemoBytes.fromBase64URL(value)
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input) shouldBe
                IndexedParameter(99u, Param.Memo(memo))
        }

        "Zcash parameter creates valid memo that contains UTF-8 characters" {
            val query = "memo"
            val index = 99u
            val value = "VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, index), value)
            Parser(ParserContext.TESTNET, addressValidation = null)
                .parseQueryKeyAndValue(
                    "memo.99=VGhpcyBpcyBhIHVuaWNvZGUgbWVtbyDinKjwn6aE8J-PhvCfjok"
                ) shouldBe input
        }

        "Zcash parameter creates safely ignored other parameter" {
            val query = "future-binary-format"
            val value = "VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"
            val input = Pair<Pair<String, UInt?>, String>(Pair(query, null), value)
            Parser(ParserContext.TESTNET, addressValidation = null).zcashParameter(input) shouldBe
                IndexedParameter(0u, Param.Other(ParamNameString(query), value))
        }
    }

    "Parses many parameters in a row" - {
        "Index parameters are parsed with no leading address" {
            val remainingString = "?address=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez&amount=1&memo=VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg&message=Thank%20you%20for%20your%20purchase"

            val recipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val expected = listOf(
                IndexedParameter(0u, Param.Address(recipient)),
                IndexedParameter(0u, Param.Amount(NonNegativeAmount("1"))),
                IndexedParameter(0u, Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(0u, Param.Message("Thank you for your purchase"))
            )

            Parser(ParserContext.TESTNET, addressValidation = null).parseParameters(remainingString, null) shouldBe expected
        }
    }

    "Index parameters are parsed with leading address" {
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

        Parser(ParserContext.TESTNET, addressValidation = null).parseParameters(remainingString, leadingAddress) shouldBe expected
    }

    "Duplicate Params are caught" - {
        "Duplicate other params are detected" {
            val params = listOf(
                Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)),
                Param.Amount(NonNegativeAmount("1")),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other(ParamNameString("future"), "is awesome")
            )

            params.hasDuplicateParam(Param.Other(ParamNameString("future"), "is dystopic")) shouldBe true
        }

        "Duplicate address params are detected" {
            val params = listOf(
                Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)),
                Param.Amount(NonNegativeAmount("1")),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other(ParamNameString("future"), "is awesome")
            )

            params.hasDuplicateParam(Param.Address(RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET))) shouldBe true
        }
    }

    "Payment can be created from uniquely indexed Params" - {
        "Payment is created from indexed parameters" {
            val recipient = RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val params = listOf(
                Param.Address(recipient),
                Param.Amount(NonNegativeAmount("1")),
                Param.Message("Thanks"),
                Param.Label("payment"),
                Param.Other(ParamNameString("future"), "is awesome")
            )

            val payment = Payment.fromUniqueIndexedParameters(index = 1u, parameters = params)

            payment shouldBe Payment(
                recipientAddress = recipient,
                nonNegativeAmount = NonNegativeAmount("1"),
                memo = null,
                label = "payment",
                message = "Thanks",
                otherParams = listOf(OtherParam(ParamNameString("future"), "is awesome"))
            )
        }

        "duplicate addresses are detected" {
            val shieldedRecipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val duplicateAddressParams: List<IndexedParameter> = listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(1)
                ))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
            )

            shouldThrow<ZIP321.Errors> {
                Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateAddressParams)
            } shouldBe ZIP321.Errors.DuplicateParameter("address", null)
        }

        "duplicate amounts are detected" {
            val shieldedRecipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val duplicateAmountParams: List<IndexedParameter> = listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(1)
                ))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(2)
                ))),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
            )

            shouldThrow<ZIP321.Errors> {
                Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateAmountParams)
            } shouldBe ZIP321.Errors.DuplicateParameter("amount", null)
        }

        "duplicate message are detected" {
            val shieldedRecipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val duplicateParams: List<IndexedParameter> = listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(1)
                ))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(2)
                ))),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
            )

            shouldThrow<ZIP321.Errors> {
                Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
            } shouldBe ZIP321.Errors.DuplicateParameter("message", null)
        }

        "duplicate memos are detected" {
            val shieldedRecipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val duplicateParams: List<IndexedParameter> = listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(1)
                ))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
            )

            shouldThrow<ZIP321.Errors> {
                Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
            } shouldBe ZIP321.Errors.DuplicateParameter("memo", null)
        }

        "duplicate other params are detected" {
            val shieldedRecipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", ParserContext.TESTNET)

            val duplicateParams: List<IndexedParameter> = listOf(
                IndexedParameter(index = 0u, param = Param.Address(shieldedRecipient)),
                IndexedParameter(index = 0u, param = Param.Label("payment")),
                IndexedParameter(index = 0u, param = Param.Amount(NonNegativeAmount(
                    value = BigDecimal(1)
                ))),
                IndexedParameter(index = 0u, param = Param.Message("Thanks")),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is dystopian")),
                IndexedParameter(index = 0u, param = Param.Memo(MemoBytes.fromBase64URL("VGhpcyBpcyBhIHNpbXBsZSBtZW1vLg"))),
                IndexedParameter(index = 0u, param = Param.Other(ParamNameString("future"), "is awesome"))
            )

            shouldThrow<ZIP321.Errors> {
                Parser(ParserContext.TESTNET, addressValidation = null).mapToPayments(duplicateParams)
            } shouldBe ZIP321.Errors.DuplicateParameter("future", null)
        }
    }
})
