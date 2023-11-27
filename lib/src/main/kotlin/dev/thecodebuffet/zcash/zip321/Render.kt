package dev.thecodebuffet.zcash.zip321

import Amount
import MemoBytes
import Payment
import PaymentRequest
import RecipientAddress
import dev.thecodebuffet.zcash.zip321.extensions.qcharEncoded

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
        return "$label${parameterIndex(index)}=$qcharValue"
    }

    fun parameter(amount: Amount, index: UInt?): String {
        return "${ParamName.AMOUNT.value}${parameterIndex(index)}=$amount"
    }

    fun parameter(memo: MemoBytes, index: UInt?): String {
        return "${ParamName.MEMO.value}${parameterIndex(index)}=${memo.toBase64URL()}"
    }

    fun parameter(address: RecipientAddress, index: UInt?, omittingAddressLabel: Boolean = false): String {
        return if (index == null && omittingAddressLabel) {
            address.value
        } else {
            "${ParamName.ADDRESS.value}${parameterIndex(index)}=${address.value}"
        }
    }

    fun parameterLabel(label: String, index: UInt?): String {
        return parameter(ParamName.LABEL.value, label, index) ?: ""
    }

    fun parameterMessage(message: String, index: UInt?): String {
        return parameter(ParamName.MESSAGE.value, message, index) ?: ""
    }

    fun payment(payment: Payment, index: UInt?, omittingAddressLabel: Boolean = false): String {
        var result = ""

        result += parameter(payment.recipientAddress, index, omittingAddressLabel)

        if (index == null && omittingAddressLabel) {
            result += "?"
        } else {
            result += "&"
        }

        result += "${parameter(payment.amount, index)}"

        payment.memo?.let { result += "&${parameter(it, index)}" }
        payment.label?.let { result += "&${parameterLabel(label = it, index)}" }
        payment.message?.let { result += "&${parameterMessage(message = it, index)}" }

        return result
    }

    fun request(paymentRequest: PaymentRequest, startIndex: UInt?, omittingFirstAddressLabel: Boolean = false): String {
        var result = "zcash:"
        val payments = paymentRequest.payments.toMutableList()
        val paramIndexOffset: UInt = (startIndex ?: 1) as UInt

        if (startIndex == null) {
            result += if (omittingFirstAddressLabel) "" else "?"
            result += payment(payments[0], startIndex, omittingFirstAddressLabel)
            payments.removeFirst()

            if (payments.isNotEmpty()) {
                result += "&"
            }
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
