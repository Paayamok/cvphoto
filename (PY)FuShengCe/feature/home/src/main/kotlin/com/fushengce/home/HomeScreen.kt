package com.fushengce.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onOpenGallery: () -> Unit,
    scholarDrawableRes: Int,
    generalDrawableRes: Int,
    feedbackAnimationRes: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onFeedback: (HomeFeedback) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember(context) { MainRealmStore(context) }
    val mainRealm by store.mainRealm.collectAsState(initial = MainRealm.Life)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var motion by remember { mutableStateOf(CharacterMotion.Idle) }
    var motionSequence by remember { mutableIntStateOf(0) }

    LaunchedEffect(motionSequence) {
        val sequence = motionSequence
        if (motion != CharacterMotion.Idle) {
            delay(MOTION_RETURN_DELAY_MILLIS)
            if (sequence == motionSequence) motion = CharacterMotion.Idle
        }
    }

    fun triggerFeedback(feedback: HomeFeedback) {
        motion = feedback.toCharacterMotion()
        motionSequence += 1
        onFeedback(feedback)
    }

    RealmNarrator(realm = mainRealm, enabled = soundEnabled)

    HomeScreen(
        state = HomeState(mainRealm = mainRealm),
        motion = motion,
        motionSequence = motionSequence,
        scholarDrawableRes = scholarDrawableRes,
        generalDrawableRes = generalDrawableRes,
        feedbackAnimationRes = feedbackAnimationRes,
        soundEnabled = soundEnabled,
        onToggleSound = {
            triggerFeedback(HomeFeedback.Complete)
            onToggleSound()
        },
        onAction = { action ->
            when (action) {
                is HomeAction.SelectMainRealm -> {
                    triggerFeedback(HomeFeedback.RealmSwitch(action.realm))
                    scope.launch { store.save(action.realm) }
                }
                is HomeAction.ShowMessage -> {
                    triggerFeedback(action.feedback)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(action.message)
                    }
                }
                HomeAction.OpenGallery -> {
                    triggerFeedback(HomeFeedback.Found)
                    onOpenGallery()
                }
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    state: HomeState,
    motion: CharacterMotion,
    motionSequence: Int,
    scholarDrawableRes: Int,
    generalDrawableRes: Int,
    feedbackAnimationRes: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onAction: (HomeAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF17130F),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        HomeScrollCanvas(
            state = state,
            motion = motion,
            motionSequence = motionSequence,
            feedbackAnimationRes = feedbackAnimationRes,
            soundEnabled = soundEnabled,
            onToggleSound = onToggleSound,
            onAction = onAction,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun HomeScrollCanvas(
    state: HomeState,
    motion: CharacterMotion,
    motionSequence: Int,
    feedbackAnimationRes: Int,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onAction: (HomeAction) -> Unit,
    contentPadding: PaddingValues,
) {
    val inkProgress = remember { Animatable(0f) }
    var observedInitialRealm by remember { mutableStateOf(false) }
    var quoteVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.mainRealm) {
        quoteVisible = false
        if (!observedInitialRealm) {
            observedInitialRealm = true
            delay(520)
        } else {
            inkProgress.snapTo(0f)
            launch {
                inkProgress.animateTo(1f, tween(INK_RISE_MILLIS))
                inkProgress.animateTo(0f, tween(INK_FALL_MILLIS))
            }
            delay(QUOTE_REVEAL_DELAY_MILLIS)
        }
        quoteVisible = true
        delay(QUOTE_VISIBLE_MILLIS)
        quoteVisible = false
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
    ) {
        val compact = maxHeight < 680.dp || maxWidth < 380.dp
        RealmScroll(state, motion, motionSequence, feedbackAnimationRes, compact, onAction)
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = inkProgress.value },
        ) {
            val spread = size.width * (0.28f + inkProgress.value * 0.72f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent, Color(0xD91A1714), Color(0xF20B0A09),
                        Color(0xD91A1714), Color.Transparent,
                    ),
                    startX = size.width / 2f - spread,
                    endX = size.width / 2f + spread,
                ),
            )
            repeat(7) { index ->
                val x = size.width * (0.10f + index * 0.14f)
                val y = size.height * (0.18f + (index % 3) * 0.27f)
                drawCircle(
                    color = Color(0xB9131110),
                    radius = spread * (0.18f + (index % 2) * 0.07f),
                    center = Offset(x, y),
                )
            }
        }
        ScrollHeader(
            soundEnabled = soundEnabled,
            compact = compact,
            onToggleSound = onToggleSound,
            onSwitchRealm = { onAction(HomeAction.SelectMainRealm(state.mainRealm.opposite())) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        TransitionVerse(
            visible = quoteVisible,
            realm = state.mainRealm,
            compact = compact,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        BottomScrollControls(
            state = state,
            compact = compact,
            onAction = onAction,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RealmScroll(
    state: HomeState,
    motion: CharacterMotion,
    motionSequence: Int,
    feedbackAnimationRes: Int,
    compact: Boolean,
    onAction: (HomeAction) -> Unit,
) {
    val lifeShare by animateFloatAsState(
        targetValue = state.lifeWeight / (state.lifeWeight + state.affairsWeight),
        animationSpec = tween(REALM_TRANSITION_MILLIS),
        label = "长卷焦点推移",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            RealmScene(
                MainRealm.Life, state.mainRealm == MainRealm.Life, state.lifeWeight,
                motion, motionSequence, feedbackAnimationRes, compact,
            ) { onAction(state.onRealmPressed(MainRealm.Life)) }
            RealmScene(
                MainRealm.Affairs, state.mainRealm == MainRealm.Affairs, state.affairsWeight,
                motion, motionSequence, feedbackAnimationRes, compact,
            ) { onAction(state.onRealmPressed(MainRealm.Affairs)) }
        }
        ScrollInkBlend(lifeShare = lifeShare, mainRealm = state.mainRealm)
    }
}

@Composable
private fun RowScope.RealmScene(
    realm: MainRealm,
    isMain: Boolean,
    targetWeight: Float,
    motion: CharacterMotion,
    motionSequence: Int,
    feedbackAnimationRes: Int,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = tween(REALM_TRANSITION_MILLIS),
        label = "册页推移",
    )
    val sceneScale by animateFloatAsState(
        targetValue = if (isMain) 1f else 1.08f,
        animationSpec = tween(REALM_TRANSITION_MILLIS),
        label = "景深变化",
    )
    val sceneAlpha by animateFloatAsState(
        targetValue = if (isMain) 1f else 0.84f,
        animationSpec = tween(REALM_TRANSITION_MILLIS),
        label = "光影变化",
    )

    Box(
        modifier = Modifier.weight(animatedWeight).fillMaxHeight().clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.fushengce_home_large_master),
            contentDescription = realm.contentDescription(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(
                horizontalBias = if (realm == MainRealm.Life) -0.82f else 0.82f,
                verticalBias = -0.02f,
            ),
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isMain) 0.dp else 2.dp)
                .graphicsLayer {
                    scaleX = sceneScale
                    scaleY = sceneScale
                    alpha = sceneAlpha
                },
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.18f),
                    0.45f to Color.Transparent,
                    0.76f to Color.Black.copy(alpha = 0.18f),
                    1f to Color.Black.copy(alpha = 0.84f),
                ),
            ),
        )
        RealmTitleTag(
            realm = realm,
            isMain = isMain,
            compact = compact,
            modifier = Modifier
                .align(if (realm == MainRealm.Life) Alignment.TopStart else Alignment.TopEnd)
                .padding(
                    top = if (compact) 86.dp else 102.dp,
                    start = if (realm == MainRealm.Life) 8.dp else 0.dp,
                    end = if (realm == MainRealm.Affairs) 8.dp else 0.dp,
                ),
        )
        if (isMain) {
            QuickScrollSlips(
                realm = realm,
                compact = compact,
                modifier = Modifier.align(Alignment.BottomStart).padding(
                    start = 12.dp, end = 12.dp, bottom = if (compact) 124.dp else 136.dp,
                ),
            )
        } else {
            RealmSwitchHint(
                realm = realm,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        key(motionSequence) {
            CharacterFeedbackAnimation(
                rawRes = feedbackAnimationRes,
                motion = motion,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
                    .size(if (isMain) 58.dp else 32.dp),
            )
        }
    }
}

@Composable
private fun ScrollInkBlend(lifeShare: Float, mainRealm: MainRealm) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val boundary = size.width * lifeShare
        val feather = size.width * 0.15f
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x24171310),
                    Color(0x66130F0C),
                    Color(0x30171310),
                    Color.Transparent,
                ),
                startX = boundary - feather,
                endX = boundary + feather,
            ),
        )
        repeat(9) { index ->
            val side = if (index % 2 == 0) -1f else 1f
            val x = boundary + side * feather * (0.08f + (index % 3) * 0.13f)
            val y = size.height * (index + 0.65f) / 9.8f
            drawCircle(
                color = Color(0x24110E0C),
                radius = feather * (0.22f + (index % 4) * 0.055f),
                center = Offset(x, y),
            )
        }
        drawRect(
            brush = if (mainRealm == MainRealm.Life) {
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.72f to Color.Transparent,
                    1f to Color(0x2B100D0B),
                )
            } else {
                Brush.horizontalGradient(
                    0f to Color(0x2B100D0B),
                    0.28f to Color.Transparent,
                    1f to Color.Transparent,
                )
            },
        )
    }
}

@Composable
private fun RealmSwitchHint(realm: MainRealm, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "点\n此\n换\n境",
            color = Color(0xFFF8EBD2).copy(alpha = 0.88f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(6.dp))
        Canvas(modifier = Modifier.size(13.dp)) {
            drawCircle(
                color = realm.accentColor().copy(alpha = 0.82f),
                radius = size.minDimension / 2f,
            )
            drawCircle(
                color = Color(0xFFFFF4DE).copy(alpha = 0.62f),
                radius = size.minDimension * 0.22f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ScrollHeader(
    soundEnabled: Boolean,
    compact: Boolean,
    onToggleSound: () -> Unit,
    onSwitchRealm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(if (compact) 78.dp else 92.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE9F7EEDC), Color(0xB5F4E8D1), Color.Transparent),
                ),
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "浮生册",
                    color = Color(0xFF1D1712),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(7.dp))
                FuShengSeal(modifier = Modifier.size(22.dp))
            }
            Text(
                text = "一册藏光影 · 一念入两境",
                color = Color(0xFF5C4A38),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        SealControl(if (soundEnabled) "声" else "静", if (soundEnabled) "有声" else "静音", Color(0xFF5E5637), onToggleSound)
        Spacer(Modifier.width(8.dp))
        SealControl("换", "换境", Color(0xFF873424), onSwitchRealm)
    }
}

@Composable
private fun SealControl(mark: String, label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.width(42.dp).clickable(onClick = onClick),
        color = accent.copy(alpha = 0.92f),
        contentColor = Color(0xFFFFF5E5),
        shape = CutCornerShape(topStart = 9.dp, bottomEnd = 9.dp),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(mark, style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TransitionVerse(
    visible: Boolean,
    realm: MainRealm,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.padding(top = if (compact) 102.dp else 120.dp),
        enter = fadeIn(tween(560)) + slideInVertically(tween(560)) { -it / 3 },
        exit = fadeOut(tween(760)) + slideOutVertically(tween(760)) { -it / 5 },
    ) {
        Surface(
            color = Color(0xEAF8F0E2),
            contentColor = Color(0xFF241A13),
            shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
            shadowElevation = 5.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = realm.transitionQuote(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (realm == MainRealm.Life) "归于烟火" else "起而行事",
                    color = realm.accentColor(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun RealmTitleTag(
    realm: MainRealm,
    isMain: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(if (compact) 46.dp else 54.dp),
        color = realm.accentColor().copy(alpha = if (isMain) 0.94f else 0.78f),
        contentColor = Color(0xFFFFF8E9),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        shadowElevation = if (isMain) 4.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (realm == MainRealm.Life) "生\n活\n境" else "行\n事\n境",
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            if (isMain) {
                Spacer(Modifier.height(7.dp))
                Text("主境", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun QuickScrollSlips(
    realm: MainRealm,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = if (realm == MainRealm.Life) "拾光入册" else "要事有序",
            color = Color(0xFFFFF5E5),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            realm.quickEntries().forEach { entry ->
                Surface(
                    color = Color(0xB5221B16),
                    contentColor = Color(0xFFFFF3DE),
                    shape = CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp),
                ) {
                    Text(
                        text = entry,
                        modifier = Modifier.padding(
                            horizontal = if (compact) 6.dp else 8.dp,
                            vertical = 5.dp,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomScrollControls(
    state: HomeState,
    compact: Boolean,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        VoiceEntrustBox(compact) { onAction(state.onBottomItemPressed(HomeBottomItem.Voice)) }
        AncientBottomBar { onAction(state.onBottomItemPressed(it)) }
    }
}

@Composable
private fun VoiceEntrustBox(compact: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            .height(if (compact) 54.dp else 62.dp).clickable(onClick = onClick),
        color = Color(0xEDEFE2CE),
        contentColor = Color(0xFF241A13),
        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
        shadowElevation = 7.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FuShengSeal(modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("一语相托", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "寻影、理卷、留事，皆可相托",
                    color = Color(0xFF6D5945),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                "按住诉说",
                color = Color(0xFF873424),
                modifier = Modifier.widthIn(min = 58.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun AncientBottomBar(onItemClick: (HomeBottomItem) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color(0xF7F4E8D5), Color(0xFFF0DFC2))),
        ),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(5.dp)) {
            drawLine(
                color = Color(0xFF7E6348),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx(),
            )
            repeat(9) { index ->
                drawCircle(
                    color = Color(0x667E6348),
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * (index + 1) / 10f, size.height / 2f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(59.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeBottomItem.entries.forEach { item ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clickable { onItemClick(item) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    HomeBottomIcon(item, item == HomeBottomItem.Gallery)
                    Text(
                        text = item.label,
                        color = if (item == HomeBottomItem.Gallery) Color(0xFF8B3326) else Color(0xFF46382B),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

private fun MainRealm.accentColor(): Color = when (this) {
    MainRealm.Life -> Color(0xFF5E5637)
    MainRealm.Affairs -> Color(0xFF873424)
}

private fun MainRealm.quickEntries(): List<String> = when (this) {
    MainRealm.Life -> listOf("全部照片", "人物谱", "足迹")
    MainRealm.Affairs -> listOf("工作照片", "任务清单", "现场记录")
}

private fun MainRealm.contentDescription(): String = when (this) {
    MainRealm.Life -> "生活境少女、相册与猫的冻结画卷"
    MainRealm.Affairs -> "行事境黑红行事角色与案卷的冻结画卷"
}

@Composable
private fun CharacterFeedbackAnimation(
    rawRes: Int,
    motion: CharacterMotion,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (motion.loops) LottieConstants.IterateForever else 1,
        clipSpec = LottieClipSpec.Marker(motion.marker),
        restartOnPlay = true,
    )
    LottieAnimation(composition = composition, progress = { progress }, modifier = modifier)
}

@Composable
private fun FuShengSeal(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.08f)
        drawRect(color = color, style = stroke)
        drawLine(color, Offset(size.width * 0.25f, size.height * 0.28f), Offset(size.width * 0.75f, size.height * 0.28f), strokeWidth = stroke.width)
        drawLine(color, Offset(size.width * 0.28f, size.height * 0.25f), Offset(size.width * 0.28f, size.height * 0.75f), strokeWidth = stroke.width)
        drawLine(color, Offset(size.width * 0.72f, size.height * 0.25f), Offset(size.width * 0.72f, size.height * 0.75f), strokeWidth = stroke.width)
        drawLine(color, Offset(size.width * 0.25f, size.height * 0.72f), Offset(size.width * 0.75f, size.height * 0.72f), strokeWidth = stroke.width)
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.28f), Offset(size.width * 0.5f, size.height * 0.72f), strokeWidth = stroke.width)
    }
}

@Composable
private fun HomeBottomIcon(
    item: HomeBottomItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) Color(0xFF98382A) else Color(0xFF5C4938)
    Canvas(modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.075f)
        when (item) {
            HomeBottomItem.Gallery -> {
                drawRect(color, Offset(size.width * 0.12f, size.height * 0.20f), Size(size.width * 0.76f, size.height * 0.62f), style = stroke)
                drawLine(color, Offset(size.width * 0.50f, size.height * 0.20f), Offset(size.width * 0.50f, size.height * 0.82f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * 0.20f, size.height * 0.31f), Offset(size.width * 0.42f, size.height * 0.31f), strokeWidth = stroke.width)
            }
            HomeBottomItem.Voice -> {
                repeat(5) { index ->
                    val x = size.width * (0.22f + index * 0.14f)
                    val half = size.height * (0.12f + (2 - kotlin.math.abs(index - 2)) * 0.07f)
                    drawLine(color, Offset(x, size.height / 2f - half), Offset(x, size.height / 2f + half), strokeWidth = stroke.width)
                }
            }
            HomeBottomItem.Profile -> {
                drawCircle(color, size.width * 0.16f, Offset(size.width * 0.5f, size.height * 0.31f), style = stroke)
                drawArc(color, 200f, 140f, false, Offset(size.width * 0.18f, size.height * 0.46f), Size(size.width * 0.64f, size.height * 0.40f), style = stroke)
            }
        }
    }
}

private const val REALM_TRANSITION_MILLIS = 1_420
private const val INK_RISE_MILLIS = 520
private const val INK_FALL_MILLIS = 760
private const val QUOTE_REVEAL_DELAY_MILLIS = 360L
private const val QUOTE_VISIBLE_MILLIS = 3_600L
private const val MOTION_RETURN_DELAY_MILLIS = 1_500L
