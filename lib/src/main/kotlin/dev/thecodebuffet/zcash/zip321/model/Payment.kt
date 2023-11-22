data class Payment(
    val recipientAddress: RecipientAddress,
    val amount: Amount,
    val memo: MemoBytes?,
    val label: String?,
    val message: String?,
    val otherParams: List<RequestParams>?
)