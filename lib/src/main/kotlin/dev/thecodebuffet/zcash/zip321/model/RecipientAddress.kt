typealias RequestParams = Pair<String, String>

data class RecipientAddress(val value: String) {
    init {
        require(value.isNotBlank()) { "Recipient address must not be blank" }
    }
}
