package com.fushengce.search

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
internal fun OnDeviceVoiceInput(
    onRecognized: (String) -> Unit,
    label: String = "语音输入日期、地点或事项",
) {
    val currentOnRecognized by rememberUpdatedState(onRecognized)
    val context = LocalContext.current
    val available = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
                .getOrDefault(false)
    }
    var status by remember { mutableStateOf<String?>(null) }
    val recognizer = remember(context, available) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && available) {
            runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }.getOrNull()
        } else null
    }
    DisposableEffect(recognizer) {
        if (recognizer != null) {
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status = "请说出要查找的日期、地点或事项名称" }
                override fun onBeginningOfSpeech() { status = "正在倾听…" }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { status = "正在识别…" }
                override fun onError(error: Int) { status = "没有识别到内容，请重试或输入文字" }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (words.isNullOrEmpty()) {
                        status = "没有识别到内容，请重试或输入文字"
                    } else {
                        status = null
                        currentOnRecognized(words)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
        onDispose { recognizer?.destroy() }
    }

    fun startListening() {
        val service = recognizer ?: return
        status = "正在启动离线语音识别…"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { service.startListening(intent) }
            .onFailure { status = "离线语音启动失败，请输入文字" }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startListening() else status = "未获得麦克风权限，可继续输入文字"
    }

    Column {
        TextButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            enabled = recognizer != null,
        ) { Text(label) }
        if (recognizer == null) {
            Text("本机暂不支持离线语音识别，请输入文字", modifier = Modifier.padding(horizontal = 16.dp))
        }
        status?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp)) }
    }
}
