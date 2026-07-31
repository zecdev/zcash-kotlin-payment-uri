#!/usr/bin/env bash
#
# check-readme-snippets.sh — verify that every ```kotlin code block found in this project's
# canonical KDoc samples appears verbatim somewhere in README.md, so the README's "Quick start"
# snippets can never silently drift from the runnable KDoc samples they were copied from.
#
# Unlike the Swift sibling library (which has a single DocC landing-page markdown file with one
# fenced snippet per scenario), this library's runnable samples live directly as KDoc ```kotlin
# blocks on the symbols they document — the `paymentRequest` DSL entry point (scenarios: a single
# address, a multi-payment request via the DSL, and parsing with an injected validator) and
# `Payment.Builder` (the amount + memo scenario via the explicit builder chain). Both files are
# checked; this generalizes to N discovered ```kotlin blocks per file, so it keeps working if a
# sample is later split into more fenced blocks.
#
# This is intentionally one-directional: README.md may contain prose and markup the KDoc doesn't
# have, but every fenced kotlin block in the sources below must be byte-for-byte present in
# README.md.
#
# Usage:
#   scripts/check-readme-snippets.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

README="$REPO_ROOT/README.md"

# Canonical KDoc sample sources, in the order their scenarios appear in the README's Quick start.
SOURCES=(
    "$REPO_ROOT/lib/src/commonMain/kotlin/org/zecdev/zip321/model/Payment.kt"
    "$REPO_ROOT/lib/src/commonMain/kotlin/org/zecdev/zip321/PaymentRequestDsl.kt"
)

for f in "${SOURCES[@]}" "$README"; do
    if [[ ! -f "$f" ]]; then
        echo "error: expected file not found: $f" >&2
        exit 1
    fi
done

# Extract the Nth ```kotlin ... ``` fenced block (1-indexed) from a file as raw lines (fences
# excluded, and the leading KDoc " * " comment-line prefix stripped), using grep -n to locate fence
# line numbers and sed to slice between them. Pure grep/sed, no python/perl dependency.
extract_block() {
    local file="$1" n="$2"
    local start end
    start="$(grep -n '^\s*\*\s*```kotlin\s*$' "$file" | sed -n "${n}p" | cut -d: -f1)"
    if [[ -z "$start" ]]; then
        return 1
    fi
    end="$(grep -n '^\s*\*\s*```\s*$' "$file" | awk -F: -v s="$start" '$1 > s { print $1; exit }')"
    if [[ -z "$end" ]]; then
        return 1
    fi
    sed -n "$((start + 1)),$((end - 1))p" "$file" | sed -E 's/^[[:space:]]*\* ?//'
}

total_blocks=0
missing=0

for src in "${SOURCES[@]}"; do
    block_count="$(grep -c '^\s*\*\s*```kotlin\s*$' "$src" || true)"
    if [[ "$block_count" -eq 0 ]]; then
        echo "error: no \`\`\`kotlin blocks found in $src" >&2
        exit 1
    fi

    for ((i = 1; i <= block_count; i++)); do
        total_blocks=$((total_blocks + 1))
        block="$(extract_block "$src" "$i")"
        if [[ -z "$block" ]]; then
            echo "error: could not extract kotlin block #$i from $src" >&2
            missing=$((missing + 1))
            continue
        fi

        # Whole-block verbatim containment: join both the block and README on a sentinel byte so
        # a multi-line match can be checked with a single bash pattern-match, rather than grep -F
        # (which alternates per line, not what we want for a contiguous multi-line run).
        joined_block="$(printf '%s' "$block" | tr '\n' '\x01')"
        joined_readme="$(tr '\n' '\x01' < "$README")"

        if [[ "$joined_readme" != *"$joined_block"* ]]; then
            echo "error: kotlin block #$i in $src is not present verbatim in README.md:" >&2
            echo "---" >&2
            printf '%s\n' "$block" >&2
            echo "---" >&2
            missing=$((missing + 1))
        fi
    done
done

if [[ "$missing" -gt 0 ]]; then
    echo "error: $missing of $total_blocks KDoc kotlin code block(s) not found verbatim in README.md." >&2
    echo "README.md's Quick start snippets must be copied EXACTLY from the KDoc samples on" >&2
    echo "org.zecdev.zip321.paymentRequest and org.zecdev.zip321.model.Payment.Builder." >&2
    exit 1
fi

echo "OK: all $total_blocks KDoc kotlin code blocks found verbatim in README.md."
