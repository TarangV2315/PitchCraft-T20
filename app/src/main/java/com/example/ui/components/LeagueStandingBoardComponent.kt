package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.WorldGenerator
import com.example.ui.theme.*

enum class LeaderboardCategory(val label: String) {
    BATTING("Batting"),
    BOWLING("Bowling")
}

enum class BattingSortMetric(val label: String) {
    MOST_RUNS("Most Runs"),
    STRIKE_RATE("Strike Rate"),
    FIFTIES("50s"),
    THIRTIES("30s")
}

enum class BowlingSortMetric(val label: String) {
    WICKETS("Wickets"),
    DOT_BALLS("Dot Balls"),
    MAIDENS("Maidens"),
    THREE_WK_HAULS("3W Hauls")
}

fun buildLeagueBattingLeaders(
    career: CareerState,
    tier: Int,
    sortBy: BattingSortMetric
): List<TournamentPlayerStat> {
    val userStats = if (tier == career.divisionTier) {
        career.squad
            .filter { !it.isYouthAcademy && (it.seasonStats.runs > 0 || it.seasonStats.ballsFaced > 0) }
            .map { p ->
                TournamentPlayerStat(
                    playerId = p.id,
                    playerName = p.name,
                    clubId = career.clubId,
                    clubName = career.clubName,
                    clubShortCode = career.clubShortCode,
                    tier = career.divisionTier,
                    role = p.role,
                    stats = p.seasonStats
                )
            }
    } else {
        emptyList()
    }

    val aiStats = career.tournamentStats.filter {
        it.tier == tier &&
            it.clubId != career.clubId &&
            (it.stats.runs > 0 || it.stats.ballsFaced > 0)
    }

    val combined = userStats + aiStats
    return when (sortBy) {
        BattingSortMetric.MOST_RUNS -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.runs }
                .thenByDescending { it.stats.strikeRate }
                .thenByDescending { it.stats.fifties }
        )
        BattingSortMetric.STRIKE_RATE -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { if (it.stats.ballsFaced >= 10) it.stats.strikeRate else -1.0 }
                .thenByDescending { it.stats.strikeRate }
                .thenByDescending { it.stats.runs }
        )
        BattingSortMetric.FIFTIES -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.fifties }
                .thenByDescending { it.stats.hundreds }
                .thenByDescending { it.stats.runs }
        )
        BattingSortMetric.THIRTIES -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.thirties }
                .thenByDescending { it.stats.runs }
                .thenByDescending { it.stats.strikeRate }
        )
    }
}

fun buildLeagueBowlingLeaders(
    career: CareerState,
    tier: Int,
    sortBy: BowlingSortMetric
): List<TournamentPlayerStat> {
    val userStats = if (tier == career.divisionTier) {
        career.squad
            .filter { !it.isYouthAcademy && (it.seasonStats.ballsBowled > 0 || it.seasonStats.wickets > 0) }
            .map { p ->
                TournamentPlayerStat(
                    playerId = p.id,
                    playerName = p.name,
                    clubId = career.clubId,
                    clubName = career.clubName,
                    clubShortCode = career.clubShortCode,
                    tier = career.divisionTier,
                    role = p.role,
                    stats = p.seasonStats
                )
            }
    } else {
        emptyList()
    }

    val aiStats = career.tournamentStats.filter {
        it.tier == tier &&
            it.clubId != career.clubId &&
            (it.stats.ballsBowled > 0 || it.stats.wickets > 0)
    }

    val combined = userStats + aiStats
    return when (sortBy) {
        BowlingSortMetric.WICKETS -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.wickets }
                .thenByDescending { it.stats.threeWicketHauls }
                .thenBy { it.stats.economyRate }
        )
        BowlingSortMetric.DOT_BALLS -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.dotBalls }
                .thenByDescending { it.stats.wickets }
                .thenBy { it.stats.economyRate }
        )
        BowlingSortMetric.MAIDENS -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.maidens }
                .thenByDescending { it.stats.dotBalls }
                .thenByDescending { it.stats.wickets }
        )
        BowlingSortMetric.THREE_WK_HAULS -> combined.sortedWith(
            compareByDescending<TournamentPlayerStat> { it.stats.threeWicketHauls }
                .thenByDescending { it.stats.wickets }
                .thenBy { it.stats.economyRate }
        )
    }
}

@Composable
fun LeagueStandingBoardSection(
    career: CareerState,
    selectedTier: Int,
    onSelectTier: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTier = if (career.country.supportsPromotionRelegation) selectedTier.coerceIn(1, 2) else 1
    val div = career.divisions.firstOrNull { it.tier == activeTier }
    val sortedClubs = remember(div) {
        div?.let { SeasonProgressionEngine.sortDivision(it.clubs) }.orEmpty()
    }

    var leaderboardCategory by remember { mutableStateOf(LeaderboardCategory.BATTING) }
    var battingSort by remember { mutableStateOf(BattingSortMetric.MOST_RUNS) }
    var bowlingSort by remember { mutableStateOf(BowlingSortMetric.WICKETS) }

    val battingLeaders = remember(career.squad, career.tournamentStats, activeTier, battingSort) {
        buildLeagueBattingLeaders(career, activeTier, battingSort).take(10)
    }
    val bowlingLeaders = remember(career.squad, career.tournamentStats, activeTier, bowlingSort) {
        buildLeagueBowlingLeaders(career, activeTier, bowlingSort).take(10)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("pyramid_standings_list")
    ) {
        // Domestic League Context Header
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Leaderboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${career.country.leagueBrand} (${career.country.label})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "S${career.season} • Wk ${career.week}",
                                style = TelemetryTextStyles.statSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = career.country.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Division Tier Switcher for 2-tier pyramids (India / England)
        if (career.country.supportsPromotionRelegation) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        1 to "Tier 1 • Major Cities",
                        2 to "Tier 2 • Minor Cities"
                    ).forEachIndexed { idx, (tier, label) ->
                        SegmentedButton(
                            selected = activeTier == tier,
                            onClick = { onSelectTier(tier) },
                            shape = SegmentedButtonDefaults.itemShape(idx, 2),
                            modifier = Modifier.testTag("pyramid_tier_$tier")
                        ) {
                            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Dynamic League Standing Table Card
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (div != null) {
                            Text(
                                text = div.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "LIVE TABLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = PitchMint
                        )
                    }

                    // Zone Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (career.country.supportsPromotionRelegation && activeTier == 2) {
                            ZoneBadge(label = "PROMO (#1)", color = PitchMint)
                        }
                        ZoneBadge(label = "PLAYOFFS (TOP 4)", color = MaterialTheme.colorScheme.tertiary)
                        if (career.country.supportsPromotionRelegation && activeTier == 1) {
                            ZoneBadge(label = "REL (#8)", color = AlertCrimson)
                        }
                    }

                    // Standings Column Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#  Club", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2.3f))
                        Text("P", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.36f))
                        Text("W", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.36f))
                        Text("L", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.36f))
                        Text("NRR", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.75f))
                        Text("Pts", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.48f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    sortedClubs.forEachIndexed { idx, club ->
                        val pos = idx + 1
                        val isRelegationSpot = career.country.supportsPromotionRelegation && activeTier == 1 && pos == sortedClubs.size
                        val isPromotionSpot = career.country.supportsPromotionRelegation && activeTier == 2 && pos == 1
                        val isPlayoffSpot = pos <= 4

                        val rowColor = when {
                            club.isUserClub -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            isRelegationSpot -> AlertCrimson.copy(alpha = 0.14f)
                            isPromotionSpot -> PitchMint.copy(alpha = 0.16f)
                            isPlayoffSpot -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceContainerLow
                        }
                        val stripeColor = when {
                            isRelegationSpot -> AlertCrimson
                            isPromotionSpot -> PitchMint
                            isPlayoffSpot -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }
                        val tagSuffix = when {
                            isRelegationSpot -> " (REL)"
                            isPromotionSpot -> " (PROMO)"
                            else -> ""
                        }

                        Surface(
                            color = rowColor,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag("standing_row_${club.clubId}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(stripeColor)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 6.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$pos. ${club.name}$tagSuffix",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (club.isUserClub || isPromotionSpot || isRelegationSpot) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                club.isUserClub -> MaterialTheme.colorScheme.primary
                                                isRelegationSpot -> AlertCrimson
                                                isPromotionSpot -> PitchMint
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        ),
                                        modifier = Modifier.weight(2.3f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${club.played}",
                                        style = TelemetryTextStyles.statSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.36f)
                                    )
                                    Text(
                                        text = "${club.won}",
                                        style = TelemetryTextStyles.statSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.36f)
                                    )
                                    Text(
                                        text = "${club.lost}",
                                        style = TelemetryTextStyles.statSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.36f)
                                    )
                                    Text(
                                        text = "%+.2f".format(club.netRunRate),
                                        style = TelemetryTextStyles.statSmall.copy(
                                            color = if (club.netRunRate >= 0.0) PitchMint else AlertCrimson
                                        ),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.75f)
                                    )
                                    Text(
                                        text = "${club.points}",
                                        style = TelemetryTextStyles.statValue,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.48f)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = when {
                            !career.country.supportsPromotionRelegation ->
                                "Single domestic league • Top 4 qualify for Championship Playoffs"
                            activeTier == 1 ->
                                "Top 4 qualify for Playoffs • Last place (8th) relegates to Minor Cities League"
                            else ->
                                "Top 4 qualify for Playoffs • Minor Cities League Winner promotes to Major Cities League"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Individual Tournament Batting & Bowling Stats Leaderboard Board
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.testTag("tournament_stats_board")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tournament Leaders",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${WorldGenerator.divisionTierShortLabel(career.country, activeTier)} • Batting & Bowling Charts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Tournament Stat Leaders",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Category Switcher (Batting / Bowling)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = leaderboardCategory == LeaderboardCategory.BATTING,
                            onClick = { leaderboardCategory = LeaderboardCategory.BATTING },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("leaderboard_batting_tab")
                        ) {
                            Text("Batting Stats")
                        }
                        SegmentedButton(
                            selected = leaderboardCategory == LeaderboardCategory.BOWLING,
                            onClick = { leaderboardCategory = LeaderboardCategory.BOWLING },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("leaderboard_bowling_tab")
                        ) {
                            Text("Bowling Stats")
                        }
                    }

                    // Sort Chips
                    if (leaderboardCategory == LeaderboardCategory.BATTING) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BattingSortMetric.entries) { metric ->
                                FilterChip(
                                    selected = battingSort == metric,
                                    onClick = { battingSort = metric },
                                    label = { Text(metric.label) },
                                    modifier = Modifier.testTag("bat_sort_${metric.name.lowercase()}")
                                )
                            }
                        }

                        // Batting Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#  Batter", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2.1f))
                            Text("M", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.35f))
                            Text("Runs", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
                            Text("SR", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.62f))
                            Text("50s", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                            Text("30s", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        if (battingLeaders.isEmpty()) {
                            EmptyLeaderboardNotice(
                                text = "Complete Round 1 to unlock Most Runs, Strike Rate, 50s & 30s leaders."
                            )
                        } else {
                            battingLeaders.forEachIndexed { idx, entry ->
                                val isUser = entry.clubId == career.clubId
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    modifier = Modifier.testTag("bat_leader_row_${entry.playerId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(2.1f)) {
                                            Text(
                                                text = "${idx + 1}. ${entry.playerName}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isUser || idx == 0) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.clubShortCode} • ${entry.role.shortLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${entry.stats.matches}",
                                            style = TelemetryTextStyles.statSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.35f)
                                        )
                                        Text(
                                            text = "${entry.stats.runs}",
                                            style = TelemetryTextStyles.statValue.copy(
                                                color = if (battingSort == BattingSortMetric.MOST_RUNS) PitchMint else MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.55f)
                                        )
                                        Text(
                                            text = "%.1f".format(entry.stats.strikeRate),
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (battingSort == BattingSortMetric.STRIKE_RATE) PitchMint else MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.62f)
                                        )
                                        Text(
                                            text = "${entry.stats.fifties}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (battingSort == BattingSortMetric.FIFTIES) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (entry.stats.fifties > 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.42f)
                                        )
                                        Text(
                                            text = "${entry.stats.thirties}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (battingSort == BattingSortMetric.THIRTIES) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (entry.stats.thirties > 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.42f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BowlingSortMetric.entries) { metric ->
                                FilterChip(
                                    selected = bowlingSort == metric,
                                    onClick = { bowlingSort = metric },
                                    label = { Text(metric.label) },
                                    modifier = Modifier.testTag("bowl_sort_${metric.name.lowercase()}")
                                )
                            }
                        }

                        // Bowling Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#  Bowler", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2.1f))
                            Text("Ov", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.45f))
                            Text("Wkt", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.48f))
                            Text("Dots", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
                            Text("Mdn", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.45f))
                            Text("3W", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.End, modifier = Modifier.weight(0.42f))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        if (bowlingLeaders.isEmpty()) {
                            EmptyLeaderboardNotice(
                                text = "Complete Round 1 to unlock Wickets, Dot Balls, Maidens & 3W Hauls leaders."
                            )
                        } else {
                            bowlingLeaders.forEachIndexed { idx, entry ->
                                val isUser = entry.clubId == career.clubId
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    modifier = Modifier.testTag("bowl_leader_row_${entry.playerId}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(2.1f)) {
                                            Text(
                                                text = "${idx + 1}. ${entry.playerName}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isUser || idx == 0) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.clubShortCode} • Econ ${"%.1f".format(entry.stats.economyRate)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = entry.stats.oversFormatted,
                                            style = TelemetryTextStyles.statSmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(0.45f)
                                        )
                                        Text(
                                            text = "${entry.stats.wickets}",
                                            style = TelemetryTextStyles.statValue.copy(
                                                color = if (bowlingSort == BowlingSortMetric.WICKETS) PitchMint else MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.48f)
                                        )
                                        Text(
                                            text = "${entry.stats.dotBalls}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (bowlingSort == BowlingSortMetric.DOT_BALLS) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.55f)
                                        )
                                        Text(
                                            text = "${entry.stats.maidens}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (bowlingSort == BowlingSortMetric.MAIDENS) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (entry.stats.maidens > 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.45f)
                                        )
                                        Text(
                                            text = "${entry.stats.threeWicketHauls}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (bowlingSort == BowlingSortMetric.THREE_WK_HAULS) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (entry.stats.threeWicketHauls > 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.42f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Club Match Results
        if (career.matchHistory.isNotEmpty()) {
            item {
                Text("Recent Club Match Results", style = MaterialTheme.typography.titleMedium)
            }
            items(career.matchHistory.take(10), key = { it.matchId }) { m ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "S${m.season} • ${m.label} • ${m.resultText}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (m.userWon) PitchMint else AlertCrimson
                        )
                        Text(
                            text = "${m.firstInnings.battingClubName} ${m.firstInnings.runs}/${m.firstInnings.wickets} vs ${m.secondInnings.battingClubName} ${m.secondInnings.runs}/${m.secondInnings.wickets} • MOTM: ${m.manOfTheMatch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyLeaderboardNotice(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }
}
