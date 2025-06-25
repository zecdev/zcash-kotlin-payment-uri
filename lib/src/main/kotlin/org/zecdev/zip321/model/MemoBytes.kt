package org.zecdev.zip321.model

import java.nio.charset.Charset
import java.util.Base64

class MemoBytes {
    companion object {
        const val maxLength: Int = 512
        fun fromBase64URL(string: String): MemoBytes {
            return string.decodeBase64URL()?.let { MemoBytes(it) } ?: throw MemoError.InvalidBase64URL
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
        return 31 * data.contentHashCode()
    }
}

fun String.decodeBase64URL(): ByteArray? {
    return try {
        // Replace Base64URL specific characters
        val base64URL = replace('-', '+').replace('_', '/')

        // Pad the string with '=' characters to make the length a multiple of 4
        val paddedBase64 = base64URL + "=".repeat((4 - base64URL.length % 4) % 4)

        // Decode the Base64 string into a byte array
        Base64.getDecoder().decode(paddedBase64.encodeToByteArray())
    } catch (e: IllegalArgumentException) {
        // Handle decoding failure and return null
        null
    }
}
