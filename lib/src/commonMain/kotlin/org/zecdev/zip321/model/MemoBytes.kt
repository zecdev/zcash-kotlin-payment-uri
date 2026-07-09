package org.zecdev.zip321.model

import org.zecdev.zip321.parser.Base64URL

/**
 * The raw bytes of a ZIP-302 memo attached to a payment, as carried by the ZIP-321 `memo`
 * parameter.
 *
 * Any byte sequence of **0 to 512 bytes** is valid: consensus zero-pads memos to 512 bytes,
 * so a zero-length memo is a well-defined (empty) memo — matching the reference
 * implementation, which accepts an empty byte slice. Note the distinction between an
 * *omitted* memo (`Payment.memo == null`) and an *empty* memo (`memo=` in a URI, 0 bytes).
 */
class MemoBytes {
    /** Namespace for [MemoBytes] construction from an encoded string and the memo size bound. */
    companion object {
        /** The maximum number of bytes a memo may contain (the ZIP-302 consensus zero-padded size). */
        const val maxLength: Int = 512

        /**
         * Initializes a [MemoBytes] from an unpadded
         * [RFC-4648 §5 base64url](https://datatracker.ietf.org/doc/html/rfc4648#section-5)
         * string, as mandated by ZIP-321 for `memo` parameter values.
         * @param string an unpadded base64url string.
         * @throws MemoError.InvalidBase64URL if the string is not a canonical unpadded
         * base64url encoding (`+`, `/`, `=` padding, whitespace, out-of-alphabet characters,
         * impossible lengths, and nonzero trailing bits are all rejected), or
         * [MemoError.MemoTooLong] if it decodes to more than 512 bytes.
         */
        fun fromBase64URL(string: String): MemoBytes {
            return Base64URL.decode(string)?.let { MemoBytes(it) } ?: throw MemoError.InvalidBase64URL
        }
    }

    /** The raw memo bytes (0 to [maxLength] bytes). */
    val data: ByteArray

    /** The errors [MemoBytes] construction can raise. */
    sealed class MemoError(message: String) : RuntimeException(message) {
        /** The provided content exceeds [maxLength] bytes. */
        object MemoTooLong : MemoError("MemoBytes exceeds max length of 512 bytes") {
            private fun readResolve(): Any = MemoTooLong
        }

        /** The string passed to [fromBase64URL] was not a canonical unpadded base64url encoding. */
        object InvalidBase64URL : MemoError("MemoBytes can't be initialized with invalid Base64URL") {
            private fun readResolve(): Any = InvalidBase64URL
        }
    }

    /**
     * Initializes a `MemoBytes` from raw bytes.
     * @param data 0 to 512 bytes of memo content.
     * @throws MemoError.MemoTooLong if more than 512 bytes are provided.
     */
    @Throws(MemoError::class)
    constructor(data: ByteArray) {
        require(data.size <= maxLength) { throw MemoError.MemoTooLong }

        this.data = data
    }

    /**
     * Initializes a Memo from a UTF-8 String. The 512-byte bound is checked on the
     * **UTF-8 encoded bytes** of [string].
     * - Important: use [MemoBytes.fromBase64URL] to initialize a memo from base64URL
     * @throws MemoError.MemoTooLong if the string encodes to more than 512 bytes.
     */
    @Throws(MemoError::class)
    constructor(string: String) {
        val bytes = string.encodeToByteArray()
        require(bytes.size <= maxLength) { throw MemoError.MemoTooLong }

        this.data = bytes
    }

    /** Conversion of the present bytes to an unpadded RFC-4648 §5 base64url string. */
    fun toBase64URL(): String {
        return Base64URL.encode(data)
    }

    /** Two [MemoBytes] are equal when their raw byte content is identical. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MemoBytes

        if (!data.contentEquals(other.data)) return false

        return true
    }

    /** Consistent with [equals]: derived from [data]'s content. */
    override fun hashCode(): Int {
        return 31 * data.contentHashCode()
    }
}
