package com.fushengce.viewer

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPage(
    item: MediaItem,
    modifier: Modifier = Modifier,
) {
    require(item.kind == MediaKind.Video) { "VideoPage only accepts video media" }

    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerGeneration by remember(item.id) { mutableIntStateOf(0) }
    var playbackFailed by remember(item.id, playerGeneration) { mutableStateOf(false) }
    val player = remember(item.id, playerGeneration) {
        check(playerLifecycleDecision(PlayerLifecycleEvent.Create).createPlayer)
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(Uri.parse(item.uri)))
            playWhenReady = false
            prepare()
        }
    }
    var released by remember(player) { mutableStateOf(false) }

    fun applyLifecycleEvent(event: PlayerLifecycleEvent) {
        val decision = playerLifecycleDecision(event)
        if (decision.pause && !released) player.pause()
        if (decision.release && !released) {
            player.release()
            released = true
        }
        if (decision.showError) playbackFailed = true
    }

    DisposableEffect(player, lifecycleOwner, item.id) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                applyLifecycleEvent(PlayerLifecycleEvent.PlaybackFailed)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (released) playerGeneration += 1
                }

                Lifecycle.Event.ON_STOP -> {
                    applyLifecycleEvent(PlayerLifecycleEvent.AppBackgrounded)
                }

                Lifecycle.Event.ON_DESTROY -> {
                    applyLifecycleEvent(PlayerLifecycleEvent.Destroyed)
                }

                else -> Unit
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            player.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            applyLifecycleEvent(PlayerLifecycleEvent.MediaChanged)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    setKeepContentOnPlayerReset(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    this.player = player
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (playbackFailed) {
            Surface(color = Color.Black.copy(alpha = 0.66f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = "暂不能播放",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "可返回或滑动查看其他影像",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
