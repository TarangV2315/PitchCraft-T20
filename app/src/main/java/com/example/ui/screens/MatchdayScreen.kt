package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MatchdayScreen(
    career: CareerState,
    liveMatch: LiveMatchState?,
    onToggleDelegation: (DelegationArea) -> Unit,
    onTogglePlayerInXI: (String) -> Unit,
    onSetImpactSub: (String) -> Unit,
    onAutoPickXI: () -> Unit,
    onSetDefaultTactics: (BattingTempo?, BowlingPlan?, FieldSpread?) -> Unit,
    onStartMatch: (Boolean) -> Unit,
    onSubmitKeyMoment: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, Boolean) -> Unit,
    onFinishMatch: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (liveMatch != null) {
        if (liveMatch.finishedRecord != null) {
            PostMatchScorecardView(
                record = liveMatch.finishedRecord,
                onContinue = onFinishMatch,
                modifier = modifier
            )
        } else {
            LiveMatchWarRoomView(
                live = liveMatch,
                onSubmitDecision = onSubmitKeyMoment,
                modifier = modifier
            )
        }
        return
    }

    val nextFixture = career.nextUserFixture
    val headCoachName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name
    val seniorAvailable = remember(career.squad) {
        career.squad.filter { it.isAvailableForMatch }.sortedByDescending { it.overall }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (nextFixture == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("No Pending Fixture", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Return to Club HQ to advance the calendar or launch the next season.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // Match Preview Hero
        item {
            val isHome = nextFixture.homeClubId == career.clubId
            val opponent = if (isHome) nextFixture.awayClubName else nextFixture.homeClubName
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (nextFixture.isPlayoff) nextFixture.playoffLabel.uppercase()
                            else "ROUND ${nextFixture.round} • ${if (isHome) "HOME" else "AWAY"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = nextFixture.pitchType.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text("${career.clubName} vs $opponent", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = nextFixture.pitchType.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartMatch(false) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                                .testTag("kickoff_key_moments_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play Highlights")
                        }
                        OutlinedButton(
                            onClick = { onStartMatch(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("instant_sim_button")
                        ) {
                            Icon(Icons.Filled.FastForward, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Quick Sim")
                        }
                    }
                }
            }
        }

        // Delegation Toggles
        item {
            DelegationBanner(
                area = DelegationArea.SELECTION,
                isDelegated = career.delegation.selectionDelegated,
                staffOwnerName = headCoachName,
                onToggle = { onToggleDelegation(DelegationArea.SELECTION) }
            )
        }
        item {
            DelegationBanner(
                area = DelegationArea.TACTICS,
                isDelegated = career.delegation.tacticsDelegated,
                staffOwnerName = headCoachName,
                onToggle = { onToggleDelegation(DelegationArea.TACTICS) }
            )
        }

        // Baseline Tactical Posture
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Opening Tactical Plan", style = MaterialTheme.typography.titleSmall)
                    Text("Batting Tempo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BattingTempo.entries) { tempo ->
                            FilterChip(
                                selected = career.defaultTempo == tempo,
                                enabled = !career.delegation.tacticsDelegated,
                                onClick = { onSetDefaultTactics(tempo, null, null) },
                                label = { Text(tempo.label) }
                            )
                        }
                    }
                    Text("Bowling & Field Plan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BowlingPlan.entries) { plan ->
                            FilterChip(
                                selected = career.defaultBowlingPlan == plan,
                                enabled = !career.delegation.tacticsDelegated,
                                onClick = { onSetDefaultTactics(null, plan, null) },
                                label = { Text(plan.label) }
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FieldSpread.entries) { field ->
                            FilterChip(
                                selected = career.defaultFieldSpread == field,
                                enabled = !career.delegation.tacticsDelegated,
                                onClick = { onSetDefaultTactics(null, null, field) },
                                label = { Text(field.label) }
                            )
                        }
                    }
                }
            }
        }

        // Playing XI Selection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Playing XI (${career.selectedXIIds.size}/11)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Tap checkbox for XI or star for Impact Sub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onAutoPickXI,
                    modifier = Modifier.testTag("auto_pick_xi_button")
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Coach XI")
                }
            }
        }

        items(seniorAvailable, key = { "xi_${it.id}" }) { player ->
            val inXI = career.selectedXIIds.contains(player.id)
            val isSub = career.impactSubId == player.id
            PlayerSummaryCard(
                player = player,
                isSelectedInXI = inXI,
                isImpactSub = isSub,
                onClick = { onOpenPlayer(player.id) },
                trailingAction = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!inXI) {
                            IconButton(
                                onClick = { onSetImpactSub(player.id) },
                                enabled = !career.delegation.selectionDelegated
                            ) {
                                Icon(
                                    imageVector = if (isSub) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Set Impact Sub",
                                    tint = if (isSub) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Checkbox(
                            checked = inXI,
                            enabled = !career.delegation.selectionDelegated,
                            onCheckedChange = { onTogglePlayerInXI(player.id) }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LiveMatchWarRoomView(
    live: LiveMatchState,
    onSubmitDecision: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompt = live.pendingKeyMoment
    var chosenTempo by remember(prompt?.momentIndex) { mutableStateOf(prompt?.recommendedTempo ?: live.currentTempo) }
    var chosenPlan by remember(prompt?.momentIndex) { mutableStateOf(prompt?.recommendedBowlingPlan ?: live.currentBowlingPlan) }
    var chosenField by remember(prompt?.momentIndex) { mutableStateOf(prompt?.recommendedField ?: live.currentFieldSpread) }
    var deploySub by remember(prompt?.momentIndex) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live Scoreboard Ribbon
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (live.userIsBattingNow) "BATTING" else "BOWLING"} • INN ${live.currentInnings}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = live.fixture.pitchType.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${live.runs}/${live.wickets}",
                            style = TelemetryTextStyles.scoreboardLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Ov ${live.oversText} / 20.0",
                                style = TelemetryTextStyles.statValue
                            )
                            Text(
                                text = buildString {
                                    append("CRR ${"%.2f".format(live.currentRunRate)}")
                                    live.requiredRunRate?.let { append(" • RRR ${"%.2f".format(it)}") }
                                },
                                style = TelemetryTextStyles.statSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    live.target?.let { tgt ->
                        val need = (tgt - live.runs).coerceAtLeast(0)
                        val ballsLeft = (120 - live.balls).coerceAtLeast(0)
                        Text(
                            text = "Target $tgt • Need $need off $ballsLeft balls",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    // Recent Balls Tape
                    if (live.recentBalls.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            live.recentBalls.takeLast(8).forEach { ball ->
                                val bg = when (ball) {
                                    "W" -> AlertCrimson
                                    "6" -> BoundaryAmber
                                    "4" -> PitchMint
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                                val fg = if (ball in listOf("W", "6", "4")) Color.Black else MaterialTheme.colorScheme.onSurface
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ball, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = fg))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Crease & Bowler Strip
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    live.activeBatters.forEach { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${b.playerName} (${b.roleLabel})", style = MaterialTheme.typography.bodyMedium)
                            Text("${b.runs} (${b.balls})", style = TelemetryTextStyles.statValue)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    live.activeBowler?.let { bw ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bowling: ${bw.playerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("${bw.wickets}/${bw.runsConceded} (${bw.oversText})", style = TelemetryTextStyles.statSmall)
                        }
                    }
                }
            }
        }

        // Key Moment Tactical Intervention Card
        if (prompt != null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(prompt.title, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(prompt.description, style = MaterialTheme.typography.bodyMedium)

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                Text(prompt.staffTip, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (prompt.isUserBatting) {
                            Text("Select Batting Tempo", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BattingTempo.entries) { tempo ->
                                    FilterChip(
                                        selected = chosenTempo == tempo,
                                        onClick = { chosenTempo = tempo },
                                        label = { Text(tempo.label) }
                                    )
                                }
                            }
                        } else {
                            Text("Select Bowling Plan", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BowlingPlan.entries) { plan ->
                                    FilterChip(
                                        selected = chosenPlan == plan,
                                        onClick = { chosenPlan = plan },
                                        label = { Text(plan.label) }
                                    )
                                }
                            }
                            Text("Field Spread", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(FieldSpread.entries) { field ->
                                    FilterChip(
                                        selected = chosenField == field,
                                        onClick = { chosenField = field },
                                        label = { Text(field.label) }
                                    )
                                }
                            }
                        }

                        if (live.availableImpactSubs.isNotEmpty() && !live.impactSubUsed) {
                            val sub = live.availableImpactSubs.first()
                            FilterChip(
                                selected = deploySub,
                                onClick = { deploySub = !deploySub },
                                label = { Text("Deploy Impact Sub: ${sub.name} (${sub.role.shortLabel})") },
                                leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSubmitDecision(
                                        if (prompt.isUserBatting) chosenTempo else null,
                                        if (!prompt.isUserBatting) chosenPlan else null,
                                        if (!prompt.isUserBatting) chosenField else null,
                                        if (deploySub) live.availableImpactSubs.firstOrNull()?.id else null,
                                        false
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("resume_match_button")
                            ) {
                                Text("Apply & Play On")
                            }
                            OutlinedButton(
                                onClick = {
                                    onSubmitDecision(
                                        if (prompt.isUserBatting) chosenTempo else null,
                                        if (!prompt.isUserBatting) chosenPlan else null,
                                        if (!prompt.isUserBatting) chosenField else null,
                                        if (deploySub) live.availableImpactSubs.firstOrNull()?.id else null,
                                        true
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("sim_remainder_button")
                            ) {
                                Text("Sim Rest")
                            }
                        }
                    }
                }
            }
        }

        // Commentary Ticker
        item {
            Text("Match Highlights Feed", style = MaterialTheme.typography.titleSmall)
        }
        items(live.commentaryLog) { line ->
            Text(
                text = "• $line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PostMatchScorecardView(
    record: MatchRecord,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (record.userWon) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    1.5.dp,
                    if (record.userWon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (record.userWon) "VICTORY • ${record.label.uppercase()}" else "DEFEAT • ${record.label.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (record.userWon) PitchMint else AlertCrimson
                    )
                    Text(record.resultText, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Player of the Match: ${record.manOfTheMatch}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Matchday Revenue & Bonuses: +${formatCurrency(record.revenueEarned)}",
                        style = TelemetryTextStyles.statValue,
                        color = PitchMint
                    )
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("post_match_continue_button")
                    ) {
                        Text("Commit Result & Advance Week")
                    }
                }
            }
        }

        item { InningsCardView("1st Innings", record.firstInnings) }
        item { InningsCardView("2nd Innings", record.secondInnings) }
    }
}

@Composable
private fun InningsCardView(title: String, card: InningsScorecard) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$title • ${card.battingClubName}", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${card.runs}/${card.wickets} (${card.oversFormatted})",
                    style = TelemetryTextStyles.statValue,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            card.batters.take(7).forEach { b ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${b.playerName}${if (!b.isOut) "*" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "${b.runs}(${b.balls})",
                        style = TelemetryTextStyles.statSmall,
                        modifier = Modifier.weight(0.7f)
                    )
                    Text(
                        text = "SR ${"%.0f".format(b.strikeRate)}",
                        style = TelemetryTextStyles.statSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Bowling (${card.bowlingClubName})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            card.bowlers.forEach { bw ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(bw.playerName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
                    Text("${bw.oversText} ov", style = TelemetryTextStyles.statSmall, modifier = Modifier.weight(0.6f))
                    Text("${bw.wickets}/${bw.runsConceded}", style = TelemetryTextStyles.statSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
