package org.zecdev.zip321

import org.zecdev.zip321.support.ReferenceAddressValidator
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ZIP-321 URI Semantics: `zcash:<addr>` and `zcash:?address=<addr>` denote the
 * SAME request. Which spelling the URI used is a syntax choice and MUST NOT be
 * observable in the parsed model — matching the reference implementation, whose
 * `TransactionRequest` has no notion of the difference either.
 *
 * This is why the `ParsedRequest` sealed type (which had a dedicated
 * `SingleAddress` case for the leading-address spelling) was deleted: it made a
 * purely syntactic distinction observable, and forced every caller to branch on
 * it.
 */
class SingleRecipientSpellingTests {
    private val address =
        "ztestsapling10yy2ex5dcqkclhc7z7yrnjq2z6feyjad56ptwlfgmy77dmaqqrl9gyhprdx59qgmsnyfska2kez"

    private val canonical = ZIP321.FormattingOptions.UseEmptyParamIndex(omitAddressLabel = true)

    private fun parse(uri: String) = ZIP321.parse(uri, Network.TESTNET, ReferenceAddressValidator.TESTNET).getOrThrow()

    @Test
    fun `legacy and labeled single recipient are equal`() {
        assertEquals(parse("zcash:$address"), parse("zcash:?address=$address"))
    }

    @Test
    fun `legacy and labeled single recipient are equal with other parameters`() {
        assertEquals(
            parse("zcash:$address?amount=1.2345"),
            parse("zcash:?address=$address&amount=1.2345"),
        )
    }

    /**
     * The equality is structural, not incidental: both spellings must also
     * agree on the payment's stored `paramindex` (the empty one, `0`) so that
     * re-rendering either produces the same canonical URI.
     */
    @Test
    fun `both spellings store the payment at the empty paramindex`() {
        for (uri in listOf("zcash:$address", "zcash:?address=$address")) {
            val request = parse(uri)
            assertEquals(1, request.indexedPayments.size, uri)
            assertEquals(0u, request.indexedPayments[0].index, uri)
            assertEquals(address, request.payments[0].recipientAddress.value, uri)
        }
    }

    /** Both spellings re-render to the same canonical URI. */
    @Test
    fun `both spellings re-render identically`() {
        assertEquals(
            ZIP321.uriString(parse("zcash:$address"), canonical),
            ZIP321.uriString(parse("zcash:?address=$address"), canonical),
        )
    }
}
