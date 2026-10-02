package com.fushengce.app

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.fushengce.app.permission.MediaPermissionState
import com.fushengce.media.AndroidMediaRepository
import com.fushengce.media.MediaItem
import com.fushengce.media.MediaKind
import com.fushengce.media.MediaPageRequest
import com.fushengce.media.MediaQueryResult
import kotlinx.coroutines.delay

internal data class PrototypePhoto(
    val key: String,
    val title: String,
    val uri: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val width: Int = 4,
    val height: Int = 3,
)

@Composable
internal fun VisualPrototypeExperience(
    permissionState: MediaPermissionState,
    onRequestPermission: () -> Unit,
    onDismissPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onPlaySound: (UiSoundEffect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember(context.applicationContext) {
        context.applicationContext.getSharedPreferences(PROTOTYPE_PREFERENCES, 0)
    }
    val introCompleted = remember { preferences.getBoolean(INTRO_COMPLETED_KEY, false) }
    var state by remember { mutableStateOf(initialPrototypeState(introCompleted)) }
    var mediaPhotos by remember { mutableStateOf<List<PrototypePhoto>>(emptyList()) }
    var welcomeVisible by remember { mutableStateOf(introCompleted) }
    val repository = remember(context.applicationContext) {
        AndroidMediaRepository(context.applicationContext.contentResolver)
    }
    val photos = if (mediaPhotos.isNotEmpty()) mediaPhotos else fallbackPrototypePhotos()

    fun dispatch(action: PrototypeAction) {
        welcomeVisible = false
        when (action) {
            PrototypeAction.SwitchRealm -> onPlaySound(UiSoundEffect.RealmSwitch)
            PrototypeAction.OpenVoice -> onPlaySound(UiSoundEffect.ListenStart)
            PrototypeAction.OpenGallery -> onPlaySound(UiSoundEffect.AlbumOpen)
            PrototypeAction.BeginWork -> onPlaySound(UiSoundEffect.BrushWrite)
            is PrototypeAction.OpenPhoto -> onPlaySound(UiSoundEffect.PageFlip)
            PrototypeAction.Back -> onPlaySound(UiSoundEffect.TaskEnd)
            PrototypeAction.FinishIntro -> {
                preferences.edit().putBoolean(INTRO_COMPLETED_KEY, true).apply()
                onPlaySound(UiSoundEffect.Complete)
            }
            else -> Unit
        }
        state = state.reduce(action)
    }

    LaunchedEffect(permissionState.hasMediaAccess) {
        if (!permissionState.hasMediaAccess) {
            mediaPhotos = emptyList()
            return@LaunchedEffect
        }
        when (
            val result = repository.loadPage(
                MediaPageRequest(limit = 20, generation = repository.currentGeneration),
            )
        ) {
            is MediaQueryResult.Success -> mediaPhotos = result.value.items
                .filter { it.kind == MediaKind.Image }
                .take(5)
                .map(MediaItem::toPrototypePhoto)
            is MediaQueryResult.Failure -> mediaPhotos = emptyList()
        }
    }

    LaunchedEffect(state.introStage, permissionState.hasMediaAccess) {
        when (state.introStage) {
            IntroStage.World -> {
                delay(1_250)
                dispatch(PrototypeAction.AdvanceIntro)
            }
            IntroStage.LifeDuty -> {
                delay(2_600)
                dispatch(PrototypeAction.AdvanceIntro)
            }
            IntroStage.Permission -> if (permissionState.hasMediaAccess) {
                delay(260)
                dispatch(PrototypeAction.ContinueWithSamples)
            }
            IntroStage.PhotoBloom -> {
                delay(2_050)
                dispatch(PrototypeAction.AdvanceIntro)
            }
            IntroStage.Crossing -> {
                delay(2_050)
                dispatch(PrototypeAction.AdvanceIntro)
            }
            IntroStage.WorkDuty -> {
                delay(2_650)
                dispatch(PrototypeAction.FinishIntro)
            }
            null -> Unit
        }
    }

    LaunchedEffect(welcomeVisible) {
        if (welcomeVisible) {
            delay(1_650)
            welcomeVisible = false
        }
    }

    val soundRealm = when (state.introStage) {
        IntroStage.Crossing, IntroStage.WorkDuty -> PrototypeRealm.Affairs
        else -> state.realm
    }
    PrototypeSoundscape(realm = soundRealm, enabled = soundEnabled)
    PrototypeNarrator(
        cue = when (state.introStage) {
            IntroStage.LifeDuty -> NarrationCue.Life
            IntroStage.WorkDuty -> NarrationCue.Affairs
            null -> if (welcomeVisible) NarrationCue.Welcome else null
            else -> null
        },
        enabled = soundEnabled,
    )

    BackHandler(enabled = state.voiceOpen || state.introVisible || state.page != PrototypePage.Home) {
        when {
            state.voiceOpen -> dispatch(PrototypeAction.CloseVoice)
            state.introVisible -> onDismissPermission()
            else -> dispatch(PrototypeAction.Back)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF15110E))) {
        if (state.introVisible) {
            IntroExperience(
                stage = requireNotNull(state.introStage),
                permissionState = permissionState,
                photos = photos,
                onAdvance = { dispatch(PrototypeAction.AdvanceIntro) },
                onRequestPermission = onRequestPermission,
                onOpenSystemSettings = onOpenSystemSettings,
                onContinueWithSamples = { dispatch(PrototypeAction.ContinueWithSamples) },
            )
        } else {
            Crossfade(
                targetState = state.page,
                animationSpec = tween(420),
                label = "浮影层次",
            ) { page ->
                when (page) {
                    PrototypePage.Home -> PrototypeHome(
                        realm = state.realm,
                        photos = photos,
                        soundEnabled = soundEnabled,
                        welcomeVisible = welcomeVisible,
                        onSwitchRealm = { dispatch(PrototypeAction.SwitchRealm) },
                        onToggleSound = onToggleSound,
                        onOpenVoice = { dispatch(PrototypeAction.OpenVoice) },
                        onOpenGallery = { dispatch(PrototypeAction.OpenGallery) },
                        onOpenPhoto = { dispatch(PrototypeAction.OpenPhoto(it)) },
                        onRevisitIntro = { dispatch(PrototypeAction.RevisitIntro) },
                    )
                    PrototypePage.Gallery -> PrototypeGallery(
                        realm = state.realm,
                        presence = state.presence,
                        photos = photos,
                        onBack = { dispatch(PrototypeAction.Back) },
                        onBeginWork = { dispatch(PrototypeAction.BeginWork) },
                        onFinishWork = { dispatch(PrototypeAction.FinishWork) },
                        onOpenPhoto = { dispatch(PrototypeAction.OpenPhoto(it)) },
                        onOpenVoice = { dispatch(PrototypeAction.OpenVoice) },
                    )
                    PrototypePage.Immersive -> PrototypeImmersive(
                        photo = photos[state.selectedPhotoIndex.coerceIn(0, photos.lastIndex)],
                        onBack = { dispatch(PrototypeAction.Back) },
                        onOpenVoice = { dispatch(PrototypeAction.OpenVoice) },
                    )
                }
            }
            if (state.voiceOpen) {
                VoiceEntrustOverlay(
                    onDismiss = { dispatch(PrototypeAction.CloseVoice) },
                    onShowPhotos = {
                        dispatch(PrototypeAction.OpenGallery)
                        dispatch(PrototypeAction.BeginWork)
                    },
                )
            }
        }
    }
}

@Composable
private fun IntroExperience(
    stage: IntroStage,
    permissionState: MediaPermissionState,
    photos: List<PrototypePhoto>,
    onAdvance: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onContinueWithSamples: () -> Unit,
) {
    val isPermission = stage == IntroStage.Permission
    Box(
        modifier = Modifier.fillMaxSize().clickable(enabled = !isPermission, onClick = onAdvance),
    ) {
        val background = if (stage == IntroStage.Crossing) {
            com.fushengce.home.R.drawable.fushengce_home_large_master
        } else {
            com.fushengce.home.R.drawable.fushengce_home_large_master
        }
        ConceptBackground(
            drawableRes = background,
            horizontalBias = when (stage) {
                IntroStage.World, IntroStage.LifeDuty, IntroStage.Permission, IntroStage.PhotoBloom -> -0.55f
                IntroStage.Crossing -> 0f
                IntroStage.WorkDuty -> 0.58f
            },
            dim = if (isPermission) 0.48f else 0.23f,
        )
        Crossfade(targetState = stage, animationSpec = tween(620), label = "初见章节") { current ->
            when (current) {
                IntroStage.World -> IntroTitle()
                IntroStage.LifeDuty -> IntroDuty(
                    eyebrow = "生活境 · 拾影",
                    title = "把平凡日子，轻轻收好",
                    detail = "她会寻影、理影，也替你记住那些容易被忘掉的光。",
                    align = Alignment.BottomStart,
                )
                IntroStage.Permission -> PermissionChapter(
                    state = permissionState,
                    onRequestPermission = onRequestPermission,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onContinueWithSamples = onContinueWithSamples,
                )
                IntroStage.PhotoBloom -> Box(Modifier.fillMaxSize()) {
                    PhotoCluster(
                        photos = photos,
                        onPhotoClick = {},
                        modifier = Modifier.align(Alignment.Center).padding(top = 40.dp),
                    )
                    Text(
                        "影像已归来",
                        color = Color(0xFFFFF4DD),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 86.dp),
                    )
                }
                IntroStage.Crossing -> CrossingChapter()
                IntroStage.WorkDuty -> IntroDuty(
                    eyebrow = "行事境 · 司录",
                    title = "把重要的事，理得清楚",
                    detail = "她会循地找影、按事归卷，让现场与工作记录各有来处。",
                    align = Alignment.BottomEnd,
                )
            }
        }
        if (!isPermission) {
            Text(
                "轻触继续",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun IntroTitle() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "浮生册",
            color = Color(0xFFFFF3D6),
            fontSize = 42.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "一册藏光影 · 一念入两境",
            color = Color(0xFFE8D4B2),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun IntroDuty(
    eyebrow: String,
    title: String,
    detail: String,
    align: Alignment,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 64.dp),
        verticalArrangement = if (align == Alignment.BottomStart || align == Alignment.BottomEnd) {
            Arrangement.Bottom
        } else {
            Arrangement.Center
        },
        horizontalAlignment = if (align == Alignment.BottomEnd) Alignment.End else Alignment.Start,
    ) {
        Text(eyebrow, color = Color(0xFFE7C99E), style = MaterialTheme.typography.labelLarge)
        Text(
            title,
            color = Color(0xFFFFF5E4),
            textAlign = if (align == Alignment.BottomEnd) TextAlign.End else TextAlign.Start,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            detail,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = if (align == Alignment.BottomEnd) TextAlign.End else TextAlign.Start,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 9.dp).fillMaxWidth(0.78f),
        )
    }
}

@Composable
private fun PermissionChapter(
    state: MediaPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onContinueWithSamples: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            color = Color(0xF4F1E5CE),
            contentColor = Color(0xFF2B2118),
            shape = CutCornerShape(topStart = 22.dp, bottomEnd = 22.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SealGlyph(Modifier.size(48.dp))
                Text("请准我阅影", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "浮生册需要读取照片与视频，才能替你寻影、理影、归册。内容仍留在本机。",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
                val denied = state as? MediaPermissionState.Denied
                InscriptionAction(
                    text = if (denied?.requiresSystemSettings == true) "前往系统设置" else "允许读取照片",
                    onClick = if (denied?.requiresSystemSettings == true) onOpenSystemSettings else onRequestPermission,
                    modifier = Modifier.padding(top = 22.dp),
                )
                Text(
                    "先以样例入册",
                    color = Color(0xFF725849),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp).clickable(onClick = onContinueWithSamples),
                )
            }
        }
    }
}

@Composable
private fun CrossingChapter() {
    val transition = rememberInfiniteTransition(label = "跨境微光")
    val sweep by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1_500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "过界光",
    )
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0x66FFF1CE), Color.Transparent),
                    startX = size.width * (sweep - 0.22f),
                    endX = size.width * (sweep + 0.22f),
                ),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("跨境而行", color = Color(0xFFFFF1D4), style = MaterialTheme.typography.headlineSmall)
            Text(
                "少女穿过月洞门，衣袂收束，长成行事的模样",
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, start = 34.dp, end = 34.dp),
            )
        }
    }
}

@Composable
private fun PrototypeHome(
    realm: PrototypeRealm,
    photos: List<PrototypePhoto>,
    soundEnabled: Boolean,
    welcomeVisible: Boolean,
    onSwitchRealm: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    onRevisitIntro: () -> Unit,
) {
    val sceneBias by animateFloatAsState(
        targetValue = if (realm == PrototypeRealm.Life) -0.32f else 0.32f,
        animationSpec = tween(720, easing = FastOutSlowInEasing),
        label = "镜头微转",
    )
    Box(Modifier.fillMaxSize()) {
        ConceptBackground(
            drawableRes = com.fushengce.home.R.drawable.fushengce_home_large_master,
            horizontalBias = sceneBias,
            dim = 0.17f,
        )
        SecondaryRealmVeil(
            realm = realm,
            onSwitchRealm = onSwitchRealm,
            modifier = Modifier.align(
                if (realm == PrototypeRealm.Life) Alignment.CenterEnd else Alignment.CenterStart,
            ),
        )
        HomeTitle(
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onRevisitIntro = onRevisitIntro,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        PhotoCluster(
            photos = photos,
            onPhotoClick = onOpenPhoto,
            modifier = Modifier.align(Alignment.Center).padding(top = 10.dp),
        )
        Text(
            if (realm == PrototypeRealm.Life) "最近 · 值得再看" else "近日 · 行事留影",
            color = Color(0xFFFFF1D6),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Center).offset(y = (-154).dp),
        )
        VoiceSeal(
            onClick = onOpenVoice,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 26.dp, bottom = 34.dp),
        )
        RealmBi(
            realm = realm,
            onClick = onSwitchRealm,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
        Text(
            "阅影",
            color = Color(0xFFFFECCE),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 48.dp)
                .clickable(onClick = onOpenGallery),
        )
        AnimatedVisibility(
            visible = welcomeVisible,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(320)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 118.dp),
        ) {
            Text(
                "你回来了，旧影都在。",
                color = Color(0xFFFFF0D2),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
internal fun ConceptBackground(
    @DrawableRes drawableRes: Int,
    horizontalBias: Float,
    dim: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias, 0f),
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = dim + 0.08f),
                    0.42f to Color.Black.copy(alpha = dim * 0.32f),
                    1f to Color.Black.copy(alpha = dim + 0.29f),
                ),
            ),
        )
    }
}

@Composable
private fun HomeTitle(
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onRevisitIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("浮生册", color = Color(0xFFFFF1D6), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                SealGlyph(Modifier.padding(start = 8.dp).size(22.dp))
            }
            Text(
                "一册两境",
                color = Color(0xFFE5CFAB),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "重温初见",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onRevisitIntro),
            )
        }
        SoundJade(enabled = soundEnabled, onClick = onToggleSound)
    }
}

@Composable
private fun SecondaryRealmVeil(
    realm: PrototypeRealm,
    onSwitchRealm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alignment = if (realm == PrototypeRealm.Life) Alignment.CenterEnd else Alignment.CenterStart
    val label = if (realm == PrototypeRealm.Life) "行事境" else "生活境"
    Box(
        modifier = modifier.fillMaxHeight().fillMaxWidth(0.22f).background(
            Brush.horizontalGradient(
                if (realm == PrototypeRealm.Life) {
                    listOf(Color.Transparent, Color(0xA71B1513))
                } else {
                    listOf(Color(0xA71B1513), Color.Transparent)
                },
            ),
        ).clickable(onClick = onSwitchRealm),
        contentAlignment = alignment,
    ) {
        Text(
            "$label\n轻触易境",
            color = Color(0xFFFFE9C7),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

@Composable
internal fun PrototypePhotoTile(
    photo: PrototypePhoto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val ratio = (photo.width.coerceAtLeast(1).toFloat() / photo.height.coerceAtLeast(1)).coerceIn(0.62f, 1.8f)
    Box(
        modifier = modifier.aspectRatio(ratio).shadow(9.dp, CutCornerShape(3.dp))
            .clip(CutCornerShape(3.dp)).background(Color(0xFF201A15)).clickable(onClick = onClick),
    ) {
        if (photo.uri != null) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (photo.drawableRes != null) {
            Image(
                painter = painterResource(photo.drawableRes),
                contentDescription = photo.title,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PhotoCluster(
    photos: List<PrototypePhoto>,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(width = 310.dp, height = 286.dp)) {
        val shown = photos.take(5)
        val placements = listOf(
            PhotoPlacement(68.dp, 60.dp, 176.dp, 5f),
            PhotoPlacement(8.dp, 20.dp, 112.dp, 2f),
            PhotoPlacement(197.dp, 27.dp, 105.dp, 3f),
            PhotoPlacement(20.dp, 171.dp, 122.dp, 1f),
            PhotoPlacement(188.dp, 172.dp, 114.dp, 1f),
        )
        shown.forEachIndexed { index, photo ->
            val place = placements[index]
            PrototypePhotoTile(
                photo = photo,
                onClick = { onPhotoClick(index) },
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(place.width).offset(place.x, place.y).zIndex(place.z),
            )
        }
    }
}

private data class PhotoPlacement(val x: Dp, val y: Dp, val width: Dp, val z: Float)

@Composable
private fun VoiceSeal(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xE8E9D4B3), radius = size.minDimension * 0.47f)
                drawCircle(Color(0xFF8D3427), radius = size.minDimension * 0.36f, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0x558D3427), radius = size.minDimension * 0.25f)
            }
            Text("托", color = Color(0xFF5D281F), style = MaterialTheme.typography.titleMedium)
        }
        Text("一语相托", color = Color(0xFFFFEED2), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RealmBi(realm: PrototypeRealm, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (realm == PrototypeRealm.Life) 0f else 180f,
        animationSpec = tween(680, easing = FastOutSlowInEasing),
        label = "旋璧翻面",
    )
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(58.dp).graphicsLayer { rotationY = rotation },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xEEDFC9A2), radius = size.minDimension * 0.46f)
                drawCircle(Color(0xFF715A43), radius = size.minDimension * 0.43f, style = Stroke(2.dp.toPx()))
                drawCircle(Color(0xB01B1612), radius = size.minDimension * 0.17f)
                repeat(6) { index ->
                    val angle = Math.toRadians(index * 60.0)
                    val center = Offset(
                        size.width / 2f + mathCos(angle) * size.width * 0.29f,
                        size.height / 2f + mathSin(angle) * size.height * 0.29f,
                    )
                    drawCircle(
                        if (realm == PrototypeRealm.Life) Color(0xFF71927B) else Color(0xFF985141),
                        radius = 2.dp.toPx(),
                        center = center,
                    )
                }
            }
        }
        Text("易境", color = Color(0xFFFFEBCB), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SoundJade(enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(width = 42.dp, height = 50.dp)) {
            val jade = if (enabled) Color(0xFFD6D4A8) else Color(0xFF898477)
            drawOval(jade, topLeft = Offset(size.width * 0.18f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.72f))
            drawCircle(Color(0xFF4D4034), radius = 3.dp.toPx(), center = Offset(size.width / 2f, size.height * 0.18f))
            if (enabled) {
                drawArc(Color(0x99F4E5BC), -30f, 60f, false, topLeft = Offset(size.width * 0.02f, size.height * 0.17f), size = androidx.compose.ui.geometry.Size(size.width * 0.96f, size.height * 0.72f), style = Stroke(1.dp.toPx()))
            }
        }
        Text(if (enabled) "鸣玉" else "息声", color = Color(0xFFFFEAC8), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SealGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val color = Color(0xFF9A392A)
        val stroke = Stroke(size.minDimension * 0.075f)
        drawRect(color, style = stroke)
        drawLine(color, Offset(size.width * 0.24f, size.height * 0.32f), Offset(size.width * 0.76f, size.height * 0.32f), stroke.width)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.28f, size.height * 0.74f), stroke.width)
        drawLine(color, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.74f), stroke.width)
        drawLine(color, Offset(size.width * 0.25f, size.height * 0.70f), Offset(size.width * 0.75f, size.height * 0.70f), stroke.width)
    }
}

@Composable
internal fun InscriptionAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF74352A),
        contentColor = Color(0xFFFFF1D7),
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
        )
    }
}

private fun MediaItem.toPrototypePhoto() = PrototypePhoto(
    key = "media-$id",
    title = displayName.ifBlank { "未命名影像" },
    uri = uri,
    width = width.coerceAtLeast(1),
    height = height.coerceAtLeast(1),
)

@Composable
private fun fallbackPrototypePhotos(): List<PrototypePhoto> = listOf(
    PrototypePhoto("sample-home", "浮生册样例影像", drawableRes = com.fushengce.home.R.drawable.fushengce_home_large_master, width = 1214, height = 1295),
    PrototypePhoto("sample-people", "生活与行事样例", drawableRes = com.fushengce.home.R.drawable.fushengce_home_large_master, width = 1536, height = 1024),
    PrototypePhoto("sample-crossing", "双境跨界样例", drawableRes = com.fushengce.home.R.drawable.fushengce_home_large_master, width = 1536, height = 1024),
)

private fun mathCos(value: Double): Float = kotlin.math.cos(value).toFloat()
private fun mathSin(value: Double): Float = kotlin.math.sin(value).toFloat()

private const val PROTOTYPE_PREFERENCES = "fushengce-visual-prototype"
private const val INTRO_COMPLETED_KEY = "intro-completed"
