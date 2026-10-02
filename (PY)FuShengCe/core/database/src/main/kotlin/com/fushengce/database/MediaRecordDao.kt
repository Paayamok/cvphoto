package com.fushengce.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaRecordDao {
    @Upsert
    suspend fun upsertAll(records: List<MediaRecord>)

    @Query("SELECT MAX(indexedAtMillis) FROM media_records")
    suspend fun latestSnapshotToken(): Long?

    @Query("DELETE FROM media_records WHERE indexedAtMillis != :snapshotToken")
    suspend fun removeRecordsOutsideSnapshot(snapshotToken: Long): Int

    @Transaction
    suspend fun replaceSnapshot(
        records: List<MediaRecord>,
        snapshotToken: Long,
    ): Int {
        require(records.all { it.indexedAtMillis == snapshotToken }) {
            "Every media record must belong to the replacement snapshot"
        }
        if (records.isNotEmpty()) upsertAll(records)
        return removeRecordsOutsideSnapshot(snapshotToken)
    }

    @Upsert
    suspend fun upsertStaging(records: List<MediaScanStagingRecord>)

    @Query("DELETE FROM media_scan_staging")
    suspend fun clearStaging()

    @Query("SELECT * FROM media_scan_staging WHERE snapshotToken = :snapshotToken")
    suspend fun stagedRecords(snapshotToken: Long): List<MediaScanStagingRecord>

    @Upsert
    suspend fun upsertCheckpoint(checkpoint: MediaScanCheckpoint)

    @Query("SELECT * FROM media_scan_checkpoint WHERE id = 1 LIMIT 1")
    suspend fun scanCheckpoint(): MediaScanCheckpoint?

    @Query("SELECT * FROM media_scan_checkpoint WHERE id = 1 LIMIT 1")
    fun observeScanCheckpoint(): Flow<MediaScanCheckpoint?>

    @Transaction
    suspend fun startScan(checkpoint: MediaScanCheckpoint) {
        require(checkpoint.status == MediaScanStatus.RUNNING)
        clearStaging()
        upsertCheckpoint(checkpoint)
    }

    @Transaction
    suspend fun persistScanPage(
        records: List<MediaScanStagingRecord>,
        checkpoint: MediaScanCheckpoint,
    ) {
        require(checkpoint.status == MediaScanStatus.RUNNING)
        require(records.all { it.snapshotToken == checkpoint.snapshotToken }) {
            "Every staged record must belong to the active snapshot"
        }
        if (records.isNotEmpty()) upsertStaging(records)
        upsertCheckpoint(checkpoint)
    }

    @Transaction
    suspend fun completeScan(checkpoint: MediaScanCheckpoint): Int {
        require(checkpoint.status == MediaScanStatus.COMPLETED)
        val records = stagedRecords(checkpoint.snapshotToken).map { it.toMediaRecord() }
        val removed = replaceSnapshot(records, checkpoint.snapshotToken)
        clearStaging()
        upsertCheckpoint(checkpoint)
        return removed
    }

    @Transaction
    suspend fun stopScan(checkpoint: MediaScanCheckpoint) {
        require(checkpoint.status == MediaScanStatus.STOPPED)
        clearStaging()
        upsertCheckpoint(checkpoint)
    }

    @Query("SELECT COUNT(*) FROM media_records")
    suspend fun count(): Int
}
