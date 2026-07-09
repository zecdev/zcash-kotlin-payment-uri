package org.zecdev.zip321.extensions

import org.zecdev.zip321.encodings.QCharCodec

fun String.qcharEncoded(): String {
    return QCharCodec.encode(this)
}

fun String.qcharDecode(): String {
    return QCharCodec.decode(this)
}
