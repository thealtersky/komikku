package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

// KMK -->
/**
 * Attaches a GitHub Personal Access Token to every request sent to GitHub hosts so that private
 * repositories can be used as extension stores.
 *
 * Each store may have its own token, resolved by the "owner/repo" that the request targets. The
 * token(s) are read lazily on each request so that changes apply without restarting the app, and
 * are never attached to non-GitHub hosts (OkHttp re-runs interceptors per redirect hop, so the
 * host check is applied on the final host too).
 */
internal class GitHubAuthInterceptor(
    private val tokenResolver: (String) -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        if (url.host !in GITHUB_HOSTS) {
            return chain.proceed(originalRequest)
        }

        // Resolve the token from the "owner/repo" encoded in the URL path, falling back to a
        // single global token so a legacy setup keeps working.
        val repoKey = extractOwnerRepo(url.host, url.encodedPath)
        val token = tokenResolver(repoKey)?.ifBlank { null }

        return if (token != null) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("Authorization")
                .addHeader("Authorization", "token $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }

    /**
     * Returns the "owner/repo" for a GitHub URL, or the raw path when it cannot be derived.
     *
     * github.com/thealtersky/extensions/raw/...        -> thealtersky/extensions
     * raw.githubusercontent.com/thealtersky/extensions/... -> thealtersky/extensions
     * api.github.com/repos/thealtersky/extensions/...    -> thealtersky/extensions
     */
    private fun extractOwnerRepo(host: String, encodedPath: String): String {
        var path = encodedPath.trim('/')
        if (path.startsWith("/")) path = path.substring(1)

        val segments = path.split("/").filter { it.isNotEmpty() }
        return when (host) {
            "api.github.com" -> {
                // /repos/{owner}/{repo}/... or /orgs/{owner}/repos ...
                if (segments.size >= 2 && segments[0] == "repos") {
                    "${segments[1]}/${segments[2]}"
                } else {
                    path
                }
            }
            "github.com" -> {
                if (segments.size >= 2) {
                    "${segments[0]}/${segments[1]}"
                } else {
                    path
                }
            }
            "raw.githubusercontent.com" -> {
                if (segments.size >= 2) {
                    "${segments[0]}/${segments[1]}"
                } else {
                    path
                }
            }
            else -> path
        }
    }

    private companion object {
        val GITHUB_HOSTS = setOf("github.com", "raw.githubusercontent.com", "api.github.com")
    }
}
// KMK <--
