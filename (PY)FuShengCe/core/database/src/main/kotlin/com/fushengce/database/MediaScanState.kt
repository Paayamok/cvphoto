package com.fushengce.database

import androidx.room.Entity
import androidx.room.PrimaryKey

const val MEDIA_SCAN_CHECKPOINT_ID = 1

object MediaScanStatus {
    const val RUNNING = "RUNNING"
    const val RETRYABLE = "RETRYABLE"
    const val COMPLETED = "COMPLETED"
    const val STOPPED = "STOPPED"

    val resumable: Set<String> = setOf(RUNNING, RETRYABLE)
    val all: Set<String> = resumable + COMPLETED + STOPPED
}

@Entity(tableName = "media_scan_checkpoint")
data class MediaScanCheckpoint(
    @PrimaryKey val id: Int = MEDIA_SCAN_CHECKPOINT_ID,
    val snapshotToken: Long,
    val generation: Long,
    val sourceGeneration: Long?,
    val nextOffset: Int,
    val processedCount: Int,
    val failedCount: Int,
    val status: String,
    val lastError: String?,
    val updatedAtMillis: Long,
) {
    init {
        require(id == MEDIA_SCAN_CHECKPOINT_ID) { "Only one media scan checkpoint is supported" }
        require(snapshotToken >= 0) { "Snapshot token must not be negative" }
        require(generation >= 0) { "Generation must not be negative" }
        require(sourceGeneration == null || sourceGeneration >= 0) {
            "Source generation must not be negative"
        }
        require(nextOffset >= 0) { "Next offset must not be negative" }
        require(processedCount >= 0) { "Processed count must not be negative" }
        require(failedCount in 0..processedCount) { "Failed count must be within processed count" }
        require(status in MediaScanStatus.all) { "Unknown media scan status: $status" }
        require(updatedAtMillis >= 0) { "Updated time must not be negative" }
    }
}

@Entity(tableName = "media_scan_staging")
data class MediaScanStagingRecord(
    @PrimaryKey val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val snapshotToken: Long,
) {
    init {
        require(uri.isNotBlank()) { "Staged media URI must not be blank" }
        require(dateTakenMillis >= 0) { "Staged media date must not be negative" }
        require(sizeBytes >= 0) { "Staged media size must not be negative" }
        require(snapshotToken >= 0) { "Staged snapshot token must not be negative" }
    }

    fun toMediaRecord() = MediaRecord(
        uri = uri,
        displayName = displayName,
        mimeType = mimeType,
        dateTakenMillis = dateTakenMillis,
        sizeBytes = sizeBytes,
        indexedAtMillis = snapshotToken,
    )
}
