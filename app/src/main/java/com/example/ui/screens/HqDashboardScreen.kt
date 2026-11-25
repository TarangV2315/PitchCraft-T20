package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.MainTab
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun HqDashboardScreen(
    career: CareerState,
    onPrepareMatchday: () -> Unit,
    onStartNextSeason: () -> Unit,
    onAdvanceWeek: () -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onOpenFinances: () -> Unit,
    onOpenStaffOffice: () -> Unit,
    onOpenCalendar: () -> Unit = {},
    onOpenMedicalRoom: () -> Unit = {},
    onOpenStandings: () -> Unit = {},
    onExitToLauncher: () -> Unit,
    onStartSeason: () -> Unit = {},
    onSaveGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (career.isPreSeason) {
        PreSeasonHqDashboard(
            career = career,
            onNavigateTab = onNavigateTab,
            onOpenFinances = onOpenFinances,
            onStartSeason = onStartSeason,
            onExitToLauncher = onExitToLauncher,
            onOpenPlayer = onOpenPlayer,
            modifier = modifier
        )
    } else {
        OngoingSeasonHqDashboard(
            career = career,
            onPrepareMatchday = onPrepareMatchday,
            onStartNextSeason = onStartNextSeason,
            onAdvanceWeek = onAdvanceWeek,
            onToggleDelegation = onToggleDelegation,
            onNavigateTab = onNavigateTab,
            onOpenStaffOffice = onOpenStaffOffice,
            onOpenPlayer = onOpenPlayer,
            onSaveGame = onSaveGame,
            onExitToLauncher = onExitToLauncher,
            modifier = modifier
        )
    }
}

@Composable
private fun PreSeasonHqDashboard(
    career: CareerState,
    onNavigateTab: (MainTab) -> Unit,
    onOpenFinances: () -> Unit,
    onStartSeason: () -> Unit,
    onExitToLauncher: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val seniorSquad = remember(career.squad) {
        career.squad.filter { !it.isYouthAcademy }
    }
    val avgFitness = remember(seniorSquad) {
        if (seniorSquad.isEmpty()) 100 else seniorSquad.map { it.fitness }.average().roundToInt()
    }
    val injuredCount = remember(career.squad) {
        career.squad.count { it.injury.isInjured }
    }
    val fitCount = seniorSquad.count { !it.injury.isInjured }
    val netWeekly = career.weeklySponsorIncome - (career.weeklyPlayerWages + career.weeklyStaffWages + career.budgets.weeklyUpkeep)
    val leagueLabel = com.example.simulation.WorldGenerator.divisionTierShortLabel(career.country, career.divisionTier)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("hq_preseason_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Club Hero Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero),
                        contentDescription = "Club stadium",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.24f
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ClubBadge(career.clubShortCode, career.divisionTier, sizeDp = 46)
                            Column {
                                Text(
                                    text = career.clubName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Season ${career.season} Pre-Season • ${career.country.shortCode} $leagueLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BoundaryAmber
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BoundaryAmber.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, BoundaryAmber.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "PRE-SEASON PREP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BoundaryAmber,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            IconButton(
                                onClick = onExitToLauncher,
                                modifier = Modifier.testTag("exit_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderShared,
                                    contentDescription = "Switch Career Save",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Pre-Season Retained Stats Row: Club Cash, Board Confidence, Sponsor Income, Fitness & Injured Stats
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            label = "Club Cash",
                            value = formatCompactCurrency(career.bankBalance),
                            subtitle = "${if (netWeekly >= 0) "+" else ""}${formatCompactCurrency(netWeekly)}/wk net",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accentColor = if (netWeekly >= 0) PitchMint else CautionOchre,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hq_stat_cash")
                                .clickable(onClick = onOpenFinances)
                        )
                        MetricTile(
                            label = "Board Confidence",
                            value = "${career.boardConfidence}%",
                            subtitle = "Rep ${career.managerReputation} • ${career.trophiesWon} Cups",
                            icon = Icons.Filled.VerifiedUser,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hq_stat_board")
                        )
                        MetricTile(
                            label = "Sponsor Income",
                            value = "${formatCompactCurrency(career.weeklySponsorIncome)}/wk",
                            subtitle = "${career.sponsors.count { it.isActive }} of 3 • Open Wk 1",
                            icon = Icons.Filled.Handshake,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hq_stat_sponsor")
                                .clickable(onClick = onOpenFinances)
                        )
                        MetricTile(
                            label = "Squad Fitness & Injured",
                            value = "$avgFitness% Avg • $injuredCount Inj",
                            subtitle = "$fitCount Fit of ${seniorSquad.size} Senior",
                            icon = Icons.Filled.MonitorHeart,
                            accentColor = if (injuredCount > 0) AlertCrimson else PitchMint,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hq_stat_fitness")
                                .clickable { onNavigateTab(MainTab.SQUAD) }
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                label = "Club Cash",
                                value = formatCompactCurrency(career.bankBalance),
                                subtitle = "${if (netWeekly >= 0) "+" else ""}${formatCompactCurrency(netWeekly)}/wk net",
                                icon = Icons.Filled.AccountBalanceWallet,
                                accentColor = if (netWeekly >= 0) PitchMint else CautionOchre,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hq_stat_cash")
                                    .clickable(onClick = onOpenFinances)
                            )
                            MetricTile(
                                label = "Board Confidence",
                                value = "${career.boardConfidence}%",
                                subtitle = "Rep ${career.managerReputation} • ${career.trophiesWon} Cups",
                                icon = Icons.Filled.VerifiedUser,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hq_stat_board")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                label = "Sponsor Income",
                                value = "${formatCompactCurrency(career.weeklySponsorIncome)}/wk",
                                subtitle = "${career.sponsors.count { it.isActive }} of 3 • Open Wk 1",
                                icon = Icons.Filled.Handshake,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hq_stat_sponsor")
                                    .clickable(onClick = onOpenFinances)
                            )
                            MetricTile(
                                label = "Squad Fitness & Injured",
                                value = "$avgFitness% Avg • $injuredCount Inj",
                                subtitle = "$fitCount Fit of ${seniorSquad.size} Senior",
                                icon = Icons.Filled.MonitorHeart,
                                accentColor = if (injuredCount > 0) AlertCrimson else PitchMint,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hq_stat_fitness")
                                    .clickable { onNavigateTab(MainTab.SQUAD) }
                            )
                        }
                    }
                }
            }
        }

        // Pre-Season Direct Navigation Actions: Squad, Transfers, Office & Proceed to Season
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Pre-Season Operations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Finalize your roster, complete 1-week transfers, hire staff & negotiate sponsors before starting the season.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 600.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HqNavActionButton(
                                    label = "Squad",
                                    subtitle = "Roster & Academy",
                                    icon = Icons.Filled.Groups,
                                    onClick = { onNavigateTab(MainTab.SQUAD) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hq_go_squad_btn")
                                )
                                HqNavActionButton(
                                    label = "Transfers",
                                    subtitle = if (career.isTransferWindowOpen) "Window Open" else "Closed",
                                    icon = Icons.Filled.SwapHoriz,
                                    enabled = career.isTransferWindowOpen,
                                    onClick = { onNavigateTab(MainTab.TRANSFERS) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hq_go_transfers_btn")
                                )
                                HqNavActionButton(
                                    label = "Office",
                                    subtitle = "Staff & Sponsors",
                                    icon = Icons.Filled.BusinessCenter,
                                    onClick = { onNavigateTab(MainTab.OFFICE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hq_go_office_btn")
                                )
                                Button(
                                    onClick = onStartSeason,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TurfEmerald80,
                                        contentColor = OnTurfEmerald20
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .height(62.dp)
                                        .testTag("hq_proceed_season_btn")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Proceed to Season",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HqNavActionButton(
                                        label = "Squad",
                                        subtitle = "Roster & Academy",
                                        icon = Icons.Filled.Groups,
                                        onClick = { onNavigateTab(MainTab.SQUAD) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hq_go_squad_btn")
                                    )
                                    HqNavActionButton(
                                        label = "Transfers",
                                        subtitle = if (career.isTransferWindowOpen) "Window Open" else "Closed",
                                        icon = Icons.Filled.SwapHoriz,
                                        enabled = career.isTransferWindowOpen,
                                        onClick = { onNavigateTab(MainTab.TRANSFERS) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hq_go_transfers_btn")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HqNavActionButton(
                                        label = "Office",
                                        subtitle = "Staff & Sponsors",
                                        icon = Icons.Filled.BusinessCenter,
                                        onClick = { onNavigateTab(MainTab.OFFICE) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("hq_go_office_btn")
                                    )
                                    Button(
                                        onClick = onStartSeason,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TurfEmerald80,
                                            contentColor = OnTurfEmerald20
                                        ),
                                        shape = MaterialTheme.shapes.medium,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(62.dp)
                                            .testTag("hq_proceed_season_btn")
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Proceed to Season",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pre-Season Staff Briefing
        if (career.inbox.isNotEmpty()) {
            item {
                Text(
                    text = "Pre-Season Briefing",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(career.inbox.take(3), key = { it.id }) { msg ->
                HqInboxCard(msg = msg, onOpenPlayer = onOpenPlayer)
            }
        }
    }
}

@Composable
private fun OngoingSeasonHqDashboard(
    career: CareerState,
    onPrepareMatchday: () -> Unit,
    onStartNextSeason: () -> Unit,
    onAdvanceWeek: () -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onOpenStaffOffice: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSaveGame: () -> Unit,
    onExitToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nextFixture = career.nextUserFixture

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("hq_ongoing_screen")
    ) {
        val isWide = maxWidth >= 640.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Horizontal Split: Matchday Prep & Live Tactics | Staff Delegation Controls
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.05f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OngoingMatchdayPrepCard(
                                career = career,
                                nextFixture = nextFixture,
                                onPrepareMatchday = onPrepareMatchday,
                                onStartNextSeason = onStartNextSeason,
                                onAdvanceWeek = onAdvanceWeek
                            )
                        }

                        Column(
                            modifier = Modifier.weight(0.95f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OngoingStaffDelegationCard(
                                career = career,
                                onToggleDelegation = onToggleDelegation,
                                onOpenStaffOffice = onOpenStaffOffice
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OngoingMatchdayPrepCard(
                            career = career,
                            nextFixture = nextFixture,
                            onPrepareMatchday = onPrepareMatchday,
                            onStartNextSeason = onStartNextSeason,
                            onAdvanceWeek = onAdvanceWeek
                        )
                        OngoingStaffDelegationCard(
                            career = career,
                            onToggleDelegation = onToggleDelegation,
                            onOpenStaffOffice = onOpenStaffOffice
                        )
                    }
                }
            }

            // Staff Activity Feed
            if (career.inbox.isNotEmpty()) {
                item {
                    Text(
                        text = "Club Activity Feed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(career.inbox.take(4), key = { it.id }) { msg ->
                    HqInboxCard(msg = msg, onOpenPlayer = onOpenPlayer)
                }
            }
        }
    }
}

@Composable
private fun OngoingMatchdayPrepCard(
    career: CareerState,
    nextFixture: Fixture?,
    onPrepareMatchday: () -> Unit,
    onStartNextSeason: () -> Unit,
    onAdvanceWeek: () -> Unit
) {
    if (career.seasonComplete) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = career.seasonSummaryBanner ?: "Season ${career.season} Complete",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = onStartNextSeason,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_next_season_button")
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch Season ${career.season + 1}")
                }
            }
        }
    } else if (nextFixture != null) {
        val opponent = if (nextFixture.homeClubId == career.clubId) {
            "vs ${nextFixture.awayClubName} (Home)"
        } else {
            "@ ${nextFixture.homeClubName} (Away)"
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (nextFixture.isPlayoff) nextFixture.playoffLabel.uppercase()
                        else "ROUND ${nextFixture.round} OF ${career.totalRounds}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = nextFixture.pitchType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Text(
                    text = opponent,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = nextFixture.pitchType.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onPrepareMatchday,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("hq_matchday_cta")
                ) {
                    Icon(Icons.Filled.SportsCricket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Matchday Prep & Live Tactics")
                }
            }
        }
    } else {
        Button(
            onClick = onAdvanceWeek,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("advance_week_button")
        ) {
            Text("Advance Calendar Week")
        }
    }
}

@Composable
private fun OngoingStaffDelegationCard(
    career: CareerState,
    onToggleDelegation: (DelegationArea) -> Unit,
    onOpenStaffOffice: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Delegation Controls",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = onOpenStaffOffice,
                    modifier = Modifier.testTag("hq_open_staff_office_btn")
                ) {
                    Text("Staff Settings")
                }
            }
            DelegationArea.entries.forEach { area ->
                val isDelegated = career.delegation.isDelegated(area)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDelegated) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleDelegation(area) }
                        .testTag("hq_delegate_${area.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDelegated) Icons.Filled.AutoAwesome else Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (isDelegated) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = area.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = if (isDelegated) "Staff" else "You",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDelegated) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HqNavActionButton(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        modifier = modifier.height(62.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HqInboxCard(
    msg: InboxMessage,
    onOpenPlayer: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = msg.relatedPlayerId != null) {
                msg.relatedPlayerId?.let(onOpenPlayer)
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (msg.isUrgent) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)) else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${msg.senderRole.label} • ${msg.senderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (msg.isUrgent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "S${msg.season} W${msg.week}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(msg.title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = msg.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
