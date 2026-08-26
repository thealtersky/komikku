package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

// KMK -->
/**
 * Attaches a GitHub Personal Access Token to every request sent to GitHub hosts, so that private
 * repositories can be used as extension stores. The token is read lazily on each request so that
 * changes apply without restarting the app, and is never attached to non-GitHub hosts (OkHttp
 * re-runs interceptors per redirect hop, so the host check is applied on the final host too).
 */
internal class GitHubAuthInterceptor(
    private val tokenProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider()

        return if (originalRequest.url.host in GITHUB_HOSTS && token.isNotBlank()) {
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

    private companion object {
        val GITHUB_HOSTS = setOf("github.com", "raw.githubusercontent.com", "api.github.com")
    }
}
// KMK <--
