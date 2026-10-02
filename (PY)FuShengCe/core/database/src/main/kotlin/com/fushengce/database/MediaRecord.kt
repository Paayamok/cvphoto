package com.fushengce.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_records")
data class MediaRecord(
    @PrimaryKey val uri: String,
    val displayName: String,
    val mimeType: String,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val indexedAtMillis: Long,
)
