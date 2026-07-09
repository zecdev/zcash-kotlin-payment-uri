// `LegacyAmount` (the v1 amount type; it carried the `NonNegativeAmount` name before v2) is
// deprecated in favor of the v2 `NonNegativeAmount` but remains in use until the parser adopts
// it (v2 parser rewrite); keep this file warning-free meanwhile.
@file:Suppress("DEPRECATION")

package org.zecdev.zip321.parser

import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network
import org.zecdev.zip321.ParamName
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.ZIP321.Errors.TooManyPayments
import org.zecdev.zip321.encodings.QCharCodec
import org.zecdev.zip321.extensions.qcharDecode
import org.zecdev.zip321.extensions.qcharEncoded
import org.zecdev.zip321.model.LegacyAmount
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.RecipientAddress

sealed class Param {
    companion object {
        @Throws(ZIP321.Errors::class)
        fun from(
            queryKey: String,
            value: String?,
            index: UInt,
            network: Network,
            validator: AddressValidator,
        ): Param {
            if (queryKey.isEmpty()) {
                throw ZIP321.Errors.InvalidParamName("paramName cannot be empty")
            }

            if ((index + 1u) >= ZIP321.maxPaymentsAllowed) {
                throw TooManyPayments(index + 1u)
            }
            return when (queryKey) {
                ParamName.ADDRESS.value -> {
                    // ADDRESS param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }

                    // Address validity is DELEGATED: the caller-supplied
                    // validator is the only authority, and rejecting the
                    // address rejects the request.
                    Address(
                        recipient(value, network, validator)
                            ?: throw ZIP321.Errors.InvalidAddress(if (index > 0u) index else null),
                    )
                }
                ParamName.AMOUNT.value -> {
                    // AMOUNT param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }

                    // Strict ZIP-321 `amountparam` grammar via `NonNegativeAmount`, bridged to
                    // the still-`LegacyAmount`-typed `Payment.nonNegativeAmount`.
                    Amount(LegacyAmount.fromNonNegativeAmount(AmountParser.parse(value, index)))
                }
                ParamName.LABEL.value -> {
                    // LABEL param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }
                    Label(value.qcharDecode())
                }
                ParamName.MESSAGE.value -> {
                    // MESSAGE param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }
                    Message(value.qcharDecode())
                }
                ParamName.MEMO.value -> {
                    // MEMO param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }
                    try {
                        Memo(MemoBytes.fromBase64URL(value))
                    } catch (error: MemoBytes.MemoError) {
                        throw ZIP321.Errors.MemoBytesError(error, index)
                    }
                }
                else -> {
                    if (queryKey.startsWith("req-")) {
                        throw ZIP321.Errors.UnknownRequiredParameter(queryKey)
                    }
                    Other(ParamNameString(queryKey), value?.qcharDecode())
                }
            }
        }
    }

    data class Address(val recipientAddress: RecipientAddress) : Param()

    data class Amount(val amount: LegacyAmount) : Param()

    data class Memo(val memoBytes: MemoBytes) : Param()

    data class Label(val label: String) : Param()

    data class Message(val message: String) : Param()

    data class Other(val paramName: ParamNameString, val value: String?) : Param()

    val name: String
        get() =
            when (this) {
                is Address -> ParamName.ADDRESS.name.lowercase()
                is Amount -> ParamName.AMOUNT.name.lowercase()
                is Memo -> ParamName.MEMO.name.lowercase()
                is Label -> ParamName.LABEL.name.lowercase()
                is Message -> ParamName.MESSAGE.name.lowercase()
                is Other -> paramName.value
            }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Param

        if (name != other.name) return false

        return when (this) {
            is Address -> recipientAddress == (other as? Address)?.recipientAddress
            is Amount -> amount == (other as? Amount)?.amount
            is Memo -> memoBytes == (other as? Memo)?.memoBytes
            is Label -> label == (other as? Label)?.label
            is Message -> message == (other as? Message)?.message
            is Other ->
                (other as? Other)?.let {
                        p ->
                    p.paramName == paramName && p.value == value
                } ?: false
        }
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result +
            when (this) {
                is Address -> recipientAddress.hashCode()
                is Amount -> amount.hashCode()
                is Memo -> memoBytes.hashCode()
                is Label -> label.hashCode()
                is Message -> message.hashCode()
                is Other -> {
                    result = 31 * result + paramName.hashCode()
                    result = 31 * result + value.hashCode()
                    result
                }
            }
        return result
    }

    /**
     * Checks if this `Param` is the same kind of
     * the other given regardless of the value.
     * this is useful to check if a list of `Param`
     * conforming a `Payment` has duplicate query keys
     * telling the porser to fail.
     */
    fun partiallyEqual(other: Param): Boolean {
        if (this === other) return true

        if (name != other.name) return false

        return when (this) {
            is Address -> other is Address
            is Amount -> other is Amount
            is Memo -> other is Memo
            is Label -> other is Label
            is Message -> other is Message
            is Other -> other is Other && other.paramName == paramName
        }
    }
}

fun List<Param>.hasDuplicateParam(param: Param): Boolean {
    for (i in this) {
        if (i.partiallyEqual(param)) return true else continue
    }
    return false
}

/**
 *  A  `paramname` encoded string according to [ZIP-321](https://zips.z.cash/zip-0321)
 *
 *  ZIP-321 defines:
 *  ```
 *   paramname       = ALPHA *( ALPHA / DIGIT / "+" / "-" )
 */
class ParamNameString(val value: String) {
    init {
        // String can't be empty
        require(value.isNotEmpty()) { throw ZIP321.Errors.InvalidParamName(value) }
        // String can't start with a digit, "+" or "-"
        require(value.first().isAsciiLetter())
        // The whole String conforms to the character set defined in ZIP-321
        require(
            value.map {
                CharsetValidations.Companion.ParamNameCharacterSet.characters.contains(it)
            }.reduce { acc, b -> acc && b },
        ) {
            throw ZIP321.Errors.InvalidParamName(value)
        }
    }

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParamNameString) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

class QcharString private constructor(private val encoded: String) {
    companion object {
        /**
         * Initializes a [QcharString] from a non-qchar-encoded input string.
         *
         * This constructor checks whether decoding the input string would change it,
         * in order to avoid nested or duplicate encodings.
         *
         * @param value The raw string to be qchar-encoded. The empty string is a valid
         *              (zero-length) `*qchar` value and is accepted.
         * @param strict If `true`, the initializer will fail if decoding the input string
         *                   yields a different result — which suggests the input is already qchar-encoded.
         *
         * @return A [QcharString] instance, or `null` if strict mode detects an issue.
         */
        fun from(
            value: String,
            strict: Boolean = false,
        ): QcharString? {
            // check whether value is already qchar-encoded or partially
            if (strict) {
                val qcharDecode = QCharCodec.decode(value) ?: return null

                if (qcharDecode != value) return null
            }

            return QcharString(value.qcharEncoded())
        }
    }

    fun stringValue(): String {
        return encoded.qcharDecode()
    }

    fun qcharValue(): String {
        return encoded
    }
}
