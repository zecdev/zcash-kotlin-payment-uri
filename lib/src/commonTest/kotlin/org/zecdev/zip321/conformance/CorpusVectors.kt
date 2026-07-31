package org.zecdev.zip321.conformance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.zecdev.zip321.Network

/**
 * Model and loader for the shared ZIP-321 conformance vector corpus that
 * lives in the `test-vectors` git submodule (repository:
 * zcash-zip321-test-vectors). The corpus JSON under `vectors/valid` and
 * `vectors/invalid` is embedded into [GeneratedVectors] at build time by the
 * `generateConformanceVectors` task in `lib/build.gradle.kts`, so loading
 * is identical on every KMP target (no classloader/filesystem access).
 *
 * Parsing uses only the kotlinx-serialization-json *tree* API
 * (`Json.parseToJsonElement`), which is multiplatform; no `@Serializable`
 * classes and no serialization compiler plugin are needed; the dependency is
 * test-only.
 */
data class VectorPayment(
    /** The ZIP-321 paramindex (0 denotes the empty paramindex). */
    val index: Long,
    /** Recipient address exactly as it appears in the URI. */
    val address: String,
    /** Amount in zatoshis, or null when no `amount` param is present. */
    val amountZat: ULong?,
    /** The memo param value as it appears in the URI (base64url, no padding), or null when absent. */
    val memoBase64: String?,
    /** Percent-decoded label value, or null when absent. */
    val label: String?,
    /** Percent-decoded message value, or null when absent. */
    val message: String?,
    /** [name, decodedValue] pairs of unrecognized non-req- params, in order. */
    val other: List<Pair<String, String>>,
)

data class ValidVector(
    val name: String,
    val description: String,
    val uri: String,
    val network: String,
    val payments: List<VectorPayment>,
    val canonicalUri: String?,
    val oracleSkip: Boolean,
    val oracleSkipReason: String?,
)

data class InvalidVector(
    val name: String,
    val description: String,
    val uri: String,
    val network: String,
    /** Shared error discriminant (documentation only; v1 error types are not asserted). */
    val error: String,
    val oracleSkip: Boolean,
    val oracleSkipReason: String?,
)

fun networkOfVector(network: String): Network =
    when (network) {
        "main" -> Network.MAINNET
        "test" -> Network.TESTNET
        "regtest" -> Network.REGTEST
        else -> error("Unknown vector network '$network'")
    }

object CorpusLoader {
    fun loadValid(): List<ValidVector> =
        jsonObjectsIn("valid").map { obj ->
            ValidVector(
                name = obj.getValue("name").jsonPrimitive.content,
                description = obj.getValue("description").jsonPrimitive.content,
                uri = obj.getValue("uri").jsonPrimitive.content,
                network = obj.getValue("network").jsonPrimitive.content,
                payments =
                    obj.getValue("payments").jsonArray.map { paymentElement ->
                        val p = paymentElement.jsonObject
                        VectorPayment(
                            index = p.getValue("index").jsonPrimitive.long,
                            address = p.getValue("address").jsonPrimitive.content,
                            amountZat =
                                p.getValue("amountZat").let {
                                    // corpus amounts are non-negative JSON integers; the v2 amount
                                    // type is unsigned, so decode them straight into a ULong.
                                    if (it is JsonNull) null else it.jsonPrimitive.content.toULong()
                                },
                            memoBase64 = p.getValue("memoBase64").contentOrNull(),
                            label = p.getValue("label").contentOrNull(),
                            message = p.getValue("message").contentOrNull(),
                            other =
                                p.getValue("other").jsonArray.map { pair ->
                                    val nameValue = pair.jsonArray
                                    check(nameValue.size == 2) { "malformed other-param pair: $pair" }
                                    nameValue[0].jsonPrimitive.content to nameValue[1].jsonPrimitive.content
                                },
                        )
                    },
                canonicalUri = obj.getValue("canonicalUri").contentOrNull(),
                oracleSkip = obj.getValue("oracleSkip").jsonPrimitive.content.toBooleanStrict(),
                oracleSkipReason = obj.getValue("oracleSkipReason").contentOrNull(),
            )
        }

    fun loadInvalid(): List<InvalidVector> =
        jsonObjectsIn("invalid").map { obj ->
            InvalidVector(
                name = obj.getValue("name").jsonPrimitive.content,
                description = obj.getValue("description").jsonPrimitive.content,
                uri = obj.getValue("uri").jsonPrimitive.content,
                network = obj.getValue("network").jsonPrimitive.content,
                error = obj.getValue("error").jsonPrimitive.content,
                oracleSkip = obj.getValue("oracleSkip").jsonPrimitive.content.toBooleanStrict(),
                oracleSkipReason = obj.getValue("oracleSkipReason").contentOrNull(),
            )
        }

    private fun JsonElement.contentOrNull(): String? = if (this is JsonNull) null else jsonPrimitive.content

    /**
     * Selects the embedded `.json` corpus files under [directory] (either
     * `"valid"` or `"invalid"`) and parses each one as a JSON array of vector
     * objects, visiting files in name order for determinism.
     */
    private fun jsonObjectsIn(directory: String): List<JsonObject> {
        val files =
            GeneratedVectors.files
                .filterKeys { it.startsWith("$directory/") }
                .toList()
                .sortedBy { (name, _) -> name }
        check(files.isNotEmpty()) {
            "Conformance corpus directory '$directory' is empty in GeneratedVectors. " +
                "Did you run `git submodule update --init`? The `test-vectors` submodule " +
                "provides the vectors, and the `generateConformanceVectors` task in " +
                "lib/build.gradle.kts embeds them into commonTest sources."
        }
        return files.flatMap { (_, text) ->
            Json.parseToJsonElement(text).jsonArray.map { it.jsonObject }
        }
    }
}
