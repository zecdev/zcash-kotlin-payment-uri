package org.zecdev.zip321

import org.zecdev.zip321.model.RecipientAddress
import org.zecdev.zip321.parser.ParserContext
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RecipientTests {
    @Test
    fun `Recipient init throws when validation fails`() {
        assertFailsWith<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET) { _ -> false }
        }
    }

    @Test
    fun `Recipient throws when custom validation does not fail on invalid address`() {
        assertFailsWith<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET)
        }
    }

    @Test
    fun `Recipient init should throw when no custom validation provided`() {
        assertFailsWith<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf", ParserContext.TESTNET)
        }
    }
}
