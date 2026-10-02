package com.fushengce.background

import com.fushengce.media.CleanupCandidatePolicy
import com.fushengce.media.MediaPageRequest
import com.fushengce.media.MediaQueryResult
import com.fushengce.media.MediaRepository
import java.time.LocalDate
import java.time.ZoneId

internal class CleanupCandidateCounter(
    private val repository: MediaRepository,
) {
    suspend fun count(
        today: LocalDate,
        zoneId: ZoneId,
    ): MediaQueryResult<Int> {
        val generation = repository.currentGeneration
        var offset = 0
        var candidateCount = 0

        while (true) {
            when (
                val result = repository.loadPage(
                    MediaPageRequest(
                        offset = offset,
                        generation = generation,
                    ),
                )
            ) {
                is MediaQueryResult.Failure -> return result
                is MediaQueryResult.Success -> {
                    val page = result.value
                    candidateCount += page.items.count { item ->
                        CleanupCandidatePolicy.isCandidate(item, today, zoneId)
                    }
                    offset = page.nextOffset
                        ?: return MediaQueryResult.Success(candidateCount)
                }
            }
        }
    }
}
