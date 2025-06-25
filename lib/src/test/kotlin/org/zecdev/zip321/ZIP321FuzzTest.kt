package org.zecdev.zip321

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.zecdev.zip321.ZIP321.request
import org.zecdev.zip321.parser.ParserContext

class ZIP321FuzzTest {

    @FuzzTest
    fun testRequestParsing(data: FuzzedDataProvider) {
        val request = data.consumeRemainingAsString()
        try {
            request(request, ParserContext.TESTNET, null)
        } catch (e: ZIP321.Errors) {
            // Allowed
        }
    }
}
