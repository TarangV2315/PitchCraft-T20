package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.theme.*

enum class CalendarFilter(val label: String) {
    ALL("All Weeks"),
    TRANSFERS("Transfers"),
    CAMPS("Training Camps"),
    RIVALS("Key Rivals")
}

data class TrainingCampEvent(
    val title: String,
    val focusArea: String,
    val leadCoachName: String,
    val leadRole: String
)

data class SeasonCalendarWeek(
    val week: Int,
    val stageLabel: String,
    val isCurrentWeek: Boolean,
    val isCompleted: Boolean,
    val isPlayoffWeek: Boolean,
    val fixture: Fixture?,
    val opponentName: String?,
    val opponentShortCode: String?,
    val isHomeMatch: Boolean,
    val transferWindowLabel: String?,
    val isTransferWindowOpenNow: Boolean,
    val trainingCamp: TrainingCampEvent?,
    val isKeyRivalMatch: Boolean,
    val rivalReason: String?
) {
    val hasTransferWindow: Boolean get() = transferWindowLabel != null
    val hasTrainingCamp: Boolean get() = trainingCamp != null
    val hasSpecialHighlight: Boolean get() = hasTransferWindow || hasTrainingCamp || isKeyRivalMatch
}

fun buildSeasonCalendarWeeks(career: CareerState): List<SeasonCalendarWeek> {
    val divisionClubs = career.currentDivision?.clubs.orEmpty()
    val sortedStandings = SeasonProgressionEngine.sortDivision(divisionClubs)
    val clubCodeById = divisionClubs.associate { it.clubId to it.shortCode }

    val primaryRivalId = divisionClubs
        .filter { it.clubId != career.clubId }
        .maxByOrNull { it.strength }
        ?.clubId

    val topContenderIds = sortedStandings
        .take(2)
        .map { it.clubId }
        .filter { it != career.clubId }
        .toSet()

    val userFixtures = career.fixtures.filter {
        it.tier == career.divisionTier && (it.homeClubId == career.clubId || it.awayClubId == career.clubId)
    }

    val headCoach = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name ?: "Head Coach"
    val batCoach = career.staff.firstOrNull { it.role == StaffRole.BATTING_COACH }?.name ?: "Batting Coach"
    val bowlCoach = career.staff.firstOrNull { it.role == StaffRole.BOWLING_COACH }?.name ?: "Bowling Coach"
    val physio = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name ?: "Head Physio"

    val totalCalendarWeeks = career.totalRounds + 3 // 14 league rounds + 3 playoff weeks

    return (1..totalCalendarWeeks).map { week ->
        val isPlayoff = week > career.totalRounds
        val fixture = if (!isPlayoff) {
            userFixtures.firstOrNull { !it.isPlayoff && it.round == week }
        } else {
            when (week) {
                career.totalRounds + 1 -> userFixtures.firstOrNull {
                    it.isPlayoff && (it.playoffLabel == "Qualifier 1" || it.playoffLabel == "Eliminator")
                }
                career.totalRounds + 2 -> userFixtures.firstOrNull {
                    it.isPlayoff && it.playoffLabel == "Qualifier 2"
                }
                else -> userFixtures.firstOrNull {
                    it.isPlayoff && it.playoffLabel == "Grand Final"
                }
            }
        }

        val isHome = fixture?.homeClubId == career.clubId
        val oppId = fixture?.let { if (isHome) it.awayClubId else it.homeClubId }
        val oppName = fixture?.let { if (isHome) it.awayClubName else it.homeClubName }
        val oppCode = oppId?.let { clubCodeById[it] } ?: oppName?.take(3)?.uppercase()

        val transferLabel = when (week) {
            1 -> "Pre-Season Transfer Window (1 Wk)"
            career.midSeasonTransferWeek -> "Mid-Season Transfer Window (1 Wk)"
            else -> null
        }

        val camp = when (week) {
            1 -> TrainingCampEvent(
                title = "Pre-Season Conditioning Camp",
                focusArea = "Match Fitness & Opening Timing",
                leadCoachName = headCoach,
                leadRole = "Head Coach"
            )
            4 -> TrainingCampEvent(
                title = "Powerplay Attack Camp",
                focusArea = "Timing, Footwork & Boundary Clearing",
                leadCoachName = batCoach,
                leadRole = "Batting Coach"
            )
            7 -> TrainingCampEvent(
                title = "Mid-Season Recovery & Physio Camp",
                focusArea = "Stamina Recharge & Rehab Prevention",
                leadCoachName = physio,
                leadRole = "Head Physio"
            )
            11 -> TrainingCampEvent(
                title = "Death Overs Execution Camp",
                focusArea = "Yorker Precision & Middle-Overs Spin",
                leadCoachName = bowlCoach,
                leadRole = "Bowling Coach"
            )
            14 -> TrainingCampEvent(
                title = "Playoff High-Performance Camp",
                focusArea = "Knockout Intensity & Fielding Reflex",
                leadCoachName = headCoach,
                leadRole = "Head Coach"
            )
            else -> null
        }

        val rivalReason = when {
            fixture?.isPlayoff == true -> "${fixture.playoffLabel} Knockout Clash"
            isPlayoff -> "Championship Playoff Stage"
            oppId != null && oppId == primaryRivalId -> "Division Derby Rival"
            oppId != null && oppId in topContenderIds -> "Top-2 Contender Clash"
            else -> null
        }

        val stageLabel = when {
            week == 1 -> "Pre-Season & Round 1"
            week == career.midSeasonTransferWeek -> "Mid-Season • Round $week"
            week <= career.totalRounds -> "League Round $week"
            week == career.totalRounds + 1 -> "Qualifier 1 / Eliminator"
            week == career.totalRounds + 2 -> "Qualifier 2"
            else -> "Grand Final"
        }

        SeasonCalendarWeek(
            week = week,
            stageLabel = stageLabel,
            isCurrentWeek = !career.seasonComplete && week == career.week,
            isCompleted = fixture?.isCompleted ?: (week < career.week || career.seasonComplete),
            isPlayoffWeek = isPlayoff,
            fixture = fixture,
            opponentName = oppName,
            opponentShortCode = oppCode,
            isHomeMatch = isHome,
            transferWindowLabel = transferLabel,
            isTransferWindowOpenNow = transferLabel != null && !career.seasonComplete && week == career.week && career.isTransferWindowOpen,
            trainingCamp = camp,
            isKeyRivalMatch = rivalReason != null,
            rivalReason = rivalReason
        )
    }
}

@Composable
fun SeasonCalendarPreviewCard(
    career: CareerState,
    onOpenFullCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weeks = remember(career.season, career.week, career.fixtures, career.divisions, career.staff) {
        buildSeasonCalendarWeeks(career)
    }
    val upcomingWindowWeeks = remember(weeks, career.week) {
        val startIdx = (career.week - 1).coerceIn(0, (weeks.size - 4).coerceAtLeast(0))
        weeks.drop(startIdx).take(4)
    }
    val nextTransfer = weeks.firstOrNull { it.week >= career.week && it.hasTransferWindow }
    val nextCamp = weeks.firstOrNull { it.week >= career.week && it.hasTrainingCamp }
    val nextRival = weeks.firstOrNull { it.week >= career.week && it.isKeyRivalMatch }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hq_calendar_preview_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Season ${career.season} Calendar",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Week ${career.week} of ${weeks.size} • Schedule & Milestones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onOpenFullCalendar,
                    modifier = Modifier.testTag("hq_open_calendar_btn")
                ) {
                    Text("Full Calendar")
                }
            }

            // 4-week rolling visual calendar strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                upcomingWindowWeeks.forEach { wk ->
                    CalendarWeekCell(
                        weekData = wk,
                        isSelected = wk.isCurrentWeek,
                        dimmed = false,
                        onClick = onOpenFullCalendar,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Upcoming milestone pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                nextTransfer?.let {
                    item {
                        MilestoneSummaryChip(
                            icon = Icons.Filled.SwapHoriz,
                            label = "Transfer: W${it.week}",
                            accent = TelemetryCyan80,
                            onClick = onOpenFullCalendar
                        )
                    }
                }
                nextCamp?.let {
                    item {
                        MilestoneSummaryChip(
                            icon = Icons.Filled.FitnessCenter,
                            label = "Camp: W${it.week}",
                            accent = PitchMint,
                            onClick = onOpenFullCalendar
                        )
                    }
                }
                nextRival?.let {
                    item {
                        MilestoneSummaryChip(
                            icon = Icons.Filled.LocalFireDepartment,
                            label = "Rival (${it.opponentShortCode ?: "PO"}): W${it.week}",
                            accent = FloodlightGold80,
                            onClick = onOpenFullCalendar
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonCalendarSection(
    career: CareerState,
    onOpenTransfers: () -> Unit,
    onOpenMatchday: () -> Unit,
    onOpenSquadTraining: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weeks = remember(career.season, career.week, career.fixtures, career.divisions, career.staff, career.seasonComplete) {
        buildSeasonCalendarWeeks(career)
    }
    var activeFilter by remember { mutableStateOf(CalendarFilter.ALL) }
    var selectedWeekNumber by remember(career.season, career.week) {
        mutableIntStateOf(career.week.coerceIn(1, weeks.size))
    }
    val selectedWeek = weeks.firstOrNull { it.week == selectedWeekNumber } ?: weeks.first()
    val upcomingHighlights = remember(weeks, career.week) {
        weeks.filter { it.week >= career.week && it.hasSpecialHighlight }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("season_calendar_section"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Legend
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Season ${career.season} Match & Event Calendar",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Tap any week to inspect fixtures, transfer windows, camps & rivals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (career.seasonComplete) "COMPLETE" else "WEEK ${career.week}",
                                style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Visual Legend
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { LegendDot(color = MaterialTheme.colorScheme.primary, label = "Current Week") }
                        item { LegendDot(color = TelemetryCyan80, label = "Transfer Window") }
                        item { LegendDot(color = PitchMint, label = "Training Camp") }
                        item { LegendDot(color = FloodlightGold80, label = "Key Rival Match") }
                    }

                    // Filter Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CalendarFilter.entries) { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("calendar_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Visual Calendar Grid (4-column rows)
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "REGULAR SEASON (WEEKS 1–${career.totalRounds}) & PLAYOFFS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val rows = weeks.chunked(4)
                    rows.forEach { rowWeeks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowWeeks.forEach { wk ->
                                val matchesFilter = when (activeFilter) {
                                    CalendarFilter.ALL -> true
                                    CalendarFilter.TRANSFERS -> wk.hasTransferWindow
                                    CalendarFilter.CAMPS -> wk.hasTrainingCamp
                                    CalendarFilter.RIVALS -> wk.isKeyRivalMatch
                                }
                                CalendarWeekCell(
                                    weekData = wk,
                                    isSelected = wk.week == selectedWeek.week,
                                    dimmed = !matchesFilter,
                                    onClick = { selectedWeekNumber = wk.week },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - rowWeeks.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Selected Week Detailed Breakdown
        item {
            SelectedWeekDetailCard(
                week = selectedWeek,
                isTransfersTabOpen = career.isTransferWindowOpen,
                onOpenTransfers = onOpenTransfers,
                onOpenMatchday = onOpenMatchday,
                onOpenSquadTraining = onOpenSquadTraining
            )
        }

        // Chronological Upcoming Key Dates
        if (upcomingHighlights.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Season Highlights",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(upcomingHighlights, key = { "hl_${it.week}" }) { wk ->
                UpcomingMilestoneCard(
                    week = wk,
                    onSelectWeek = { selectedWeekNumber = wk.week }
                )
            }
        }
    }
}

@Composable
private fun CalendarWeekCell(
    weekData: SeasonCalendarWeek,
    isSelected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        weekData.isCurrentWeek -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        weekData.hasTransferWindow -> TelemetryCyan80.copy(alpha = 0.55f)
        weekData.isKeyRivalMatch -> FloodlightGold80.copy(alpha = 0.55f)
        weekData.hasTrainingCamp -> PitchMint.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
        weekData.isCurrentWeek -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        weekData.isCompleted -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .alpha(if (dimmed) 0.32f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("calendar_week_${weekData.week}"),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected || weekData.isCurrentWeek) 1.6.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "W${weekData.week}",
                    style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (weekData.isCurrentWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                when {
                    weekData.isCurrentWeek -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    weekData.isCompleted -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = PitchMint,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Match opponent code or Playoff indicator
            val matchTag = when {
                weekData.opponentShortCode != null -> {
                    val venue = if (weekData.isHomeMatch) "H" else "A"
                    "${weekData.opponentShortCode} ($venue)"
                }
                weekData.isPlayoffWeek -> "Playoff"
                else -> "TBD"
            }
            Text(
                text = matchTag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Visual event badges (Transfer, Camp, Rival)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (weekData.hasTransferWindow) {
                    CalendarEventDot(icon = Icons.Filled.SwapHoriz, color = TelemetryCyan80, desc = "Transfer Window")
                }
                if (weekData.hasTrainingCamp) {
                    CalendarEventDot(icon = Icons.Filled.FitnessCenter, color = PitchMint, desc = "Training Camp")
                }
                if (weekData.isKeyRivalMatch) {
                    CalendarEventDot(icon = Icons.Filled.LocalFireDepartment, color = FloodlightGold80, desc = "Key Rival Match")
                }
                if (!weekData.hasSpecialHighlight) {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarEventDot(
    icon: ImageVector,
    color: Color,
    desc: String
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.22f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = color,
            modifier = Modifier
                .padding(3.dp)
                .size(10.dp)
        )
    }
}

@Composable
private fun SelectedWeekDetailCard(
    week: SeasonCalendarWeek,
    isTransfersTabOpen: Boolean,
    onOpenTransfers: () -> Unit,
    onOpenMatchday: () -> Unit,
    onOpenSquadTraining: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_selected_detail"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Week ${week.week} • ${week.stageLabel}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            week.isCurrentWeek -> "Active Matchweek"
                            week.isCompleted -> "Completed Week"
                            else -> "Upcoming Scheduled Week"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (week.isCurrentWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (week.isKeyRivalMatch) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FloodlightGold80.copy(alpha = 0.18f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = FloodlightGold80,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "KEY RIVAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = FloodlightGold80
                            )
                        }
                    }
                }
            }

            // Fixture Card
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val fx = week.fixture
                    if (fx != null) {
                        Text(
                            text = if (week.isHomeMatch) "vs ${week.opponentName} (Home)" else "@ ${week.opponentName} (Away)",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "${fx.pitchType.label} • ${week.rivalReason ?: "Division Fixture"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (week.isKeyRivalMatch) FloodlightGold80 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (fx.isCompleted && fx.resultSummary.isNotBlank()) {
                            Text(
                                text = "Result: ${fx.resultSummary}",
                                style = TelemetryTextStyles.statSmall,
                                color = PitchMint
                            )
                        } else if (week.isCurrentWeek) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onOpenMatchday,
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Filled.SportsCricket, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Go to Matchday")
                            }
                        }
                    } else {
                        Text(
                            text = if (week.isPlayoffWeek) "Playoff Bracket TBD (Top 4 Finishers)" else "Schedule Pending",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Finish the regular season Top 4 to lock in knockout fixtures.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Transfer Window Highlight
            if (week.transferWindowLabel != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = TelemetryCyan80.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, TelemetryCyan80.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = TelemetryCyan80)
                            Column {
                                Text(week.transferWindowLabel, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (week.isTransferWindowOpenNow) "Window OPEN now for 1 week — Transfers tab unlocked." else "1-week window to bid on players & adjust squad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (week.isTransferWindowOpenNow && isTransfersTabOpen) {
                            FilledTonalButton(onClick = onOpenTransfers) {
                                Text("Transfers")
                            }
                        }
                    }
                }
            }

            // Training Camp Highlight
            if (week.trainingCamp != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = PitchMint.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PitchMint.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = PitchMint)
                            Column {
                                Text(week.trainingCamp.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${week.trainingCamp.focusArea} • Led by ${week.trainingCamp.leadCoachName} (${week.trainingCamp.leadRole})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (week.isCurrentWeek) {
                            FilledTonalButton(onClick = onOpenSquadTraining) {
                                Text("Drills")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingMilestoneCard(
    week: SeasonCalendarWeek,
    onSelectWeek: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onSelectWeek),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = "W${week.week}",
                    style = TelemetryTextStyles.statValue,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (week.transferWindowLabel != null) {
                    Text(
                        text = week.transferWindowLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = TelemetryCyan80
                    )
                }
                if (week.trainingCamp != null) {
                    Text(
                        text = week.trainingCamp.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = PitchMint
                    )
                }
                if (week.isKeyRivalMatch) {
                    val opp = week.opponentName ?: "Knockout Tie"
                    Text(
                        text = "${week.rivalReason}: $opp",
                        style = MaterialTheme.typography.bodySmall,
                        color = FloodlightGold80
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Inspect Week ${week.week}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
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
private fun MilestoneSummaryChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
    }
}
