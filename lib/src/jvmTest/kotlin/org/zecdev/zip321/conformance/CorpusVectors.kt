package org.zecdev.zip321.conformance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.zecdev.zip321.parser.ParserContext
import java.io.File

/**
 * Model and loader for the shared ZIP-321 conformance vector corpus that
 * lives in the `test-vectors` git submodule (repository:
 * zcash-zip321-test-vectors). The corpus directories `vectors/valid` and
 * `vectors/invalid` are wired onto the test classpath as resource roots
 * `valid/` and `invalid/` by `lib/build.gradle.kts`.
 *
 * Parsing uses only the kotlinx-serialization-json *tree* API
 * (`Json.parseToJsonElement`), so no `@Serializable` classes and no
 * serialization compiler plugin are needed; the dependency is test-only.
 */
data class VectorPayment(
    /** The ZIP-321 paramindex (0 denotes the empty paramindex). */
    val index: Long,
    /** Recipient address exactly as it appears in the URI. */
    val address: String,
    /** Amount in zatoshis, or null when no `amount` param is present. */
    val amountZat: Long?,
    /** The memo param value as it appears in the URI (base64url, no padding), or null when absent. */
    val memoBase64: String?,
    /** Percent-decoded label value, or null when absent. */
    val label: String?,
    /** Percent-decoded message value, or null when absent. */
    val message: String?,
    /** [name, decodedValue] pairs of unrecognized non-req- params, in order. */
    val other: List<Pair<String, String>>
)

data class ValidVector(
    val name: String,
    val description: String,
    val uri: String,
    val network: String,
    val payments: List<VectorPayment>,
    val canonicalUri: String?,
    val oracleSkip: Boolean,
    val oracleSkipReason: String?
)

data class InvalidVector(
    val name: String,
    val description: String,
    val uri: String,
    val network: String,
    /** Shared error discriminant (documentation only; v1 error types are not asserted). */
    val error: String,
    val oracleSkip: Boolean,
    val oracleSkipReason: String?
)

fun networkToParserContext(network: String): ParserContext = when (network) {
    "main" -> ParserContext.MAINNET
    "test" -> ParserContext.TESTNET
    "regtest" -> ParserContext.REGTEST
    else -> error("Unknown vector network '$network'")
}

object CorpusLoader {
    fun loadValid(): List<ValidVector> = jsonObjectsIn("valid").map { obj ->
        ValidVector(
            name = obj.getValue("name").jsonPrimitive.content,
            description = obj.getValue("description").jsonPrimitive.content,
            uri = obj.getValue("uri").jsonPrimitive.content,
            network = obj.getValue("network").jsonPrimitive.content,
            payments = obj.getValue("payments").jsonArray.map { paymentElement ->
                val p = paymentElement.jsonObject
                VectorPayment(
                    index = p.getValue("index").jsonPrimitive.long,
                    address = p.getValue("address").jsonPrimitive.content,
                    amountZat = p.getValue("amountZat").let {
                        if (it is JsonNull) null else it.jsonPrimitive.long
                    },
                    memoBase64 = p.getValue("memoBase64").contentOrNull(),
                    label = p.getValue("label").contentOrNull(),
                    message = p.getValue("message").contentOrNull(),
                    other = p.getValue("other").jsonArray.map { pair ->
                        val nameValue = pair.jsonArray
                        check(nameValue.size == 2) { "malformed other-param pair: $pair" }
                        nameValue[0].jsonPrimitive.content to nameValue[1].jsonPrimitive.content
                    }
                )
            },
            canonicalUri = obj.getValue("canonicalUri").contentOrNull(),
            oracleSkip = obj.getValue("oracleSkip").jsonPrimitive.content.toBooleanStrict(),
            oracleSkipReason = obj.getValue("oracleSkipReason").contentOrNull()
        )
    }

    fun loadInvalid(): List<InvalidVector> = jsonObjectsIn("invalid").map { obj ->
        InvalidVector(
            name = obj.getValue("name").jsonPrimitive.content,
            description = obj.getValue("description").jsonPrimitive.content,
            uri = obj.getValue("uri").jsonPrimitive.content,
            network = obj.getValue("network").jsonPrimitive.content,
            error = obj.getValue("error").jsonPrimitive.content,
            oracleSkip = obj.getValue("oracleSkip").jsonPrimitive.content.toBooleanStrict(),
            oracleSkipReason = obj.getValue("oracleSkipReason").contentOrNull()
        )
    }

    private fun JsonElement.contentOrNull(): String? =
        if (this is JsonNull) null else jsonPrimitive.content

    /**
     * Lists the `.json` files under [directory] on the test classpath and
     * parses each one as a JSON array of vector objects, visiting files in
     * name order for determinism.
     */
    private fun jsonObjectsIn(directory: String): List<JsonObject> {
        val url = checkNotNull(CorpusLoader::class.java.classLoader.getResource(directory)) {
            "Conformance corpus directory '$directory' not found on the test classpath. " +
                "Did you run `git submodule update --init`? The `test-vectors` submodule " +
                "provides the vectors, and lib/build.gradle.kts registers " +
                "`test-vectors/vectors` as a test resources root."
        }
        val dir = File(url.toURI())
        check(dir.isDirectory) { "Corpus resource '$directory' is not a directory: $dir" }
        val files = dir.listFiles { file -> file.name.endsWith(".json") }
            .orEmpty()
            .sortedBy(File::getName)
        check(files.isNotEmpty()) { "No vector files found under $dir" }
        return files.flatMap { file ->
            Json.parseToJsonElement(file.readText()).jsonArray.map { it.jsonObject }
        }
    }
}
