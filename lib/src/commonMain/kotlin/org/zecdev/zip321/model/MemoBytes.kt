package org.zecdev.zip321.model

import org.zecdev.zip321.parser.Base64URL

class MemoBytes {
    companion object {
        const val maxLength: Int = 512
        fun fromBase64URL(string: String): MemoBytes {
            return Base64URL.decode(string)?.let { MemoBytes(it) } ?: throw MemoError.InvalidBase64URL
        }
    }

    val data: ByteArray
    sealed class MemoError(message: String) : RuntimeException(message) {
        object MemoTooLong : MemoError("MemoBytes exceeds max length of 512 bytes") {
            private fun readResolve(): Any = MemoTooLong
        }

        object MemoEmpty : MemoError("MemoBytes can't be initialized with empty bytes") {
            private fun readResolve(): Any = MemoEmpty
        }

        object InvalidBase64URL : MemoError("MemoBytes can't be initialized with invalid Base64URL") {
            private fun readResolve(): Any = InvalidBase64URL
        }
    }

    @Throws(MemoError::class)
    constructor(data: ByteArray) {
        require(data.isNotEmpty()) { throw MemoError.MemoEmpty }
        require(data.size <= maxLength) { throw MemoError.MemoTooLong }

        this.data = data
    }

    @Throws(MemoError::class)
    constructor(string: String) {
        require(string.isNotEmpty()) { throw MemoError.MemoEmpty }
        require(string.length <= maxLength) { throw MemoError.MemoTooLong }

        this.data = string.encodeToByteArray()
    }

    fun toBase64URL(): String {
        return Base64URL.encode(data)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MemoBytes

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * data.contentHashCode()
    }
}
