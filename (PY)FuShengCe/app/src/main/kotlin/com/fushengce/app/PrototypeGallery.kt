package com.fushengce.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
internal fun PrototypeGallery(
    realm: PrototypeRealm,
    presence: PrototypePresence,
    photos: List<PrototypePhoto>,
    onBack: () -> Unit,
    onBeginWork: () -> Unit,
    onFinishWork: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    onOpenVoice: () -> Unit,
) {
    LaunchedEffect(presence) {
        if (presence == PrototypePresence.Working) {
            delay(2_250)
            onFinishWork()
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF17120F))) {
        Image(
            painter = painterResource(com.fushengce.home.R.drawable.fushengce_home_large_master),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(if (realm == PrototypeRealm.Life) -0.45f else 0.45f, 0f),
            modifier = Modifier.fillMaxSize().blur(15.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xA91A1512), Color(0xD91A1512), Color(0xF21A1512)),
                ),
            ),
        )
        Column(Modifier.fillMaxSize()) {
            GalleryHeader(onBack = onBack, onBeginWork = onBeginWork, onOpenVoice = onOpenVoice)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 44.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { ChapterTitle("今日 · 庭院微光", "长安 · 家人与日常") }
                item {
                    FloatingPhotoRow(
                        photos = photos.take(2),
                        startIndex = 0,
                        onOpenPhoto = onOpenPhoto,
                    )
                }
                item { ChapterTitle("上周 · 一程秋色", "旅途 · 山水之间") }
                item {
                    FloatingPhotoRow(
                        photos = photos.drop(2).ifEmpty { photos.take(2) },
                        startIndex = if (photos.size > 2) 2 else 0,
                        onOpenPhoto = onOpenPhoto,
                    )
                }
                item {
                    Text(
                        "照片没有外框，日期、地点与事件只作轻题记。",
                        color = Color(0x99F7E8CF),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = presence == PrototypePresence.Browsing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp),
        ) {
            Text(
                "她在后景陪你阅影",
                color = Color(0x75F4E2C7),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        AnimatedVisibility(
            visible = presence == PrototypePresence.Working,
            enter = fadeIn(tween(280)),
            exit = fadeOut(tween(360)),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            WorkingPresence(realm)
        }
    }
}

@Composable
private fun GalleryHeader(onBack: () -> Unit, onBeginWork: () -> Unit, onOpenVoice: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "归境",
            color = Color(0xFFFFE9C8),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("秩序浮影", color = Color(0xFFFFF0D6), style = MaterialTheme.typography.titleLarge)
            Text("阅影 · 纵向成章", color = Color(0xFFCFB992), style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "请她理影",
                color = Color(0xFFFFDDB4),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onBeginWork).padding(vertical = 4.dp),
            )
            Text(
                "一语相托",
                color = Color(0xFFAFA28D),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.clickable(onClick = onOpenVoice).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ChapterTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(title, color = Color(0xFFFFEED2), style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = Color(0xFFBFAE93), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FloatingPhotoRow(
    photos: List<PrototypePhoto>,
    startIndex: Int,
    onOpenPhoto: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        photos.take(2).forEachIndexed { localIndex, photo ->
            PrototypePhotoTile(
                photo = photo,
                onClick = { onOpenPhoto(startIndex + localIndex) },
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(if (localIndex == 0) 1.12f else 0.88f),
            )
        }
        if (photos.size == 1) Spacer(Modifier.weight(0.88f))
    }
}

@Composable
private fun WorkingPresence(realm: PrototypeRealm) {
    Surface(
        color = Color(0xEADFC9A8),
        contentColor = Color(0xFF2C2119),
        shape = CutCornerShape(topStart = 22.dp, bottomStart = 22.dp),
        shadowElevation = 14.dp,
        modifier = Modifier.fillMaxWidth(0.58f),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(com.fushengce.home.R.drawable.fushengce_home_large_master),
                contentDescription = "人物近前理影",
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(if (realm == PrototypeRealm.Life) -0.75f else 0.75f, 0f),
                modifier = Modifier.fillMaxWidth().height(146.dp),
            )
            Text("她已近前", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            Text(
                "正在按时间与地点归拢这一组影像",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
internal fun PrototypeImmersive(
    photo: PrototypePhoto,
    onBack: () -> Unit,
    onOpenVoice: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0D0B09))) {
        if (photo.uri != null) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(vertical = 46.dp),
            )
        } else if (photo.drawableRes != null) {
            Image(
                painter = painterResource(photo.drawableRes),
                contentDescription = photo.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(vertical = 46.dp),
            )
        }
        Box(
            Modifier.fillMaxWidth().height(98.dp).background(
                Brush.verticalGradient(listOf(Color(0xD9000000), Color.Transparent)),
            ),
        )
        Text(
            "出静",
            color = Color(0xFFEAD9BC),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp).clickable(onClick = onBack),
        )
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("入静", color = Color(0xFFFFEDD0), style = MaterialTheme.typography.titleMedium)
            Text("照片是此刻唯一主角", color = Color(0x88EAD9BC), style = MaterialTheme.typography.labelSmall)
        }
        Text(
            "托",
            color = Color(0x66EAD9BC),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).clickable(onClick = onOpenVoice),
        )
    }
}

@Composable
internal fun VoiceEntrustOverlay(onDismiss: () -> Unit, onShowPhotos: () -> Unit) {
    var recognized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1_250)
        recognized = true
    }
    val transition = rememberInfiniteTransition(label = "听令气韵")
    val breath by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(tween(1_350), RepeatMode.Reverse),
        label = "丝线呼吸",
    )
    Box(
        Modifier.fillMaxSize().background(Color(0xD816120F)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(260.dp)) {
            repeat(5) { index ->
                val inset = size.minDimension * (0.08f + index * 0.055f)
                drawArc(
                    color = Color(0xFFE8D0A8).copy(alpha = breath * (1f - index * 0.12f)),
                    startAngle = -145f + index * 11f,
                    sweepAngle = 110f + index * 22f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke((1.2f + index * 0.25f).dp.toPx()),
                )
            }
            drawCircle(Color(0xE8D6C49D), radius = size.minDimension * 0.14f)
            drawCircle(Color(0xFF8D3D2E), radius = size.minDimension * 0.09f, style = Stroke(2.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(14.dp))
            Text("一语相托", color = Color(0xFFFFEDD0), style = MaterialTheme.typography.titleLarge)
            Text(
                if (recognized) "已听见：找出最近的旅行照片" else "正在听你诉说……",
                color = Color(0xFFD9C3A2),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 116.dp, start = 28.dp, end = 28.dp),
            )
            AnimatedVisibility(visible = recognized, enter = fadeIn(tween(320))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "她已接令，正从卷中取影",
                        color = Color(0xFFAD9B81),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    InscriptionAction(
                        text = "看看这些照片",
                        onClick = onShowPhotos,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
            }
        }
        Text(
            "收起",
            color = Color(0x99E5D1B0),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopEnd).padding(22.dp),
        )
    }
}
