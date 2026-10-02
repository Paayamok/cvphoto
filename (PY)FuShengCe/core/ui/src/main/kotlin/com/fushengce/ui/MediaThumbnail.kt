package com.fushengce.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import androidx.compose.ui.platform.LocalContext
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind
import java.util.Locale

@Composable
fun MediaThumbnail(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var failed by remember(item.id, item.uri) { mutableStateOf(false) }
    val context = LocalContext.current
    val thumbnailModel = remember(item.uri, item.kind, context) {
        if (item.kind == MediaKind.Video) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .decoderFactory { result, options, _ ->
                    VideoFrameDecoder(result.source, options)
                }
                .build()
        } else {
            item.uri
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (!failed) {
            AsyncImage(
                model = thumbnailModel,
                contentDescription = item.displayName.ifBlank { "影像" },
                contentScale = ContentScale.Crop,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = if (item.kind == MediaKind.Video) "视频封面不可读" else "影像不可读",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
            )
        }

        if (item.kind == MediaKind.Video) {
            Surface(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp),
            ) {
                Text(
                    text = "▶ ${formatDuration(item.durationMillis ?: 0L)}",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
    }
}

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0) / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
