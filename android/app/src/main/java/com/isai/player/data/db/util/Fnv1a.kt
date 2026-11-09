package com.isai.player.data.db.util

/**
 * 64-bit Fowler–Noll–Vo (FNV-1a) hash implementation.
 *
 * Provides fast, deterministic, and uniformly distributed 64-bit Long keys from
 * arbitrary string input (such as SAF file URIs). This guarantees idempotent primary keys
 * across library rescans without relying on auto-incrementing database sequence IDs.
 */
object Fnv1a {
    private const val FNV_64_OFFSET_BASIS = -0x340d631b7bdddcdbL // 0xcbf29ce484222325L
    private const val FNV_64_PRIME = 0x100000001b3L

    /**
     * Hashes the given [input] string into a 64-bit signed [Long] using the FNV-1a algorithm.
     */
    fun hash64(input: String): Long {
        val bytes = input.toByteArray(Charsets.UTF_8)
        var hash = FNV_64_OFFSET_BASIS
        for (b in bytes) {
            hash = hash xor (b.toLong() and 0xFFL)
            hash *= FNV_64_PRIME
        }
        return hash
    }
}

