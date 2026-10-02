package com.fushengce.search

import com.fushengce.media.CleanupCandidatePolicy
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

fun searchMedia(
    items: List<MediaItem>,
    query: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
    favoriteUris: Set<String> = emptySet(),
    captions: Map<String, String> = emptyMap(),
): List<MediaItem> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()
    if (needle == "收藏") {
        return items.filter { it.uri in favoriteUris }
            .sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle in setOf("待整理", "整理候选", "待整理候选")) {
        return CleanupCandidatePolicy.findCandidates(items, today, zoneId)
    }
    if (needle == "截图" || needle == "截屏") return items.filter(::isScreenshotCandidate)
    if (needle in setOf("旧截图", "7天前截图", "待整理截图")) {
        val cutoff = today.minusDays(7)
        return items.filter { item ->
            isScreenshotCandidate(item) &&
                !Instant.ofEpochMilli(item.dateTakenMillis).atZone(zoneId).toLocalDate()
                    .isAfter(cutoff)
        }.sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle in setOf("录屏", "屏幕录制", "录屏候选")) {
        return items.filter(::isScreenRecordingCandidate)
            .sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle in setOf("旧录屏", "7天前录屏", "待整理录屏")) {
        val cutoff = today.minusDays(7)
        return items.filter { item ->
            isScreenRecordingCandidate(item) &&
                !Instant.ofEpochMilli(item.dateTakenMillis).atZone(zoneId).toLocalDate()
                    .isAfter(cutoff)
        }.sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle in setOf("下载图片", "下载的图片", "下载候选")) {
        return items.filter(::isDownloadedImageCandidate)
            .sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle in setOf("旧下载", "7天前下载", "待整理下载图片")) {
        val cutoff = today.minusDays(7)
        return items.filter { item ->
            isDownloadedImageCandidate(item) &&
                !Instant.ofEpochMilli(item.dateTakenMillis).atZone(zoneId).toLocalDate()
                    .isAfter(cutoff)
        }.sortedByDescending(MediaItem::dateTakenMillis)
    }
    if (needle == "视频") return items.filter { it.kind == MediaKind.Video }
    if (needle == "大视频" || needle == "大文件") {
        return items
            .filter { it.kind == MediaKind.Video && it.sizeBytes >= LARGE_VIDEO_BYTES }
            .sortedWith(
                compareByDescending<MediaItem> { it.sizeBytes }
                    .thenByDescending { it.dateTakenMillis },
            )
    }
    if (needle == "照片") return items.filter { it.kind == MediaKind.Image }
    val dateRange = dateRangeFor(needle, today)
    return items.filter { item ->
        if (item.displayName.contains(needle, ignoreCase = true) ||
            captions[item.uri]?.contains(needle, ignoreCase = true) == true
        ) return@filter true
        val date = Instant.ofEpochMilli(item.dateTakenMillis).atZone(zoneId).toLocalDate()
        if (dateRange != null && !date.isBefore(dateRange.first) && !date.isAfter(dateRange.second)) {
            return@filter true
        }
        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth
        val dateLabels = listOf(
            "${year}年${month}月${day}日",
            "${year}年${month}月",
            "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
            "${year}-${month.toString().padStart(2, '0')}",
            "${year}/${month}/${day}",
            "${year}/${month}",
        )
        dateLabels.any { it.contains(needle, ignoreCase = true) }
    }
}

private fun dateRangeFor(query: String, today: LocalDate): Pair<LocalDate, LocalDate>? {
    val normalized = query.removeSuffix("的照片").removeSuffix("照片").trim()
    val month = YearMonth.from(today)
    val lastMonth = month.minusMonths(1)
    val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val year = today.year
    return when (normalized) {
        "今天" -> today to today
        "昨天" -> today.minusDays(1) to today.minusDays(1)
        "前天" -> today.minusDays(2) to today.minusDays(2)
        "本月", "这个月" -> month.atDay(1) to month.atEndOfMonth()
        "上个月" -> lastMonth.atDay(1) to lastMonth.atEndOfMonth()
        "本周", "这周" -> weekStart to weekStart.plusDays(6)
        "上周" -> weekStart.minusWeeks(1) to weekStart.minusDays(1)
        "最近7天", "近7天", "最近七天" -> today.minusDays(6) to today
        "最近30天", "近30天", "最近三十天" -> today.minusDays(29) to today
        "今年" -> LocalDate.of(year, 1, 1) to LocalDate.of(year, 12, 31)
        "去年" -> LocalDate.of(year - 1, 1, 1) to LocalDate.of(year - 1, 12, 31)
        else -> relativeMonth(normalized, year)
    }
}

private fun relativeMonth(query: String, currentYear: Int): Pair<LocalDate, LocalDate>? {
    val prefix = when {
        query.startsWith("今年") -> "今年"
        query.startsWith("去年") -> "去年"
        else -> return null
    }
    val monthText = query.removePrefix(prefix).removeSuffix("月")
    if (!query.endsWith("月")) return null
    val chineseMonths = listOf(
        "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二",
    )
    val monthNumber = monthText.toIntOrNull()
        ?: (chineseMonths.indexOf(monthText) + 1).takeIf { it > 0 }
        ?: return null
    if (monthNumber !in 1..12) return null
    val month = YearMonth.of(currentYear - if (prefix == "去年") 1 else 0, monthNumber)
    return month.atDay(1) to month.atEndOfMonth()
}

internal fun isScreenshotCandidate(item: MediaItem): Boolean =
    CleanupCandidatePolicy.isScreenshot(item)

internal fun isScreenRecordingCandidate(item: MediaItem): Boolean =
    CleanupCandidatePolicy.isScreenRecording(item)

internal fun isDownloadedImageCandidate(item: MediaItem): Boolean =
    CleanupCandidatePolicy.isDownloadedImage(item)

private const val LARGE_VIDEO_BYTES = 100L * 1024 * 1024
