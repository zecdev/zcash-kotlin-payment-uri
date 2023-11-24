typealias RequestParams = Pair<String, String>

class RecipientAddress private constructor(val value: String) {
    sealed class RecipientAddressError(message: String) : Exception(message) {
        object InvalidRecipient : RecipientAddressError("The provided recipient is invalid")
    }

    /**
     * Initialize an opaque Recipient address that's convertible to a String with or without a
     * validating function.
     * @param value the string representing the recipient
     * @param validating a closure that validates the given input.
     * @throws `null` if the validating function resolves the input as invalid,
     * or a [RecipientAddress] if the input is valid or no validating closure is passed.
     */
    @Throws(RecipientAddressError::class)
    constructor(value: String, validating: ((String) -> Boolean)? = null) : this(
        when (validating?.invoke(value)) {
            null, true -> value
            false -> throw RecipientAddressError.InvalidRecipient
        }
    )
}
