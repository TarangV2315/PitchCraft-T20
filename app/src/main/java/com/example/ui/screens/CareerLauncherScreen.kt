package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CareerSummary
import com.example.data.local.WelcomeRewardsState
import com.example.model.CricketNation
import com.example.simulation.ClubTemplate
import com.example.simulation.WorldGenerator
import com.example.ui.components.ClubBadge
import com.example.ui.components.ScreenGuideDialog
import com.example.ui.components.ScreenGuideId
import com.example.ui.components.TopLeftInfoButton
import com.example.ui.components.formatCompactCurrency
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class WelcomeDestination {
    HUB,
    NEW_CAREER
}

private data class SparkSeed(
    val xFraction: Float,
    val baseYFraction: Float,
    val speed: Float,
    val phaseOffset: Float,
    val radiusDp: Float,
    val colorIndex: Int
)

private val STADIUM_SPARK_SEEDS = listOf(
    SparkSeed(0.10f, 0.85f, 0.55f, 0.1f, 3.0f, 0),
    SparkSeed(0.18f, 0.72f, 0.72f, 1.4f, 2.2f, 1),
    SparkSeed(0.25f, 0.90f, 0.48f, 2.7f, 3.6f, 2),
    SparkSeed(0.34f, 0.65f, 0.63f, 0.8f, 2.0f, 0),
    SparkSeed(0.44f, 0.88f, 0.80f, 3.5f, 2.8f, 1),
    SparkSeed(0.56f, 0.82f, 0.52f, 1.9f, 3.2f, 0),
    SparkSeed(0.66f, 0.76f, 0.68f, 4.1f, 2.4f, 2),
    SparkSeed(0.74f, 0.92f, 0.59f, 2.2f, 3.4f, 1),
    SparkSeed(0.83f, 0.70f, 0.75f, 0.5f, 2.5f, 0),
    SparkSeed(0.90f, 0.86f, 0.46f, 3.1f, 3.0f, 2),
    SparkSeed(0.14f, 0.50f, 0.44f, 4.7f, 2.1f, 1),
    SparkSeed(0.86f, 0.48f, 0.50f, 5.2f, 2.3f, 0)
)

@Composable
fun CareerLauncherScreen(
    savedCareers: List<CareerSummary>,
    welcomeRewards: WelcomeRewardsState,
    onLoadCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    onCreateCareer: (ClubTemplate, String) -> Unit,
    onWatchAdReward: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destination by remember { mutableStateOf(WelcomeDestination.HUB) }
    var showAdDialog by remember { mutableStateOf(false) }

    if (destination == WelcomeDestination.NEW_CAREER) {
        BackHandler { destination = WelcomeDestination.HUB }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            fadeIn(tween(260)) togetherWith fadeOut(tween(180))
        },
        label = "welcomeDestination",
        modifier = modifier.fillMaxSize()
    ) { target ->
        when (target) {
            WelcomeDestination.HUB -> WelcomePlayerHub(
                savedCareers = savedCareers,
                welcomeRewards = welcomeRewards,
                onResumeCareer = onLoadCareer,
                onDeleteCareer = onDeleteCareer,
                onStartNewGameClick = { destination = WelcomeDestination.NEW_CAREER },
                onWatchAdClick = { showAdDialog = true }
            )
            WelcomeDestination.NEW_CAREER -> NewFranchiseCreatorScreen(
                reserveCurrency = welcomeRewards.reserveCurrency,
                onBack = { destination = WelcomeDestination.HUB },
                onCreateCareer = onCreateCareer
            )
        }
    }

    if (showAdDialog) {
        SponsorAdPlayerDialog(
            adsWatchedToday = welcomeRewards.adsWatchedToday,
            maxAdsPerDay = welcomeRewards.maxAdsPerDay,
            rewardPerAd = welcomeRewards.rewardPerAd,
            onClaimReward = {
                showAdDialog = false
                onWatchAdReward()
            },
            onDismiss = { showAdDialog = false }
        )
    }
}

@Composable
private fun WelcomePlayerHub(
    savedCareers: List<CareerSummary>,
    welcomeRewards: WelcomeRewardsState,
    onResumeCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    onStartNewGameClick: () -> Unit,
    onWatchAdClick: () -> Unit
) {
    val latestSave = savedCareers.firstOrNull()
    var showAllSaves by remember { mutableStateOf(false) }
    var showTutorialPopup by remember { mutableStateOf(false) }

    // Staggered entrance choreography right after user enters the landing page
    var heroVisible by remember { mutableStateOf(false) }
    var actionsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        heroVisible = true
        delay(120L)
        actionsVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("welcome_hub_screen")
    ) {
        // Full-bleed Floodlit Stadium Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = "Floodlit T20 Cricket Stadium",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.32f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF06110E).copy(alpha = 0.80f),
                            Color(0xFF0A1B16).copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pinned Top Corner Bar: Small "i" Tutorial Button on Top-Left + Compact Reserve Pill on Top-Right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopLeftInfoButton(
                    pulseHighlight = true,
                    onClick = { showTutorialPopup = true }
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, FloodlightGold80.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = welcomeRewards.canWatchAd, onClick = onWatchAdClick)
                        .testTag("welcome_currency_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = "Club Reserve Bonus",
                            tint = FloodlightGold80,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = formatCompactCurrency(welcomeRewards.reserveCurrency),
                            style = TelemetryTextStyles.statSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = FloodlightGold80
                            )
                        )
                        if (welcomeRewards.canWatchAd) {
                            Text(
                                text = "• +Ad",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isLandscape = maxWidth > maxHeight && maxWidth >= 600.dp

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = heroVisible,
                            enter = fadeIn(tween(420)) + scaleIn(
                                initialScale = 0.90f,
                                animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            JazzyLandingArenaStage(
                                latestSave = latestSave,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("welcome_jazzy_stage")
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedVisibility(
                                visible = actionsVisible,
                                enter = fadeIn(tween(380)) + slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StartNewGameOptionCard(
                                        reserveCurrency = welcomeRewards.reserveCurrency,
                                        onClick = onStartNewGameClick
                                    )

                                    ResumeSavedGameOptionCard(
                                        latestSave = latestSave,
                                        savedCareers = savedCareers,
                                        showAllSaves = showAllSaves,
                                        onToggleAllSaves = { showAllSaves = !showAllSaves },
                                        onResumeCareer = onResumeCareer,
                                        onDeleteCareer = onDeleteCareer
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Jazzy Animated Cricket Landing Stage
                            AnimatedVisibility(
                                visible = heroVisible,
                                enter = fadeIn(tween(420)) + scaleIn(
                                    initialScale = 0.90f,
                                    animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow)
                                )
                            ) {
                                JazzyLandingArenaStage(
                                    latestSave = latestSave,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("welcome_jazzy_stage")
                                )
                            }

                            // Simple, Bold Options: Start New Game & Resume Saved Game
                            AnimatedVisibility(
                                visible = actionsVisible,
                                enter = fadeIn(tween(380)) + slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    StartNewGameOptionCard(
                                        reserveCurrency = welcomeRewards.reserveCurrency,
                                        onClick = onStartNewGameClick
                                    )

                                    ResumeSavedGameOptionCard(
                                        latestSave = latestSave,
                                        savedCareers = savedCareers,
                                        showAllSaves = showAllSaves,
                                        onToggleAllSaves = { showAllSaves = !showAllSaves },
                                        onResumeCareer = onResumeCareer,
                                        onDeleteCareer = onDeleteCareer
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showTutorialPopup) {
        ScreenGuideDialog(
            initialScreenId = ScreenGuideId.LANDING,
            onDismiss = { showTutorialPopup = false }
        )
    }
}

@Composable
private fun JazzyLandingArenaStage(
    latestSave: CareerSummary?,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "landingJazzyLoop")

    val sweepAngle by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightSweep"
    )

    val orbitSpin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitSpin"
    )

    val ballCycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballCycle"
    )

    val shimmerShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crestPulse"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = DarkSurfaceContainer.copy(alpha = 0.86f),
        border = BorderStroke(
            width = 1.4.dp,
            brush = Brush.linearGradient(
                listOf(
                    TurfEmerald80.copy(alpha = 0.65f),
                    FloodlightGold80.copy(alpha = 0.55f + shimmerShift * 0.30f),
                    TelemetryCyan80.copy(alpha = 0.60f)
                )
            )
        ),
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(276.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h * 0.42f

                // 1. Dynamic volumetric floodlight beams crossing the arena
                val beamShift = sweepAngle * w * 0.16f
                drawPath(
                    path = Path().apply {
                        moveTo(w * 0.14f, 0f)
                        lineTo(cx - w * 0.18f + beamShift, h)
                        lineTo(cx + w * 0.14f + beamShift, h)
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(
                            TurfEmerald80.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
                drawPath(
                    path = Path().apply {
                        moveTo(w * 0.86f, 0f)
                        lineTo(cx - w * 0.14f - beamShift, h)
                        lineTo(cx + w * 0.18f - beamShift, h)
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(
                            FloodlightGold80.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )

                // 2. Floating stadium ember / pyro particles
                val sparkPalette = listOf(FloodlightGold80, TurfEmerald80, TelemetryCyan80)
                STADIUM_SPARK_SEEDS.forEach { seed ->
                    val cycle = (ballCycle * seed.speed + seed.phaseOffset) % 1f
                    val px = w * seed.xFraction + sin((cycle * 2 * PI + seed.phaseOffset).toDouble()).toFloat() * 10.dp.toPx()
                    val py = h * seed.baseYFraction - cycle * h * 0.65f
                    val alpha = sin(cycle * PI).toFloat().coerceIn(0f, 1f) * 0.75f
                    if (py in 0f..h) {
                        drawCircle(
                            color = sparkPalette[seed.colorIndex].copy(alpha = alpha),
                            radius = seed.radiusDp.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // 3. Outer & inner rotating telemetry pitch rings
                val outerR = 72.dp.toPx()
                val innerR = 54.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            TurfEmerald80.copy(alpha = 0.22f + shimmerShift * 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = outerR * 1.35f
                    ),
                    radius = outerR * 1.35f,
                    center = Offset(cx, cy)
                )
                rotate(degrees = orbitSpin, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = TurfEmerald80.copy(alpha = 0.55f),
                        radius = outerR,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
                        )
                    )
                }
                rotate(degrees = -orbitSpin * 1.4f, pivot = Offset(cx, cy)) {
                    drawCircle(
                        color = FloodlightGold80.copy(alpha = 0.45f),
                        radius = innerR,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.6.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                // 4. Central Animated Cricket Stumps, Sweeping Bat & Orbiting Boundary Six Ball
                val stumpBaseY = cy + 18.dp.toPx()
                val stumpH = 30.dp.toPx()
                val stumpW = 3.2.dp.toPx()
                val stumpGap = 7.dp.toPx()
                listOf(-stumpGap, 0f, stumpGap).forEach { dx ->
                    drawRoundRect(
                        color = FloodlightGold80,
                        topLeft = Offset(cx + dx - stumpW / 2f, stumpBaseY - stumpH),
                        size = Size(stumpW, stumpH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                drawLine(
                    color = Color(0xFFFFE59E),
                    start = Offset(cx - stumpGap - 2.dp.toPx(), stumpBaseY - stumpH - 2.dp.toPx()),
                    end = Offset(cx + stumpGap + 2.dp.toPx(), stumpBaseY - stumpH - 2.dp.toPx()),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val swingDeg = -28f + sin(ballCycle * 2 * PI).toFloat() * 34f
                val batPivot = Offset(cx + 18.dp.toPx(), cy + 6.dp.toPx())
                rotate(degrees = swingDeg, pivot = batPivot) {
                    drawRoundRect(
                        color = Color(0xFFF3DBAE),
                        topLeft = Offset(batPivot.x - 4.5.dp.toPx(), batPivot.y - 34.dp.toPx()),
                        size = Size(9.dp.toPx(), 28.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                    drawRoundRect(
                        color = TurfEmerald80,
                        topLeft = Offset(batPivot.x - 2.dp.toPx(), batPivot.y - 6.dp.toPx()),
                        size = Size(4.dp.toPx(), 13.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Glowing leather cricket ball orbiting along a tilted stadium trajectory
                val rad = ballCycle * 2.0 * PI
                val bx = cx + (cos(rad) * outerR * 0.96f).toFloat()
                val by = cy + (sin(rad) * innerR * 0.56f).toFloat()
                for (tIdx in 1..4) {
                    val prevRad = (ballCycle - tIdx * 0.035f) * 2.0 * PI
                    val tx = cx + (cos(prevRad) * outerR * 0.96f).toFloat()
                    val ty = cy + (sin(prevRad) * innerR * 0.56f).toFloat()
                    drawCircle(
                        color = BoundaryAmber.copy(alpha = (0.34f - tIdx * 0.07f).coerceAtLeast(0f)),
                        radius = (6.5f - tIdx).dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF666B), Color(0xFFB71C24), Color(0xFF6B0B10)),
                        center = Offset(bx - 2.dp.toPx(), by - 2.dp.toPx()),
                        radius = 12.dp.toPx()
                    ),
                    radius = 8.dp.toPx(),
                    center = Offset(bx, by)
                )
            }

            // Bottom Brand & Dynamic Welcome Lockup
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TurfEmerald80.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, TurfEmerald80.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "T20 CRICKET FRANCHISE SIMULATOR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TurfEmerald80,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "PitchCraft T20",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = DarkOnSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (latestSave != null) {
                        "Active Franchise: ${latestSave.clubName} • Season ${latestSave.season}, Week ${latestSave.week}"
                    } else {
                        "Build your club, command live overs & chase the championship."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StartNewGameOptionCard(
    reserveCurrency: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("welcome_new_game_button"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
        border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsCricket,
                    contentDescription = "Start New Game",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Start New Game",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append("Pick a nation, league tier & club to launch a new career")
                        if (reserveCurrency > 0) {
                            append(" (+${formatCompactCurrency(reserveCurrency)} bonus)")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ResumeSavedGameOptionCard(
    latestSave: CareerSummary?,
    savedCareers: List<CareerSummary>,
    showAllSaves: Boolean,
    onToggleAllSaves: () -> Unit,
    onResumeCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSave = latestSave != null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("welcome_resume_card"),
        shape = MaterialTheme.shapes.large,
        color = if (hasSave) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.75f)
        },
        border = BorderStroke(
            width = 1.2.dp,
            color = if (hasSave) {
                FloodlightGold80.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = if (hasSave) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (latestSave != null) {
                    ClubBadge(latestSave.clubShortCode, latestSave.divisionTier, sizeDp = 48)
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Resume Saved Game",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (hasSave) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (latestSave != null) {
                            "${latestSave.clubName} • ${latestSave.managerName} • S${latestSave.season} W${latestSave.week}"
                        } else {
                            "No saved career yet — start a new game above."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                onClick = { latestSave?.let { onResumeCareer(it.careerId) } },
                enabled = hasSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FloodlightGold80,
                    contentColor = OnFloodlightGold20
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("welcome_resume_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (latestSave != null) {
                        "Resume Saved Game (${latestSave.clubShortCode})"
                    } else {
                        "No Saved Game Available"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (savedCareers.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onToggleAllSaves,
                        modifier = Modifier.testTag("toggle_all_saves_button")
                    ) {
                        Text(if (showAllSaves) "Hide Other Saves" else "Switch Save (${savedCareers.size})")
                    }
                }
            }

            AnimatedVisibility(visible = showAllSaves && savedCareers.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    savedCareers.forEach { save ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onResumeCareer(save.careerId) }
                                .testTag("resume_career_${save.careerId}"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ClubBadge(save.clubShortCode, save.divisionTier, sizeDp = 36)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(save.clubName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${save.managerName} • S${save.season} W${save.week} • ${formatCompactCurrency(save.bankBalance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCareer(save.careerId) },
                                    modifier = Modifier.testTag("delete_career_${save.careerId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete career",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsorAdPlayerDialog(
    adsWatchedToday: Int,
    maxAdsPerDay: Int,
    rewardPerAd: Int,
    onClaimReward: () -> Unit,
    onDismiss: () -> Unit
) {
    val sponsors = remember {
        listOf(
            "Apex Aero • Official Kit Flight Partner",
            "Veloce Sports • Carbon Willow Blades",
            "Crown Energy • Matchday Hydration Fuel",
            "TurfTech Pro • Hybrid Wicket Systems",
            "Beacon Fin • T20 Franchise Banking"
        )
    }
    val sponsorTitle = sponsors[adsWatchedToday % sponsors.size]
    val totalSeconds = 4
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    val progress by animateFloatAsState(
        targetValue = (totalSeconds - secondsLeft).toFloat() / totalSeconds.toFloat(),
        label = "adCountdownProgress"
    )

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = FloodlightGold80
                )
                Text("Sponsor Broadcast")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, FloodlightGold80.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SIMULATED SPONSOR AD • ${adsWatchedToday + 1} OF $maxAdsPerDay",
                            style = MaterialTheme.typography.labelSmall,
                            color = FloodlightGold80
                        )
                        Text(
                            text = sponsorTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(72.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = FloodlightGold80,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = if (secondsLeft > 0) "${secondsLeft}s" else "DONE",
                                style = TelemetryTextStyles.statValue,
                                color = if (secondsLeft == 0) PitchMint else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (secondsLeft > 0) {
                                "Watching sponsor spotlight to unlock +${formatCompactCurrency(rewardPerAd)}..."
                            } else {
                                "Reward unlocked! Claim +${formatCompactCurrency(rewardPerAd)} to your Reserve."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClaimReward,
                enabled = secondsLeft == 0,
                modifier = Modifier.testTag("claim_ad_reward_button")
            ) {
                Text("Claim +${formatCompactCurrency(rewardPerAd)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFranchiseCreatorScreen(
    reserveCurrency: Int,
    onBack: () -> Unit,
    onCreateCareer: (ClubTemplate, String) -> Unit
) {
    var selectedNation by remember { mutableStateOf(CricketNation.INDIA) }
    var selectedTier by remember(selectedNation) {
        mutableIntStateOf(if (selectedNation.supportsPromotionRelegation) 2 else 1)
    }
    var managerName by remember { mutableStateOf("Alex Mercer") }
    var selectedClub by remember(selectedNation, selectedTier) {
        mutableStateOf(WorldGenerator.clubsFor(selectedNation, selectedTier).first())
    }
    var showScreenGuide by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start New Franchise", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TopLeftInfoButton(onClick = { showScreenGuide = true })
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("new_career_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Welcome Screen"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (reserveCurrency > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FloodlightGold80.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, FloodlightGold80.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MonetizationOn,
                                        contentDescription = null,
                                        tint = FloodlightGold80
                                    )
                                    Text(
                                        text = "${formatCompactCurrency(reserveCurrency)} Bonus Reserve will be added to your starting club budget!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Manager Name") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manager_name_input")
                        )

                        Text(
                            text = "Select Domestic League Nation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CricketNation.entries) { nation ->
                                FilterChip(
                                    selected = selectedNation == nation,
                                    onClick = { selectedNation = nation },
                                    label = { Text("${nation.label} (${nation.shortCode})") },
                                    modifier = Modifier.testTag("country_select_${nation.name.lowercase()}")
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedNation.leagueBrand,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedNation.summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedNation.supportsPromotionRelegation) {
                            Text(
                                text = "Select Domestic Division",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                listOf(
                                    1 to "Tier 1 • Major Cities",
                                    2 to "Tier 2 • Minor Cities"
                                ).forEachIndexed { idx, (tier, label) ->
                                    SegmentedButton(
                                        selected = selectedTier == tier,
                                        onClick = { selectedTier = tier },
                                        shape = SegmentedButtonDefaults.itemShape(idx, 2),
                                        modifier = Modifier.testTag("tier_select_$tier")
                                    ) {
                                        Text(label, maxLines = 1)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PitchMint.copy(alpha = 0.14f),
                                modifier = Modifier.testTag("single_league_badge")
                            ) {
                                Text(
                                    text = "Single Tier National League • 8 Domestic Clubs",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PitchMint,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                val clubs = WorldGenerator.clubsFor(selectedNation, selectedTier)
                items(clubs, key = { it.clubId }) { club ->
                    val isPicked = selectedClub.clubId == club.clubId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { selectedClub = club }
                            .testTag("club_pick_${club.clubId}"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isPicked) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = if (isPicked) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClubBadge(club.shortCode, club.tier)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(club.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${club.cityTag} • ${club.stadiumCapacity / 1000}k Arena • OVR ${club.baseStrength}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCompactCurrency(club.startingBudget + reserveCurrency),
                                    style = TelemetryTextStyles.statValue.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = if (reserveCurrency > 0) "Incl. Bonus" else "Budget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onCreateCareer(selectedClub, managerName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_career_button")
                    ) {
                        Icon(Icons.Filled.SportsCricket, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Take Charge of ${selectedClub.name}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    if (showScreenGuide) {
        ScreenGuideDialog(
            initialScreenId = ScreenGuideId.NEW_CAREER,
            onDismiss = { showScreenGuide = false }
        )
    }
}
