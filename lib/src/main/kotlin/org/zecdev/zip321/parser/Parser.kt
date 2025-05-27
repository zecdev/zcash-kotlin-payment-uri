
package org.zecdev.zip321.parser

import com.copperleaf.kudzu.node.mapped.ValueNode
import com.copperleaf.kudzu.parser.ParserContext
import com.copperleaf.kudzu.parser.ParserException
import com.copperleaf.kudzu.parser.chars.AnyCharParser
import com.copperleaf.kudzu.parser.chars.CharInParser
import com.copperleaf.kudzu.parser.chars.DigitParser
import com.copperleaf.kudzu.parser.choice.PredictiveChoiceParser
import com.copperleaf.kudzu.parser.many.ManyParser
import com.copperleaf.kudzu.parser.many.SeparatedByParser
import com.copperleaf.kudzu.parser.many.UntilParser
import com.copperleaf.kudzu.parser.mapped.MappedParser
import com.copperleaf.kudzu.parser.maybe.MaybeParser
import com.copperleaf.kudzu.parser.sequence.SequenceParser
import com.copperleaf.kudzu.parser.text.LiteralTokenParser
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.ZIP321.ParserResult
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.OtherParam
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.CharsetValidations.Companion.QcharCharacterSet

class Parser(
    private val context: org.zecdev.zip321.parser.ParserContext,
    addressValidation: ((String) -> Boolean)?
) {

    val defaultValidation = addressValidation?.let { customValidation ->
        { address: String ->
            context.isValid(address) && customValidation(address)
        }
    }?:
        { address: String ->
            context.isValid(address)
        }

    val maybeLeadingAddressParse = MappedParser(
        SequenceParser(
            LiteralTokenParser("zcash:"),
            MaybeParser(
                AddressTextParser(context)
            )
        )
    ) {
        val addressValue: IndexedParameter? = it.node2.node?.let { textNode ->

            IndexedParameter(
                index = 0u,
                param = Param.Address(
                    RecipientAddress(
                        textNode.text,
                        context,
                        validating = defaultValidation
                    )
                )
            )
        }
        addressValue
    }

    val parameterIndexParser = MappedParser(
        SequenceParser(
            CharInParser(CharRange('1', '9')),
            MaybeParser(
                ManyParser(
                    DigitParser()
                )
            )
        )
    ) {
        val firstDigit = it.node1.text

        (
            firstDigit + it.node2.let { node ->
                if (node.text.length > 3) {
                    throw ZIP321.Errors.InvalidParamIndex(firstDigit + node.text)
                } else {
                    node.text
                }
            }
            ).toUInt()
    }

    val optionallyIndexedParamName = MappedParser(
        SequenceParser(
            UntilParser(
                AnyCharParser(),
                PredictiveChoiceParser(
                    LiteralTokenParser("."),
                    LiteralTokenParser("=")
                )
            ),
            MaybeParser(
                SequenceParser(
                    LiteralTokenParser("."),
                    parameterIndexParser
                )
            )
        )
    ) {
        val paramName = it.node1.text

        if (!paramName.all { c -> CharsetValidations.isValidParamNameChar(c) }) {
            throw ZIP321.Errors.ParseError("Invalid paramname $paramName")
        } else {
            Pair(
                it.node1.text,
                it.node2.node?.node2?.value
            )
        }
    }

    val queryKeyAndValueParser = MappedParser(
        SequenceParser(
            optionallyIndexedParamName,
            LiteralTokenParser("="),
            ManyParser(
                CharInParser(QcharCharacterSet.characters.toList())
            )
        )
    ) {
        Pair(it.node1.value, it.node3.text)
    }

    /**
     * parses a sequence of query parameters lead by query separator char (?)
     */
    private val queryParamsParser = MappedParser(
        SequenceParser(
            LiteralTokenParser("?"),
            SeparatedByParser(
                queryKeyAndValueParser,
                LiteralTokenParser("&")
            )
        )
    ) {
        it.node2.nodeList.map { node -> node.value }
    }

    /**
     * maps a parsed Query Parameter key and value into an `IndexedParameter`
     * providing validation of Query keys and values. An address validation can be provided.
     */
    fun zcashParameter(
        parsedQueryKeyValue: Pair<Pair<String, UInt?>, String>,
        validatingAddress: ((String) -> Boolean)? = null
    ): IndexedParameter {
        val queryKey = parsedQueryKeyValue.first.first
        val queryKeyIndex = parsedQueryKeyValue.first.second?.let {
            if (it == 0u) {
                throw ZIP321.Errors.InvalidParamIndex("$queryKey.0")
            } else {
                it
            }
        } ?: 0u
        val queryValue = parsedQueryKeyValue.second

        val param = Param.from(
            queryKey,
            queryValue,
            queryKeyIndex,
            context,
            validatingAddress
        )

        return IndexedParameter(queryKeyIndex, param)
    }

    /**
     * Parses the rest of the URI after the `zcash:` and possible
     * leading address have been captured, validating the found addresses
     * if validation is provided
     */
    fun parseParameters(
        remainingString: ParserContext,
        leadingAddress: IndexedParameter?,
    ): List<IndexedParameter> {
        val list = ArrayList<IndexedParameter>()

        leadingAddress?.let { list.add(it) }

        list.addAll(
            queryParamsParser.parse(remainingString)
                .first
                .value
                .map { zcashParameter(it, defaultValidation) }
        )

        if (list.isEmpty()) {
            throw ZIP321.Errors.RecipientMissing(null)
        }

        return list
    }

    /**
     * Maps a list of `IndexedParameter` into a list of validated `Payment`
     */
    @Throws(ZIP321.Errors::class)
    fun mapToPayments(indexedParameters: List<IndexedParameter>): List<Payment> {
        if (indexedParameters.isEmpty()) {
            throw ZIP321.Errors.RecipientMissing(null)
        }

        val paramsByIndex: MutableMap<UInt, MutableList<Param>> = mutableMapOf()

        for (idxParam in indexedParameters) {
            val paramVecByIndex = paramsByIndex[idxParam.index]
            if (paramVecByIndex != null) {
                if (paramVecByIndex.hasDuplicateParam(idxParam.param)) {
                    throw ZIP321.Errors.DuplicateParameter(
                        idxParam.param.name,
                        idxParam.index.mapToParamIndex()
                    )
                } else {
                    paramVecByIndex.add(idxParam.param)
                }
            } else {
                paramsByIndex[idxParam.index] = mutableListOf(idxParam.param)
            }
        }

        return paramsByIndex
            .map { (index, parameters) ->
                Payment.fromUniqueIndexedParameters(index, parameters)
            }
    }

    @Throws(ZIP321.Errors::class)
    fun parse(uriString: String): ParserResult {
        try {
            val maybeNode: ValueNode<IndexedParameter?>?
            val maybeRemainingText: ParserContext?
            try {
                val (node, remainingText) = maybeLeadingAddressParse.parse(
                    ParserContext.fromString(uriString)
                )
                maybeNode = node
                maybeRemainingText = remainingText
            } catch (e: ParserException) {
                throw ZIP321.Errors.InvalidAddress(null)
            }


            val leadingAddress = maybeNode.value

            // no remaining text to parse and no address found. Not a valid URI
            if (maybeRemainingText.isEmpty() && leadingAddress == null) {
                throw ZIP321.Errors.InvalidURI
            }

            if (maybeRemainingText.isEmpty() && leadingAddress != null) {
                leadingAddress.let {
                    when (val param = it.param) {
                        is Param.Address -> return ParserResult.SingleAddress(param.recipientAddress)
                        else ->
                            throw ZIP321.Errors.ParseError(
                                "leading parameter after `zcash:` that is not an address"
                            )
                    }
                }
            }

            // remaining text is not empty there's still work to do
            val payments = mapToPayments(
                parseParameters(maybeRemainingText, maybeNode.value)
            )

            return if (payments.size == 1 && payments.first().isSingleAddress()) {
                ParserResult.SingleAddress(payments.first().recipientAddress)
            } else {
                ParserResult.Request(
                    PaymentRequest(
                        payments
                    )
                )
            }
        } catch (e: ParserException) {
            throw ZIP321.Errors.ParseError(e.message)
        }
    }
}

@Suppress("detekt:CyclomaticComplexMethod")
fun Payment.Companion.fromUniqueIndexedParameters(index: UInt, parameters: List<Param>): Payment {
    val recipient = parameters.firstOrNull { param ->
        when (param) {
            is Param.Address -> true
            else -> false
        }
    }?.let { address ->
        when (address) {
            is Param.Address -> address.recipientAddress
            else -> null
        }
    } ?: throw ZIP321.Errors.RecipientMissing(index.mapToParamIndex())

    var amount: NonNegativeAmount? = null
    var memo: MemoBytes? = null
    var label: String? = null
    var message: String? = null
    val other = ArrayList<OtherParam>()

    for (param in parameters) {
        when (param) {
            is Param.Address -> continue
            is Param.Amount -> amount = param.amount
            is Param.Label -> label = param.label
            is Param.Memo -> {
                if (recipient.isTransparent()) {
                    throw ZIP321.Errors.TransparentMemoNotAllowed(index.mapToParamIndex())
                }

                memo = param.memoBytes
            }
            is Param.Message -> message = param.message
            is Param.Other -> other.add(OtherParam(param.paramName, param.value))
        }
    }

    return Payment(
        recipient,
        amount,
        memo,
        label,
        message,
        when (other.isEmpty()) {
            true -> null
            false -> other
        }
    )
}

fun UInt.mapToParamIndex(): UInt? {
    return when (this == 0u) {
        false -> this
        true -> null
    }
}
