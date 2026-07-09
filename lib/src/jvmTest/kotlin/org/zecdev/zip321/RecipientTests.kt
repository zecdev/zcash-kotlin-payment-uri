package org.zecdev.zip321

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext

class RecipientTests : FunSpec({

    test("Recipient init throws when validation fails") {
        shouldThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET) { _ -> false }
        }
    }

    test("Recipient throws when custom validation does not fail on invalid address") {
        shouldThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET)
        }
    }

    test("Recipient init should throw when no custom validation provided") {
        shouldThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET)
        }
    }
})
