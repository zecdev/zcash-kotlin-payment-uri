
package org.zecdev.zip321.parser

import com.copperleaf.kudzu.parser.text.BaseTextParser

class ParameterNameParser : BaseTextParser(
    isValidChar = { _, char -> CharsetValidations.isValidParamNameChar(char) },
    isValidText = {
        it.isNotEmpty() &&
            it.all {
                    c ->
                CharsetValidations.isValidParamNameChar(c)
            }
    },
    allowEmptyInput = false,
    invalidTextErrorMessage = { "Expected [A-Za-z0-9+-], got '$it'" }
)
