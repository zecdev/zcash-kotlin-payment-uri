package org.zecdev.zip321

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.zecdev.zip321.model.RecipientAddress

class RecipientTests : FunSpec({

    test("Recipient init throws when validation fails") {
        shouldThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf") { _ -> false }
        }
    }

    test("Recipient init does not throw when validation does not fail") {
        shouldNotThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            val expected = "asdf"
            val recipient = RecipientAddress(expected) { _ -> true }

            recipient.value shouldBe expected
        }
    }

    test("Recipient init should not throw when no validation provided") {

        shouldNotThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            val expected = "asdf"
            val recipient: RecipientAddress = RecipientAddress(expected)

            recipient.value shouldBe expected
        }
    }
})
