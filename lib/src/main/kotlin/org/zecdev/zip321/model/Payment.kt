package org.zecdev.zip321.model

import org.zecdev.zip321.parser.ParamNameString

data class Payment(
    val recipientAddress: RecipientAddress,
    val nonNegativeAmount: NonNegativeAmount?,
    val memo: MemoBytes?,
    val label: String?,
    val message: String?,
    val otherParams: List<OtherParam>?
) {
    fun isSingleAddress(): Boolean {
        return nonNegativeAmount == null &&
                memo == null &&
                label == null &&
                message == null &&
                otherParams == null
    }

    @Suppress("EmptyClassBlock")
    companion object {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false

        if (recipientAddress != other.recipientAddress) return false
        if (nonNegativeAmount != other.nonNegativeAmount) return false
        if (memo != other.memo) return false
        if (label != other.label) return false
        if (message != other.message) return false
        if (otherParams != other.otherParams) return false

        return true
    }

    override fun hashCode(): Int {
        var result = recipientAddress.hashCode()
        result = 31 * result + (nonNegativeAmount?.hashCode() ?:0)
        result = 31 * result + (memo?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (otherParams?.hashCode() ?: 0)
        return result
    }
}

data class OtherParam(val key: ParamNameString, val value: String?) {
    override fun toString(): String {
        if (value == null) {
            return key.toString()
        }
        return "$key=$value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OtherParam) return false

        if (key != other.key) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}
