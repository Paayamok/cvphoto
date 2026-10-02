package com.fushengce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaRecord::class,
        MediaScanStagingRecord::class,
        MediaScanCheckpoint::class,
        TaskRecord::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FuShengCeDatabase : RoomDatabase() {
    abstract fun mediaRecordDao(): MediaRecordDao
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL, `realm` TEXT NOT NULL, `title` TEXT NOT NULL,
                        `address` TEXT NOT NULL, `notes` TEXT NOT NULL, `emergency` INTEGER NOT NULL,
                        `remindAtMillis` INTEGER, `completed` INTEGER NOT NULL,
                        `reminderRevision` TEXT NOT NULL, `notifiedRevision` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_scan_staging` (
                        `uri` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `dateTakenMillis` INTEGER NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `snapshotToken` INTEGER NOT NULL,
                        PRIMARY KEY(`uri`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `media_scan_checkpoint` (
                        `id` INTEGER NOT NULL,
                        `snapshotToken` INTEGER NOT NULL,
                        `generation` INTEGER NOT NULL,
                        `sourceGeneration` INTEGER,
                        `nextOffset` INTEGER NOT NULL,
                        `processedCount` INTEGER NOT NULL,
                        `failedCount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastError` TEXT,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile private var instance: FuShengCeDatabase? = null

        fun get(context: Context): FuShengCeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                FuShengCeDatabase::class.java,
                "fushengce.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
