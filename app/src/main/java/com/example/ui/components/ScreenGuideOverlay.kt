package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

enum class ScreenGuideId(val shortLabel: String) {
    LANDING("Welcome Hub"),
    NEW_CAREER("New Franchise"),
    HQ_DASHBOARD("Club HQ"),
    SQUAD_ROSTER("Squad Roster"),
    SQUAD_DEPTH("Depth Chart"),
    SQUAD_TRAINING("Training & Medical"),
    SQUAD_MEDICAL("Medical Room"),
    SQUAD_ACADEMY("Youth Academy"),
    SQUAD_FORM_STATS("Form & Stats"),
    PLAYER_ACTIVITY("Player Telemetry"),
    MATCHDAY_PREP("Matchday Setup"),
    MATCHDAY_LIVE("Live War Room"),
    MATCHDAY_SCORECARD("Match Scorecard"),
    TRANSFERS("Transfer Market"),
    OFFICE_STAFF("Staff & Delegation"),
    OFFICE_CALENDAR("Season Calendar"),
    OFFICE_FINANCES("Finances & Sponsors"),
    OFFICE_STANDINGS("Standings & Stats")
}

data class ScreenGuideContent(
    val id: ScreenGuideId,
    val title: String,
    val badge: String,
    val summary: String,
    val tips: List<String>,
    val icon: ImageVector,
    val accent: Color
)

private val TUTORIAL_FLOW_IDS = listOf(
    ScreenGuideId.LANDING,
    ScreenGuideId.HQ_DASHBOARD,
    ScreenGuideId.SQUAD_TRAINING,
    ScreenGuideId.MATCHDAY_PREP,
    ScreenGuideId.TRANSFERS,
    ScreenGuideId.OFFICE_FINANCES,
    ScreenGuideId.OFFICE_STANDINGS
)

fun guideContentFor(id: ScreenGuideId): ScreenGuideContent = when (id) {
    ScreenGuideId.LANDING -> ScreenGuideContent(
        id = id,
        title = "Welcome & Career Launch",
        badge = "START HERE",
        summary = "Your franchise journey starts on the landing hub. Pick an active save to continue or launch a new club across five T20 nations.",
        tips = listOf(
            "Start New Game lets you choose a country, division tier, club badge, and manager identity.",
            "Resume Saved Game jumps straight back into your most recent club, week, and league table.",
            "Tap the small \"i\" button in the top-left corner on any screen to view a popup guide without losing your place."
        ),
        icon = Icons.Filled.SportsCricket,
        accent = TurfEmerald80
    )
    ScreenGuideId.NEW_CAREER -> ScreenGuideContent(
        id = id,
        title = "Franchise Setup & Tiers",
        badge = "CAREER SETUP",
        summary = "Select your manager name, nation, and domestic competition structure before taking charge on Week 1.",
        tips = listOf(
            "India & England feature a 2-tier pyramid (Major Cities Tier 1 and Minor Cities Tier 2) with promotion & relegation.",
            "South Africa, Australia & Caribbean run a 8-club single premier league.",
            "Compare each club's arena capacity, squad overall rating, and starting budget before tapping Take Charge."
        ),
        icon = Icons.Filled.Public,
        accent = TelemetryCyan80
    )
    ScreenGuideId.HQ_DASHBOARD -> ScreenGuideContent(
        id = id,
        title = "Club HQ & Weekly Loop",
        badge = "COMMAND HUB",
        summary = "Monitor club bank balance, net weekly cashflow, matchday readiness, and jump directly into key club operations.",
        tips = listOf(
            "Check the top KPIs for Cash, Net Weekly Cash, Current Rank, and Next Opponent before advancing.",
            "Use Quick Shortcuts to jump directly to Matchday, Medical Room, Calendar, Standings, or Finances.",
            "Advance Week / Play Matchday progresses training, recovery, finances, and league fixtures."
        ),
        icon = Icons.Filled.Home,
        accent = TurfEmerald80
    )
    ScreenGuideId.SQUAD_ROSTER -> ScreenGuideContent(
        id = id,
        title = "Squad Roster & Player Cards",
        badge = "SQUAD",
        summary = "Inspect every contracted cricketer, filter by T20 role, and open individual telemetry screens.",
        tips = listOf(
            "Tap any player row to open their full Player Telemetry page for training, rehab, renewal, or transfer listing.",
            "Watch fitness bars and injury badges closely before picking your matchday Playing XI.",
            "Switch sub-tabs above to review Role Depth, Training, Medical Room, and Youth Academy."
        ),
        icon = Icons.Filled.Groups,
        accent = TurfEmerald80
    )
    ScreenGuideId.SQUAD_DEPTH -> ScreenGuideContent(
        id = id,
        title = "Role Depth Chart",
        badge = "SQUAD DEPTH",
        summary = "Audit your squad balance across Openers, Anchors, Finishers, Spinners, and Pace Spearheads.",
        tips = listOf(
            "Verify backup options in every specialist T20 role before transfer windows close.",
            "Spot thin positions early so your Chief Scout and transfer budget target the right replacements."
        ),
        icon = Icons.Filled.ViewList,
        accent = TelemetryCyan80
    )
    ScreenGuideId.SQUAD_TRAINING -> ScreenGuideContent(
        id = id,
        title = "Training Intensity & Focus",
        badge = "PROGRESSION",
        summary = "Balance weekly skill growth against fatigue and injury probability across your active roster.",
        tips = listOf(
            "Higher training intensity speeds up attribute progression but elevates weekly fatigue and injury risk.",
            "Assign individual player focuses (Batting, Bowling, Fielding, Recovery) to shape their role ratings.",
            "Pair with specialized Coaches in the Staff Office for extra weekly progression multipliers."
        ),
        icon = Icons.Filled.FitnessCenter,
        accent = BoundaryAmber
    )
    ScreenGuideId.SQUAD_MEDICAL -> ScreenGuideContent(
        id = id,
        title = "Medical Room & Recovery",
        badge = "MEDICAL",
        summary = "Track injured players, estimated recovery weeks, workload strain, and active rehabilitation protocols.",
        tips = listOf(
            "Switch injured or fatigued players to Light or Intensive Rehab to speed recovery back to full fitness.",
            "High match workload + heavy training increases injury probability—rotate tired pacers before big games.",
            "Upgrading Sports Science & Medical budget in the Office improves weekly recovery across the squad."
        ),
        icon = Icons.Filled.MedicalServices,
        accent = AlertCrimson
    )
    ScreenGuideId.SQUAD_ACADEMY -> ScreenGuideContent(
        id = id,
        title = "Youth Academy Pipeline",
        badge = "ACADEMY",
        summary = "Develop emerging homegrown prospects and promote ready talents directly into your senior T20 roster.",
        tips = listOf(
            "Compare Current Overall vs Potential ceiling to spot future franchise stars.",
            "Tap Promote to move an academy prospect onto a senior contract with low weekly wages.",
            "Higher Youth Academy budget investment produces stronger annual youth intakes."
        ),
        icon = Icons.Filled.School,
        accent = PitchMint
    )
    ScreenGuideId.SQUAD_FORM_STATS -> ScreenGuideContent(
        id = id,
        title = "Squad Form Chart & Playing Stats",
        badge = "ONGOING SEASON",
        summary = "Monitor live squad form trends alongside a full individual stats table covering runs scored, fatigue, and overs bowled.",
        tips = listOf(
            "Use the Form Chart to spot hot hitters and out-of-form players before locking your Playing XI.",
            "Sort the Individual Stats Table by Form, Runs, Fatigue, or Overs to rotate tired bowlers into Training & Medical."
        ),
        icon = Icons.Filled.Leaderboard,
        accent = TelemetryCyan80
    )
    ScreenGuideId.PLAYER_ACTIVITY -> ScreenGuideContent(
        id = id,
        title = "Player Telemetry & Actions",
        badge = "PLAYER PROFILE",
        summary = "Inspect one player's technical ratings, season form, staff advice, contract, and weekly assignments.",
        tips = listOf(
            "Review Staff Advisor recommendations to add high-form players to your XI or rest overworked bowlers.",
            "Adjust individual Training Focus or Rehab mode directly from this screen.",
            "Use Contract Renewal or Transfer Listing to manage long-term wage commitments."
        ),
        icon = Icons.Filled.PersonSearch,
        accent = TelemetryCyan80
    )
    ScreenGuideId.MATCHDAY_PREP -> ScreenGuideContent(
        id = id,
        title = "Matchday Tactical Setup",
        badge = "MATCHDAY",
        summary = "Select your Playing XI, deliver your Pre-Match Team Talk, and set tactical plans before stepping onto the pitch.",
        tips = listOf(
            "Deliver a Pre-Match Team Talk (Aggressive, Calm, Encouraging, or Assertive) to subtly boost batting punch, composure, or bowling threat.",
            "Check 11 players for your Playing XI (switch between Tile and Table views) and star 1 Impact Sub.",
            "Set Batting Tempo, Bowling Plan, and Field Spread tailored to the pitch, then launch Live Feed or Quick Sim."
        ),
        icon = Icons.Filled.SportsCricket,
        accent = TurfEmerald80
    )
    ScreenGuideId.MATCHDAY_LIVE -> ScreenGuideContent(
        id = id,
        title = "Live Ball-by-Ball War Room",
        badge = "LIVE MATCH",
        summary = "Command every delivery in real time using the CRR vs RRR graph, ball feed updates, Key Moments, and Innings Break Team Talk.",
        tips = listOf(
            "Step +1 Ball, +1 Over, Auto-Stream, or jump to Key Moments—including the Innings Break Huddle to shift Team Talk tone.",
            "Change Batting Tempo, Bowling Plan, Field Spread, or trigger your Impact Sub during live overs.",
            "Watch the real-time Current Run Rate vs Required Run Rate graph to time your chases and bowling plans."
        ),
        icon = Icons.Filled.PlayCircle,
        accent = PitchMint
    )
    ScreenGuideId.MATCHDAY_SCORECARD -> ScreenGuideContent(
        id = id,
        title = "Post-Match Scorecard & Debrief",
        badge = "RESULT",
        summary = "Review innings scorecards, sponsor payouts, and deliver a Post-Match Team Talk to shape squad morale and form.",
        tips = listOf(
            "Choose a Post-Match Team Talk tone (Encouraging, Calm, Aggressive, Assertive) to lift dressing-room morale and form for next week.",
            "Check Batting 50s/100s and Bowling 3W hauls—active performance sponsors pay bonuses automatically.",
            "Tap Commit Result & Advance Week after reviewing stats to log the result into the league table."
        ),
        icon = Icons.Filled.Scoreboard,
        accent = FloodlightGold80
    )
    ScreenGuideId.TRANSFERS -> ScreenGuideContent(
        id = id,
        title = "Transfer Market & Contract Bids",
        badge = "TRANSFERS",
        summary = "Scout free agents and rival stars, shortlist targets, and negotiate transfer fee + wage contracts.",
        tips = listOf(
            "The Transfers tab unlocks strictly during the 1-week Pre-Season and Mid-Season transfer windows.",
            "Filter by role or Shortlist, compare base value vs OVR/potential, and tap Bid to negotiate.",
            "Balance transfer fee, weekly wage, and signing bonus—excessive lowballs drain player patience."
        ),
        icon = Icons.Filled.SwapHoriz,
        accent = BoundaryAmber
    )
    ScreenGuideId.OFFICE_STAFF -> ScreenGuideContent(
        id = id,
        title = "Staff Office & Delegation",
        badge = "STAFF",
        summary = "Hire specialized coaches, assign training focus areas, and delegate routine tasks to your backroom team.",
        tips = listOf(
            "Assign Batting, Bowling, and Fitness coaches to amplify weekly progression in those specific areas.",
            "Upgrade staff quality when your weekly revenue allows—better scouts and physios unlock hidden gains.",
            "Toggle Delegation switches if you want assistants to auto-handle selection, training, or transfers."
        ),
        icon = Icons.Filled.Badge,
        accent = TurfEmerald80
    )
    ScreenGuideId.OFFICE_CALENDAR -> ScreenGuideContent(
        id = id,
        title = "Season Calendar & Windows",
        badge = "SCHEDULE",
        summary = "Visual timeline of upcoming league fixtures, 1-week transfer windows, training camps, and rivalry clashes.",
        tips = listOf(
            "Spot Pre-Season and Mid-Season Transfer Window markers early so you reserve cash before they open.",
            "Watch for Training Camp boosts and Rival Derby weeks to peak your squad's fitness."
        ),
        icon = Icons.Filled.CalendarMonth,
        accent = TelemetryCyan80
    )
    ScreenGuideId.OFFICE_FINANCES -> ScreenGuideContent(
        id = id,
        title = "Finances & Sponsorships",
        badge = "COMMERCIAL",
        summary = "Manage ticket pricing, club investments, weekly ledger cashflow, and Pre-Season sponsorship contracts.",
        tips = listOf(
            "Sponsorship selection and negotiation are open ONLY in Week 1, then freeze for the rest of the season.",
            "Compare Guaranteed Anchor deals vs Performance Escalators (per-win, 50s, 100s, 3W hauls) & Star Branding.",
            "Your 4 Leverage Pillars (City Demography, Buying Pattern, Tournament History, Player Calibre) raise max counter-offer uplift."
        ),
        icon = Icons.Filled.AccountBalance,
        accent = FloodlightGold80
    )
    ScreenGuideId.OFFICE_STANDINGS -> ScreenGuideContent(
        id = id,
        title = "League Pyramid & Leaderboards",
        badge = "STANDINGS & STATS",
        summary = "Track live points, Net Run Rate, Promotion/Relegation zones, and individual tournament batting & bowling leaders.",
        tips = listOf(
            "In 2-tier nations (India & England), toggle between Tier 1 & Tier 2 to follow Promotion (PROMO) and Relegation (REL) spots.",
            "Switch the leaderboard between Batting (Runs, SR, 50s, 30s) and Bowling (Wickets, Dots, Maidens, 3W hauls).",
            "Sort columns by tapping any stat header pill to scout top performers across the league."
        ),
        icon = Icons.Filled.EmojiEvents,
        accent = FloodlightGold80
    )
}

@Composable
fun TopLeftInfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pulseHighlight: Boolean = false
) {
    val pulseScale = if (pulseHighlight) {
        val transition = rememberInfiniteTransition(label = "infoButtonPulse")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "infoPulseScale"
        )
        value
    } else {
        1f
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("screen_info_button")
    ) {
        Surface(
            modifier = Modifier
                .size(32.dp)
                .scale(pulseScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            border = BorderStroke(1.2.dp, FloodlightGold80.copy(alpha = 0.7f)),
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Screen tutorial and guide",
                    tint = FloodlightGold80,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun InGameTopContextBar(
    guideId: ScreenGuideId,
    clubShortCode: String,
    divisionTier: Int,
    season: Int,
    week: Int,
    bankBalance: Int,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPreSeason: Boolean? = null,
    clubName: String = "",
    onSaveClick: (() -> Unit)? = null,
    onExitToLauncher: (() -> Unit)? = null
) {
    val guide = remember(guideId) { guideContentFor(guideId) }
    val showOngoingRibbonControls = isPreSeason == false

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        ) {
            val isWide = maxWidth >= 600.dp

            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 12.dp, top = 2.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopLeftInfoButton(onClick = onInfoClick)

                    if (showOngoingRibbonControls && clubName.isNotBlank()) {
                        Text(
                            text = clubName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("top_ribbon_team_name")
                        )
                        Text(
                            text = "• ${guide.id.shortLabel} • T$divisionTier • S$season W$week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = guide.id.shortLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "• $clubShortCode • Tier $divisionTier • S$season W$week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isPreSeason != null) {
                        PhaseRibbonBadge(isPreSeason = isPreSeason)
                    }

                    BankRibbonChip(bankBalance = bankBalance)

                    if (showOngoingRibbonControls && onSaveClick != null) {
                        TopRibbonSaveButton(onClick = onSaveClick)
                    }

                    if (showOngoingRibbonControls && onExitToLauncher != null) {
                        TopRibbonSwitchButton(onClick = onExitToLauncher)
                    }
                }
            } else {
                // Vertical / Portrait compact 2-tier ribbon so Team Name, Save & Stats never clip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TopLeftInfoButton(onClick = onInfoClick)

                        if (showOngoingRibbonControls && clubName.isNotBlank()) {
                            Text(
                                text = clubName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("top_ribbon_team_name")
                            )
                        } else {
                            Text(
                                text = guide.id.shortLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        BankRibbonChip(bankBalance = bankBalance)

                        if (showOngoingRibbonControls && onSaveClick != null) {
                            TopRibbonSaveButton(onClick = onSaveClick, compact = true)
                        }

                        if (showOngoingRibbonControls && onExitToLauncher != null) {
                            TopRibbonSwitchButton(onClick = onExitToLauncher)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showOngoingRibbonControls) {
                                "${guide.id.shortLabel} • T$divisionTier • S$season W$week"
                            } else {
                                "$clubShortCode • Tier $divisionTier • S$season W$week"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isPreSeason != null) {
                            PhaseRibbonBadge(isPreSeason = isPreSeason)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseRibbonBadge(isPreSeason: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPreSeason) BoundaryAmber.copy(alpha = 0.18f) else PitchMint.copy(alpha = 0.16f),
        border = BorderStroke(
            1.dp,
            if (isPreSeason) BoundaryAmber.copy(alpha = 0.45f) else PitchMint.copy(alpha = 0.45f)
        )
    ) {
        Text(
            text = if (isPreSeason) "PRE-SEASON" else "SEASON ONGOING",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isPreSeason) BoundaryAmber else PitchMint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun BankRibbonChip(bankBalance: Int) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Text(
            text = formatCompactCurrency(bankBalance),
            style = TelemetryTextStyles.statSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TopRibbonSaveButton(
    onClick: () -> Unit,
    compact: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 12.dp, vertical = 4.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("top_ribbon_save_button")
    ) {
        Icon(
            imageVector = Icons.Filled.Save,
            contentDescription = "Save Game",
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Save",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun TopRibbonSwitchButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("exit_save_button")
    ) {
        Icon(
            imageVector = Icons.Filled.FolderShared,
            contentDescription = "Switch Career Save",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ScreenGuideDialog(
    initialScreenId: ScreenGuideId,
    onDismiss: () -> Unit
) {
    var tutorialTourActive by remember(initialScreenId) {
        mutableStateOf(initialScreenId == ScreenGuideId.LANDING)
    }
    var tourIndex by remember(initialScreenId) { mutableIntStateOf(0) }

    val activeGuide = if (tutorialTourActive) {
        guideContentFor(TUTORIAL_FLOW_IDS[tourIndex])
    } else {
        guideContentFor(initialScreenId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .widthIn(max = 500.dp)
                .testTag("screen_guide_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            border = BorderStroke(1.2.dp, activeGuide.accent.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = activeGuide.accent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (tutorialTourActive) {
                                "GAME TUTORIAL • ${tourIndex + 1} / ${TUTORIAL_FLOW_IDS.size}"
                            } else {
                                "SCREEN GUIDE • ${activeGuide.badge}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = activeGuide.accent,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (initialScreenId != ScreenGuideId.LANDING) {
                            TextButton(
                                onClick = {
                                    tutorialTourActive = !tutorialTourActive
                                    tourIndex = 0
                                },
                                modifier = Modifier.testTag("toggle_tutorial_tour_button")
                            ) {
                                Text(
                                    text = if (tutorialTourActive) "This Screen" else "Full Tour",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("tutorial_close_icon_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close guide popup")
                        }
                    }
                }

                // Animated Guide Slide Content (scrolls independently when viewport is compact)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = activeGuide,
                        transitionSpec = {
                            (fadeIn(tween(200)) + slideInHorizontally { it / 10 }) togetherWith
                                (fadeOut(tween(150)) + slideOutHorizontally { -it / 10 })
                        },
                        label = "screenGuideSlide"
                    ) { guide ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = guide.accent.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, guide.accent.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = guide.icon,
                                    contentDescription = null,
                                    tint = guide.accent,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = guide.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = guide.id.shortLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = guide.accent
                                )
                            }
                        }

                        Text(
                            text = guide.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "How It Operates",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                guide.tips.forEach { tip ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = guide.accent,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(15.dp)
                                        )
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Step Indicator Pips in Tutorial Mode
                if (tutorialTourActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TUTORIAL_FLOW_IDS.forEachIndexed { idx, _ ->
                            val selected = idx == tourIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (selected) activeGuide.accent
                                        else MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                            )
                        }
                    }
                }

                // Footer Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tutorialTourActive) {
                        OutlinedButton(
                            onClick = { if (tourIndex > 0) tourIndex-- },
                            enabled = tourIndex > 0,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("tutorial_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Prev")
                        }
                        if (tourIndex < TUTORIAL_FLOW_IDS.lastIndex) {
                            Button(
                                onClick = { tourIndex++ },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("tutorial_next_button")
                            ) {
                                Text("Next Screen")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("tutorial_close_button")
                            ) {
                                Text("Got It")
                            }
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("tutorial_close_button")
                        ) {
                            Text("Got It")
                        }
                    }
                }
            }
        }
    }
}
