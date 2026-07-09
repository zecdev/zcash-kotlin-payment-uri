package org.zecdev.zip321.internal

/**
 * Marks a declaration as exempt from the K16 100% coverage gate (see `lib/build.gradle.kts`'s
 * `kover { ... }` block), for the rare case of genuinely unreachable defensive code that cannot be
 * deleted outright — e.g. an exhaustive `when` branch or `catch` clause the Kotlin compiler
 * requires but which the current call graph can never actually reach.
 *
 * POLICY: at most 3 sites may carry this annotation across the whole `commonMain`/`jvmMain`
 * production source (enforced by review, not machine-checked). Every use MUST be paired with a
 * `// KOVER-EXEMPT: <rationale>` comment on the annotated declaration explaining WHY the code is
 * unreachable. This is preferred over a blanket class exclusion or a filter-DSL wildcard: it is
 * attached directly to the declaration it exempts, so it shows up in code review and in an IDE's
 * "find usages", and it cannot silently widen to cover unrelated code the way a package/class-name
 * filter can.
 *
 * Current exemption sites (1 of the 3 allowed budget; see each site's own `// KOVER-EXEMPT`
 * comment for the full rationale):
 * - `org.zecdev.zip321.parser.rethrowTagged` (`Parser.kt`): the `?: error` fallback for a
 *   `Payment.create` failure that today is always a `ZIP321Error`, kept defensively in case that
 *   ever changes.
 *
 * [AnnotationRetention.BINARY] is the minimum retention Kover's `annotatedBy` filter accepts
 * (`RUNTIME` also works; `BINARY` avoids adding a runtime-visible reflection surface to the
 * exempted declaration).
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class KoverExcludeWithRationale
