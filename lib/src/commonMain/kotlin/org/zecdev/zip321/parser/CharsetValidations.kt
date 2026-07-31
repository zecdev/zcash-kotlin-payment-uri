package org.zecdev.zip321.parser

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

        object UnreservedCharacterSet {
            val characters =
                setOf(
                    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~', '!',
                )
        }

        object PctEncodedCharacterSet {
            val characters =
                setOf(
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                    'A', 'B', 'C', 'D', 'E', 'F',
                    'a', 'b', 'c', 'd', 'e', 'f', '%',
                )
        }

        object AllowedDelimsCharacterSet {
            val characters = setOf('!', '$', '\'', '(', ')', '*', '+', ',', ';')
        }

        object QcharCharacterSet {
            val characters =
                UnreservedCharacterSet.characters.union(
                    PctEncodedCharacterSet.characters,
                )
                    .union(
                        AllowedDelimsCharacterSet.characters,
                    )
                    .union(
                        setOf(':', '@'),
                    )
        }

        val isValidParamNameChar: (Char) -> Boolean = { it in ParamNameCharacterSet.characters }
    }
}

fun Char.isAsciiLetterOrDigit(): Boolean {
    return isLetterOrDigit() && this.code < 128
}

fun Char.isAsciiLetter(): Boolean {
    return isLetter() && this.code < 128
}
