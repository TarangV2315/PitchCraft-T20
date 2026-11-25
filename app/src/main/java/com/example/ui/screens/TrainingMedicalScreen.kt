package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.InjuryRiskTier
import com.example.simulation.MedicalRoomEngine
import com.example.simulation.PlayerMedicalAssessment
import com.example.ui.components.DelegationBanner
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.RoleChip
import com.example.ui.components.SquadViewModeToggle
import com.example.ui.theme.*

enum class TrainingMedicalSubTab(val label: String) {
    TRAINING("Training"),
    MEDICAL("Medical")
}

@Composable
fun TrainingMedicalScreen(
    career: CareerState,
    activeSubTab: TrainingMedicalSubTab,
    onSelectSubTab: (TrainingMedicalSubTab) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    var trainingViewMode by remember { mutableStateOf(SquadViewMode.TABLE) }
    var medicalViewMode by remember { mutableStateOf(SquadViewMode.TABLE) }

    val (medicalSummary, assessments) = remember(
        career.squad,
        career.staff,
        career.budgets.medicalLevel,
        career.trainingIntensity,
        career.selectedXIIds
    ) {
        MedicalRoomEngine.assessSquad(career)
    }

    val assessmentByPlayerId = remember(assessments) {
        assessments.associateBy { it.player.id }
    }

    val seniorPlayers = remember(career.squad) {
        career.squad.filter { !it.isYouthAcademy }.sortedByDescending { it.overall }
    }

    val headCoachName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name
    val physioName = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("training_medical_screen")
    ) {
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            TrainingMedicalSubTab.entries.forEach { tab ->
                Tab(
                    selected = activeSubTab == tab,
                    onClick = { onSelectSubTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("tm_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            TrainingMedicalSubTab.TRAINING -> {
                TrainingTabularView(
                    career = career,
                    seniorPlayers = seniorPlayers,
                    assessmentByPlayerId = assessmentByPlayerId,
                    headCoachName = headCoachName,
                    viewMode = trainingViewMode,
                    onViewModeChange = { trainingViewMode = it },
                    onOpenPlayer = onOpenPlayer,
                    onSetTraining = onSetTraining,
                    onSetTrainingIntensity = onSetTrainingIntensity,
                    onToggleDelegation = onToggleDelegation
                )
            }

            TrainingMedicalSubTab.MEDICAL -> {
                MedicalTabularView(
                    career = career,
                    summary = medicalSummary,
                    assessments = assessments,
                    physioName = physioName,
                    viewMode = medicalViewMode,
                    onViewModeChange = { medicalViewMode = it },
                    onOpenPlayer = onOpenPlayer,
                    onSetRehab = onSetRehab,
                    onSetTraining = onSetTraining,
                    onToggleDelegation = onToggleDelegation
                )
            }
        }
    }
}

@Composable
private fun TrainingTabularView(
    career: CareerState,
    seniorPlayers: List<Player>,
    assessmentByPlayerId: Map<String, PlayerMedicalAssessment>,
    headCoachName: String?,
    viewMode: SquadViewMode,
    onViewModeChange: (SquadViewMode) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("training_tabular_view"),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Squad Training Intensity + Delegation Controls
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrainingIntensityCard(
                            trainingIntensity = career.trainingIntensity,
                            onSetTrainingIntensity = onSetTrainingIntensity,
                            modifier = Modifier.weight(1.3f)
                        )
                        DelegationBanner(
                            area = DelegationArea.TRAINING,
                            isDelegated = career.delegation.trainingDelegated,
                            staffOwnerName = headCoachName,
                            onToggle = { onToggleDelegation(DelegationArea.TRAINING) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrainingIntensityCard(
                            trainingIntensity = career.trainingIntensity,
                            onSetTrainingIntensity = onSetTrainingIntensity,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DelegationBanner(
                            area = DelegationArea.TRAINING,
                            isDelegated = career.delegation.trainingDelegated,
                            staffOwnerName = headCoachName,
                            onToggle = { onToggleDelegation(DelegationArea.TRAINING) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Header + Tile/Table Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Individual Training (${seniorPlayers.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Adjust focus drills directly in Tile or Table view",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SquadViewModeToggle(
                        mode = viewMode,
                        onModeChange = onViewModeChange,
                        testTagPrefix = "training"
                    )
                }
            }

            if (viewMode == SquadViewMode.TABLE) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("training_table"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TmHeaderCell("Player", Modifier.weight(if (isWide) 2.0f else 1.75f), TextAlign.Start)
                                if (isWide) {
                                    TmHeaderCell("Role", Modifier.weight(0.75f), TextAlign.Center)
                                    TmHeaderCell("OVR", Modifier.weight(0.6f), TextAlign.End)
                                }
                                TmHeaderCell("Fit%", Modifier.weight(0.65f), TextAlign.End)
                                TmHeaderCell("Risk", Modifier.weight(0.65f), TextAlign.End)
                                TmHeaderCell("Training Focus", Modifier.weight(if (isWide) 2.15f else 1.75f), TextAlign.End)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            seniorPlayers.forEachIndexed { idx, player ->
                                val assessment = assessmentByPlayerId[player.id]
                                val riskPct = assessment?.injuryProbabilityPct ?: 0
                                val riskTier = assessment?.riskTier ?: InjuryRiskTier.LOW
                                val rowBg = if (idx % 2 == 1) {
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
                                } else {
                                    Color.Transparent
                                }
                                val canEditDrill = !career.delegation.trainingDelegated && !player.injury.isInjured

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("training_row_${player.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(if (isWide) 2.0f else 1.75f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onOpenPlayer(player.id) }
                                            .padding(vertical = 3.dp, horizontal = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (career.selectedXIIds.contains(player.id)) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = "XI",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (!isWide) {
                                            Text(
                                                text = "${player.role.shortLabel} • OVR ${player.overall}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    if (isWide) {
                                        Box(
                                            modifier = Modifier.weight(0.75f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RoleChip(player.role)
                                        }

                                        Text(
                                            text = "${player.overall}",
                                            style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.6f)
                                        )
                                    }

                                    Text(
                                        text = if (player.injury.isInjured) "INJ" else "${player.fitness}%",
                                        style = TelemetryTextStyles.statSmall.copy(
                                            color = when {
                                                player.injury.isInjured -> AlertCrimson
                                                player.fitness < 72 -> CautionOchre
                                                else -> PitchMint
                                            }
                                        ),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.65f)
                                    )

                                    Text(
                                        text = if (player.injury.isInjured) "Ward" else "$riskPct%",
                                        style = TelemetryTextStyles.statSmall.copy(
                                            color = if (player.injury.isInjured) AlertCrimson else riskTierColor(riskTier)
                                        ),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.65f)
                                    )

                                    Box(
                                        modifier = Modifier.weight(if (isWide) 2.15f else 1.75f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        TrainingFocusSelectorButton(
                                            player = player,
                                            canEditDrill = canEditDrill,
                                            onSelectFocus = { drill -> onSetTraining(player.id, drill) }
                                        )
                                    }
                                }
                                if (idx < seniorPlayers.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            } else {
                val columns = if (isWide) 2 else 1
                itemsIndexed(
                    seniorPlayers.chunked(columns),
                    key = { _, row -> "tr_tile_row_${row.first().id}" }
                ) { _, chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("training_tiles_view"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { player ->
                            val assessment = assessmentByPlayerId[player.id]
                            val riskPct = assessment?.injuryProbabilityPct ?: 0
                            val riskTier = assessment?.riskTier ?: InjuryRiskTier.LOW
                            val canEditDrill = !career.delegation.trainingDelegated && !player.injury.isInjured

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onOpenPlayer(player.id) }
                                    .testTag("training_tile_${player.id}"),
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PlayerAvatar(player = player, sizeDp = 42, showOvrBadge = true)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = player.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                RoleChip(player.role)
                                            }
                                            Text(
                                                text = if (player.injury.isInjured) {
                                                    "In Rehab (${player.injury.weeksRemaining}w)"
                                                } else {
                                                    "Fit ${player.fitness}% • Risk $riskPct% • Form ${player.form}"
                                                },
                                                style = TelemetryTextStyles.statSmall,
                                                color = if (player.injury.isInjured) AlertCrimson else riskTierColor(riskTier),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = player.trainingFocus.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        TrainingFocusSelectorButton(
                                            player = player,
                                            canEditDrill = canEditDrill,
                                            onSelectFocus = { drill -> onSetTraining(player.id, drill) }
                                        )
                                    }
                                }
                            }
                        }
                        if (columns == 2 && chunk.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingIntensityCard(
    trainingIntensity: TrainingIntensity,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Squad Training Intensity",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${trainingIntensity.growthMultiplier}x Growth",
                    style = TelemetryTextStyles.statSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrainingIntensity.entries.forEach { level ->
                    FilterChip(
                        selected = trainingIntensity == level,
                        onClick = { onSetTrainingIntensity(level) },
                        label = { Text(level.label) },
                        modifier = Modifier.testTag("tm_intensity_${level.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingFocusSelectorButton(
    player: Player,
    canEditDrill: Boolean,
    onSelectFocus: (TrainingFocus) -> Unit
) {
    var menuExpanded by remember(player.id) { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { menuExpanded = true },
            enabled = canEditDrill,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
                .heightIn(min = 38.dp)
                .testTag("training_focus_btn_${player.id}")
        ) {
            Text(
                text = if (player.injury.isInjured) {
                    "Rehab (${player.injury.weeksRemaining}w)"
                } else {
                    player.trainingFocus.label
                },
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (canEditDrill) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Change drill",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            TrainingFocus.entries.forEach { drill ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = drill.label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (player.trainingFocus == drill) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            Text(
                                text = drill.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (player.trainingFocus == drill) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onSelectFocus(drill)
                    },
                    modifier = Modifier.testTag("training_drill_option_${player.id}_${drill.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun MedicalTabularView(
    career: CareerState,
    summary: com.example.simulation.SquadMedicalSummary,
    assessments: List<PlayerMedicalAssessment>,
    physioName: String?,
    viewMode: SquadViewMode,
    onViewModeChange: (SquadViewMode) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit
) {
    val orderedAssessments = remember(assessments) {
        assessments.sortedWith(
            compareByDescending<PlayerMedicalAssessment> { it.player.injury.isInjured }
                .thenByDescending { it.injuryProbabilityPct }
                .thenBy { it.player.fitness }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("medical_tabular_view"),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Physio Summary + Rehab Delegation Header
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedicalSummaryHeaderCard(
                            summary = summary,
                            isCompact = false,
                            modifier = Modifier.weight(1.3f)
                        )
                        DelegationBanner(
                            area = DelegationArea.REHAB,
                            isDelegated = career.delegation.rehabDelegated,
                            staffOwnerName = physioName,
                            onToggle = { onToggleDelegation(DelegationArea.REHAB) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedicalSummaryHeaderCard(
                            summary = summary,
                            isCompact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        DelegationBanner(
                            area = DelegationArea.REHAB,
                            isDelegated = career.delegation.rehabDelegated,
                            staffOwnerName = physioName,
                            onToggle = { onToggleDelegation(DelegationArea.REHAB) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Title + Tile/Table Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Medical & Rehab Roster (${orderedAssessments.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Assign rehab intensity or recovery rest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SquadViewModeToggle(
                        mode = viewMode,
                        onModeChange = onViewModeChange,
                        testTagPrefix = "medical"
                    )
                }
            }

            if (viewMode == SquadViewMode.TABLE) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medical_table"),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TmHeaderCell("Player", Modifier.weight(if (isWide) 1.9f else 1.75f), TextAlign.Start)
                                if (isWide) {
                                    TmHeaderCell("Status", Modifier.weight(1.35f), TextAlign.Start)
                                    TmHeaderCell("Fit %", Modifier.weight(0.65f), TextAlign.End)
                                    TmHeaderCell("Load %", Modifier.weight(0.75f), TextAlign.End)
                                    TmHeaderCell("Risk", Modifier.weight(0.7f), TextAlign.End)
                                    TmHeaderCell("ETA", Modifier.weight(0.65f), TextAlign.End)
                                } else {
                                    TmHeaderCell("Fit/Load", Modifier.weight(0.95f), TextAlign.End)
                                    TmHeaderCell("Risk", Modifier.weight(0.65f), TextAlign.End)
                                }
                                TmHeaderCell("Action", Modifier.weight(if (isWide) 2.1f else 1.7f), TextAlign.End)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            orderedAssessments.forEachIndexed { idx, assessment ->
                                val player = assessment.player
                                val rowBg = when {
                                    player.injury.isInjured -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                                    idx % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("medical_row_${player.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(if (isWide) 1.9f else 1.75f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onOpenPlayer(player.id) }
                                            .padding(vertical = 3.dp, horizontal = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isWide) {
                                                RoleChip(player.role)
                                            }
                                        }
                                        if (!isWide) {
                                            val statusSub = if (player.injury.isInjured) {
                                                "${player.injury.injuryName} (${assessment.estimatedRecoveryWeeks}w)"
                                            } else if (player.trainingFocus == TrainingFocus.FITNESS_RECOVERY) {
                                                "${player.role.shortLabel} • Recovery"
                                            } else {
                                                "${player.role.shortLabel} • ${assessment.riskTier.label}"
                                            }
                                            Text(
                                                text = statusSub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (player.injury.isInjured) AlertCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isWide) {
                                        Text(
                                            text = if (player.injury.isInjured) {
                                                player.injury.injuryName
                                            } else if (player.trainingFocus == TrainingFocus.FITNESS_RECOVERY) {
                                                "Recovery Session"
                                            } else {
                                                assessment.riskTier.label
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (player.injury.isInjured) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (player.injury.isInjured) AlertCrimson else riskTierColor(assessment.riskTier)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1.35f)
                                        )

                                        Text(
                                            text = "${player.fitness}%",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                color = if (player.fitness < 72) CautionOchre else PitchMint
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.65f)
                                        )

                                        Text(
                                            text = "${player.recentMatchWorkload}%",
                                            style = TelemetryTextStyles.statSmall,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.75f)
                                        )

                                        Text(
                                            text = "${assessment.injuryProbabilityPct}%",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = riskTierColor(assessment.riskTier)
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.7f)
                                        )

                                        Text(
                                            text = if (player.injury.isInjured) "${assessment.estimatedRecoveryWeeks}w" else "-",
                                            style = TelemetryTextStyles.statSmall,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.65f)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.weight(0.95f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "${player.fitness}%",
                                                style = TelemetryTextStyles.statSmall.copy(
                                                    color = if (player.fitness < 72) CautionOchre else PitchMint,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            Text(
                                                text = "Ld ${player.recentMatchWorkload}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${assessment.injuryProbabilityPct}%",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = riskTierColor(assessment.riskTier)
                                            ),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.weight(0.65f)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.weight(if (isWide) 2.1f else 1.7f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        MedicalActionButton(
                                            career = career,
                                            player = player,
                                            onSetRehab = onSetRehab,
                                            onSetTraining = onSetTraining
                                        )
                                    }
                                }
                                if (idx < orderedAssessments.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            } else {
                val columns = if (isWide) 2 else 1
                itemsIndexed(
                    orderedAssessments.chunked(columns),
                    key = { _, row -> "med_tile_row_${row.first().player.id}" }
                ) { _, chunk ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medical_tiles_view"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { assessment ->
                            val player = assessment.player
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onOpenPlayer(player.id) }
                                    .testTag("medical_tile_${player.id}"),
                                shape = MaterialTheme.shapes.medium,
                                color = if (player.injury.isInjured) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (player.injury.isInjured) AlertCrimson.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PlayerAvatar(player = player, sizeDp = 42, showOvrBadge = true)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = player.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                RoleChip(player.role)
                                            }
                                            Text(
                                                text = if (player.injury.isInjured) {
                                                    "${player.injury.injuryName} • ETA ${assessment.estimatedRecoveryWeeks}w"
                                                } else {
                                                    "Fit ${player.fitness}% • Load ${player.recentMatchWorkload}% • Risk ${assessment.injuryProbabilityPct}%"
                                                },
                                                style = TelemetryTextStyles.statSmall,
                                                color = if (player.injury.isInjured) AlertCrimson else riskTierColor(assessment.riskTier),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (player.injury.isInjured) {
                                                "Rehab: ${player.injury.rehabMode.label}"
                                            } else {
                                                "Status: ${if (player.trainingFocus == TrainingFocus.FITNESS_RECOVERY) "Recovery Session" else assessment.riskTier.label}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        MedicalActionButton(
                                            career = career,
                                            player = player,
                                            onSetRehab = onSetRehab,
                                            onSetTraining = onSetTraining
                                        )
                                    }
                                }
                            }
                        }
                        if (columns == 2 && chunk.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicalSummaryHeaderCard(
    summary: com.example.simulation.SquadMedicalSummary,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isCompact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Medical & Rehab Ward",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${summary.physioName} (${summary.physioRating}) • Medical Lvl ${summary.medicalLevel}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MedicalKpiPill("Injured", "${summary.injuredCount}", if (summary.injuredCount > 0) AlertCrimson else PitchMint)
                    MedicalKpiPill("High Risk", "${summary.highRiskCount}", if (summary.highRiskCount > 0) CautionOchre else PitchMint)
                    MedicalKpiPill("Avg Load", "${summary.avgWorkloadPct}%", MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Medical & Rehab Ward",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${summary.physioName} (${summary.physioRating}) • Medical Lvl ${summary.medicalLevel}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MedicalKpiPill("Injured", "${summary.injuredCount}", if (summary.injuredCount > 0) AlertCrimson else PitchMint)
                    MedicalKpiPill("High Risk", "${summary.highRiskCount}", if (summary.highRiskCount > 0) CautionOchre else PitchMint)
                    MedicalKpiPill("Avg Load", "${summary.avgWorkloadPct}%", MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MedicalActionButton(
    career: CareerState,
    player: Player,
    onSetRehab: (String, RehabMode) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit
) {
    var rehabMenuExpanded by remember(player.id) { mutableStateOf(false) }

    if (player.injury.isInjured) {
        Box {
            OutlinedButton(
                onClick = { rehabMenuExpanded = true },
                enabled = !career.delegation.rehabDelegated,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .heightIn(min = 38.dp)
                    .testTag("medical_rehab_btn_${player.id}")
            ) {
                Text(
                    text = player.injury.rehabMode.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!career.delegation.rehabDelegated) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Change rehab plan",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = rehabMenuExpanded,
                onDismissRequest = { rehabMenuExpanded = false }
            ) {
                listOf(
                    RehabMode.CONSERVATIVE,
                    RehabMode.BALANCED,
                    RehabMode.ACCELERATED
                ).forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${mode.label} (${mode.setbackRiskPercent}% setback)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingIcon = {
                            if (player.injury.rehabMode == mode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            rehabMenuExpanded = false
                            onSetRehab(player.id, mode)
                        },
                        modifier = Modifier.testTag("medical_rehab_option_${player.id}_${mode.name.lowercase()}")
                    )
                }
            }
        }
    } else {
        val inRecovery = player.trainingFocus == TrainingFocus.FITNESS_RECOVERY
        FilledTonalButton(
            onClick = {
                onSetTraining(
                    player.id,
                    if (inRecovery) TrainingFocus.BALANCED else TrainingFocus.FITNESS_RECOVERY
                )
            },
            enabled = !career.delegation.trainingDelegated,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
                .heightIn(min = 38.dp)
                .testTag("medical_rest_btn_${player.id}")
        ) {
            Icon(
                imageVector = if (inRecovery) Icons.Filled.CheckCircle else Icons.Filled.Hotel,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (inRecovery) "Resting" else "Rest",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TmHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun MedicalKpiPill(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = TelemetryTextStyles.statValue.copy(color = color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun riskTierColor(tier: InjuryRiskTier): Color = when (tier) {
    InjuryRiskTier.LOW -> PitchMint
    InjuryRiskTier.MODERATE -> TelemetryCyan80
    InjuryRiskTier.HIGH -> CautionOchre
    InjuryRiskTier.CRITICAL -> AlertCrimson
}
