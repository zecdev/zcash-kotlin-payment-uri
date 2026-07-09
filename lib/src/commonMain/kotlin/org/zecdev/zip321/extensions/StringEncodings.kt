package org.zecdev.zip321.extensions

import org.zecdev.zip321.encodings.QCharCodec

fun String.qcharEncoded(): String {
    return QCharCodec.encode(this)
}

/**
 * Strictly percent-decodes a `qchar` value, delegating to [QCharCodec.decode].
 *
 * The empty string is a valid zero-length `*qchar` value and decodes to itself. Throws
 * [IllegalArgumentException] for a malformed `%XX` escape, a raw non-`qchar` byte, or invalid
 * UTF-8. Call [QCharCodec.decode] directly when a nullable (non-throwing) result is preferred.
 */
fun String.qcharDecode(): String {
    return QCharCodec.decode(this)
        ?: throw IllegalArgumentException("invalid qchar-encoded string: $this")
}
