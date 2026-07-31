package org.zecdev.zip321.parser

/**
 * Character-set definitions retained after the K11 Scanner-based grammar rewrite. The URI
 * tokenizer now validates `paramname`, `paramindex` and `qchar` bytes inline (see [Parser] and
 * [org.zecdev.zip321.encodings.QCharCodec]); the only surviving set is [ParamNameCharacterSet],
 * used by [ParamNameString] to validate an `otherparam` key at construction time.
 */
class CharsetValidations {
    companion object {
        object ParamNameCharacterSet {
            val characters: Set<Char> =
                setOf(
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                    'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
                    'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                    'y', 'z', '+', '-',
                )
        }
    }
}

fun Char.isAsciiLetter(): Boolean {
    return isLetter() && this.code < 128
}
