package org.zecdev.zip321.parser

import com.copperleaf.kudzu.parser.text.BaseTextParser

class AddressTextParser(parserContext: ParserContext) : BaseTextParser(
    isValidChar = { _, char -> char.isAsciiLetterOrDigit() },
    isValidText = { parserContext.isValid(it) },
    allowEmptyInput = false,
    invalidTextErrorMessage = { "Expected valid Zcash address, got '$it'" }
)
