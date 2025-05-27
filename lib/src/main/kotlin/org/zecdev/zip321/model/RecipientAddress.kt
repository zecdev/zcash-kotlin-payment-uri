package org.zecdev.zip321.model

import org.zecdev.zip321.parser.ParserContext

typealias RequestParams = Pair<String, String>

class RecipientAddress private constructor(
    val value: String,
    val network: ParserContext
) {
    sealed class RecipientAddressError(message: String) : Exception(message) {
        object InvalidRecipient : RecipientAddressError("The provided recipient is invalid") {
            private fun readResolve(): Any = InvalidRecipient
        }
    }

    /**
     * Initialize an opaque Recipient address that's convertible to a String with or without a
     * validating function.
     * @param value the string representing the recipient
     * @param network: The parser context for the network this address would belong to
     * @param validating a closure that validates the given input.
     * @throws `null` if the validating function resolves the input as invalid,
     * or a [RecipientAddress] if the input is valid or no validating closure is passed.
     */
    @Throws(RecipientAddressError::class)
    constructor(value: String, network: ParserContext, validating: ((String) -> Boolean)? = null) : this(
        when (validating?.invoke(value)) {
            null, true -> {
                if (network.isValid(value)) {
                    value
                } else {
                    throw RecipientAddressError.InvalidRecipient
                }

            }
            false -> {
                throw RecipientAddressError.InvalidRecipient
            }
        },
        network
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecipientAddress) return false

        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    fun isTransparent(): Boolean {
        return network.isTransparent(value)
    }
}
