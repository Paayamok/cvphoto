package com.fushengce.gallery

import com.fushengce.media.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DateSection(
    val date: LocalDate,
    val title: String,
    val items: List<MediaItem>,
)

data class MonthAlbum(
    val month: YearMonth,
    val title: String,
    val items: List<MediaItem>,
) {
    val cover: MediaItem get() = items.firstOrNull { it.kind == com.fushengce.media.MediaKind.Image } ?: items.first()
}

fun createMonthAlbums(
    items: List<MediaItem>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MonthAlbum> = items
    .groupBy { YearMonth.from(Instant.ofEpochMilli(it.dateTakenMillis).atZone(zoneId)) }
    .toSortedMap(compareByDescending<YearMonth> { it })
    .map { (month, grouped) ->
        MonthAlbum(
            month = month,
            title = "${month.year}年${month.monthValue}月",
            items = grouped.sortedWith(MediaItem.newestFirst),
        )
    }

fun createDateSections(
    items: List<MediaItem>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<DateSection> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    return items
        .groupBy { item ->
            Instant.ofEpochMilli(item.dateTakenMillis).atZone(zoneId).toLocalDate()
        }
        .toSortedMap(compareByDescending<LocalDate> { it })
        .map { (date, sectionItems) ->
            DateSection(
                date = date,
                title = when (date) {
                    today -> "今日"
                    today.minusDays(1) -> "昨日"
                    else -> EXPLICIT_DATE_FORMAT.format(date)
                },
                items = sectionItems.sortedWith(MediaItem.newestFirst),
            )
        }
}

private val EXPLICIT_DATE_FORMAT = DateTimeFormatter.ofPattern(
    "yyyy年M月d日",
    Locale.SIMPLIFIED_CHINESE,
)
