package com.fushengce.viewer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fushengce.media.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun PhotoNavigation(item: MediaItem, enabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permitted by remember { mutableStateOf(hasPhotoLocationPermission(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    var destination by remember(item.uri, permitted, refresh) { mutableStateOf<String?>(null) }
    var label by remember(item.uri, permitted, refresh) {
        mutableStateOf(if (permitted) "导航 · 读取位置中" else "导航 · 未获照片位置权限")
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permitted = it }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permitted = hasPhotoLocationPermission(context)
                refresh++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(item.uri, permitted, refresh) {
        if (!permitted) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { readPhotoLocation(context, item.uri) }
        destination = result.first
        label = result.second
    }

    Row {
        TextButton(
            enabled = enabled && destination != null,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.45f),
            ),
            onClick = {
                destination?.let { uri ->
                    val error = try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        null
                    } catch (_: ActivityNotFoundException) {
                        "未安装地图应用，请安装后重试"
                    } catch (_: SecurityException) {
                        "地图暂时无法打开，请稍后重试"
                    }
                    if (error != null) Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
            },
        ) { Text(label) }
        if (!permitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            TextButton(onClick = {
                permissionLauncher.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }) { Text("读取照片位置") }
        }
    }
}

private fun hasPhotoLocationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun readPhotoLocation(context: Context, photoUri: String): Pair<String?, String> {
    return try {
        val uri = Uri.parse(photoUri).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.setRequireOriginal(it) else it
        }
        val stream = context.contentResolver.openInputStream(uri)
            ?: return null to "导航 · 无法读取照片位置"
        val destination = stream.use {
            ExifInterface(it).latLong?.let { coordinates ->
                photoNavigationUri(coordinates[0], coordinates[1])
            }
        } ?: return null to "导航 · 无位置信息"
        destination to "导航"
    } catch (_: SecurityException) {
        null to "导航 · 无法读取照片位置"
    } catch (_: IOException) {
        null to "导航 · 无法读取照片位置"
    } catch (_: IllegalArgumentException) {
        null to "导航 · 无法读取照片位置"
    } catch (_: UnsupportedOperationException) {
        null to "导航 · 无法读取照片位置"
    }
}

internal fun photoNavigationUri(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null ||
        !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0
    ) return null
    return "geo:$latitude,$longitude?q=$latitude,$longitude"
}
