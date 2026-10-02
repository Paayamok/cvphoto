package com.fushengce.gallery

import com.fushengce.ui.MediaThumbnail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    state: GalleryUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    onAddMedia: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenSearch: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    selectedMonth: String? = null,
    onOpenMonth: (String) -> Unit = {},
    onBackToMonths: () -> Unit = {},
    selectedFilter: GalleryMediaFilter = GalleryMediaFilter.All,
    onFilterSelected: (GalleryMediaFilter) -> Unit = {},
    guideDrawableRes: Int? = null,
    feedbackAnimationRes: Int? = null,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selectedMonth == null) "时光归册" else selectedMonth.replace("-", "年") + "月") },
                actions = { TextButton(onClick = onOpenSearch) { Text("找照片") } },
                navigationIcon = {
                    if (selectedMonth != null) TextButton(onClick = onBackToMonths) { Text("返回") }
                    GalleryGuide(
                        drawableRes = guideDrawableRes,
                        feedbackAnimationRes = feedbackAnimationRes,
                        marker = state.feedbackMarker(),
                    )
                },
            )
        },
    ) { padding ->
        val isRefreshing = (state as? GalleryUiState.Content)?.isRefreshing == true
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state) {
                GalleryUiState.PermissionRequired -> GalleryMessage(
                    title = "尚未获得照片与视频权限",
                    detail = "需要允许访问后才能归册。",
                    actionLabel = "重新授权",
                    onAction = onRequestPermission,
                )

                is GalleryUiState.Loading -> CenteredLoading()
                is GalleryUiState.Empty -> GalleryMessage(
                    title = "卷中尚无影像",
                    detail = if (state.access == GalleryAccess.Partial) {
                        "当前允许的影像为空，可以增选影像。"
                    } else {
                        "设备中暂时没有可归册的照片或视频。"
                    },
                    actionLabel = if (state.access == GalleryAccess.Partial) "增选影像" else null,
                    onAction = onAddMedia,
                )

                is GalleryUiState.Failure -> GalleryMessage(
                    title = "暂不能读取影像",
                    detail = state.message,
                    actionLabel = "重试",
                    onAction = onRetry,
                )

                is GalleryUiState.Content -> GalleryGrid(
                    state = state,
                    onRetry = onRetry,
                    onAddMedia = onAddMedia,
                    onLoadNextPage = onLoadNextPage,
                    onMediaClick = onMediaClick,
                    selectedMonth = selectedMonth,
                    onOpenMonth = onOpenMonth,
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
                    initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
                    onScrollPositionChanged = onScrollPositionChanged,
                )
            }
        }
    }
}

enum class GalleryMediaFilter {
    All,
    Images,
    Videos,
}

internal fun filterGalleryMedia(
    items: List<MediaItem>,
    filter: GalleryMediaFilter,
): List<MediaItem> = when (filter) {
    GalleryMediaFilter.All -> items
    GalleryMediaFilter.Images -> items.filter { it.kind == MediaKind.Image }
    GalleryMediaFilter.Videos -> items.filter { it.kind == MediaKind.Video }
}

@Composable
private fun GalleryFilterBar(
    selected: GalleryMediaFilter,
    onSelected: (GalleryMediaFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            GalleryMediaFilter.All to "全部",
            GalleryMediaFilter.Images to "照片",
            GalleryMediaFilter.Videos to "视频",
        ).forEach { (filter, label) ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelected(filter) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun GalleryGuide(
    drawableRes: Int?,
    feedbackAnimationRes: Int?,
    marker: String,
) {
    if (drawableRes == null) return
    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = "浮生册引路人",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
        )
        if (feedbackAnimationRes != null) {
            key(marker) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(feedbackAnimationRes),
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = if (marker == "thinking") LottieConstants.IterateForever else 1,
                    clipSpec = LottieClipSpec.Marker(marker),
                    restartOnPlay = true,
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp),
                )
            }
        }
    }
}

internal fun GalleryUiState.feedbackMarker(): String = when (this) {
    GalleryUiState.PermissionRequired -> "error"
    is GalleryUiState.Loading -> "thinking"
    is GalleryUiState.Empty -> "not_found"
    is GalleryUiState.Failure -> "error"
    is GalleryUiState.Content -> if (isRefreshing) "thinking" else "found"
}

@Composable
private fun GalleryGrid(
    state: GalleryUiState.Content,
    onRetry: () -> Unit,
    onAddMedia: () -> Unit,
    onLoadNextPage: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    selectedMonth: String?,
    onOpenMonth: (String) -> Unit,
    selectedFilter: GalleryMediaFilter,
    onFilterSelected: (GalleryMediaFilter) -> Unit,
    initialFirstVisibleItemIndex: Int,
    initialFirstVisibleItemScrollOffset: Int,
    onScrollPositionChanged: (index: Int, offset: Int) -> Unit,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    val filteredItems = remember(state.items, selectedFilter) {
        filterGalleryMedia(state.items, selectedFilter)
    }
    val albums = remember(filteredItems) { createMonthAlbums(filteredItems) }
    val selected = selectedMonth?.let { key ->
        albums.firstOrNull { it.month.toString() == key }
    }
    val sections = remember(selected?.items) {
        createDateSections(selected?.items.orEmpty())
    }

    LaunchedEffect(selectedMonth) { gridState.scrollToItem(0) }
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }

    LaunchedEffect(
        gridState,
        state.nextOffset,
        state.isLoadingMore,
        state.isRefreshing,
        state.errorMessage,
        selectedMonth,
    ) {
        if (state.nextOffset != null && !state.isLoadingMore &&
            !state.isRefreshing && state.errorMessage == null
        ) {
            snapshotFlow {
                val layout = gridState.layoutInfo
                shouldRequestMore(
                    lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1,
                    totalItemCount = layout.totalItemsCount,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    isRefreshing = state.isRefreshing,
                    hasError = state.errorMessage != null,
                )
            }
                .distinctUntilChanged()
                .collect { nearEnd -> if (nearEnd) onLoadNextPage() }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (selectedMonth == null) 2 else 3),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "media-filters", span = { GridItemSpan(maxLineSpan) }) {
            GalleryFilterBar(selectedFilter, onFilterSelected)
        }
        if (state.access == GalleryAccess.Partial) {
            item(key = "partial-access", span = { GridItemSpan(maxLineSpan) }) {
                GalleryBanner(
                    message = "当前仅显示已允许访问的影像",
                    actionLabel = "增选影像",
                    onAction = onAddMedia,
                )
            }
        }
        state.errorMessage?.let { message ->
            item(key = "read-error", span = { GridItemSpan(maxLineSpan) }) {
                GalleryBanner(message = message, actionLabel = "重试", onAction = onRetry)
            }
        }
        if (selectedMonth == null) {
            items(albums, key = { "month-${it.month}" }) { album ->
                Column {
                    MediaThumbnail(
                        item = album.cover,
                        onClick = { onOpenMonth(album.month.toString()) },
                        modifier = Modifier.aspectRatio(1.25f),
                    )
                    Text(
                        text = album.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .clickable { onOpenMonth(album.month.toString()) }
                            .padding(top = 6.dp),
                    )
                    Text(
                        text = "${album.items.size}项已载入",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            sections.forEach { section ->
                item(key = "date-${section.date}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = section.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
                items(section.items, key = MediaItem::id) { item ->
                    MediaThumbnail(
                        item = item,
                        onClick = { onMediaClick(item) },
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
            }
        }
        if (state.hasMore || state.isLoadingMore) {
            item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                if (state.isLoadingMore) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.errorMessage != null) {
                    TextButton(onClick = onRetry) {
                        Text("重试载入影像")
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GalleryMessage(
    title: String,
    detail: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}


internal fun shouldRequestMore(
    lastVisibleIndex: Int,
    totalItemCount: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    hasError: Boolean,
): Boolean = hasMore && !isLoadingMore && !isRefreshing && !hasError &&
    totalItemCount > 0 && lastVisibleIndex >= totalItemCount - 4
