package org.zecdev.zip321.conformance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Sanity checks for the embedded conformance corpus wiring (the per-vector
 * checks themselves live in the generated `GeneratedZip321ConformanceTest`).
 */
class Zip321ConformanceWiringTest {

    private val validVectors = ConformanceRunner.validVectors
    private val invalidVectors = ConformanceRunner.invalidVectors
    private val allNames = validVectors.map { it.name } + invalidVectors.map { it.name }

    @Test
    fun `corpus contains the expected number of uniquely-named vectors`() {
        assertEquals(22, validVectors.size, "valid vector count")
        assertEquals(28, invalidVectors.size, "invalid vector count")
        assertEquals(
            allNames.size,
            allNames.toSet().size,
            "vector names must be unique corpus-wide"
        )
    }

    @Test
    fun `expected-failure maps only reference vectors that exist in the corpus`() {
        assertTrue(
            allNames.containsAll(ExpectedFailures.conformance.keys),
            "stale names in ExpectedFailures.conformance: " +
                (ExpectedFailures.conformance.keys - allNames.toSet())
        )
        assertTrue(
            validVectors.map { it.name }.containsAll(ExpectedFailures.renderMismatch.keys),
            "stale names in ExpectedFailures.renderMismatch: " +
                (ExpectedFailures.renderMismatch.keys - validVectors.map { it.name }.toSet())
        )
    }
}
