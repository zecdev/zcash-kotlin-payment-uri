package org.zecdev.zip321.parser

/**
 * Character-set definitions retained after the K11 Scanner-based grammar rewrite. The URI
 * tokenizer now validates `paramname`, `paramindex` and `qchar` bytes inline (see [Parser] and
 * [org.zecdev.zip321.encodings.QCharCodec]); the only surviving set is [ParamNameCharacterSet],
 * used by [org.zecdev.zip321.model.OtherParam] to validate an `otherparam` key at construction
 * time.
 *
 * K16: this used to be a `class CharsetValidations` wrapping a `companion object` wrapping THIS
 * object — two full levels of pure namespacing, never instantiated. Both wrapper levels are gone:
 * accessing a nested (non-`inner`) Kotlin `object` doesn't require initializing its enclosing
 * declaration at all, so the outer `class`'s (and its `companion object`'s) own init code was
 * dead — permanently unreachable, not just untested — no matter how many times
 * `ParamNameCharacterSet.characters` was read.
 */
object ParamNameCharacterSet {
    /** Every character a ZIP-321 `paramname` may contain: `ALPHA / DIGIT / "+" / "-"`. */
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

/** Whether this character is an ASCII (`< 128`) letter. */
@Suppress("MagicNumber")
fun Char.isAsciiLetter(): Boolean {
    return isLetter() && this.code < 128
}
