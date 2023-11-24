package dev.thecodebuffet.zcash.zip321

import RecipientAddress
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RecipientTests : FunSpec({

    test("Recipient init throws when validation fails") {
        shouldThrow<RecipientAddress.RecipientAddressError.InvalidRecipient> {
            RecipientAddress("asdf") { _ -> false }
        }
    }

    test("Recipient init is not null when validation fails") {
        val expected = "asdf"
        val recipient = RecipientAddress(expected) { _ -> true }

        recipient shouldNotBe null
        recipient?.value shouldBe "asdf"
    }

    test("Recipient init is not null when no validation provided") {
        val expected = "asdf"
        val recipient = RecipientAddress(expected)

        recipient shouldNotBe null
        recipient?.value shouldBe "asdf"
    }
})