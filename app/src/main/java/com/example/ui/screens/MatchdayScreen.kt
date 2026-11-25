package com.example.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MatchdayScreen(
    career: CareerState,
    liveMatch: LiveMatchState?,
    isAutoStreaming: Boolean,
    streamSpeedMs: Long,
    onToggleDelegation: (DelegationArea) -> Unit,
    onTogglePlayerInXI: (String) -> Unit,
    onSetImpactSub: (String) -> Unit,
    onAutoPickXI: () -> Unit,
    onSetDefaultTactics: (BattingTempo?, BowlingPlan?, FieldSpread?) -> Unit,
    onSetPreMatchTeamTalk: (TeamTalkTone) -> Unit = {},
    onStartMatch: (Boolean) -> Unit,
    onToggleAutoStream: () -> Unit,
    onCycleStreamSpeed: () -> Unit,
    onStepBalls: (Int) -> Unit,
    onAdvanceToMoment: () -> Unit,
    onApplyLiveTactics: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, TeamTalkTone?) -> Unit,
    onSubmitKeyMoment: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, Boolean, TeamTalkTone?) -> Unit,
    onDeliverPostMatchTalk: (TeamTalkTone) -> Unit = {},
    onFinishMatch: (TeamTalkTone?) -> Unit,
    onOpenPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (liveMatch != null) {
        if (liveMatch.finishedRecord != null) {
            PostMatchScorecardView(
                record = liveMatch.finishedRecord,
                deliveredTalk = liveMatch.postMatchTeamTalkDelivered,
                onDeliverPostMatchTalk = onDeliverPostMatchTalk,
                onContinue = onFinishMatch,
                modifier = modifier
            )
        } else {
            LiveMatchWarRoomView(
                live = liveMatch,
                isAutoStreaming = isAutoStreaming,
                streamSpeedMs = streamSpeedMs,
                onToggleAutoStream = onToggleAutoStream,
                onCycleStreamSpeed = onCycleStreamSpeed,
                onStepBalls = onStepBalls,
                onAdvanceToMoment = onAdvanceToMoment,
                onApplyLiveTactics = onApplyLiveTactics,
                onSubmitDecision = onSubmitKeyMoment,
                modifier = modifier
            )
        }
        return
    }

    var xiViewMode by remember { mutableStateOf(SquadViewMode.TILE) }

    val nextFixture = career.nextUserFixture
    val headCoachName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name
    val seniorAvailable = remember(career.squad) {
        career.squad.filter { it.isAvailableForMatch }.sortedByDescending { it.overall }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("matchday_setup_list"),
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
                            Text("Play Live Feed")
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

        // Pre-Match Dressing Room Team Talk
        item {
            TeamTalkSelectorCard(
                stage = TeamTalkStage.PRE_MATCH,
                selectedTone = career.preMatchTeamTalk,
                onSelectTone = onSetPreMatchTeamTalk,
                cardTag = "team_talk_prematch_card",
                chipPrefix = "prematch_talk"
            )
        }

        // Playing XI Selection with Tile / Table View Toggle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Playing XI (${career.selectedXIIds.size}/11)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Checkbox for XI • Star for Impact Sub",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Squad Display",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SquadViewModeToggle(
                        mode = xiViewMode,
                        onModeChange = { xiViewMode = it },
                        testTagPrefix = "xi"
                    )
                }
            }
        }

        if (xiViewMode == SquadViewMode.TILE) {
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
        } else {
            item {
                MatchdayXiTable(
                    players = seniorAvailable,
                    selectedXIIds = career.selectedXIIds,
                    impactSubId = career.impactSubId,
                    selectionDelegated = career.delegation.selectionDelegated,
                    onOpenPlayer = onOpenPlayer,
                    onSetImpactSub = onSetImpactSub,
                    onTogglePlayerInXI = onTogglePlayerInXI
                )
            }
        }
    }
}

@Composable
internal fun LiveMatchWarRoomView(
    live: LiveMatchState,
    isAutoStreaming: Boolean,
    streamSpeedMs: Long,
    onToggleAutoStream: () -> Unit,
    onCycleStreamSpeed: () -> Unit,
    onStepBalls: (Int) -> Unit,
    onAdvanceToMoment: () -> Unit,
    onApplyLiveTactics: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, TeamTalkTone?) -> Unit,
    onSubmitDecision: (BattingTempo?, BowlingPlan?, FieldSpread?, String?, Boolean, TeamTalkTone?) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompt = live.pendingKeyMoment
    var chosenTempo by remember(prompt?.momentIndex, live.currentTempo) {
        mutableStateOf(prompt?.recommendedTempo ?: live.currentTempo)
    }
    var chosenPlan by remember(prompt?.momentIndex, live.currentBowlingPlan) {
        mutableStateOf(prompt?.recommendedBowlingPlan ?: live.currentBowlingPlan)
    }
    var chosenField by remember(prompt?.momentIndex, live.currentFieldSpread) {
        mutableStateOf(prompt?.recommendedField ?: live.currentFieldSpread)
    }
    var chosenTalk by remember(prompt?.momentIndex, live.inningsBreakTeamTalk, live.preMatchTeamTalk) {
        mutableStateOf(
            prompt?.recommendedTeamTalk
                ?: if (prompt?.isInningsBreak == true) (live.inningsBreakTeamTalk ?: live.preMatchTeamTalk) else null
        )
    }
    var deploySub by remember(prompt?.momentIndex) { mutableStateOf(false) }
    var highlightsOnly by remember { mutableStateOf(false) }

    val displayedFeed = remember(live.ballFeed, highlightsOnly) {
        if (highlightsOnly) {
            live.ballFeed.filter { it.isWicket || it.isBoundary }
        } else {
            live.ballFeed
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWide = maxWidth >= 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_war_room_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Live Scoreboard Ribbon + Real-Time CRR vs RRR Graph
            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        LiveScoreboardCard(
                            live = live,
                            isAutoStreaming = isAutoStreaming,
                            modifier = Modifier.weight(1.05f)
                        )
                        RunRateComparisonGraphCard(
                            live = live,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }
            } else {
                item {
                    LiveScoreboardCard(
                        live = live,
                        isAutoStreaming = isAutoStreaming,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    RunRateComparisonGraphCard(
                        live = live,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        // 2. Real-Time Playback & Stepping Controls
        if (prompt == null) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onToggleAutoStream,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("toggle_live_stream_btn")
                            ) {
                                Icon(
                                    imageVector = if (isAutoStreaming) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAutoStreaming) "Pause Feed" else "Stream Live")
                            }
                            FilledTonalButton(
                                onClick = onCycleStreamSpeed,
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("cycle_stream_speed_btn")
                            ) {
                                Text(if (streamSpeedMs < 500L) "2x Speed" else "1x Speed")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onStepBalls(1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("step_one_ball_btn")
                            ) {
                                Text("+1 Ball")
                            }
                            OutlinedButton(
                                onClick = { onStepBalls(6) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("step_one_over_btn")
                            ) {
                                Text("+1 Over")
                            }
                            OutlinedButton(
                                onClick = onAdvanceToMoment,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("skip_to_moment_btn")
                            ) {
                                Text("Next Key")
                            }
                        }
                    }
                }
            }
        }

        // 3. Crease & Bowler Telemetry Strip (Stats & Fitness Levels)
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (live.matchupSummary.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = live.matchupSummary,
                                style = TelemetryTextStyles.statSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                    live.activeBatters.forEach { b ->
                        val fit = live.activeBatterFitness[b.playerId] ?: 88
                        val fitColor = when {
                            fit >= 78 -> PitchMint
                            fit >= 65 -> CautionOchre
                            else -> AlertCrimson
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${b.playerName} (${b.roleLabel})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(
                                text = "FIT $fit%",
                                style = TelemetryTextStyles.statSmall,
                                color = fitColor,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text("${b.runs} (${b.balls})", style = TelemetryTextStyles.statValue)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    live.activeBowler?.let { bw ->
                        val bFitColor = when {
                            live.activeBowlerFitness >= 78 -> PitchMint
                            live.activeBowlerFitness >= 65 -> CautionOchre
                            else -> AlertCrimson
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bowling: ${bw.playerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                            Text(
                                text = "FIT ${live.activeBowlerFitness}%",
                                style = TelemetryTextStyles.statSmall,
                                color = bFitColor,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text("${bw.wickets}/${bw.runsConceded} (${bw.oversText})", style = TelemetryTextStyles.statSmall)
                        }
                    }
                }
            }
        }

        // 4. Key Moment Tactical Intervention OR Always-On Live Tactical Desk
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
                    }
                }
            }

            if (prompt.isInningsBreak) {
                item {
                    val activeInningsTone = chosenTalk ?: live.preMatchTeamTalk
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_talk_innings_break_section")
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Innings-Break Team Talk",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = activeInningsTone.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(TeamTalkTone.entries) { tone ->
                                    FilterChip(
                                        selected = activeInningsTone == tone,
                                        onClick = { chosenTalk = tone },
                                        label = { Text(tone.label) },
                                        modifier = Modifier.testTag("innings_talk_${tone.name.lowercase()}")
                                    )
                                }
                            }
                            Text(
                                text = activeInningsTone.speechLine(TeamTalkStage.INNINGS_BREAK),
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeInningsTone.summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        val talkToCommit = if (prompt.isInningsBreak) {
                            chosenTalk ?: live.preMatchTeamTalk
                        } else {
                            chosenTalk
                        }
                        Button(
                            onClick = {
                                onSubmitDecision(
                                    if (prompt.isUserBatting) chosenTempo else null,
                                    if (!prompt.isUserBatting) chosenPlan else null,
                                    if (!prompt.isUserBatting) chosenField else null,
                                    if (deploySub) live.availableImpactSubs.firstOrNull()?.id else null,
                                    false,
                                    talkToCommit
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("resume_match_button")
                        ) {
                            Text("Apply & Stream")
                        }
                        OutlinedButton(
                            onClick = {
                                onSubmitDecision(
                                    if (prompt.isUserBatting) chosenTempo else null,
                                    if (!prompt.isUserBatting) chosenPlan else null,
                                    if (!prompt.isUserBatting) chosenField else null,
                                    if (deploySub) live.availableImpactSubs.firstOrNull()?.id else null,
                                    true,
                                    talkToCommit
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
        } else {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (live.userIsBattingNow) "Live Batting Tempo" else "Live Bowling & Field Tactics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (live.userIsBattingNow) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BattingTempo.entries) { tempo ->
                                    FilterChip(
                                        selected = live.currentTempo == tempo,
                                        onClick = { onApplyLiveTactics(tempo, null, null, null, null) },
                                        label = { Text(tempo.label) }
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(BowlingPlan.entries) { plan ->
                                    FilterChip(
                                        selected = live.currentBowlingPlan == plan,
                                        onClick = { onApplyLiveTactics(null, plan, null, null, null) },
                                        label = { Text(plan.label) }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(FieldSpread.entries) { field ->
                                    FilterChip(
                                        selected = live.currentFieldSpread == field,
                                        onClick = { onApplyLiveTactics(null, null, field, null, null) },
                                        label = { Text(field.label) }
                                    )
                                }
                            }
                        }
                        if (live.availableImpactSubs.isNotEmpty() && !live.impactSubUsed) {
                            val sub = live.availableImpactSubs.first()
                            FilledTonalButton(
                                onClick = { onApplyLiveTactics(null, null, null, sub.id, null) },
                                modifier = Modifier.testTag("live_deploy_sub_btn")
                            ) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Deploy Impact Sub: ${sub.name} (${sub.role.shortLabel})")
                            }
                        }
                    }
                }
            }
        }

        // 5. Real-Time Ball-by-Ball Text Feed Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Real-Time Ball-by-Ball Feed (${live.ballFeed.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                FilterChip(
                    selected = highlightsOnly,
                    onClick = { highlightsOnly = !highlightsOnly },
                    label = { Text(if (highlightsOnly) "Key Events" else "All Balls") },
                    modifier = Modifier.testTag("toggle_feed_highlights_btn")
                )
            }
        }

        if (displayedFeed.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = if (highlightsOnly) "No boundaries or wickets in this spell yet." else "Press Stream Live or +1 Ball to begin ball-by-ball commentary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }
        } else {
            items(displayedFeed, key = { it.id }) { ballEvent ->
                BallEventFeedRow(event = ballEvent)
            }
        }

        if (live.commentaryLog.isNotEmpty()) {
            item {
                Text("Match Milestones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(live.commentaryLog.take(6)) { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
}

@Composable
private fun LiveScoreboardCard(
    live: LiveMatchState,
    isAutoStreaming: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${if (live.userIsBattingNow) "BATTING" else "BOWLING"} • INN ${live.currentInnings}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isAutoStreaming) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PitchMint.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = PitchMint,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                val activeTalk = if (live.currentInnings >= 2 && live.inningsBreakTeamTalk != null) {
                    live.inningsBreakTeamTalk
                } else {
                    live.preMatchTeamTalk
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("live_active_team_talk_badge")
                    ) {
                        Text(
                            text = "Talk: ${activeTalk.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = live.fixture.pitchType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

@Composable
private fun BallEventFeedRow(event: BallEvent) {
    val badgeBg = when (event.symbol) {
        "W" -> AlertCrimson
        "6" -> BoundaryAmber
        "4" -> PitchMint
        "Wd" -> CautionOchre
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val badgeFg = if (event.symbol in listOf("W", "6", "4", "Wd")) Color.Black else MaterialTheme.colorScheme.onSurface
    val cardBg = when {
        event.isWicket -> AlertCrimson.copy(alpha = 0.12f)
        event.symbol == "6" -> BoundaryAmber.copy(alpha = 0.10f)
        event.symbol == "4" -> PitchMint.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ball_event_${event.id}"),
        shape = MaterialTheme.shapes.small,
        color = cardBg,
        border = if (event.isWicket || event.isBoundary) {
            BorderStroke(1.dp, badgeBg.copy(alpha = 0.45f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = event.symbol,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeFg
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "I${event.innings} ${event.overBallText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = event.commentary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = event.scoreAfterBall,
                    style = TelemetryTextStyles.statSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PostMatchScorecardView(
    record: MatchRecord,
    deliveredTalk: TeamTalkTone?,
    onDeliverPostMatchTalk: (TeamTalkTone) -> Unit,
    onContinue: (TeamTalkTone?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTalk by remember(record.matchId, deliveredTalk) {
        mutableStateOf(
            deliveredTalk ?: if (record.userWon) TeamTalkTone.ENCOURAGING else TeamTalkTone.CALM
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("post_match_scorecard_list"),
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
                        onClick = { onContinue(if (deliveredTalk == null) selectedTalk else null) },
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

        // Post-Match Dressing Room Team Talk
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_talk_postmatch_card"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
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
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Post-Match Team Talk",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (deliveredTalk != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PitchMint.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "Delivered: ${deliveredTalk.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchMint,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TeamTalkTone.entries) { tone ->
                            FilterChip(
                                selected = selectedTalk == tone,
                                enabled = deliveredTalk == null,
                                onClick = { selectedTalk = tone },
                                label = { Text(tone.label) },
                                modifier = Modifier.testTag("postmatch_talk_${tone.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = selectedTalk.speechLine(TeamTalkStage.POST_MATCH, wonMatch = record.userWon),
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val winMoraleAdj = if (record.userWon) selectedTalk.moraleDelta else (selectedTalk.moraleDelta - 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Squad Impact: Morale +$winMoraleAdj • Form +${selectedTalk.formDelta}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        FilledTonalButton(
                            onClick = { onDeliverPostMatchTalk(selectedTalk) },
                            enabled = deliveredTalk == null,
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("deliver_post_match_talk_btn")
                        ) {
                            Text(if (deliveredTalk == null) "Deliver Speech" else "Delivered")
                        }
                    }
                }
            }
        }

        item { InningsCardView("1st Innings", record.firstInnings) }
        item { InningsCardView("2nd Innings", record.secondInnings) }
    }
}

@Composable
private fun TeamTalkSelectorCard(
    stage: TeamTalkStage,
    selectedTone: TeamTalkTone,
    onSelectTone: (TeamTalkTone) -> Unit,
    cardTag: String,
    chipPrefix: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(cardTag),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = selectedTone.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(TeamTalkTone.entries) { tone ->
                    FilterChip(
                        selected = selectedTone == tone,
                        onClick = { onSelectTone(tone) },
                        label = { Text(tone.label) },
                        modifier = Modifier.testTag("${chipPrefix}_${tone.name.lowercase()}")
                    )
                }
            }

            Text(
                text = selectedTone.speechLine(stage),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedTone.summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MatchdayXiTable(
    players: List<Player>,
    selectedXIIds: List<String>,
    impactSubId: String?,
    selectionDelegated: Boolean,
    onOpenPlayer: (String) -> Unit,
    onSetImpactSub: (String) -> Unit,
    onTogglePlayerInXI: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("matchday_xi_table"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Player", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2.0f))
                Text("OVR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
                Text("Frm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
                Text("Sub", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
                Text("XI", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(0.6f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            players.forEachIndexed { idx, player ->
                val inXI = selectedXIIds.contains(player.id)
                val isSub = impactSubId == player.id
                val rowBg = when {
                    inXI -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    idx % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("xi_table_row_${player.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(2.0f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenPlayer(player.id) }
                            .padding(vertical = 3.dp, horizontal = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${player.role.shortLabel} • Fit ${player.fitness}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (player.fitness < 72) CautionOchre else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${player.overall}",
                        style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.55f)
                    )
                    Text(
                        text = "${player.form}",
                        style = TelemetryTextStyles.statSmall,
                        color = if (player.form >= 75) PitchMint else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.55f)
                    )
                    Box(
                        modifier = Modifier.weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!inXI) {
                            IconButton(
                                onClick = { onSetImpactSub(player.id) },
                                enabled = !selectionDelegated,
                                modifier = Modifier.testTag("xi_table_sub_${player.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSub) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Set Impact Sub",
                                    tint = if (isSub) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = inXI,
                            enabled = !selectionDelegated,
                            onCheckedChange = { onTogglePlayerInXI(player.id) },
                            modifier = Modifier.testTag("xi_table_checkbox_${player.id}")
                        )
                    }
                }
                if (idx < players.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
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
