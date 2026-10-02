package com.fushengce.viewer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind

@Composable
internal fun ImagePage(
    item: MediaItem,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(item.id) {
                detectTapGestures(onTap = { onToggleControls() })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.kind == MediaKind.Image) {
            SubcomposeAsyncImage(
                model = Uri.parse(item.uri),
                contentDescription = item.displayName.ifBlank { "照片" },
                contentScale = ContentScale.Fit,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    ViewerPageMessage("此影暂不能显示")
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ViewerPageMessage("松手后可播放视频")
        }
    }
}

@Composable
private fun ViewerPageMessage(message: String) {
    Text(
        text = message,
        color = Color.White,
        style = MaterialTheme.typography.bodyLarge,
    )
}
