package com.fushengce.app

import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun PrototypeSoundscape(
    realm: PrototypeRealm,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val lifePlayer = remember(context.applicationContext) {
        MediaPlayer.create(context.applicationContext, R.raw.life_ambient)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }
    val affairsPlayer = remember(context.applicationContext) {
        MediaPlayer.create(context.applicationContext, R.raw.affairs_ambient)?.apply {
            isLooping = true
            setVolume(0f, 0f)
        }
    }

    DisposableEffect(lifePlayer, affairsPlayer) {
        onDispose {
            lifePlayer?.release()
            affairsPlayer?.release()
        }
    }

    LaunchedEffect(realm, enabled, lifePlayer, affairsPlayer) {
        if (!enabled) {
            lifePlayer?.pause()
            affairsPlayer?.pause()
            return@LaunchedEffect
        }
        runCatching { if (lifePlayer?.isPlaying == false) lifePlayer.start() }
        runCatching { if (affairsPlayer?.isPlaying == false) affairsPlayer.start() }
        repeat(13) { step ->
            val progress = step / 12f
            val lifeVolume = if (realm == PrototypeRealm.Life) progress else 1f - progress
            val affairsVolume = if (realm == PrototypeRealm.Affairs) progress else 1f - progress
            lifePlayer?.setVolume(lifeVolume * LIFE_VOLUME, lifeVolume * LIFE_VOLUME)
            affairsPlayer?.setVolume(affairsVolume * AFFAIRS_VOLUME, affairsVolume * AFFAIRS_VOLUME)
            delay(42)
        }
    }
}

internal enum class NarrationCue(val phrase: String) {
    Life("寻来的光阴，我替你轻轻收好。"),
    Affairs("重要的影像，我会替你理清来处。"),
    Welcome("你回来了，旧影都在。"),
}

@Composable
internal fun PrototypeNarrator(
    cue: NarrationCue?,
    enabled: Boolean,
) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val speaker = remember(context.applicationContext) {
        TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(speaker) {
        onDispose {
            speaker.stop()
            speaker.shutdown()
        }
    }

    LaunchedEffect(ready) {
        if (ready) {
            speaker.language = Locale.SIMPLIFIED_CHINESE
            speaker.setSpeechRate(0.88f)
            speaker.setPitch(1.04f)
        }
    }

    LaunchedEffect(cue, enabled, ready) {
        speaker.stop()
        if (cue == null || !enabled || !ready) return@LaunchedEffect
        delay(360)
        speaker.speak(cue.phrase, TextToSpeech.QUEUE_FLUSH, null, "prototype-${cue.name}")
    }
}

private const val LIFE_VOLUME = 0.13f
private const val AFFAIRS_VOLUME = 0.11f
