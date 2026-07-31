package org.zecdev.zip321.parser

import org.zecdev.zip321.AddressValidator
import org.zecdev.zip321.Network
import org.zecdev.zip321.ParamName
import org.zecdev.zip321.ZIP321
import org.zecdev.zip321.encodings.QCharCodec
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.RecipientAddress

internal sealed class Param {
    companion object {
        @Throws(ZIP321.Errors::class)
        @Suppress("CyclomaticComplexMethod", "ThrowsCount", "LongMethod")
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

            // NOTE: no payment-count/index cap is enforced here. The `paramindex` grammar
            // (`NONZERO 0*3DIGIT`, at most 4 digits) already bounds every index to 9999, and
            // `PaymentRequest`'s MAX_PAYMENT_COUNT cap covers programmatic construction. The v1
            // `maxPaymentsAllowed = 2109` remnant that rejected indices in [2108, 9999] was
            // removed (K13).
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

                    // Strict ZIP-321 `amountparam` grammar via `NonNegativeAmount`.
                    Amount(AmountParser.parse(value, index))
                }
                ParamName.LABEL.value -> {
                    // LABEL param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }
                    Label(decodeQcharValue(value, queryKey, index))
                }
                ParamName.MESSAGE.value -> {
                    // MESSAGE param can't have no value
                    if (value == null) {
                        throw ZIP321.Errors.InvalidParamValue(queryKey, index)
                    }
                    Message(decodeQcharValue(value, queryKey, index))
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
                    // `otherparam` values are percent-decoded per the `qchar` grammar (matching
                    // the reference), then preserved. An absent value (no `=`) stays `null`.
                    Other(queryKey, value?.let { decodeQcharValue(it, queryKey, index) })
                }
            }
        }

        /**
         * Strictly percent-decodes a `label`/`message`/`otherparam` value per the ZIP-321 `qchar`
         * grammar, mapping a decode failure onto [ZIP321.Errors.QcharDecodeFailed]. The URI
         * tokenizer already restricts the raw value to `qchar` characters / percent-escapes, so
         * the only failures here are a malformed `%XX` escape or a decoded byte sequence that is
         * not valid UTF-8.
         */
        @Throws(ZIP321.Errors::class)
        private fun decodeQcharValue(
            value: String,
            queryKey: String,
            index: UInt,
        ): String =
            QCharCodec.decode(value)
                ?: throw ZIP321.Errors.QcharDecodeFailed(index.mapToParamIndex(), queryKey, value)
    }

    data class Address(val recipientAddress: RecipientAddress) : Param()

    data class Amount(val amount: NonNegativeAmount) : Param()

    data class Memo(val memoBytes: MemoBytes) : Param()

    data class Label(val label: String) : Param()

    data class Message(val message: String) : Param()

    data class Other(val paramName: String, val value: String?) : Param()

    val name: String
        get() =
            when (this) {
                is Address -> ParamName.ADDRESS.name.lowercase()
                is Amount -> ParamName.AMOUNT.name.lowercase()
                is Memo -> ParamName.MEMO.name.lowercase()
                is Label -> ParamName.LABEL.name.lowercase()
                is Message -> ParamName.MESSAGE.name.lowercase()
                is Other -> paramName
            }

    // NOTE (K16): `Param` used to hand-write `equals`/`hashCode` here, dispatching on `this::class`.
    // That override was unreachable dead code: every concrete `Param` is one of the `data class`
    // subtypes below, and Kotlin ALWAYS synthesizes `equals`/`hashCode` for a `data class` (from
    // its own constructor properties) regardless of what the sealed superclass declares — the
    // subtype's synthesized override wins over the superclass's hand-written one via ordinary
    // virtual dispatch. So `Param.Address(x) == Param.Address(y)` was always resolving to
    // `Address`'s auto-generated `equals` (which already checks `other is Address` and compares
    // `recipientAddress`), never to this class's version — confirmed empirically: the removed
    // code had zero coverage no matter how many equality assertions the test suite made. Each
    // subtype's synthesized equals/hashCode is exactly the by-value, type-safe comparison this
    // manual version was trying to hand-roll, so deleting it changes no observable behavior.

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

internal fun List<Param>.hasDuplicateParam(param: Param): Boolean {
    for (i in this) {
        if (i.partiallyEqual(param)) return true else continue
    }
    return false
}
