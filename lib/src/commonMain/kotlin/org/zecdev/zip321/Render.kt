package org.zecdev.zip321

import org.zecdev.zip321.extensions.qcharEncoded
import org.zecdev.zip321.model.MemoBytes
import org.zecdev.zip321.model.NonNegativeAmount
import org.zecdev.zip321.model.Payment
import org.zecdev.zip321.model.PaymentRequest
import org.zecdev.zip321.model.RecipientAddress

enum class ParamName(val value: String) {
    ADDRESS("address"),
    AMOUNT("amount"),
    LABEL("label"),
    MEMO("memo"),
    MESSAGE("message"),
}

object Render {
    private fun parameterIndex(idx: UInt?): String {
        return idx?.let { ".$it" } ?: ""
    }

    fun otherParameter(
        key: String,
        maybeValue: String?,
        index: UInt?,
    ): String {
        return maybeValue?.let {
            parameter(key, it, index)
        } ?: "$key${parameterIndex(index)}"
    }

    fun parameter(
        label: String,
        value: String,
        index: UInt?,
    ): String {
        val qcharValue = value.qcharEncoded()
        return "$label${parameterIndex(index)}=$qcharValue"
    }

    fun parameter(
        amount: NonNegativeAmount,
        index: UInt?,
    ): String {
        return "${ParamName.AMOUNT.value}${
            parameterIndex(
                index,
            )
        }=${amount.decimalString()}"
    }

    fun parameter(
        memo: MemoBytes,
        index: UInt?,
    ): String {
        return "${ParamName.MEMO.value}${
            parameterIndex(
                index,
            )
        }=${memo.toBase64URL()}"
    }

    fun parameter(
        address: RecipientAddress,
        index: UInt?,
        omittingAddressLabel: Boolean = false,
    ): String {
        return if (index == null && omittingAddressLabel) {
            address.value
        } else {
            "${ParamName.ADDRESS.value}${
                parameterIndex(
                    index,
                )
            }=${address.value}"
        }
    }

    fun parameterLabel(
        label: String,
        index: UInt?,
    ): String {
        return parameter(
            ParamName.LABEL.value,
            label,
            index,
        ) ?: ""
    }

    fun parameterMessage(
        message: String,
        index: UInt?,
    ): String {
        return parameter(
            ParamName.MESSAGE.value,
            message,
            index,
        ) ?: ""
    }

    fun payment(
        payment: Payment,
        index: UInt?,
        omittingAddressLabel: Boolean = false,
    ): String {
        var result = ""

        result +=
            parameter(
                payment.recipientAddress,
                index,
                omittingAddressLabel,
            )

        if (index == null && omittingAddressLabel) {
            result += "?"
        }

        payment.amount?.let {
            if (result.last() != '?') {
                result += "&"
            }
            result += parameter(it, index)
        }
        payment.memo?.let {
            if (result.last() != '?') {
                result += "&"
            }
            result += parameter(it, index)
        }
        payment.label?.let {
            if (result.last() != '?') {
                result += "&"
            }
            result +=
                parameterLabel(
                    label = it,
                    index,
                )
        }
        payment.message?.let {
            if (result.last() != '?') {
                result += "&"
            }
            result +=
                parameterMessage(
                    message = it,
                    index,
                )
        }

        for (param in payment.otherParams) {
            if (result.last() != '?') {
                result += "&"
            }
            result += otherParameter(param.name, param.value, index)
        }

        // Leading-address form with no query params renders as the bare address:
        // `zcash:<addr>`, not `zcash:<addr>?` (matching the reference `to_uri`).
        // This is now reachable because `zcash:<addr>` parses to an ordinary
        // one-payment request rather than to a distinct "single address" result.
        return result.removeSuffix("?")
    }

    fun request(
        paymentRequest: PaymentRequest,
        startIndex: UInt?,
        omittingFirstAddressLabel: Boolean = false,
    ): String {
        var result = "zcash:"
        val payments = paymentRequest.payments.toMutableList()
        val paramIndexOffset: UInt = startIndex ?: 1u

        // An empty request renders as the bare `zcash:` scheme.
        if (payments.isEmpty()) {
            return result
        }

        if (startIndex == null) {
            result += if (omittingFirstAddressLabel) "" else "?"
            result +=
                payment(
                    payments[0],
                    startIndex,
                    omittingFirstAddressLabel,
                )
            if (payments.isNotEmpty()) {
                payments.removeAt(0)
            }
            if (payments.isNotEmpty()) {
                result += "&"
            }
        } else {
            result += "?"
        }

        val count = payments.size

        for ((elementIndex, element) in payments.withIndex()) {
            val paramIndex = elementIndex.toUInt() + paramIndexOffset
            result += payment(element, paramIndex)

            if (paramIndex < count.toUInt()) {
                result += "&"
            }
        }

        return result
    }
}
