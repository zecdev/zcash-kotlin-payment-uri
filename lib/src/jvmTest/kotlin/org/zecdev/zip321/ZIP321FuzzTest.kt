package org.zecdev.zip321

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.zecdev.zip321.support.ReferenceAddressValidator

class ZIP321FuzzTest {
    @FuzzTest
    fun testRequestParsing(data: FuzzedDataProvider) {
        val request = data.consumeRemainingAsString()
        // ZIP321.parse is total: it never throws and always returns a Result.
        ZIP321.parse(request, Network.TESTNET, ReferenceAddressValidator.TESTNET)
    }
}
