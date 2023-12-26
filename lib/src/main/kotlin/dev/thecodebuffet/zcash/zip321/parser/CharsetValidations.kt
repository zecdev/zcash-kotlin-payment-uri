package dev.thecodebuffet.zcash.zip321.parser

class CharsetValidations {
    companion object {
        object Base58CharacterSet {
            val characters: Set<Char> = setOf(
                '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
                'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
                'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                'g', 'h', 'i', 'j', 'k', 'm', 'n', 'o', 'p', 'q',
                'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
            )
        }

        object Bech32CharacterSet {
            val characters: Set<Char> = setOf(
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
                'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
            )
        }

        object ParamNameCharacterSet {
            val characters: Set<Char> = setOf(
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
                'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '+', '-'
            )
        }

        object UnreservedCharacterSet {
            val characters = setOf(
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '.', '_', '~', '!'
            )
        }

        object PctEncodedCharacterSet {
            val characters = setOf(
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F',
                'a', 'b', 'c', 'd', 'e', 'f', '%'
            )
        }

        object AllowedDelimsCharacterSet {
            val characters = setOf('!', '$', '\'', '(', ')', '*', '+', ',', ';')
        }

        object QcharCharacterSet {
            val characters = UnreservedCharacterSet.characters.union(
                PctEncodedCharacterSet.characters
            )
                .union(
                    AllowedDelimsCharacterSet.characters
                )
                .union(
                    setOf(':', '@')
                )
        }

        val isValidBase58OrBech32Char: (Char) -> Boolean = { char ->
            isValidBase58Char(char) || isValidBech32Char(char)
        }

        val isValidBase58Char: (Char) -> Boolean = { it in Base58CharacterSet.characters }

        val isValidBech32Char: (Char) -> Boolean = { it in Bech32CharacterSet.characters }

        val isValidParamNameChar: (Char) -> Boolean = { it in ParamNameCharacterSet.characters }
    }
}
