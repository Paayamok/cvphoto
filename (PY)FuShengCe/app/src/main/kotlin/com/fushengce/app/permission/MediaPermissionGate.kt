package com.fushengce.app.permission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MediaPermissionGate(
    state: MediaPermissionState,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
    grantedContent: @Composable (MediaPermissionState) -> Unit,
) {
    when (state) {
        MediaPermissionState.FullAccess,
        is MediaPermissionState.PartialAccess -> grantedContent(state)

        MediaPermissionState.NotRequested -> PermissionExplanation(
            title = "为旧影成册",
            message = "需要访问设备中的照片与视频。内容只在本机整理，不上传云端。",
            primaryLabel = "启卷",
            onPrimaryAction = onRequestPermission,
            onDismiss = onDismiss,
            modifier = modifier,
        )

        is MediaPermissionState.Denied -> PermissionExplanation(
            title = "尚未获得照片与视频权限",
            message = if (state.requiresSystemSettings) {
                "请前往系统设置，为浮生册开启照片与视频权限。"
            } else {
                "未得阅影之许，暂不能归册。你可以重新授权。"
            },
            primaryLabel = if (state.requiresSystemSettings) "前往系统设置" else "重新授权",
            onPrimaryAction = if (state.requiresSystemSettings) {
                onOpenSystemSettings
            } else {
                onRequestPermission
            },
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

@Composable
private fun PermissionExplanation(
    title: String,
    message: String,
    primaryLabel: String,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PermissionSeal(modifier = Modifier.size(52.dp))
        Text(title)
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onDismiss) {
                Text("暂不")
            }
            Button(onClick = onPrimaryAction) {
                Text(primaryLabel)
            }
        }
    }
}

@Composable
private fun PermissionSeal(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val width = size.minDimension * 0.07f
        drawRect(color = color, style = Stroke(width))
        drawLine(color, Offset(size.width * 0.25f, size.height * 0.3f), Offset(size.width * 0.75f, size.height * 0.3f), width)
        drawLine(color, Offset(size.width * 0.3f, size.height * 0.25f), Offset(size.width * 0.3f, size.height * 0.75f), width)
        drawLine(color, Offset(size.width * 0.7f, size.height * 0.25f), Offset(size.width * 0.7f, size.height * 0.75f), width)
        drawLine(color, Offset(size.width * 0.25f, size.height * 0.7f), Offset(size.width * 0.75f, size.height * 0.7f), width)
    }
}
