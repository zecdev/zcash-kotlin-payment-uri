package org.zecdev.zcash.zip321.parser

import RecipientAddress
import com.copperleaf.kudzu.parser.ParserContext
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ParserTests : FreeSpec({
    "Parser detects leading addresses" - {
        "detects single recipient with leading address" {
            val validURI = "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
            val (node, remainingText) = Parser(null).maybeLeadingAddressParse.parse(
                ParserContext.fromString(validURI)
            )

            val recipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", null)
            remainingText.isEmpty() shouldBe true
            node.value shouldBe IndexedParameter(0u, Param.Address(recipient))
        }

        "detects leading address with other params" {
            val validURI = "zcash:ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez?amount=1.0001"
            val (node, remainingText) = Parser(null).maybeLeadingAddressParse.parse(
                ParserContext.fromString(validURI)
            )

            val recipient =
                RecipientAddress("ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez", null)
            remainingText.isEmpty() shouldBe false
            node.value shouldBe IndexedParameter(0u, Param.Address(recipient))
        }

        "returns null when no leading address is present" {
            val validURI = "zcash:?amount=1.0001&address=ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"
            val (node, remainingText) = Parser(null).maybeLeadingAddressParse.parse(
                ParserContext.fromString(validURI)
            )
            remainingText.isEmpty() shouldBe false
            node.value shouldBe null
        }
    }
})
