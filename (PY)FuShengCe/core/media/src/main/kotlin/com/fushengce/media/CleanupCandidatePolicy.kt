package com.fushengce.media

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object CleanupCandidatePolicy {
    private const val MINIMUM_AGE_DAYS = 7L

    fun findCandidates(
        items: List<MediaItem>,
        today: LocalDate,
        zoneId: ZoneId,
    ): List<MediaItem> = items
        .filter { item -> isCandidate(item, today, zoneId) }
        .sortedByDescending(MediaItem::dateTakenMillis)

    fun isCandidate(
        item: MediaItem,
        today: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        val cutoff = today.minusDays(MINIMUM_AGE_DAYS)
        return isOldEnough(item, cutoff, zoneId) &&
            (
                isScreenshot(item) ||
                    isDownloadedImage(item) ||
                    isScreenRecording(item)
                )
    }

    fun isScreenshot(item: MediaItem): Boolean {
        if (item.kind != MediaKind.Image) return false
        val name = item.displayName.lowercase()
        val pathSegments = item.relativePath.lowercase().split('/')
        return name.startsWith("screenshot") ||
            name.startsWith("screen_shot") ||
            name.startsWith("截屏") ||
            name.startsWith("屏幕截图") ||
            pathSegments.any {
                it in setOf("screenshots", "screen shots", "截屏", "屏幕截图")
            }
    }

    fun isScreenRecording(item: MediaItem): Boolean {
        if (item.kind != MediaKind.Video) return false
        val name = item.displayName.lowercase()
        val pathSegments = item.relativePath.lowercase().split('/')
        return name.startsWith("screenrecord") ||
            name.startsWith("screen_record") ||
            name.startsWith("screen-record") ||
            name.startsWith("screenrecording") ||
            name.startsWith("screen_recording") ||
            name.startsWith("screen-recording") ||
            name.startsWith("录屏") ||
            name.startsWith("屏幕录制") ||
            pathSegments.any {
                it in setOf(
                    "screen recordings",
                    "screenrecordings",
                    "screen recording",
                    "screenrecord",
                    "录屏",
                    "屏幕录制",
                )
            }
    }

    fun isDownloadedImage(item: MediaItem): Boolean {
        if (item.kind != MediaKind.Image) return false
        val pathSegments = item.relativePath.lowercase().split('/')
        return pathSegments.any { it in setOf("download", "downloads", "下载") }
    }

    private fun isOldEnough(
        item: MediaItem,
        cutoff: LocalDate,
        zoneId: ZoneId,
    ): Boolean = !Instant.ofEpochMilli(item.dateTakenMillis)
        .atZone(zoneId)
        .toLocalDate()
        .isAfter(cutoff)
}
