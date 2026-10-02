package com.fushengce.background

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.fushengce.database.FuShengCeDatabase
import com.fushengce.database.MediaRecordDao
import com.fushengce.database.MediaScanCheckpoint
import com.fushengce.database.MediaScanStagingRecord
import com.fushengce.database.MediaScanStatus
import com.fushengce.media.AndroidMediaRepository
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaPage
import com.fushengce.media.MediaPageRequest
import com.fushengce.media.MediaQueryError
import com.fushengce.media.MediaQueryResult
import kotlinx.coroutines.CancellationException

class MediaScanWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = FuShengCeDatabase.get(applicationContext).mediaRecordDao()
        val repository = AndroidMediaRepository(applicationContext.contentResolver)
        var checkpoint = prepareCheckpoint(
            dao = dao,
            queryGeneration = repository.currentGeneration,
            sourceGeneration = currentSourceGeneration(),
        )

        return try {
            while (true) {
                val sourceGeneration = currentSourceGeneration()
                if (!MediaScanProgress.sourceMatches(checkpoint, sourceGeneration)) {
                    checkpoint = startNewCheckpoint(
                        dao = dao,
                        queryGeneration = repository.currentGeneration,
                        sourceGeneration = sourceGeneration,
                        previous = checkpoint,
                    )
                    continue
                }
                when (
                    val result = repository.loadPage(
                        MediaPageRequest(
                            offset = checkpoint.nextOffset,
                            generation = checkpoint.generation,
                        ),
                    )
                ) {
                    is MediaQueryResult.Failure -> {
                        return finishFailure(dao, checkpoint, result.error)
                    }

                    is MediaQueryResult.Success -> {
                        val sourceGenerationAfterQuery = currentSourceGeneration()
                        if (!MediaScanProgress.sourceMatches(
                                checkpoint,
                                sourceGenerationAfterQuery,
                            )
                        ) {
                            checkpoint = startNewCheckpoint(
                                dao = dao,
                                queryGeneration = repository.currentGeneration,
                                sourceGeneration = sourceGenerationAfterQuery,
                                previous = checkpoint,
                            )
                            continue
                        }
                        val page = result.value
                        val records = page.items.mapNotNull { item ->
                            item.toStagingRecordOrNull(checkpoint.snapshotToken)
                        }
                        checkpoint = MediaScanProgress.afterPage(
                            checkpoint = checkpoint,
                            page = page,
                            mappedItemCount = records.size,
                            nowMillis = System.currentTimeMillis(),
                        )
                        dao.persistScanPage(records, checkpoint)
                        setProgress(
                            workDataOf(
                                PROGRESS_PROCESSED to checkpoint.processedCount,
                                PROGRESS_FAILED to checkpoint.failedCount,
                            ),
                        )

                        if (!page.hasMore) {
                            val sourceGenerationBeforeCommit = currentSourceGeneration()
                            if (!MediaScanProgress.sourceMatches(
                                    checkpoint,
                                    sourceGenerationBeforeCommit,
                                )
                            ) {
                                checkpoint = startNewCheckpoint(
                                    dao = dao,
                                    queryGeneration = repository.currentGeneration,
                                    sourceGeneration = sourceGenerationBeforeCommit,
                                    previous = checkpoint,
                                )
                                continue
                            }
                            dao.completeScan(
                                checkpoint.copy(
                                    status = MediaScanStatus.COMPLETED,
                                    lastError = null,
                                    updatedAtMillis = System.currentTimeMillis().coerceAtLeast(0L),
                                ),
                            )
                            return Result.success(
                                workDataOf(
                                    PROGRESS_PROCESSED to checkpoint.processedCount,
                                    PROGRESS_FAILED to checkpoint.failedCount,
                                ),
                            )
                        }
                    }
                }
            }
            @Suppress("UNREACHABLE_CODE")
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: SecurityException) {
            finishFailure(dao, checkpoint, MediaQueryError.PermissionDenied)
        } catch (error: Throwable) {
            finishFailure(dao, checkpoint, MediaQueryError.QueryFailed(error))
        }
    }

    private suspend fun prepareCheckpoint(
        dao: MediaRecordDao,
        queryGeneration: Long,
        sourceGeneration: Long?,
    ): MediaScanCheckpoint {
        val existing = dao.scanCheckpoint()
        if (
            existing != null &&
            MediaScanProgress.isResumable(existing) &&
            MediaScanProgress.sourceMatches(existing, sourceGeneration)
        ) {
            return existing.copy(
                status = MediaScanStatus.RUNNING,
                lastError = null,
                updatedAtMillis = System.currentTimeMillis().coerceAtLeast(0L),
            ).also { dao.upsertCheckpoint(it) }
        }

        return startNewCheckpoint(
            dao = dao,
            queryGeneration = queryGeneration,
            sourceGeneration = sourceGeneration,
            previous = existing,
        )
    }

    private suspend fun startNewCheckpoint(
        dao: MediaRecordDao,
        queryGeneration: Long,
        sourceGeneration: Long?,
        previous: MediaScanCheckpoint?,
    ): MediaScanCheckpoint {

        val latestToken = listOfNotNull(
            dao.latestSnapshotToken(),
            previous?.snapshotToken,
        ).maxOrNull()
        return MediaScanCheckpoint(
            snapshotToken = MediaSnapshotToken.next(
                nowMillis = System.currentTimeMillis(),
                latestToken = latestToken,
            ),
            generation = queryGeneration,
            sourceGeneration = sourceGeneration,
            nextOffset = 0,
            processedCount = 0,
            failedCount = 0,
            status = MediaScanStatus.RUNNING,
            lastError = null,
            updatedAtMillis = System.currentTimeMillis().coerceAtLeast(0L),
        ).also { dao.startScan(it) }
    }

    private fun currentSourceGeneration(): Long? = mediaSourceGeneration(applicationContext)


    private suspend fun finishFailure(
        dao: MediaRecordDao,
        checkpoint: MediaScanCheckpoint,
        error: MediaQueryError,
    ): Result = when (MediaScanDecision.failureAction(error)) {
        MediaScanFailureAction.Stop -> {
            dao.stopScan(
                checkpoint.copy(
                    status = MediaScanStatus.STOPPED,
                    lastError = error.summary(),
                    updatedAtMillis = System.currentTimeMillis().coerceAtLeast(0L),
                ),
            )
            Result.success()
        }

        MediaScanFailureAction.Retry -> {
            dao.upsertCheckpoint(
                checkpoint.copy(
                    status = MediaScanStatus.RETRYABLE,
                    lastError = error.summary(),
                    updatedAtMillis = System.currentTimeMillis().coerceAtLeast(0L),
                ),
            )
            Result.retry()
        }
    }

    private companion object {
        const val PROGRESS_PROCESSED = "processed"
        const val PROGRESS_FAILED = "failed"
    }
}

private fun MediaItem.toStagingRecordOrNull(snapshotToken: Long): MediaScanStagingRecord? = try {
    MediaScanStagingRecord(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        dateTakenMillis = dateTakenMillis,
        sizeBytes = sizeBytes,
        snapshotToken = snapshotToken,
    )
} catch (_: IllegalArgumentException) {
    null
}

private fun MediaQueryError.summary(): String = when (this) {
    MediaQueryError.PermissionDenied -> "PermissionDenied"
    is MediaQueryError.MediaUnavailable -> "MediaUnavailable:$mediaId"
    is MediaQueryError.StaleGeneration -> "StaleGeneration:$requested->$current"
    is MediaQueryError.QueryFailed -> "QueryFailed:${cause.javaClass.simpleName}"
}

internal object MediaScanProgress {
    fun isResumable(checkpoint: MediaScanCheckpoint): Boolean =
        checkpoint.status in MediaScanStatus.resumable

    fun sourceMatches(
        checkpoint: MediaScanCheckpoint,
        sourceGeneration: Long?,
    ): Boolean = checkpoint.sourceGeneration == sourceGeneration

    fun afterPage(
        checkpoint: MediaScanCheckpoint,
        page: MediaPage,
        mappedItemCount: Int,
        nowMillis: Long,
    ): MediaScanCheckpoint {
        require(mappedItemCount in 0..page.items.size)
        require(checkpoint.nextOffset == page.offset) {
            "The returned page must match the persisted scan offset"
        }
        val mappingFailures = page.items.size - mappedItemCount
        return checkpoint.copy(
            nextOffset = page.offset + page.consumedRowCount,
            processedCount = checkpoint.processedCount + page.consumedRowCount,
            failedCount = checkpoint.failedCount + page.skippedItemCount + mappingFailures,
            status = MediaScanStatus.RUNNING,
            lastError = null,
            updatedAtMillis = nowMillis.coerceAtLeast(0L),
        )
    }
}

internal object MediaSnapshotToken {
    fun next(
        nowMillis: Long,
        latestToken: Long?,
    ): Long {
        val nonNegativeNow = nowMillis.coerceAtLeast(0L)
        if (latestToken == null || nonNegativeNow > latestToken) {
            return nonNegativeNow
        }
        check(latestToken < Long.MAX_VALUE) {
            "Media snapshot token space is exhausted"
        }
        return latestToken + 1L
    }
}

internal fun mediaSourceGeneration(context: Context): Long? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MediaStore.getExternalVolumeNames(context)
            .sorted()
            .fold(0L) { fingerprint, volumeName ->
                (
                    fingerprint * 31L xor
                        volumeName.hashCode().toLong() xor
                        MediaStore.getGeneration(context, volumeName)
                ) and Long.MAX_VALUE
            }
    } else {
        null
    }

object MediaScanScheduler {
    private const val UNIQUE_WORK_NAME = "fushengce-media-index"

    suspend fun enqueueIfChanged(context: Context, hasAccess: Boolean): Boolean {
        if (!hasAccess) return false
        val checkpoint = FuShengCeDatabase.get(context).mediaRecordDao().scanCheckpoint()
        val source = mediaSourceGeneration(context)
        if (source != null && checkpoint != null && checkpoint.sourceGeneration == source &&
            checkpoint.status in setOf(MediaScanStatus.RUNNING, MediaScanStatus.COMPLETED)
        ) return false
        val request = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    fun enqueueAfterPermissionChange(
        context: Context,
        previouslyHadAccess: Boolean,
        currentlyHasAccess: Boolean,
    ): Boolean {
        if (!MediaScanDecision.shouldSchedule(previouslyHadAccess, currentlyHasAccess)) {
            return false
        }

        val request = OneTimeWorkRequestBuilder<MediaScanWorker>()
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }
}

object MediaScanDecision {
    fun shouldSchedule(
        previouslyHadAccess: Boolean,
        currentlyHasAccess: Boolean,
    ): Boolean = !previouslyHadAccess && currentlyHasAccess

    internal fun failureAction(error: MediaQueryError): MediaScanFailureAction = when (error) {
        MediaQueryError.PermissionDenied -> MediaScanFailureAction.Stop
        is MediaQueryError.MediaUnavailable,
        is MediaQueryError.QueryFailed,
        is MediaQueryError.StaleGeneration -> MediaScanFailureAction.Retry
    }
}

internal enum class MediaScanFailureAction {
    Stop,
    Retry,
}
