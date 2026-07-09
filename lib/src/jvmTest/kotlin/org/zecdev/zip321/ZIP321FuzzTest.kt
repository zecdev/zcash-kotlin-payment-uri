package org.zecdev.zip321

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.zecdev.zip321.support.ReferenceAddressValidator

class ZIP321FuzzTest {
    // K17: `maxDuration` only applies in FUZZING mode (`JAZZER_FUZZ=1`; it "has no effect during
    // regression testing" per the `@FuzzTest` KDoc, so `jvmTest`'s default regression-mode pass —
    // exercised on every `test-jvm` CI run — is unaffected and stays fast). CI's `fuzz-smoke` job
    // runs this test WITH `JAZZER_FUZZ=1` to get actual mutation-based fuzzing rather than a mere
    // corpus replay, bounded to a smoke-sized 45 seconds rather than an open-ended campaign.
    //
    // The bound is set here, on the annotation, rather than via the documented
    // `-Djazzer.max_duration=<duration>` runtime override: that override was tested against this
    // project's pinned jazzer-junit 0.24.0 (both as a bare `-D` flag and via explicit
    // `Test.systemProperties` forwarding in `build.gradle.kts`) and, empirically, changed nothing —
    // the run kept going for the full un-overridden 5-minute annotation default in every attempt.
    // A future deep/nightly fuzzing job should either raise this value or investigate the runtime
    // override further; don't assume `-Djazzer.max_duration` works without re-verifying it.
    @FuzzTest(maxDuration = "45s")
    fun testRequestParsing(data: FuzzedDataProvider) {
        val request = data.consumeRemainingAsString()
        // ZIP321.parse is total: it never throws and always returns a Result.
        ZIP321.parse(request, Network.TESTNET, ReferenceAddressValidator.TESTNET)
    }
}
