package com.fushengce.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fushengce.database.FuShengCeDatabase
import com.fushengce.database.MediaScanCheckpoint
import com.fushengce.database.MediaScanStatus
import com.fushengce.media.AndroidMediaRepository
import com.fushengce.media.MediaItem
import com.fushengce.metadata.LocalAlbums
import com.fushengce.metadata.LocalCaptions
import com.fushengce.metadata.LocalFavorites
import com.fushengce.albums.AlbumCollectionScreen
import com.fushengce.search.SearchScreen
import kotlinx.coroutines.CancellationException
import com.fushengce.viewer.ViewerScreen
import com.fushengce.viewer.ViewerViewModel
import com.fushengce.viewer.ViewerUiState

@Composable
fun MediaGalleryFlow(
    access: GalleryAccess,
    onRequestPermission: () -> Unit,
    onBackHome: () -> Unit,
    initialCleanupOpen: Boolean,
    mediaRefreshToken: Int,
    eveningReminderEnabled: Boolean,
    onToggleEveningReminder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AndroidMediaRepository(context.contentResolver) }
    val favorites = remember(context) { LocalFavorites(context) }
    val captionStore = remember(context) { LocalCaptions(context) }
    val albumStore = remember(context) { LocalAlbums(context) }
    var albums by remember(albumStore) { mutableStateOf(albumStore.read()) }
    var captions by remember(captionStore) { mutableStateOf(captionStore.read()) }
    var favoriteUris by remember(favorites) { mutableStateOf(favorites.read()) }
    LaunchedEffect(mediaRefreshToken) {
        favoriteUris = favorites.read()
        captions = captionStore.read()
        albums = albumStore.read()
    }
    val dao = remember(context) { FuShengCeDatabase.get(context).mediaRecordDao() }
    val checkpoint by remember(dao) { dao.observeScanCheckpoint() }.collectAsState(initial = null)
    val gallery: GalleryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GalleryViewModel(repository) }
        },
    )
    val galleryState by gallery.uiState.collectAsState()
    LaunchedEffect(access, mediaRefreshToken) {
        if (gallery.uiState.value.access == access && mediaRefreshToken > 1) {
            gallery.refresh()
        } else {
            gallery.setAccess(access)
        }
    }

    var selectedMediaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var galleryFilter by rememberSaveable { mutableStateOf(GalleryMediaFilter.All) }
    var albumsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var albumPickerUri by remember { mutableStateOf<String?>(null) }
    var albumPickerTitle by remember { mutableStateOf("") }
    var albumPickerError by remember { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(initialCleanupOpen) }
    var searchQuery by rememberSaveable {
        mutableStateOf(if (initialCleanupOpen) "待整理" else "")
    }
    var searchItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(searchOpen, albumsOpen, mediaRefreshToken, searchRetry) {
        if (searchOpen || albumsOpen) {
            searchItems = null
            searchError = null
            try {
                searchItems = repository.loadMedia()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                searchError = "检索本机影像失败，请重试"
            }
        }
    }
    BackHandler(enabled = selectedMediaId == null) {
        when {
            searchOpen -> searchOpen = false
            albumsOpen -> {
                if (selectedAlbumId != null) selectedAlbumId = null else albumsOpen = false
            }
            selectedMonth != null -> selectedMonth = null
            else -> onBackHome()
        }
    }
    BackHandler(enabled = selectedMediaId != null) { selectedMediaId = null }

    albumPickerUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { albumPickerUri = null },
            title = { Text("加入卷册") },
            text = {
                Column {
                    albums.forEach { album ->
                        TextButton(onClick = {
                            albums = albumStore.toggleMembership(album.id, uri)
                            albumPickerUri = null
                        }) {
                            Text(if (uri in album.mediaUris) "${album.title} · 已加入，点此移出"
                                else album.title)
                        }
                    }
                    OutlinedTextField(
                        value = albumPickerTitle,
                        onValueChange = {
                            albumPickerTitle = it.take(30)
                            albumPickerError = false
                        },
                        label = { Text("新卷册名称") },
                    )
                    if (albumPickerError) Text("请输入不重复的卷册名称")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val created = albumStore.create(albumPickerTitle)
                    if (created == null) {
                        albumPickerError = true
                    } else {
                        albums = albumStore.toggleMembership(created.id, uri)
                        albumPickerTitle = ""
                        albumPickerUri = null
                    }
                }) { Text("新建并加入") }
            },
            dismissButton = {
                TextButton(onClick = { albumPickerUri = null }) { Text("取消") }
            },
        )
    }

    val mediaId = selectedMediaId
    if (mediaId != null) {
        val viewer: ViewerViewModel = viewModel(
            key = "viewer-$mediaId",
            factory = viewModelFactory {
                initializer {
                    ViewerViewModel(
                        repository = repository,
                        initialMediaId = mediaId,
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            },
        )
        val viewerState by viewer.state.collectAsState()
        ViewerScreen(
            state = viewerState,
            onBack = { selectedMediaId = null },
            onRetry = viewer::retry,
            onMediaSelected = viewer::select,
            onToggleControls = viewer::toggleControls,
            isFavorite = (viewerState as? ViewerUiState.Content)
                ?.currentItem?.uri?.let { it in favoriteUris } == true,
            onToggleFavorite = { item -> favoriteUris = favorites.toggle(item.uri) },
            caption = (viewerState as? ViewerUiState.Content)
                ?.currentItem?.uri?.let(captions::get).orEmpty(),
            onSaveCaption = { item, caption ->
                captions = captionStore.save(item.uri, caption)
            },
            onChooseAlbum = { item ->
                albumPickerTitle = ""
                albumPickerError = false
                albumPickerUri = item.uri
            },
            backLabel = when {
                searchOpen -> "返回搜索"
                albumsOpen -> "返回卷册"
                else -> "返回归册"
            },
            modifier = modifier,
        )
    } else if (albumsOpen) {
        AlbumCollectionScreen(
            albums = albums,
            selectedAlbumId = selectedAlbumId,
            media = searchItems,
            error = searchError,
            onRetry = { searchRetry++ },
            onBack = {
                if (selectedAlbumId != null) selectedAlbumId = null else albumsOpen = false
            },
            onSelectAlbum = { selectedAlbumId = it },
            onCreate = { title ->
                val created = albumStore.create(title)
                if (created != null) {
                    albums = albumStore.read()
                    selectedAlbumId = created.id
                }
                created != null
            },
            onRename = { albumId, title ->
                val updated = albumStore.rename(albumId, title)
                if (updated != null) albums = updated
                updated != null
            },
            onDelete = { albumId ->
                albums = albumStore.delete(albumId)
                selectedAlbumId = null
            },
            onAddMedia = { albumId, uris ->
                albums = albumStore.addMembers(albumId, uris)
            },
            onRemoveMedia = { albumId, uri ->
                albums = albumStore.removeMember(albumId, uri)
            },
            onMediaClick = { selectedMediaId = it.id },
            modifier = modifier,
        )
    } else if (searchOpen) {
        SearchScreen(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            media = searchItems,
            error = searchError,
            onRetry = { searchRetry++ },
            onBack = { searchOpen = false },
            onMediaClick = { selectedMediaId = it.id },
            limitedAccess = access == GalleryAccess.Partial,
            favoriteUris = favoriteUris,
            captions = captions,
            modifier = modifier,
        )
    } else {
        Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBackHome) { Text("返回首页") }
                TextButton(onClick = { albumsOpen = true }) { Text("人生卷册") }
                TextButton(onClick = {
                    searchQuery = "收藏"
                    searchOpen = true
                }) { Text("查看收藏") }
            }
            TextButton(onClick = onToggleEveningReminder) {
                Text(if (eveningReminderEnabled) "晚间整理提醒：已开启（点此关闭）"
                    else "开启 22:00 整理提醒")
            }
            ScanStatus(checkpoint)
            GalleryScreen(
                state = galleryState,
                onRefresh = gallery::refresh,
                onRetry = gallery::retry,
                onRequestPermission = onRequestPermission,
                onAddMedia = onRequestPermission,
                onLoadNextPage = gallery::loadNextPage,
                onOpenSearch = { searchOpen = true },
                onMediaClick = { selectedMediaId = it.id },
                selectedMonth = selectedMonth,
                onOpenMonth = { selectedMonth = it },
                onBackToMonths = { selectedMonth = null },
                selectedFilter = galleryFilter,
                onFilterSelected = { filter ->
                    galleryFilter = filter
                    selectedMonth = null
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScanStatus(checkpoint: MediaScanCheckpoint?) {
    val message = when (checkpoint?.status) {
        null -> "正在准备扫描本机照片与视频…"
        MediaScanStatus.RUNNING -> "正在扫描：已处理 ${checkpoint.processedCount} 项"
        MediaScanStatus.RETRYABLE -> "扫描暂时中断，正在重试"
        MediaScanStatus.STOPPED -> "扫描已停止；请检查照片权限"
        MediaScanStatus.COMPLETED -> "扫描完成：已处理 ${checkpoint.processedCount} 项"
        else -> "扫描状态未知"
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
