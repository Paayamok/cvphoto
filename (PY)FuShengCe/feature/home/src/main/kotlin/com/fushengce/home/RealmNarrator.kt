package com.fushengce.home

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
internal fun RealmNarrator(
    realm: MainRealm,
    enabled: Boolean,
) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var hasObservedInitialRealm by remember { mutableStateOf(false) }
    var speaker by remember(context.applicationContext) {
        mutableStateOf<TextToSpeech?>(null)
    }

    LaunchedEffect(context.applicationContext) {
        // TTS engine discovery may involve a slow binder call on first launch. Let the
        // scroll render its first frame before creating the engine so narration never
        // holds Android's launch screen in front of the home artwork.
        delay(TTS_INITIALIZATION_DELAY_MILLIS)
        speaker = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(speaker) {
        onDispose {
            speaker?.stop()
            speaker?.shutdown()
        }
    }

    LaunchedEffect(ready) {
        if (ready) {
            speaker?.language = Locale.SIMPLIFIED_CHINESE
            speaker?.setSpeechRate(0.78f)
            speaker?.setPitch(0.92f)
        }
    }

    LaunchedEffect(realm, enabled, ready) {
        if (!hasObservedInitialRealm) {
            hasObservedInitialRealm = true
            return@LaunchedEffect
        }
        if (!enabled || !ready) {
            speaker?.stop()
            return@LaunchedEffect
        }
        delay(NARRATION_DELAY_MILLIS)
        speaker?.speak(
            realm.transitionQuote(),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "realm-${realm.name}",
        )
    }
}

internal fun MainRealm.transitionQuote(): String = when (this) {
    MainRealm.Life -> "且把光阴，慢慢收好。"
    MainRealm.Affairs -> "既有要事，便一件件办妥。"
}

private const val NARRATION_DELAY_MILLIS = 620L
private const val TTS_INITIALIZATION_DELAY_MILLIS = 1_200L
