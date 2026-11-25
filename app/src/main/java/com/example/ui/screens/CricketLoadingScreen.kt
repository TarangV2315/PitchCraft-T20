package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CricketLoadingScreen(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cricketSplashLoop")

    val deliveryProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "deliveryProgress"
    )

    val seamSpinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "seamSpin"
    )

    val floodlightSweep by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floodlightSweep"
    )

    val loadingFraction = remember { Animatable(0.08f) }
    LaunchedEffect(Unit) {
        loadingFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2150, easing = FastOutSlowInEasing)
        )
    }

    val statusText = when {
        loadingFraction.value < 0.38f -> "Preparing Floodlit T20 Arenas..."
        loadingFraction.value < 0.72f -> "Calibrating Ball-by-Ball Match Engine..."
        else -> "Syncing Club Vault & Player Profiles..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("cricket_loading_screen")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.22f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF07110E).copy(alpha = 0.88f),
                            Color(0xFF0B1814).copy(alpha = 0.76f),
                            DarkBackground
                        )
                    )
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth >= 520.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TurfEmerald80.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsCricket,
                                contentDescription = null,
                                tint = TurfEmerald80,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "T20 CLUB EDITION",
                                style = MaterialTheme.typography.labelSmall,
                                color = TurfEmerald80
                            )
                        }
                    }

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("loading_skip_button")
                    ) {
                        Text("Skip", color = DarkOnSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Skip intro",
                            tint = DarkOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(minOf(this@BoxWithConstraints.maxHeight * 0.65f, 230.dp))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            TurfEmerald80.copy(alpha = 0.16f),
                                            Color(0xFF0D1D18).copy(alpha = 0.92f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CricketDeliveryAnimationCanvas(
                                progress = deliveryProgress,
                                seamSpinDegrees = seamSpinDegrees,
                                floodlightSweep = floodlightSweep,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(
                            modifier = Modifier.widthIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "PitchCraft T20",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "TACTICAL CRICKET MANAGEMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FloodlightGold80
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinearProgressIndicator(
                                    progress = { loadingFraction.value },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = TurfEmerald80,
                                    trackColor = DarkSurfaceContainerHighest
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkOnSurfaceVariant
                                    )
                                    Text(
                                        text = "${(loadingFraction.value * 100).toInt()}%",
                                        style = TelemetryTextStyles.statSmall,
                                        color = TurfEmerald80
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                TurfEmerald80.copy(alpha = 0.16f),
                                                Color(0xFF0D1D18).copy(alpha = 0.92f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CricketDeliveryAnimationCanvas(
                                    progress = deliveryProgress,
                                    seamSpinDegrees = seamSpinDegrees,
                                    floodlightSweep = floodlightSweep,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "PitchCraft T20",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = DarkOnSurface
                                )
                                Text(
                                    text = "TACTICAL CRICKET MANAGEMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FloodlightGold80
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { loadingFraction.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = TurfEmerald80,
                                trackColor = DarkSurfaceContainerHighest
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkOnSurfaceVariant
                                )
                                Text(
                                    text = "${(loadingFraction.value * 100).toInt()}%",
                                    style = TelemetryTextStyles.statSmall,
                                    color = TurfEmerald80
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CricketDeliveryAnimationCanvas(
    progress: Float,
    seamSpinDegrees: Float,
    floodlightSweep: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Stadium floodlight beams
        val beamOffset = floodlightSweep * w * 0.12f
        drawPath(
            path = Path().apply {
                moveTo(w * 0.18f, 0f)
                lineTo(cx - w * 0.22f + beamOffset, h * 0.85f)
                lineTo(cx + w * 0.14f + beamOffset, h * 0.85f)
                close()
            },
            brush = Brush.verticalGradient(
                listOf(
                    FloodlightGold80.copy(alpha = 0.18f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.82f, 0f)
                lineTo(cx - w * 0.14f - beamOffset, h * 0.85f)
                lineTo(cx + w * 0.22f - beamOffset, h * 0.85f)
                close()
            },
            brush = Brush.verticalGradient(
                listOf(
                    TelemetryCyan80.copy(alpha = 0.14f),
                    Color.Transparent
                )
            )
        )

        // Outer stadium boundary ring
        drawCircle(
            color = TurfEmerald80.copy(alpha = 0.30f),
            radius = w * 0.44f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = FloodlightGold80.copy(alpha = 0.15f),
            radius = w * 0.36f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. 22-yard cricket pitch perspective trapezoid
        val pitchPath = Path().apply {
            moveTo(cx - w * 0.11f, h * 0.23f)
            lineTo(cx + w * 0.11f, h * 0.23f)
            lineTo(cx + w * 0.22f, h * 0.82f)
            lineTo(cx - w * 0.22f, h * 0.82f)
            close()
        }
        drawPath(
            path = pitchPath,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF9C8456).copy(alpha = 0.55f),
                    Color(0xFFC2A878).copy(alpha = 0.72f)
                )
            )
        )

        // Crease markings (Popping & Bowling creases)
        val topCreaseY = h * 0.32f
        val bottomCreaseY = h * 0.74f
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(cx - w * 0.16f, topCreaseY),
            end = Offset(cx + w * 0.16f, topCreaseY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(cx - w * 0.24f, bottomCreaseY),
            end = Offset(cx + w * 0.24f, bottomCreaseY),
            strokeWidth = 1.8.dp.toPx()
        )

        // 3. Stumps & bails at the batting end (top crease)
        val hitPhase = ((progress - 0.68f) / 0.32f).coerceIn(0f, 1f)
        val bailLift = if (progress >= 0.68f) sin(hitPhase * PI).toFloat() * 14.dp.toPx() else 0f
        drawCricketStumps(cx = cx, baseY = topCreaseY - 2.dp.toPx(), bailLift = bailLift)

        // 4. Swinging Cricket Bat near top crease
        val batSwingAngle = when {
            progress < 0.52f -> -32f
            progress < 0.72f -> -32f + ((progress - 0.52f) / 0.20f) * 78f
            else -> 46f - ((progress - 0.72f) / 0.28f) * 30f
        }
        drawCricketBat(
            pivot = Offset(cx + w * 0.08f, topCreaseY - 12.dp.toPx()),
            angleDegrees = batSwingAngle
        )

        // 5. Bounce turf ring at good length spot (~0.45 progress)
        val bounceY = h * 0.50f
        val bounceX = cx - w * 0.03f
        if (progress in 0.42f..0.75f) {
            val ringProgress = ((progress - 0.42f) / 0.33f).coerceIn(0f, 1f)
            drawCircle(
                color = TurfEmerald80.copy(alpha = (1f - ringProgress) * 0.8f),
                radius = 6.dp.toPx() + ringProgress * 24.dp.toPx(),
                center = Offset(bounceX, bounceY),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // 6. Ball trajectory: bowler release -> good length bounce -> bat impact -> lofted six arc
        val ballPos = when {
            progress < 0.45f -> {
                val t = progress / 0.45f
                val bx = lerpFloat(cx - w * 0.09f, bounceX, t)
                val by = lerpFloat(h * 0.78f, bounceY, t) - sin(t * PI).toFloat() * 18.dp.toPx()
                Offset(bx, by)
            }
            progress < 0.68f -> {
                val t = (progress - 0.45f) / 0.23f
                val bx = lerpFloat(bounceX, cx + w * 0.01f, t)
                val by = lerpFloat(bounceY, topCreaseY - 4.dp.toPx(), t) - sin(t * PI).toFloat() * 10.dp.toPx()
                Offset(bx, by)
            }
            else -> {
                val t = (progress - 0.68f) / 0.32f
                val bx = lerpFloat(cx + w * 0.01f, cx + w * 0.28f, t)
                val by = lerpFloat(topCreaseY - 4.dp.toPx(), h * 0.14f, t) - sin(t * PI).toFloat() * 24.dp.toPx()
                Offset(bx, by)
            }
        }

        // Motion trail behind cricket ball
        for (trailIdx in 1..4) {
            val trailProgress = (progress - trailIdx * 0.025f).coerceAtLeast(0f)
            val trailAlpha = (0.36f - trailIdx * 0.08f).coerceAtLeast(0f)
            val tx = lerpFloat(ballPos.x, cx, trailIdx * 0.09f)
            val ty = lerpFloat(ballPos.y, lerpFloat(h * 0.78f, topCreaseY, trailProgress), 0.12f)
            drawCircle(
                color = FloodlightGold80.copy(alpha = trailAlpha),
                radius = (8 - trailIdx).dp.toPx(),
                center = Offset(tx, ty)
            )
        }

        // Impact flash when bat connects (around progress == 0.68)
        if (progress in 0.66f..0.88f) {
            val burst = ((progress - 0.66f) / 0.22f).coerceIn(0f, 1f)
            drawCircle(
                color = FloodlightGold80.copy(alpha = (1f - burst) * 0.9f),
                radius = 8.dp.toPx() + burst * 30.dp.toPx(),
                center = Offset(cx + w * 0.01f, topCreaseY - 4.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Leather cricket ball with rotating stitched seam
        val ballRadius = when {
            progress < 0.45f -> lerpFloat(11.dp.toPx(), 9.dp.toPx(), progress / 0.45f)
            progress < 0.68f -> 9.dp.toPx()
            else -> lerpFloat(9.dp.toPx(), 11.5.dp.toPx(), (progress - 0.68f) / 0.32f)
        }
        drawCricketBall(
            center = ballPos,
            radius = ballRadius,
            seamAngle = seamSpinDegrees
        )
    }
}

private fun DrawScope.drawCricketStumps(cx: Float, baseY: Float, bailLift: Float) {
    val stumpWidth = 3.2.dp.toPx()
    val stumpHeight = 28.dp.toPx()
    val gap = 7.dp.toPx()
    val stumpColor = FloodlightGold80

    listOf(-gap, 0f, gap).forEach { dx ->
        drawRoundRect(
            color = stumpColor,
            topLeft = Offset(cx + dx - stumpWidth / 2f, baseY - stumpHeight),
            size = Size(stumpWidth, stumpHeight),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
    // Bails
    val bailY = baseY - stumpHeight - 3.dp.toPx() - bailLift
    drawLine(
        color = Color(0xFFFFE59E),
        start = Offset(cx - gap - 1.dp.toPx(), bailY),
        end = Offset(cx - 1.dp.toPx(), bailY - bailLift * 0.2f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFE59E),
        start = Offset(cx + 1.dp.toPx(), bailY - bailLift * 0.2f),
        end = Offset(cx + gap + 1.dp.toPx(), bailY),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCricketBat(pivot: Offset, angleDegrees: Float) {
    rotate(degrees = angleDegrees, pivot = pivot) {
        // Bat blade
        drawRoundRect(
            color = Color(0xFFEBD2A4),
            topLeft = Offset(pivot.x - 5.dp.toPx(), pivot.y - 38.dp.toPx()),
            size = Size(10.dp.toPx(), 32.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        // Bat splice/stripe
        drawLine(
            color = TurfEmerald80,
            start = Offset(pivot.x, pivot.y - 34.dp.toPx()),
            end = Offset(pivot.x, pivot.y - 10.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        // Bat handle
        drawRoundRect(
            color = Color(0xFF202E29),
            topLeft = Offset(pivot.x - 2.2.dp.toPx(), pivot.y - 6.dp.toPx()),
            size = Size(4.4.dp.toPx(), 16.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawCricketBall(center: Offset, radius: Float, seamAngle: Float) {
    // Leather gradient body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF5A5F), Color(0xFFB71C24), Color(0xFF6B0B10)),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
            radius = radius * 1.4f
        ),
        radius = radius,
        center = center
    )
    // Specular shine
    drawCircle(
        color = Color.White.copy(alpha = 0.28f),
        radius = radius * 0.26f,
        center = Offset(center.x - radius * 0.34f, center.y - radius * 0.34f)
    )
    // Rotating stitched seam
    rotate(degrees = seamAngle, pivot = center) {
        val rad = seamAngle * PI / 180.0
        val dx = (cos(rad) * radius * 0.18f).toFloat()
        drawLine(
            color = Color(0xFFFFF1D6),
            start = Offset(center.x - dx, center.y - radius * 0.85f),
            end = Offset(center.x + dx, center.y + radius * 0.85f),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color(0xFFFFF1D6).copy(alpha = 0.65f),
            radius = radius * 0.62f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
