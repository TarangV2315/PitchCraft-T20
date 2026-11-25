package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.StaffAdvisorEngine
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActivityScreen(
    player: Player,
    career: CareerState,
    onBack: () -> Unit,
    onApplyRecommendation: (Player, StaffRecommendation) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onPromoteYouth: (String) -> Unit,
    onOpenRenewal: (Player) -> Unit,
    onToggleTransferList: (String) -> Unit,
    onSellPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val recommendations = remember(player, career.selectedXIIds, career.staff) {
        StaffAdvisorEngine.buildPlayerRecommendations(player, career)
    }
    var showScreenGuide by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Player Telemetry", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TopLeftInfoButton(onClick = { showScreenGuide = true })
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("player_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Identity Hero Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(
                                player = player,
                                sizeDp = 64,
                                showOvrBadge = false
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(player.name, style = MaterialTheme.typography.headlineSmall)
                                    RoleChip(player.role)
                                }
                                Text(
                                    text = "${player.nationality} • ${player.age} yrs • ${player.battingHand.label} • ${player.bowlingStyle.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${player.overall}",
                                        style = TelemetryTextStyles.scoreboardLarge.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "POT ${player.potential}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Fitness",
                                value = if (player.injury.isInjured) "INJ ${player.injury.weeksRemaining}w" else "${player.fitness}%",
                                accentColor = if (player.injury.isInjured) AlertCrimson else PitchMint,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Form",
                                value = "${player.form}",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Morale",
                                value = "${player.morale}%",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Latest Activity: ${player.lastMatchNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Contextual Staff Recommendations Section (Core Requirement!)
            item {
                Text(
                    text = "Staff Recommendations (${recommendations.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (recommendations.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PitchMint)
                            Text(
                                text = "Coaching & medical staff report ${player.name} is on an optimal plan right now.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recommendations, key = { it.id }) { rec ->
                    StaffRecommendationCard(
                        rec = rec,
                        onApply = { onApplyRecommendation(player, rec) }
                    )
                }
            }

            // Skill Telemetry Bars
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("T20 Skill Profile", style = MaterialTheme.typography.titleMedium)
                        AttributeBar("Batting Timing", player.attributes.battingTiming, MaterialTheme.colorScheme.primary)
                        AttributeBar("Power Hitting", player.attributes.powerHitting, MaterialTheme.colorScheme.secondary)
                        AttributeBar("Pace & Seam", player.attributes.paceBowling, MaterialTheme.colorScheme.tertiary)
                        AttributeBar("Spin & Drift", player.attributes.spinVariation, PitchMint)
                        AttributeBar("Death Execution", player.attributes.deathExecution, CautionOchre)
                        AttributeBar("Fielding & Reflex", player.attributes.fielding, MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Season & Career Performance Tracking
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("T20 Performance Tracker", style = MaterialTheme.typography.titleMedium)
                        StatsComparisonRow("Matches", "${player.seasonStats.matches}", "${player.careerStats.matches}")
                        StatsComparisonRow("Runs (HS)", "${player.seasonStats.runs} (${player.seasonStats.highScore})", "${player.careerStats.runs} (${player.careerStats.highScore})")
                        StatsComparisonRow("Bat Avg / SR", "${"%.1f".format(player.seasonStats.battingAverage)} / ${"%.1f".format(player.seasonStats.strikeRate)}", "${"%.1f".format(player.careerStats.battingAverage)} / ${"%.1f".format(player.careerStats.strikeRate)}")
                        StatsComparisonRow("4s / 6s / 50s", "${player.seasonStats.fours} / ${player.seasonStats.sixes} / ${player.seasonStats.fifties}", "${player.careerStats.fours} / ${player.careerStats.sixes} / ${player.careerStats.fifties}")
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        StatsComparisonRow("Overs / Wkts", "${player.seasonStats.oversFormatted} / ${player.seasonStats.wickets}", "${player.careerStats.oversFormatted} / ${player.careerStats.wickets}")
                        StatsComparisonRow("Econ / Best", "${"%.2f".format(player.seasonStats.economyRate)} / ${player.seasonStats.bestBowlingFormatted}", "${"%.2f".format(player.careerStats.economyRate)} / ${player.careerStats.bestBowlingFormatted}")
                    }
                }
            }

            // Direct Activity Controls (Training / Rehab / Contract / Transfer)
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Player Management Controls", style = MaterialTheme.typography.titleMedium)

                        if (player.injury.isInjured) {
                            Text(
                                text = "Rehab Plan (${player.injury.injuryName} — ${player.injury.weeksRemaining}w)",
                                style = MaterialTheme.typography.labelLarge,
                                color = AlertCrimson
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(RehabMode.CONSERVATIVE, RehabMode.BALANCED, RehabMode.ACCELERATED)) { mode ->
                                    FilterChip(
                                        selected = player.injury.rehabMode == mode,
                                        enabled = !career.delegation.rehabDelegated,
                                        onClick = { onSetRehab(player.id, mode) },
                                        label = { Text("${mode.label} (${mode.setbackRiskPercent}% risk)") }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (career.delegation.trainingDelegated) "Training Drill (Delegated to Head Coach)"
                                else "Training Drill",
                                style = MaterialTheme.typography.labelLarge
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(TrainingFocus.entries) { drill ->
                                    FilterChip(
                                        selected = player.trainingFocus == drill,
                                        enabled = !career.delegation.trainingDelegated,
                                        onClick = { onSetTraining(player.id, drill) },
                                        label = { Text(drill.label) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Contract", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${formatCurrency(player.contract.weeklyWage)}/wk • ${player.contract.weeksRemaining}w left",
                                    style = TelemetryTextStyles.statValue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Market Value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = formatCurrency(player.marketValue),
                                    style = TelemetryTextStyles.statValue.copy(color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        if (player.isYouthAcademy) {
                            Button(
                                onClick = { onPromoteYouth(player.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Upgrade, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Promote to Senior Squad")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenRenewal(player) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("renew_contract_btn")
                                ) {
                                    Text("Renew Deal")
                                }
                                OutlinedButton(
                                    onClick = { onToggleTransferList(player.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (player.isOnTransferList) "Unlist" else "Transfer List")
                                }
                            }
                            if (player.isOnTransferList) {
                                FilledTonalButton(
                                    onClick = { onSellPlayer(player.id) },
                                    enabled = career.isTransferWindowOpen,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (career.isTransferWindowOpen) {
                                            "Accept Market Bid (${formatCompactCurrency((player.marketValue * 0.85).toInt())})"
                                        } else {
                                            "Sale Locked • Transfer Window Closed"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScreenGuide) {
        ScreenGuideDialog(
            initialScreenId = ScreenGuideId.PLAYER_ACTIVITY,
            onDismiss = { showScreenGuide = false }
        )
    }
}

@Composable
private fun StatsComparisonRow(
    label: String,
    seasonValue: String,
    careerValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.1f))
        Text("S: $seasonValue", style = TelemetryTextStyles.statSmall, modifier = Modifier.weight(1f))
        Text("Car: $careerValue", style = TelemetryTextStyles.statSmall.copy(color = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f))
    }
}
