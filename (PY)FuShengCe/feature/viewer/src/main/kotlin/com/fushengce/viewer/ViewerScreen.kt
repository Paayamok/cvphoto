package com.fushengce.viewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ViewerScreen(
    state: ViewerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onMediaSelected: (Long) -> Unit,
    onToggleControls: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (MediaItem) -> Unit = {},
    caption: String = "",
    onSaveCaption: (MediaItem, String) -> Unit = { _, _ -> },
    onChooseAlbum: ((MediaItem) -> Unit)? = null,
    backLabel: String = "返回归册",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is ViewerUiState.Loading -> CircularProgressIndicator(color = Color.White)
            is ViewerUiState.Content -> ViewerContent(
                state = state,
                onBack = onBack,
                onMediaSelected = onMediaSelected,
                onToggleControls = onToggleControls,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                caption = caption,
                onSaveCaption = onSaveCaption,
                onChooseAlbum = onChooseAlbum,
                backLabel = backLabel,
            )

            is ViewerUiState.Unavailable -> ViewerMessage(
                title = "此影已不在卷中",
                actionLabel = backLabel,
                onAction = onBack,
            )

            is ViewerUiState.PermissionRequired -> ViewerMessage(
                title = "照片与视频权限已失效",
                actionLabel = backLabel,
                onAction = onBack,
            )

            is ViewerUiState.Failure -> ViewerMessage(
                title = state.message,
                actionLabel = "重试",
                onAction = onRetry,
                secondaryAction = onBack,
                secondaryLabel = backLabel,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViewerContent(
    state: ViewerUiState.Content,
    onBack: () -> Unit,
    onMediaSelected: (Long) -> Unit,
    onToggleControls: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (MediaItem) -> Unit,
    caption: String,
    onSaveCaption: (MediaItem, String) -> Unit,
    onChooseAlbum: ((MediaItem) -> Unit)?,
    backLabel: String,
) {
    val pagerState = rememberPagerState(initialPage = state.currentIndex) { state.items.size }

    LaunchedEffect(state.mediaId, state.items.map(MediaItem::id)) {
        val index = state.items.indexOfFirst { it.id == state.mediaId }
        if (index >= 0 && pagerState.currentPage != index) pagerState.scrollToPage(index)
    }
    LaunchedEffect(pagerState, state.items) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                state.items.getOrNull(page)?.id
                    ?.takeIf { it != state.mediaId }
                    ?.let(onMediaSelected)
            }
    }

    HorizontalPager(
        state = pagerState,
        key = { state.items[it].id },
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val item = state.items[page]
        if (item.kind == MediaKind.Video && item.id == state.mediaId) {
            VideoPage(item = item)
        } else {
            ImagePage(
                item = item,
                onToggleControls = onToggleControls,
            )
        }
    }

    if (state.controlsVisible) {
        ViewerControls(
            item = state.currentItem,
            navigationEnabled = !pagerState.isScrollInProgress &&
                state.items.getOrNull(pagerState.currentPage)?.id == state.mediaId,
            position = state.currentIndex + 1,
            total = state.items.size,
            onBack = onBack,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            caption = caption,
            onSaveCaption = onSaveCaption,
            onChooseAlbum = onChooseAlbum,
            backLabel = backLabel,
        )
    }
}

@Composable
private fun ViewerControls(
    item: MediaItem,
    navigationEnabled: Boolean,
    position: Int,
    total: Int,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (MediaItem) -> Unit,
    caption: String,
    onSaveCaption: (MediaItem, String) -> Unit,
    onChooseAlbum: ((MediaItem) -> Unit)?,
    backLabel: String,
) {
    var editing by remember(item.uri) { mutableStateOf(false) }
    var draft by remember(item.uri, caption) { mutableStateOf(caption) }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("影像题记") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(200) },
                    label = { Text("写下你想记住的事") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveCaption(item, draft)
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("取消") }
            },
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(color = Color.Black.copy(alpha = 0.58f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
                Text(backLabel, color = Color.White)
            }
        }
        Surface(color = Color.Black.copy(alpha = 0.62f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(
                    text = item.displayName.ifBlank { "未命名影像" },
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatViewerDate(item.dateTakenMillis)}  ·  $position / $total",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (caption.isNotBlank()) {
                    Text(caption, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onToggleFavorite(item) }) {
                        Text(if (isFavorite) "已收藏 · 取消收藏" else "收藏此影")
                    }
                    TextButton(onClick = { editing = true }) {
                        Text(if (caption.isBlank()) "写题记" else "修改题记")
                    }
                    if (onChooseAlbum != null) {
                        TextButton(onClick = { onChooseAlbum(item) }) { Text("加入卷册") }
                    }
                }
                Text(
                    text = formatViewerProperties(item),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (item.kind == MediaKind.Image) PhotoNavigation(item, navigationEnabled)
            }
        }
    }
}

@Composable
private fun ViewerMessage(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryAction: (() -> Unit)? = null,
    secondaryLabel: String = "返回归册",
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) {
            Text(actionLabel)
        }
        if (secondaryAction != null) {
            Button(onClick = secondaryAction, modifier = Modifier.padding(top = 8.dp)) {
                Text(secondaryLabel)
            }
        }
    }
}

internal fun formatViewerDate(
    timestampMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = VIEWER_DATE_FORMAT.format(Instant.ofEpochMilli(timestampMillis).atZone(zoneId))

private val VIEWER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

internal fun formatViewerProperties(item: MediaItem): String {
    val kind = if (item.kind == MediaKind.Video) "视频" else "照片"
    val dimensions = if (item.width > 0 && item.height > 0) "${item.width}×${item.height}" else null
    val size = when {
        item.sizeBytes >= 1024L * 1024L * 1024L ->
            String.format(Locale.CHINA, "%.1f GB", item.sizeBytes / (1024.0 * 1024 * 1024))
        item.sizeBytes >= 1024L * 1024L ->
            String.format(Locale.CHINA, "%.1f MB", item.sizeBytes / (1024.0 * 1024))
        item.sizeBytes >= 1024L -> "${item.sizeBytes / 1024} KB"
        item.sizeBytes > 0 -> "${item.sizeBytes} B"
        else -> null
    }
    val duration = item.durationMillis?.takeIf { item.kind == MediaKind.Video && it > 0L }
        ?.let { millis ->
            val seconds = millis / 1000
            "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
        }
    return listOfNotNull(kind, dimensions, size, duration).joinToString(" · ")
}
