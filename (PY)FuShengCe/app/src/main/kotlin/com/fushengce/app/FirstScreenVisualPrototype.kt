package com.fushengce.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The first-screen visual gate. It is intentionally self-contained and static:
 * no permission flow, gallery data or prototype overlays can dilute the first impression.
 */
@Composable
internal fun FirstScreenVisualPrototype(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF15201C))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        val w = maxWidth
        val h = maxHeight

        Image(
            painter = painterResource(com.fushengce.home.R.drawable.fushengce_home_large_master),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(w)
                .height(h * 0.54f)
                .align(Alignment.TopCenter),
        )
        ContinuousRealmWorld()

        ArchiveTitle(
            modifier = Modifier
                .offset(x = w * 0.055f, y = h * 0.035f),
        )

        // Characters stay behind the memories. The official is only a quiet glimpse
        // through the moon gate in this static gate prototype.
        LifeGirlFigure(
            modifier = Modifier
                .size(width = h * 0.108f, height = h * 0.19f)
                .offset(x = w * 0.10f, y = h * 0.655f),
        )
        AffairsOfficialFigure(
            modifier = Modifier
                .size(width = h * 0.095f, height = h * 0.185f)
                .offset(x = w * 0.785f, y = h * 0.535f),
        )

        FloatingMemoryCluster(width = w, height = h)

        RealmBi(
            modifier = Modifier
                .size(h * 0.047f)
                .offset(x = w * 0.778f, y = h * 0.445f),
        )
        SoundJade(
            modifier = Modifier
                .width(h * 0.035f)
                .height(h * 0.092f)
                .offset(x = w * 0.914f, y = h * 0.205f),
        )
        EntrustSeal(
            modifier = Modifier
                .width(w * 0.20f)
                .height(h * 0.066f)
                .offset(x = w * 0.405f, y = h * 0.895f),
        )

        Text(
            text = "生活境",
            color = Color(0xBFF7EDD8),
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.sp,
            modifier = Modifier.offset(x = w * 0.075f, y = h * 0.875f),
        )
        Text(
            text = "行事境",
            color = Color(0x99EBD9C5),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            modifier = Modifier.offset(x = w * 0.815f, y = h * 0.735f),
        )
    }
}

@Composable
private fun ContinuousRealmWorld() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0x20000000),
                0.46f to Color(0x6B263128),
                0.72f to Color(0xE31B211C),
                1.0f to Color(0xFF101512),
            ),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0x0CF9E8C1),
                    Color(0x143D3428),
                    Color(0x99301E1B),
                    Color(0xE0101720),
                ),
            ),
        )

        // Interior beams and the low furniture give the character real places to live.
        drawRect(Color(0xB3432E22), Offset(0f, h * 0.50f), Size(w * 0.73f, h * 0.025f))
        drawRect(Color(0xA336261E), Offset(w * 0.055f, h * 0.09f), Size(w * 0.035f, h * 0.69f))
        drawRect(Color(0x9E3A281E), Offset(w * 0.61f, h * 0.13f), Size(w * 0.030f, h * 0.65f))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xB7715037), Color(0xE232211A))),
            topLeft = Offset(w * 0.035f, h * 0.77f),
            size = Size(w * 0.53f, h * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        )
        drawRect(Color(0xD12C201A), Offset(w * 0.10f, h * 0.865f), Size(w * 0.035f, h * 0.13f))
        drawRect(Color(0xD12C201A), Offset(w * 0.48f, h * 0.865f), Size(w * 0.035f, h * 0.13f))

        // Moon gate is part of the same wall, never a hard panel divider.
        val gateCenter = Offset(w * 0.84f, h * 0.47f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xC31A2227), Color(0xE0181718)),
                center = gateCenter,
                radius = w * 0.32f,
            ),
            radius = w * 0.30f,
            center = gateCenter,
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(Color(0xFF9B7550), Color(0xFF3B2B24), Color(0xFFB38A5E), Color(0xFF9B7550)),
                center = gateCenter,
            ),
            startAngle = 112f,
            sweepAngle = 292f,
            useCenter = false,
            topLeft = Offset(gateCenter.x - w * 0.305f, gateCenter.y - w * 0.305f),
            size = Size(w * 0.61f, w * 0.61f),
            style = Stroke(width = w * 0.025f),
        )
        drawRect(Color(0xB05D352D), Offset(w * 0.79f, h * 0.16f), Size(w * 0.022f, h * 0.50f))
        drawRect(Color(0x7B8B4438), Offset(w * 0.93f, h * 0.18f), Size(w * 0.035f, h * 0.48f))
        repeat(4) { index ->
            val y = h * (0.28f + index * 0.075f)
            drawLine(Color(0x687F5C44), Offset(w * 0.80f, y), Offset(w, y - h * 0.02f), w * 0.008f)
        }

        // Floor perspective makes the two realms read as one continuous volume.
        repeat(6) { index ->
            val x = w * (index / 5f)
            drawLine(
                Color(0x355F4936),
                Offset(w * 0.51f, h * 0.70f),
                Offset(x, h),
                w * 0.003f,
            )
        }
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x9E050706))),
            topLeft = Offset(0f, h * 0.84f),
            size = Size(w, h * 0.16f),
        )
    }
}

@Composable
private fun FloatingMemoryCluster(width: Dp, height: Dp) {
    MemoryPhoto(
        drawable = R.drawable.fs_photo_travel,
        description = "海边旅行",
        width = width * 0.53f,
        height = height * 0.255f,
        x = width * 0.17f,
        y = height * 0.235f,
        rotation = -0.8f,
        elevation = 22.dp,
    )
    MemoryPhoto(
        drawable = R.drawable.fs_photo_family,
        description = "家人的笑容",
        width = width * 0.35f,
        height = height * 0.19f,
        x = width * 0.055f,
        y = height * 0.475f,
        rotation = -2.1f,
        elevation = 15.dp,
    )
    MemoryPhoto(
        drawable = R.drawable.fs_photo_child,
        description = "成长时刻",
        width = width * 0.36f,
        height = height * 0.20f,
        x = width * 0.50f,
        y = height * 0.485f,
        rotation = 1.5f,
        elevation = 17.dp,
    )
    MemoryPhoto(
        drawable = R.drawable.fs_photo_table,
        description = "人间烟火",
        width = width * 0.28f,
        height = height * 0.145f,
        x = width * 0.635f,
        y = height * 0.315f,
        rotation = 2.2f,
        elevation = 11.dp,
    )
}

@Composable
private fun MemoryPhoto(
    @DrawableRes drawable: Int,
    description: String,
    width: Dp,
    height: Dp,
    x: Dp,
    y: Dp,
    rotation: Float,
    elevation: Dp,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = description,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(width)
            .height(height)
            .offset(x = x, y = y)
            .rotate(rotation)
            .shadow(elevation, RoundedCornerShape(4.dp), clip = false)
            .clip(RoundedCornerShape(4.dp)),
    )
}

@Composable
private fun ArchiveTitle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(46.dp)
            .height(106.dp)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xDDEFE2C6), Color(0xC7C7AB7B))),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0x8A65462F),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "浮\n生\n册",
            color = Color(0xFF2B2118),
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LifeGirlFigure(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val skin = Color(0xFFF0C3A7)
        val hair = Color(0xFF251B18)
        drawOval(Brush.radialGradient(listOf(Color(0x45352125), Color.Transparent)), Offset(0f, h * 0.78f), Size(w, h * 0.22f))
        val robe = Path().apply {
            moveTo(w * 0.42f, h * 0.31f)
            quadraticBezierTo(w * 0.12f, h * 0.52f, w * 0.19f, h * 0.94f)
            quadraticBezierTo(w * 0.50f, h, w * 0.80f, h * 0.93f)
            quadraticBezierTo(w * 0.88f, h * 0.52f, w * 0.59f, h * 0.31f)
            close()
        }
        drawPath(robe, Brush.horizontalGradient(listOf(Color(0xFFD9C8AA), Color(0xFFF3E8D0), Color(0xFF9CAC91))))
        drawCircle(skin, w * 0.19f, Offset(w * 0.51f, h * 0.22f))
        drawArc(hair, 175f, 195f, true, Offset(w * 0.29f, h * 0.035f), Size(w * 0.45f, w * 0.48f))
        drawCircle(hair, w * 0.09f, Offset(w * 0.39f, h * 0.045f))
        drawCircle(Color(0xFFF2D9C5), w * 0.012f, Offset(w * 0.45f, h * 0.22f))
        drawCircle(Color(0xFFF2D9C5), w * 0.012f, Offset(w * 0.57f, h * 0.22f))
        drawLine(Color(0xFF8E6356), Offset(w * 0.47f, h * 0.275f), Offset(w * 0.55f, h * 0.27f), w * 0.012f, StrokeCap.Round)
        drawLine(Color(0xFF755A46), Offset(w * 0.30f, h * 0.49f), Offset(w * 0.70f, h * 0.52f), w * 0.035f, StrokeCap.Round)
        drawLine(skin, Offset(w * 0.26f, h * 0.51f), Offset(w * 0.43f, h * 0.58f), w * 0.055f, StrokeCap.Round)
    }
}

@Composable
private fun AffairsOfficialFigure(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val skin = Color(0xFFE9B89D)
        val hair = Color(0xFF171416)
        val robe = Path().apply {
            moveTo(w * 0.39f, h * 0.30f)
            quadraticBezierTo(w * 0.05f, h * 0.48f, w * 0.13f, h * 0.97f)
            lineTo(w * 0.88f, h * 0.97f)
            quadraticBezierTo(w * 0.94f, h * 0.47f, w * 0.60f, h * 0.30f)
            close()
        }
        drawPath(robe, Brush.horizontalGradient(listOf(Color(0xFF371C22), Color(0xFF803B37), Color(0xFF20232A))))
        drawPath(
            Path().apply {
                moveTo(w * 0.30f, h * 0.38f)
                lineTo(w * 0.67f, h * 0.38f)
                lineTo(w * 0.80f, h * 0.88f)
                lineTo(w * 0.50f, h * 0.72f)
                lineTo(w * 0.17f, h * 0.90f)
                close()
            },
            Color(0xAA101820),
        )
        drawCircle(skin, w * 0.18f, Offset(w * 0.50f, h * 0.21f))
        drawArc(hair, 176f, 198f, true, Offset(w * 0.29f, h * 0.03f), Size(w * 0.43f, w * 0.47f))
        drawCircle(hair, w * 0.10f, Offset(w * 0.50f, h * 0.035f))
        drawLine(Color(0xFFC69B5B), Offset(w * 0.32f, h * 0.08f), Offset(w * 0.68f, h * 0.08f), w * 0.025f, StrokeCap.Round)
        drawCircle(Color(0xFFF2D9C5), w * 0.010f, Offset(w * 0.44f, h * 0.21f))
        drawCircle(Color(0xFFF2D9C5), w * 0.010f, Offset(w * 0.56f, h * 0.21f))
        drawLine(Color(0xFFB18D5B), Offset(w * 0.28f, h * 0.56f), Offset(w * 0.73f, h * 0.53f), w * 0.070f, StrokeCap.Round)
        drawLine(Color(0xFFF1E3C4), Offset(w * 0.29f, h * 0.56f), Offset(w * 0.71f, h * 0.53f), w * 0.040f, StrokeCap.Round)
    }
}

@Composable
private fun RealmBi(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFF0E5C9), Color(0xFF91AA9D), Color(0xFF29443D))),
            radius = size.minDimension * 0.48f,
            center = center,
        )
        drawCircle(Color(0xFF17211E), size.minDimension * 0.16f, center)
        drawArc(
            color = Color(0xFFD0B77C),
            startAngle = -70f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(size.minDimension * 0.055f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun SoundJade(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawLine(Color(0xFFC4A86D), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h * 0.25f), w * 0.08f)
        val jade = Path().apply {
            moveTo(w * 0.18f, h * 0.24f)
            quadraticBezierTo(w * 0.50f, h * 0.13f, w * 0.82f, h * 0.24f)
            lineTo(w * 0.70f, h * 0.80f)
            quadraticBezierTo(w * 0.50f, h, w * 0.30f, h * 0.80f)
            close()
        }
        drawPath(jade, Brush.verticalGradient(listOf(Color(0xFFE3F0DA), Color(0xFF789A84))))
        drawPath(jade, Color(0xFFDFCAA1), style = Stroke(w * 0.045f))
        drawArc(
            color = Color(0x6BE6D7B4),
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(w * 0.05f, h * 0.63f),
            size = Size(w * 0.90f, h * 0.35f),
            style = Stroke(w * 0.04f),
        )
    }
}

@Composable
private fun EntrustSeal(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFB85A45), Color(0xFF70271F))),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
            )
            drawRoundRect(Color(0xFFDDBB84), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawCircle(Color(0x44FFF4D4), size.minDimension * 0.22f, Offset(size.width * 0.35f, size.height * 0.28f))
        }
        Text(
            text = "一语相托",
            color = Color(0xFFF7EBD5),
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.offset(y = 31.dp),
        )
    }
}
