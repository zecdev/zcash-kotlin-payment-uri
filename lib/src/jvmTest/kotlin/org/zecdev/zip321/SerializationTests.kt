package org.zecdev.zip321

import org.zecdev.zip321.model.MemoBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * JVM-only: every error singleton in this library that is an `object` extending an exception type
 * defines a private `readResolve()` so that Java's built-in serialization mechanism reconstructs
 * the SAME singleton instance on deserialization rather than a new, `!==` copy (the standard JVM
 * idiom for serializable singletons — see Effective Java item 89). This is only exercisable via an
 * actual serialize/deserialize round trip, which is JVM-specific (no equivalent facility exists on
 * Kotlin/Native), hence this file lives in `jvmTest` rather than `commonTest`.
 */
class SerializationTests {
    private fun <T : Serializable> roundTrip(value: T): T {
        val bytes =
            ByteArrayOutputStream().use { buffer ->
                ObjectOutputStream(buffer).use { it.writeObject(value) }
                buffer.toByteArray()
            }
        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
    }

    @Test
    fun `ZIP321Errors singleton objects survive Java serialization as the same instance`() {
        assertSame(ZIP321.Errors.InvalidBase64, roundTrip(ZIP321.Errors.InvalidBase64))
        assertSame(ZIP321.Errors.InvalidURI, roundTrip(ZIP321.Errors.InvalidURI))
    }

    @Test
    fun `MemoError singleton objects survive Java serialization as the same instance`() {
        assertSame(MemoBytes.MemoError.MemoTooLong, roundTrip(MemoBytes.MemoError.MemoTooLong))
        assertSame(MemoBytes.MemoError.InvalidBase64URL, roundTrip(MemoBytes.MemoError.InvalidBase64URL))
    }
}
