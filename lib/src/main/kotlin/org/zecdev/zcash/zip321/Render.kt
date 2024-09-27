package org.zecdev.zcash.zip321

import MemoBytes
import NonNegativeAmount
import Payment
import PaymentRequest
import RecipientAddress
import org.zecdev.zcash.zip321.extensions.qcharEncoded

enum class ParamName(val value: String) {
    ADDRESS("address"),
    AMOUNT("amount"),
    LABEL("label"),
    MEMO("memo"),
    MESSAGE("message")
}

object Render {
    private fun parameterIndex(idx: UInt?): String {
        return idx?.let { ".$it" } ?: ""
    }

    fun parameter(label: String, value: String, index: UInt?): String? {
        val qcharValue = value.qcharEncoded() ?: return null
        return "$label${org.zecdev.zcash.zip321.Render.parameterIndex(index)}=$qcharValue"
    }

    fun parameter(nonNegativeAmount: NonNegativeAmount, index: UInt?): String {
        return "${org.zecdev.zcash.zip321.ParamName.AMOUNT.value}${
            org.zecdev.zcash.zip321.Render.parameterIndex(
                index
            )
        }=$nonNegativeAmount"
    }

    fun parameter(memo: MemoBytes, index: UInt?): String {
        return "${org.zecdev.zcash.zip321.ParamName.MEMO.value}${
            org.zecdev.zcash.zip321.Render.parameterIndex(
                index
            )
        }=${memo.toBase64URL()}"
    }

    fun parameter(address: RecipientAddress, index: UInt?, omittingAddressLabel: Boolean = false): String {
        return if (index == null && omittingAddressLabel) {
            address.value
        } else {
            "${org.zecdev.zcash.zip321.ParamName.ADDRESS.value}${
                org.zecdev.zcash.zip321.Render.parameterIndex(
                    index
                )
            }=${address.value}"
        }
    }

    fun parameterLabel(label: String, index: UInt?): String {
        return org.zecdev.zcash.zip321.Render.parameter(
            org.zecdev.zcash.zip321.ParamName.LABEL.value,
            label,
            index
        ) ?: ""
    }

    fun parameterMessage(message: String, index: UInt?): String {
        return org.zecdev.zcash.zip321.Render.parameter(
            org.zecdev.zcash.zip321.ParamName.MESSAGE.value,
            message,
            index
        ) ?: ""
    }

    fun payment(payment: Payment, index: UInt?, omittingAddressLabel: Boolean = false): String {
        var result = ""

        result += org.zecdev.zcash.zip321.Render.parameter(
            payment.recipientAddress,
            index,
            omittingAddressLabel
        )

        if (index == null && omittingAddressLabel) {
            result += "?"
        } else {
            result += "&"
        }

        result += "${org.zecdev.zcash.zip321.Render.parameter(payment.nonNegativeAmount, index)}"

        payment.memo?.let { result += "&${org.zecdev.zcash.zip321.Render.parameter(it, index)}" }
        payment.label?.let { result += "&${
            org.zecdev.zcash.zip321.Render.parameterLabel(
                label = it,
                index
            )
        }" }
        payment.message?.let { result += "&${
            org.zecdev.zcash.zip321.Render.parameterMessage(
                message = it,
                index
            )
        }" }

        return result
    }

    fun request(paymentRequest: PaymentRequest, startIndex: UInt?, omittingFirstAddressLabel: Boolean = false): String {
        var result = "zcash:"
        val payments = paymentRequest.payments.toMutableList()
        val paramIndexOffset: UInt = startIndex ?: 1u

        if (startIndex == null) {
            result += if (omittingFirstAddressLabel) "" else "?"
            result += org.zecdev.zcash.zip321.Render.payment(
                payments[0],
                startIndex,
                omittingFirstAddressLabel
            )
            payments.removeFirst()

            if (payments.isNotEmpty()) {
                result += "&"
            }
        }

        val count = payments.size

        for ((elementIndex, element) in payments.withIndex()) {
            val paramIndex = elementIndex.toUInt() + paramIndexOffset
            result += org.zecdev.zcash.zip321.Render.payment(element, paramIndex)

            if (paramIndex < count.toUInt()) {
                result += "&"
            }
        }

        return result
    }
}
