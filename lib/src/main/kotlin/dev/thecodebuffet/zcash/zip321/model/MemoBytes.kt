import java.util.Base64

data class MemoBytes(val data: ByteArray) {
    companion object {
        const val maxLength: Int = 512
    }

    init {
        require(data.isNotEmpty()) { "Memo bytes must not be empty" }
        require(data.size <= maxLength) { "Memo bytes must not exceed the maximum length" }
    }

    fun toBase64URL(): String {
        return Base64.getUrlEncoder().encodeToString(data)
            .replace("/", "_")
            .replace("+", "-")
            .replace("=", "")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoBytes

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}