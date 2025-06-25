package org.zecdev.zip321.encodings
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object QCharCodec {
    // RFC 3986 valid qchar characters:
    private val alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toSet()
    private val allowDelims = "-._~!\$'()*+,;:@%".toSet()
    private val qcharCharacters = alphaNumeric + allowDelims
    private val qcharComplement = " \"#%&/<=>?[\\]^`{|}".toSet()
    private val allowed = qcharCharacters - qcharComplement
    private val charset = StandardCharsets.UTF_8
    fun encode(input: String): String {
        val result = StringBuilder()
        for (ch in input) {
            if (allowed.contains(ch)) {
                result.append(ch)
            } else {
                val bytes = ch.toString().toByteArray(charset)
                for (b in bytes) {
                    result.append('%')
                    result.append(String.format("%02X", b))
                }
            }
        }
        return result.toString()
    }

    fun decode(input: String): String {
        return decodePercentEncoded(input)
    }

    /**
     * NOTE: **This is an adaptation of URLDecoder.decode()**
     * Decodes an qchar-encoded string using
     * a specific [Charset].
     * The supplied charset is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "*`%xy`*".
     *
     *
     * ***Note:** The [
 * World Wide Web Consortium Recommendation](http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars) states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.*
     *
     * @implNote This implementation will throw an [java.lang.IllegalArgumentException]
     * when illegal strings are encountered.
     *
     * @param s the `String` to decode
     * @return the newly decoded `String`
     * @throws NullPointerException if `s` or `charset` is `null`
     * @throws IllegalArgumentException if the implementation encounters illegal
     * characters
     *
     */
    private fun decodePercentEncoded(s: String): String {
        val numChars = s.length
        val sb = StringBuilder(if (numChars > 500) numChars / 2 else numChars)
        var i = 0
        var needToChange = false
        var bytes: ByteArray? = null

        while (i < numChars) {
            val c = s[i]
            when (c) {
                '%' -> {
                    try {
                        if (bytes == null) bytes = ByteArray((numChars - i) / 3)
                        var pos = 0

                        var currentChar = c
                        while (i + 2 < numChars && currentChar == '%') {
                            val hex = s.substring(i + 1, i + 3)
                            val v = hex.toInt(16)
                            if (v < 0) {
                                throw IllegalArgumentException(
                                    "URLDecoder: Illegal hex characters in escape (%) pattern - negative value"
                                )
                            }
                            bytes[pos++] = v.toByte()
                            i += 3
                            if (i < numChars) {
                                currentChar = s[i]
                            }
                        }

                        if (i < numChars && s[i] == '%') {
                            throw IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern")
                        }

                        sb.append(String(bytes, 0, pos, charset))
                        needToChange = true
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException(
                            "URLDecoder: Illegal hex characters in escape (%) pattern - ${e.message}"
                        )
                    }
                }

                else -> {
                    sb.append(c)
                    i++
                }
            }
        }

        return if (needToChange) sb.toString() else s
    }
}
