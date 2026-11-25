package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.InjuryRiskTier
import com.example.simulation.MedicalRoomEngine
import com.example.simulation.PlayerMedicalAssessment
import com.example.ui.theme.*

private enum class MedicalMonitorFilter(val label: String) {
    ALL_ACTIVE("All Active"),
    HIGH_RISK("High Risk"),
    FATIGUED("Fatigued (<75%)")
}

@Composable
fun MedicalRoomSection(
    career: CareerState,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onRestPlayerRecovery: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    var monitorFilter by remember { mutableStateOf(MedicalMonitorFilter.ALL_ACTIVE) }

    val (summary, assessments) = remember(
        career.squad,
        career.staff,
        career.budgets.medicalLevel,
        career.trainingIntensity,
        career.selectedXIIds
    ) {
        MedicalRoomEngine.assessSquad(career)
    }

    val injuredAssessments = remember(assessments) {
        assessments.filter { it.player.injury.isInjured }
    }
    val activeAssessments = remember(assessments) {
        assessments.filter { !it.player.injury.isInjured }
    }
    val filteredActive = remember(activeAssessments, monitorFilter) {
        when (monitorFilter) {
            MedicalMonitorFilter.ALL_ACTIVE -> activeAssessments
            MedicalMonitorFilter.HIGH_RISK -> activeAssessments.filter {
                it.riskTier == InjuryRiskTier.HIGH || it.riskTier == InjuryRiskTier.CRITICAL
            }
            MedicalMonitorFilter.FATIGUED -> activeAssessments.filter { it.player.fitness < 75 }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("medical_room_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Physio & Sports Science Overview Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_room_overview"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalHospital,
                                    contentDescription = "Medical Room",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Medical Room & Sports Science",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${summary.physioName} (${summary.physioRating}) • Medical Lvl ${summary.medicalLevel}/5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (summary.fitnessCoachBonusPct > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PitchMint.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "+${summary.fitnessCoachBonusPct}% Prevention",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchMint,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MedicalSummaryStat(
                            label = "Injured",
                            value = "${summary.injuredCount}",
                            accent = if (summary.injuredCount > 0) AlertCrimson else PitchMint,
                            modifier = Modifier.weight(1f)
                        )
                        MedicalSummaryStat(
                            label = "High Risk",
                            value = "${summary.highRiskCount}",
                            accent = if (summary.highRiskCount > 0) CautionOchre else PitchMint,
                            modifier = Modifier.weight(1f)
                        )
                        MedicalSummaryStat(
                            label = "Avg Load",
                            value = "${summary.avgWorkloadPct}%",
                            accent = TelemetryCyan80,
                            modifier = Modifier.weight(1f)
                        )
                        MedicalSummaryStat(
                            label = "Avg Fit",
                            value = "${summary.avgFitnessPct}%",
                            accent = if (summary.avgFitnessPct < 75) CautionOchre else PitchMint,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Rehab Delegation Banner
        item {
            DelegationBanner(
                area = DelegationArea.REHAB,
                isDelegated = career.delegation.rehabDelegated,
                staffOwnerName = summary.physioName,
                onToggle = { onToggleDelegation(DelegationArea.REHAB) }
            )
        }

        // 3. Squad Training Intensity Selector Card
        item {
            val activeIntensity = career.trainingIntensity
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_training_intensity_card"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (activeIntensity == TrainingIntensity.INTENSE) AlertCrimson.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
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
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = "Training Intensity",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Squad Training Intensity",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = "Growth x${"%.2f".format(activeIntensity.growthMultiplier)} • Load x${"%.2f".format(activeIntensity.workloadFactor)}",
                            style = TelemetryTextStyles.statSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TrainingIntensity.entries) { intensity ->
                            FilterChip(
                                selected = activeIntensity == intensity,
                                onClick = { onSetTrainingIntensity(intensity) },
                                label = { Text(intensity.label) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("medical_intensity_${intensity.name.lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = activeIntensity.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Active Rehab Ward (Injured Players)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Rehab Ward (${injuredAssessments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (injuredAssessments.isNotEmpty()) AlertCrimson else MaterialTheme.colorScheme.onSurface
                )
                if (injuredAssessments.isEmpty()) {
                    Text(
                        text = "Treatment table clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = PitchMint
                    )
                }
            }
        }

        if (injuredAssessments.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medical_no_injuries_card"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "All players fit",
                            tint = PitchMint
                        )
                        Text(
                            text = "Zero active injuries. Monitor training load and workload percentages below to prevent soft-tissue breakdowns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(injuredAssessments, key = { "med_rehab_${it.player.id}" }) { assessment ->
                InjuredRehabCard(
                    assessment = assessment,
                    rehabDelegated = career.delegation.rehabDelegated,
                    onSetRehab = { mode -> onSetRehab(assessment.player.id, mode) },
                    onClick = { onOpenPlayer(assessment.player.id) }
                )
            }
        }

        // 5. Squad Injury Risk & Workload Monitor
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Injury Probability & Workload Monitor",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MedicalMonitorFilter.entries) { filter ->
                        FilterChip(
                            selected = monitorFilter == filter,
                            onClick = { monitorFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("medical_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredActive.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "No players match the ${monitorFilter.label} filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredActive, key = { "med_card_${it.player.id}" }) { assessment ->
                MedicalWorkloadCard(
                    assessment = assessment,
                    onRestPlayerRecovery = { onRestPlayerRecovery(assessment.player.id) },
                    onClick = { onOpenPlayer(assessment.player.id) }
                )
            }
        }
    }
}

@Composable
private fun MedicalSummaryStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = TelemetryTextStyles.statValue.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent
                ),
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InjuredRehabCard(
    assessment: PlayerMedicalAssessment,
    rehabDelegated: Boolean,
    onSetRehab: (RehabMode) -> Unit,
    onClick: () -> Unit
) {
    val player = assessment.player
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("medical_rehab_card_${player.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerAvatar(player = player, sizeDp = 44, showOvrBadge = true)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(player.name, style = MaterialTheme.typography.titleSmall)
                        RoleChip(player.role)
                    }
                    Text(
                        text = "${player.injury.injuryName} • ${player.injury.weeksRemaining}w remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = AlertCrimson
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Est. ~${"%.1f".format(assessment.estimatedRecoveryWeeks)} wks",
                            style = TelemetryTextStyles.statSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "${assessment.setbackRiskPct}% setback risk",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (assessment.setbackRiskPct >= 15) AlertCrimson else CautionOchre
                        )
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(RehabMode.CONSERVATIVE, RehabMode.BALANCED, RehabMode.ACCELERATED)) { mode ->
                    val shortTitle = when (mode) {
                        RehabMode.CONSERVATIVE -> "Conservative"
                        RehabMode.BALANCED -> "Balanced"
                        RehabMode.ACCELERATED -> "Accelerated"
                        RehabMode.NONE -> "None"
                    }
                    FilterChip(
                        selected = player.injury.rehabMode == mode,
                        enabled = !rehabDelegated,
                        onClick = { onSetRehab(mode) },
                        label = { Text(shortTitle) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rehab_mode_${player.id}_${mode.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicalWorkloadCard(
    assessment: PlayerMedicalAssessment,
    onRestPlayerRecovery: () -> Unit,
    onClick: () -> Unit
) {
    val player = assessment.player
    val riskColor = when (assessment.riskTier) {
        InjuryRiskTier.LOW -> PitchMint
        InjuryRiskTier.MODERATE -> TelemetryCyan80
        InjuryRiskTier.HIGH -> CautionOchre
        InjuryRiskTier.CRITICAL -> AlertCrimson
    }
    val fitnessColor = when {
        player.fitness >= 78 -> PitchMint
        player.fitness >= 62 -> CautionOchre
        else -> AlertCrimson
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("medical_card_${player.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (assessment.riskTier == InjuryRiskTier.CRITICAL || assessment.riskTier == InjuryRiskTier.HIGH) {
            BorderStroke(1.dp, riskColor.copy(alpha = 0.45f))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlayerAvatar(player = player, sizeDp = 42, showOvrBadge = true)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RoleChip(player.role)
                    }
                    Text(
                        text = "FIT ${player.fitness}% • Age ${player.age} • ${player.trainingFocus.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = fitnessColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, riskColor.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${assessment.injuryProbabilityPct}% Risk",
                            style = TelemetryTextStyles.statSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = riskColor
                            )
                        )
                        Text(
                            text = assessment.riskTier.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = riskColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WorkloadBar(
                    label = "Match Workload",
                    percent = assessment.matchWorkloadScore,
                    accent = if (assessment.matchWorkloadScore >= 70) CautionOchre else TelemetryCyan80,
                    modifier = Modifier.weight(1f)
                )
                WorkloadBar(
                    label = "Training Load",
                    percent = assessment.trainingLoadScore,
                    accent = if (assessment.trainingLoadScore >= 70) AlertCrimson else FloodlightGold80,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assessment.medicalNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (player.trainingFocus != TrainingFocus.FITNESS_RECOVERY) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onRestPlayerRecovery,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("medical_recovery_btn_${player.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Healing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Recovery Drill")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadBar(
    label: String,
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percent%",
                style = TelemetryTextStyles.statSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            )
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}
