package org.zecdev.zcash.zip321.parser

import com.copperleaf.kudzu.parser.text.BaseTextParser

class AddressTextParser : BaseTextParser(
    isValidChar = { _, char -> char.isLetterOrDigit() },
    isValidText = { it.isNotEmpty() },
    allowEmptyInput = false,
    invalidTextErrorMessage = { "Expected bech32 or Base58 text, got '$it'" }
)
