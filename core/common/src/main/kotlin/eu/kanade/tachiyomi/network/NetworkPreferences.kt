package eu.kanade.tachiyomi.network

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class NetworkPreferences(
    private val preferenceStore: PreferenceStore,
) {

    /* KMK --> fun verboseLogging(): Preference<Boolean> {
        return preferenceStore.getBoolean("verbose_logging", verboseLogging)
    } KMK <-- */

    fun dohProvider(): Preference<Int> {
        return preferenceStore.getInt("doh_provider", -1)
    }

    fun defaultUserAgent(): Preference<String> {
        return preferenceStore.getString(
            "default_user_agent",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36",
        )
    }

    // KMK -->
    /**
     * Tokens for private extension stores, keyed by the GitHub "owner/repo" that a store
     * resolves to (e.g. "thealtersky/extensions"). Each entry is formatted as
     * "owner/repo=<token>". Kept as a [Set] so multiple private stores can each carry their own
     * token, and adding one store never leaks another store's token into the add dialog.
     */
    fun extensionStoreTokens(): Preference<Set<String>> {
        return preferenceStore.getStringSet("extension_store_tokens", emptySet())
    }

    /**
     * Resolves the GitHub token stored for a given "owner/repo" (e.g.
     * "thealtersky/extensions"). Returns null when no token is set for that exact repo, so a
     * token is never leaked from one store to another or to unrelated GitHub requests.
     */
    fun resolveStoreToken(ownerRepo: String): String? {
        val entry = extensionStoreTokens().get()
            .firstOrNull { it.startsWith("$ownerRepo=") }
            ?: return null
        return entry.substringAfter("=").trim().ifBlank { null }
    }
    // KMK <--
}
