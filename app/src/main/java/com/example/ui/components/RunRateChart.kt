package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LiveMatchState
import com.example.model.RunRatePoint
import com.example.ui.theme.*
import kotlin.math.ceil

@Composable
fun RunRateComparisonGraphCard(
    live: LiveMatchState,
    modifier: Modifier = Modifier
) {
    val points = live.runRateHistory
    val hasTarget = live.target != null
    val rrrNow = live.requiredRunRate
    val crrNow = live.currentRunRate
    val pitchParRate = remember(live.fixture.pitchType) {
        (8.4 * live.fixture.pitchType.runBias).coerceIn(7.0, 10.5)
    }

    val yMax = remember(points, crrNow, rrrNow, pitchParRate) {
        val sustainedRates = points
            .filter { it.ball >= 3 }
            .flatMap { pt ->
                listOfNotNull(
                    pt.crr,
                    pt.rrr?.takeIf { pt.ball <= 114 }
                )
            }
        val baselinePeak = maxOf(
            if (live.balls >= 3) crrNow else crrNow.coerceAtMost(14.0),
            rrrNow?.takeIf { live.balls <= 114 } ?: 0.0,
            pitchParRate
        )
        val rawPeak = maxOf(sustainedRates.maxOrNull() ?: 0.0, baselinePeak)
        (ceil(maxOf(12.0, rawPeak + 1.5) / 2.0) * 2.0).coerceIn(12.0, 24.0)
    }

    val rrrAccent = when {
        rrrNow == null -> MaterialTheme.colorScheme.onSurfaceVariant
        rrrNow - crrNow >= 2.0 -> AlertCrimson
        rrrNow > crrNow -> BoundaryAmber
        else -> PitchMint
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_run_rate_graph_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Title + Live CRR / RRR Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Run Rate vs Required Rate",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasTarget) {
                            "Inn ${live.currentInnings} Chase • Target ${live.target}"
                        } else {
                            "Inn ${live.currentInnings} • Par ~${"%.1f".format(pitchParRate)} RPO"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PitchMint.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PitchMint.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "CRR ${"%.2f".format(crrNow)}",
                            style = TelemetryTextStyles.statSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PitchMint
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("graph_crr_value")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = rrrAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, rrrAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (rrrNow != null) "RRR ${"%.2f".format(rrrNow)}" else "RRR Inn 2",
                            style = TelemetryTextStyles.statSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = rrrAccent
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag(if (rrrNow != null) "graph_rrr_value" else "graph_rrr_pending")
                        )
                    }
                }
            }

            // Graph Area: Y-Axis Labels + Real-Time Line Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .width(26.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${yMax.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(yMax / 2).toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(6.dp))

                RunRateLineCanvas(
                    points = points,
                    target = live.target,
                    pitchParRate = pitchParRate,
                    yMax = yMax,
                    crrColor = PitchMint,
                    rrrColor = BoundaryAmber,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("live_run_rate_canvas")
                )
            }

            // Bottom X-Axis Overs Scale + Compact Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDotLabel(color = PitchMint, label = "CRR")
                    if (hasTarget) {
                        LegendDotLabel(color = BoundaryAmber, label = "RRR")
                    } else {
                        LegendDotLabel(color = TelemetryCyan80.copy(alpha = 0.75f), label = "Par RPO")
                    }
                }

                Text(
                    text = "0 — 6 (PP) — 12 — 16 — 20 Ov",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (rrrNow != null) {
                    val diff = crrNow - rrrNow
                    val statusText = if (diff >= 0.0) {
                        "+${"%.2f".format(diff)} ahead"
                    } else {
                        "${"%.2f".format(diff)} behind"
                    }
                    Text(
                        text = statusText,
                        style = TelemetryTextStyles.statSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (diff >= 0.0) PitchMint else AlertCrimson
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotLabel(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunRateLineCanvas(
    points: List<RunRatePoint>,
    target: Int?,
    pitchParRate: Double,
    yMax: Double,
    crrColor: Color,
    rrrColor: Color,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val ppTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val parLineColor = TelemetryCyan80.copy(alpha = 0.65f)
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainerLow

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val padV = 6.dp.toPx()
        val chartH = (h - padV * 2f).coerceAtLeast(1f)

        fun xForBall(ball: Int): Float = ((ball.coerceIn(0, 120)) / 120f) * w
        fun yForRate(rate: Double): Float {
            val norm = (rate / yMax).toFloat().coerceIn(0f, 1f)
            return padV + chartH * (1f - norm)
        }

        // Background + Powerplay shaded region (overs 0..6)
        drawRect(color = surfaceBg, topLeft = Offset.Zero, size = Size(w, h))
        drawRect(color = ppTint, topLeft = Offset.Zero, size = Size(xForBall(36), h))

        // Horizontal gridlines (0, mid, top)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        listOf(0.0, yMax / 2.0, yMax).forEachIndexed { idx, rateVal ->
            val y = yForRate(rateVal)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = if (idx == 0) null else dashEffect
            )
        }

        // Vertical over markers (6, 12, 16, 20)
        listOf(36, 72, 96, 120).forEach { ballMark ->
            val x = xForBall(ballMark)
            drawLine(
                color = gridColor.copy(alpha = 0.35f),
                start = Offset(x, padV),
                end = Offset(x, h - padV),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        // Baseline reference: either Par RPO (1st inn) or Initial RRR horizontal guide (2nd inn)
        if (target == null) {
            val parY = yForRate(pitchParRate)
            drawLine(
                color = parLineColor,
                start = Offset(0f, parY),
                end = Offset(w, parY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = dashEffect
            )
        } else {
            val initialRrr = target / 20.0
            val startRrrY = yForRate(initialRrr)
            drawLine(
                color = rrrColor.copy(alpha = 0.32f),
                start = Offset(0f, startRrrY),
                end = Offset(w, startRrrY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        if (points.isEmpty()) return@Canvas

        // Plot RRR curve first (2nd innings chase)
        val rrrPoints = points.mapNotNull { pt -> pt.rrr?.let { pt.ball to it } }
        if (rrrPoints.isNotEmpty()) {
            val initialRrr = (target ?: 0) / 20.0
            val rrrPath = Path().apply {
                moveTo(0f, yForRate(initialRrr))
                rrrPoints.forEach { (b, rate) ->
                    lineTo(xForBall(b), yForRate(rate))
                }
            }
            drawPath(
                path = rrrPath,
                color = rrrColor,
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )
            val (lastBall, lastRrr) = rrrPoints.last()
            drawCircle(
                color = rrrColor,
                radius = 3.8.dp.toPx(),
                center = Offset(xForBall(lastBall), yForRate(lastRrr))
            )
        }

        // Plot CRR curve with subtle vertical fill underneath
        val firstBall = points.first().ball
        val firstCrr = points.first().crr
        val crrPath = Path().apply {
            moveTo(0f, yForRate(firstCrr))
            if (points.size == 1) {
                lineTo(xForBall(firstBall), yForRate(firstCrr))
            } else {
                points.forEach { pt ->
                    lineTo(xForBall(pt.ball), yForRate(pt.crr))
                }
            }
        }
        val fillPath = Path().apply {
            addPath(crrPath)
            val lastX = xForBall(points.last().ball)
            lineTo(lastX, yForRate(0.0))
            lineTo(0f, yForRate(0.0))
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(crrColor.copy(alpha = 0.28f), Color.Transparent),
                startY = padV,
                endY = h - padV
            )
        )
        drawPath(
            path = crrPath,
            color = crrColor,
            style = Stroke(
                width = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        val lastCrrPt = points.last()
        val endCenter = Offset(xForBall(lastCrrPt.ball), yForRate(lastCrrPt.crr))
        drawCircle(
            color = crrColor.copy(alpha = 0.28f),
            radius = 6.5.dp.toPx(),
            center = endCenter
        )
        drawCircle(
            color = crrColor,
            radius = 3.6.dp.toPx(),
            center = endCenter
        )
    }
}
