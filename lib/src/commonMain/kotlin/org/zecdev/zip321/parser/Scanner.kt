package org.zecdev.zip321.parser

/**
 * A minimal single-pass cursor over a [CharSequence], used by the ZIP-321 URI grammar. It
 * supports only forward motion with a single-character lookahead ([peek]) — there is no
 * backtracking beyond re-inspecting the current character — mirroring the streaming,
 * non-backtracking style of the reference `nom` parsers in librustzcash `zip321`.
 *
 * Every ZIP-321 grammar terminal (`zcash:`, `?`, `&`, `=`, `.`, `paramname`, `paramindex`,
 * `qchar`) is ASCII, and the only place arbitrary characters flow through (a recipient address,
 * or a value that will be percent-decoded) is carried verbatim as a [String] the caller then
 * hands to its own grammar or to [org.zecdev.zip321.encodings.QCharCodec]. A `Char` cursor is
 * therefore sufficient — no byte-level UTF-8 handling is required.
 */
class Scanner(private val input: CharSequence) {
    /** The index of the next character to be read. */
    var currentOffset: Int = 0
        private set

    /** Whether the scanner has consumed all input. */
    val isAtEnd: Boolean
        get() = currentOffset >= input.length

    /** Returns the character at the current offset without consuming it, or `null` at end. */
    fun peek(): Char? = if (currentOffset < input.length) input[currentOffset] else null

    /** Consumes and returns the character at the current offset, or `null` at end of input. */
    fun advance(): Char? {
        if (currentOffset >= input.length) return null
        return input[currentOffset++]
    }

    /** Consumes the current character iff it equals [char]. Returns whether it was consumed. */
    fun expect(char: Char): Boolean {
        if (peek() != char) return false
        currentOffset++
        return true
    }

    /**
     * Consumes and returns the maximal run of leading characters satisfying [predicate] (possibly
     * empty, in which case the offset is unchanged). Always succeeds.
     */
    fun takeWhile(predicate: (Char) -> Boolean): String {
        val start = currentOffset
        while (currentOffset < input.length && predicate(input[currentOffset])) {
            currentOffset++
        }
        return input.subSequence(start, currentOffset).toString()
    }

    /**
     * Consumes [literal] iff the input at the current offset starts with it. Returns whether it
     * was consumed; on failure the offset is unchanged.
     */
    fun matchLiteral(literal: String): Boolean {
        if (currentOffset + literal.length > input.length) return false
        val matches = literal.indices.all { input[currentOffset + it] == literal[it] }
        if (matches) {
            currentOffset += literal.length
        }
        return matches
    }
}
