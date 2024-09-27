package org.zecdev.zcash.zip321.extensions

fun String.qcharEncoded(): String? {
    val qcharEncodeAllowed = setOf(
        '-', '.', '_', '~', '!', '$', '\'', '(', ')', '*', '+', ',', ';', '@', ':'
    )
        .map { it.toString() }
    return this.replace(Regex("[^A-Za-z0-9\\-._~!$'()*+,;@:]")) { matched ->
        if (matched.value in qcharEncodeAllowed) {
            matched.value
        } else {
            "%" + matched.value.toCharArray().joinToString("%") { byte ->
                "%02X".format(byte.code.toByte())
            }
        }
    }
}

fun String.qcharDecode(): String? {
    return java.net.URLDecoder.decode(this, "UTF-8")
}
