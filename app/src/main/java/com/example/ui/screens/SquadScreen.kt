package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CautionOchre
import com.example.ui.theme.PitchMint
import com.example.ui.theme.TelemetryTextStyles

private enum class SquadSubTab(val label: String) {
    ROSTER("Roster"),
    DEPTH("Depth Chart"),
    TRAINING_REHAB("Training & Rehab"),
    ACADEMY("Youth Academy")
}

@Composable
fun SquadScreen(
    career: CareerState,
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onPromoteYouth: (String) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(SquadSubTab.ROSTER) }
    var roleFilter by remember { mutableStateOf<T20Role?>(null) }

    val seniorPlayers = remember(career.squad) {
        career.squad.filter { !it.isYouthAcademy }.sortedByDescending { it.overall }
    }
    val youthPlayers = remember(career.squad) {
        career.squad.filter { it.isYouthAcademy }.sortedByDescending { it.potential }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sub-navigation tabs
        ScrollableTabRow(
            selectedTabIndex = activeSubTab.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            SquadSubTab.entries.forEach { tab ->
                Tab(
                    selected = activeSubTab == tab,
                    onClick = { activeSubTab = tab },
                    text = { Text(tab.label) },
                    modifier = Modifier.testTag("squad_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeSubTab) {
            SquadSubTab.ROSTER -> {
                val filtered = if (roleFilter == null) seniorPlayers else seniorPlayers.filter { it.role == roleFilter }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = roleFilter == null,
                                    onClick = { roleFilter = null },
                                    label = { Text("All (${seniorPlayers.size})") }
                                )
                            }
                            items(T20Role.entries) { role ->
                                val count = seniorPlayers.count { it.role == role }
                                if (count > 0) {
                                    FilterChip(
                                        selected = roleFilter == role,
                                        onClick = { roleFilter = if (roleFilter == role) null else role },
                                        label = { Text("${role.shortLabel} ($count)") }
                                    )
                                }
                            }
                        }
                    }
                    items(filtered, key = { it.id }) { player ->
                        PlayerSummaryCard(
                            player = player,
                            isSelectedInXI = career.selectedXIIds.contains(player.id),
                            isImpactSub = career.impactSubId == player.id,
                            onClick = { onOpenPlayer(player.id) }
                        )
                    }
                }
            }

            SquadSubTab.DEPTH -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "T20 Squad Depth Matrix",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap any player to view activity telemetry and staff advice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(T20Role.entries) { role ->
                        val group = seniorPlayers.filter { it.role == role }
                        val youthInRole = youthPlayers.filter { it.role == role }
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
                                    Text(role.label, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${group.size} Senior • ${youthInRole.size} U-19",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (group.isEmpty()) AlertCrimson else MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (group.isEmpty()) {
                                    Text(
                                        text = "Thin coverage — consider scouting a ${role.label} or promoting from Academy.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CautionOchre
                                    )
                                } else {
                                    group.forEach { p ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenPlayer(p.id) }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${p.name} (${p.age}y)",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "OVR ${p.overall} • POT ${p.potential}",
                                                style = TelemetryTextStyles.statSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SquadSubTab.TRAINING_REHAB -> {
                val headCoachName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name
                val physioName = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name
                val injured = seniorPlayers.filter { it.injury.isInjured }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        DelegationBanner(
                            area = DelegationArea.TRAINING,
                            isDelegated = career.delegation.trainingDelegated,
                            staffOwnerName = headCoachName,
                            onToggle = { onToggleDelegation(DelegationArea.TRAINING) }
                        )
                    }
                    item {
                        DelegationBanner(
                            area = DelegationArea.REHAB,
                            isDelegated = career.delegation.rehabDelegated,
                            staffOwnerName = physioName,
                            onToggle = { onToggleDelegation(DelegationArea.REHAB) }
                        )
                    }

                    if (injured.isNotEmpty()) {
                        item {
                            Text(
                                text = "Medical & Rehab Ward (${injured.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = AlertCrimson
                            )
                        }
                        items(injured, key = { "inj_${it.id}" }) { player ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenPlayer(player.id) }
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(player.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${player.injury.injuryName} • ${player.injury.weeksRemaining}w left",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AlertCrimson
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(RehabMode.CONSERVATIVE, RehabMode.BALANCED, RehabMode.ACCELERATED).forEach { mode ->
                                            FilterChip(
                                                selected = player.injury.rehabMode == mode,
                                                enabled = !career.delegation.rehabDelegated,
                                                onClick = { onSetRehab(player.id, mode) },
                                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Individual T20 Training Focus",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(seniorPlayers.filter { !it.injury.isInjured }, key = { "tr_${it.id}" }) { player ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlayer(player.id) }
                                    .padding(12.dp),
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
                                        Text(player.name, style = MaterialTheme.typography.titleSmall)
                                        RoleChip(player.role)
                                    }
                                    Text(
                                        text = "FIT ${player.fitness}%",
                                        style = TelemetryTextStyles.statSmall,
                                        color = if (player.fitness < 72) CautionOchre else PitchMint
                                    )
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        }
                    }
                }
            }

            SquadSubTab.ACADEMY -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = "Academy",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Youth Academy • Level ${career.budgets.academyLevel}/5",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Invest in Academy Budget inside Club Office to attract higher-potential U-19 prospects each season.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (youthPlayers.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("All U-19 prospects promoted!", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Fresh academy graduates will join at the start of Season ${career.season + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(youthPlayers, key = { it.id }) { prospect ->
                            PlayerSummaryCard(
                                player = prospect,
                                onClick = { onOpenPlayer(prospect.id) },
                                trailingAction = {
                                    FilledTonalButton(
                                        onClick = { onPromoteYouth(prospect.id) },
                                        modifier = Modifier.testTag("promote_youth_${prospect.id}")
                                    ) {
                                        Text("Promote")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
