package com.fushengce.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fushengce.ui.MediaThumbnail
import com.fushengce.media.MediaItem
import com.fushengce.metadata.LocalAlbum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumCollectionScreen(
    albums: List<LocalAlbum>,
    selectedAlbumId: String?,
    media: List<MediaItem>?,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSelectAlbum: (String) -> Unit,
    onCreate: (String) -> Boolean,
    onRename: (String, String) -> Boolean,
    onDelete: (String) -> Unit,
    onAddMedia: (String, Set<String>) -> Unit,
    onRemoveMedia: (String, String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var creating by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var invalidTitle by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var renameTitle by remember { mutableStateOf("") }
    var invalidRename by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var removingItem by remember(selectedAlbumId) { mutableStateOf<MediaItem?>(null) }
    var selecting by remember(selectedAlbumId) { mutableStateOf(false) }
    var selectedUris by remember(selectedAlbumId) { mutableStateOf(emptySet<String>()) }
    BackHandler(enabled = selecting) {
        selecting = false
        selectedUris = emptySet()
    }
    val selected = albums.firstOrNull { it.id == selectedAlbumId }
    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("新建卷册") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it.take(30)
                            invalidTitle = false
                        },
                        label = { Text("卷册名称") },
                    )
                    if (invalidTitle) Text("请输入新名称，且不要与已有卷册重复")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onCreate(title)) {
                        title = ""
                        creating = false
                    } else invalidTitle = true
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text("取消") }
            },
        )
    }
    if (renaming && selected != null) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("修改卷册名称") },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = {
                            renameTitle = it.take(30)
                            invalidRename = false
                        },
                        label = { Text("卷册名称") },
                    )
                    if (invalidRename) Text("请输入新名称，且不要与其他卷册重复")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onRename(selected.id, renameTitle)) {
                        renaming = false
                    } else invalidRename = true
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renaming = false }) { Text("取消") }
            },
        )
    }
    if (confirmingDelete && selected != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除这本卷册？") },
            text = { Text("只删除本机卷册和收录关系，原照片与视频不会删除。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete(selected.id)
                }) { Text("删除卷册") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("取消") }
            },
        )
    }
    if (removingItem != null && selected != null) {
        val item = removingItem!!
        AlertDialog(
            onDismissRequest = { removingItem = null },
            title = { Text("移出卷册？") },
            text = { Text("只移出这本卷册，原照片或视频仍在系统相册。") },
            confirmButton = {
                TextButton(onClick = {
                    removingItem = null
                    onRemoveMedia(selected.id, item.uri)
                }) { Text("移出") }
            },
            dismissButton = {
                TextButton(onClick = { removingItem = null }) { Text("取消") }
            },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selecting) "选择影像" else selected?.title ?: "人生卷册") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (selecting) {
                            selecting = false
                            selectedUris = emptySet()
                        } else onBack()
                    }) { Text(if (selecting) "取消" else "返回") }
                },
                actions = {
                    if (selecting && selected != null) {
                        TextButton(
                            enabled = selectedUris.isNotEmpty(),
                            onClick = {
                                onAddMedia(selected.id, selectedUris)
                                selectedUris = emptySet()
                                selecting = false
                            },
                        ) { Text("完成（${selectedUris.size}）") }
                    } else if (selected == null) {
                        TextButton(onClick = { creating = true }) { Text("新建") }
                    } else {
                        TextButton(onClick = {
                            renameTitle = selected.title
                            invalidRename = false
                            renaming = true
                        }) { Text("改名") }
                        TextButton(onClick = { confirmingDelete = true }) { Text("删除") }
                    }
                },
            )
        },
    ) { padding ->
        when {
            selected == null -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (albums.isEmpty()) {
                    item {
                        Column {
                            Text("还没有卷册。先建一本，再从照片查看页加入影像。")
                            Button(onClick = { creating = true }) { Text("新建卷册") }
                        }
                    }
                }
                items(albums, key = LocalAlbum::id) { album ->
                    TextButton(onClick = { onSelectAlbum(album.id) }) {
                        Text("${album.title} · ${album.mediaUris.size} 项已标记")
                    }
                }
            }
            error != null -> Column(Modifier.padding(padding).padding(16.dp)) {
                Text(error)
                Button(onClick = onRetry) { Text("重试") }
            }
            media == null -> Text("正在读取本机影像…", modifier = Modifier.padding(padding).padding(16.dp))
            else -> {
                val visible = media.filter { it.uri in selected.mediaUris }
                val candidates = media.filter { it.uri !in selected.mediaUris }
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (selecting) {
                        Text("从已授权的本机影像选择 · 已选 ${selectedUris.size} 项",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        if (candidates.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    selectedUris = if (selectedUris.size == candidates.size) {
                                        emptySet()
                                    } else candidates.mapTo(mutableSetOf()) { it.uri }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                Text(if (selectedUris.size == candidates.size) "取消全选"
                                    else "全选可加入影像（${candidates.size}）")
                            }
                        }
                        if (candidates.isEmpty()) {
                            Text("没有更多可加入的影像。", modifier = Modifier.padding(16.dp))
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                gridItems(candidates, key = MediaItem::id) { item ->
                                    Column {
                                        MediaThumbnail(
                                            item = item,
                                            onClick = {
                                                selectedUris = if (item.uri in selectedUris) {
                                                    selectedUris - item.uri
                                                } else selectedUris + item.uri
                                            },
                                            modifier = Modifier.aspectRatio(1f),
                                        )
                                        Text(
                                            if (item.uri in selectedUris) "已选中" else "点按选择",
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                selectedUris = emptySet()
                                selecting = true
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text("添加影像") }
                        if (visible.isEmpty()) {
                            Text(
                                "此卷暂无可查看的影像。可以添加影像，或检查照片权限。",
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                gridItems(visible, key = MediaItem::id) { item ->
                                    Column {
                                        MediaThumbnail(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            modifier = Modifier.aspectRatio(1f),
                                        )
                                        TextButton(onClick = { removingItem = item }) {
                                            Text("移出卷册")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
