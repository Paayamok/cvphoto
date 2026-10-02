package com.fushengce.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fushengce.ui.MediaThumbnail
import com.fushengce.media.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** A non-null taskResults slot enables task search; its caller owns matching and empty states. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    media: List<MediaItem>?,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    limitedAccess: Boolean,
    favoriteUris: Set<String> = emptySet(),
    captions: Map<String, String> = emptyMap(),
    taskResults: (@Composable () -> Unit)? = null,
    hasMediaAccess: Boolean = true,
    onRequestMediaAccess: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showTasks by rememberSaveable { mutableStateOf(taskResults != null) }
    var results by remember(media, query, favoriteUris, captions) { mutableStateOf<List<MediaItem>?>(null) }
    LaunchedEffect(media, query, favoriteUris, captions) {
        if (media != null && query.isNotBlank()) {
            delay(120)
            results = withContext(Dispatchers.Default) { searchMedia(media, query, favoriteUris = favoriteUris, captions = captions) }
        }
    }
    val visibleResults = results
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (taskResults == null) "找照片" else "语音搜索") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(if (taskResults == null) "日期、文件名或题记" else "日期、文件名、地点或事项名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OnDeviceVoiceInput(onRecognized = onQueryChange)
            if (taskResults != null) {
                Row {
                    TextButton(onClick = { showTasks = true }) { Text(if (showTasks) "事项 · 已选" else "事项") }
                    TextButton(onClick = { showTasks = false }) { Text(if (!showTasks) "照片 · 已选" else "照片") }
                }
            }
            if (taskResults != null && showTasks) {
                taskResults()
                return@Column
            }
            if (!hasMediaAccess) {
                Text("搜索照片需要照片权限，事项搜索仍可使用", Modifier.padding(16.dp))
                Button(onClick = onRequestMediaAccess, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("授权照片访问")
                }
                return@Column
            }
            val quickFilters = listOf(
                "收藏" to "收藏",
                "待整理" to "待整理",
                "截图" to "截图候选",
                "旧截图" to "7天前截图",
                "录屏" to "录屏候选",
                "旧录屏" to "7天前录屏",
                "下载图片" to "下载图片",
                "旧下载" to "7天前下载",
                "大视频" to "大视频",
                "视频" to "只看视频",
                "照片" to "只看照片",
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowItems(quickFilters, key = { it.first }) { filter ->
                    TextButton(onClick = { onQueryChange(filter.first) }) { Text(filter.second) }
                }
            }
            if (limitedAccess) {
                Text(
                    "当前只能搜索已授权的影像",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            when {
                error != null -> Column(Modifier.padding(16.dp)) {
                    Text(error)
                    Button(onClick = onRetry) { Text("重试") }
                }
                media == null -> Text("正在检索本机影像…", modifier = Modifier.padding(16.dp))
                query.isBlank() -> Text(
                    "输入日期、文件名、题记，或“昨天”“上周”“最近7天”",
                    modifier = Modifier.padding(16.dp),
                )
                visibleResults == null -> Text("正在筛选本机影像…", modifier = Modifier.padding(16.dp))
                visibleResults.isEmpty() -> Text("没有找到匹配的影像", modifier = Modifier.padding(16.dp))
                else -> {
                    Text(
                        when (query) {
                            "收藏" -> "本机收藏 ${visibleResults.size} 项"
                            "待整理", "整理候选", "待整理候选" -> "待整理候选 ${visibleResults.size} 项"
                            "截图", "截屏" -> "截图候选 ${visibleResults.size} 项"
                            "旧截图", "7天前截图", "待整理截图" -> "7天前截图候选 ${visibleResults.size} 项"
                            "录屏", "屏幕录制", "录屏候选" -> "录屏候选 ${visibleResults.size} 项"
                            "旧录屏", "7天前录屏", "待整理录屏" -> "7天前录屏候选 ${visibleResults.size} 项"
                            "下载图片", "下载的图片", "下载候选" -> "下载图片候选 ${visibleResults.size} 项"
                            "旧下载", "7天前下载", "待整理下载图片" -> "7天前下载图片候选 ${visibleResults.size} 项"
                            "大视频", "大文件" -> "100 MB以上视频 ${visibleResults.size} 项 · 合计 ${formatCandidateSize(visibleResults.sumOf { it.sizeBytes })}"
                            "视频" -> "视频 ${visibleResults.size} 项"
                            "照片" -> "照片 ${visibleResults.size} 项"
                            else -> "找到 ${visibleResults.size} 项"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (query in setOf(
                            "待整理", "整理候选", "待整理候选",
                            "截图", "截屏", "旧截图", "7天前截图", "待整理截图",
                            "录屏", "屏幕录制", "录屏候选",
                            "旧录屏", "7天前录屏", "待整理录屏",
                            "下载图片", "下载的图片", "下载候选",
                            "旧下载", "7天前下载", "待整理下载图片", "大视频", "大文件",
                        )
                    ) {
                        Text(
                            when (query) {
                                "待整理", "整理候选", "待整理候选" ->
                                    "合并7天前截图、下载图片和录屏；只供查看，不移动或删除原文件"

                                "旧截图", "7天前截图", "待整理截图" ->
                                    "仅识别截图中7天前的图片；只供查看，不移动或删除原文件"

                                "录屏", "屏幕录制", "录屏候选" ->
                                    "按常见录屏文件名或相册目录识别；只供查看，不移动或删除原文件"

                                "旧录屏", "7天前录屏", "待整理录屏" ->
                                    "仅识别录屏中7天前的视频；只供查看，不移动或删除原文件"

                                "下载图片", "下载的图片", "下载候选" ->
                                    "仅识别下载目录中的图片；只供查看，不移动或删除原文件"

                                "旧下载", "7天前下载", "待整理下载图片" ->
                                    "仅识别下载目录中7天前的图片；只供查看，不移动或删除原文件"
                                else -> "仅供查看，不自动删除原文件"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(visibleResults, key = MediaItem::id) { item ->
                            MediaThumbnail(
                                item = item,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}


internal fun formatCandidateSize(sizeBytes: Long): String = when {
    sizeBytes >= 1024L * 1024L * 1024L ->
        String.format(Locale.CHINA, "%.1f GB", sizeBytes / (1024.0 * 1024 * 1024))
    sizeBytes >= 1024L * 1024L ->
        String.format(Locale.CHINA, "%.1f MB", sizeBytes / (1024.0 * 1024))
    sizeBytes >= 1024L -> "${sizeBytes / 1024} KB"
    else -> "${sizeBytes.coerceAtLeast(0)} B"
}
