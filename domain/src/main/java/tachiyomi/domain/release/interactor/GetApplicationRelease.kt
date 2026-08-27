package tachiyomi.domain.release.interactor

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetApplicationRelease(
    private val service: ReleaseService,
    private val preferenceStore: PreferenceStore,
) {

    private val lastChecked: Preference<Long> by lazy {
        preferenceStore.getLong(Preference.appStateKey("last_app_check"), 0)
    }

    suspend fun await(arguments: Arguments): Result {
        val now = Instant.now()

        // Limit checks to once every 3 days at most
        val nextCheckTime = Instant.ofEpochMilli(lastChecked.get()).plus(2, ChronoUnit.DAYS)
        if (!arguments.forceCheck && now.isBefore(nextCheckTime)) {
            return Result.NoNewUpdate
        }

        // KMK -->
        val releases = service.releaseNotes(arguments)
            .filter {
                !it.preRelease &&
                    isNewVersion(
                        arguments.isPreview,
                        arguments.commitCount,
                        arguments.versionName,
                        it.version,
                    )
            }

        val latest = releases.getLatest() ?: return Result.NoNewUpdate
        // KMK <--

        lastChecked.set(now.toEpochMilli())

        // Check if latest version is different from current version
        val isNewVersion = isNewVersion(
            isPreview = arguments.isPreview,
            commitCount = arguments.commitCount,
            versionName = arguments.versionName,
            versionTag = latest.version,
        )
        return when {
            isNewVersion -> Result.NewUpdate(latest)
            else -> Result.NoNewUpdate
        }
    }

    // KMK -->
    /**
     * Releases are tagged as "v<semver>-r<commit count>" (e.g. "v1.14.1-r7") and contain BOTH
     * stable and preview assets, so they are considered for both channels. Only the download
     * link differs per channel (handled in ReleaseServiceImpl).
     */
    private fun isNewVersion(
        isPreview: Boolean,
        commitCount: Int,
        versionName: String,
        versionTag: String,
    ): Boolean {
        // Commit count, e.g. "7" from "v1.14.1-r7" (or "2000" from "r2000")
        val newCommitCount = commitCountRegex.find(versionTag)?.groupValues?.get(1)?.toIntOrNull()
        // Semantic version, e.g. "1.14.1" from "v1.14.1-r7"
        val newSemVer = versionTag
            .removePrefix("v")
            .removePrefix("r")
            .substringBefore("-")
            .split(".")
            .mapNotNull { it.toIntOrNull() }

        return if (isPreview) {
            // Beta: a newer preview exists when its commit count is higher
            newCommitCount != null && newCommitCount > commitCount
        } else {
            // Stable: bump on a higher semver, or an equal semver with more commits
            val oldSemVer = versionName.replace("[^\\d.]".toRegex(), "").split(".").mapNotNull { it.toIntOrNull() }
            val comparison = compareSemVer(newSemVer, oldSemVer)
            comparison > 0 || (comparison == 0 && newCommitCount != null && newCommitCount > commitCount)
        }
    }

    private fun compareSemVer(newSemVer: List<Int>, oldSemVer: List<Int>): Int {
        val length = maxOf(newSemVer.size, oldSemVer.size)
        for (i in 0 until length) {
            val a = newSemVer.getOrElse(i) { 0 }
            val b = oldSemVer.getOrElse(i) { 0 }
            if (a > b) return 1
            if (a < b) return -1
        }
        return 0
    }
    // KMK <--

    // KMK -->
    suspend fun awaitReleaseNotes(arguments: Arguments): Result {
        val releases = service.releaseNotes(arguments)
            .filter { !it.preRelease }

        val latest = releases.getLatest() ?: return Result.NoNewUpdate
        return Result.NewUpdate(latest)
    }
    // KMK <--

    data class Arguments(
        val isFoss: Boolean,
        /** If current version is Preview (beta) build */
        val isPreview: Boolean,
        /** Commit count of current version */
        val commitCount: Int,
        /** Current version name, could be version tag (v0.1.2) or commit count (r1234) */
        val versionName: String,
        /** Repository name */
        val repository: String,
        /** Force check for new update */
        val forceCheck: Boolean = false,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }

    private companion object {
        // Matches the commit-count suffix, e.g. "7" in "v1.14.1-r7" or "2000" in "r2000".
        val commitCountRegex = Regex("r(\\d+)")
    }
}

// KMK --.
internal fun List<Release>.getLatest(): Release? {
    return firstOrNull()
        ?.copy(
            info = joinToString("\r-----\r") {
                "## ${it.version}\r\r" +
                    it.info
            },
        )
}
// KMK <--
