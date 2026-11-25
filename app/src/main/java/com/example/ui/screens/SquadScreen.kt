package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

enum class SquadSubTab(val label: String) {
    ROSTER("Roster"),
    DEPTH("Depth Chart"),
    ACADEMY("Youth Academy"),
    FORM_STATS("Form & Stats"),
    TRAINING_REHAB("Training & Medical");

    companion object {
        fun forPhase(isPreSeason: Boolean): List<SquadSubTab> = if (isPreSeason) {
            listOf(ROSTER, DEPTH, ACADEMY)
        } else {
            listOf(FORM_STATS)
        }
    }
}

private enum class SquadStatsSort(val label: String) {
    FORM("Form"),
    RUNS("Runs"),
    FATIGUE("Fatigue"),
    OVERS("Overs")
}

@Composable
fun SquadScreen(
    career: CareerState,
    activeSubTab: SquadSubTab = SquadSubTab.ROSTER,
    onSelectSubTab: (SquadSubTab) -> Unit = {},
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit = {},
    onSetRehab: (String, RehabMode) -> Unit,
    onPromoteYouth: (String) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    var roleFilter by remember { mutableStateOf<T20Role?>(null) }
    var rosterViewMode by remember { mutableStateOf(SquadViewMode.TILE) }
    var depthViewMode by remember { mutableStateOf(SquadViewMode.TILE) }
    var academyViewMode by remember { mutableStateOf(SquadViewMode.TILE) }
    var formStatsViewMode by remember { mutableStateOf(SquadViewMode.TABLE) }

    val seniorPlayers = remember(career.squad) {
        career.squad.filter { !it.isYouthAcademy }.sortedByDescending { it.overall }
    }
    val youthPlayers = remember(career.squad) {
        career.squad.filter { it.isYouthAcademy }.sortedByDescending { it.potential }
    }

    val visibleTabs = remember(career.isPreSeason) {
        SquadSubTab.forPhase(career.isPreSeason)
    }
    val effectiveTab = if (activeSubTab in visibleTabs) activeSubTab else visibleTabs.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("squad_screen")
    ) {
        // Phase-specific sub-navigation tabs
        TabRow(
            selectedTabIndex = visibleTabs.indexOf(effectiveTab).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = effectiveTab == tab,
                    onClick = { onSelectSubTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("squad_tab_${tab.name.lowercase()}")
                )
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            when (effectiveTab) {
                SquadSubTab.ROSTER -> {
                    val filtered = if (roleFilter == null) seniorPlayers else seniorPlayers.filter { it.role == roleFilter }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Senior Roster (${filtered.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                SquadViewModeToggle(
                                    mode = rosterViewMode,
                                    onModeChange = { rosterViewMode = it },
                                    testTagPrefix = "roster"
                                )
                            }
                        }
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

                        if (rosterViewMode == SquadViewMode.TILE) {
                            val columns = if (isWide) 2 else 1
                            items(filtered.chunked(columns), key = { row -> "sq_row_${row.first().id}" }) { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    chunk.forEach { player ->
                                        SquadPlayerDashboardCard(
                                            player = player,
                                            isSelectedInXI = career.selectedXIIds.contains(player.id),
                                            isImpactSub = career.impactSubId == player.id,
                                            onClick = { onOpenPlayer(player.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (columns == 2 && chunk.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            item {
                                SquadRosterTable(
                                    players = filtered,
                                    selectedXIIds = career.selectedXIIds,
                                    impactSubId = career.impactSubId,
                                    onOpenPlayer = onOpenPlayer
                                )
                            }
                        }
                    }
                }

                SquadSubTab.DEPTH -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "T20 Squad Depth Matrix",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Tap any player to view telemetry & staff advice.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SquadViewModeToggle(
                                    mode = depthViewMode,
                                    onModeChange = { depthViewMode = it },
                                    testTagPrefix = "depth"
                                )
                            }
                        }

                        if (depthViewMode == SquadViewMode.TILE) {
                            val columns = if (isWide) 2 else 1
                            items(T20Role.entries.chunked(columns), key = { row -> "depth_${row.first().name}" }) { roleChunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    roleChunk.forEach { role ->
                                        val group = seniorPlayers.filter { it.role == role }
                                        val youthInRole = youthPlayers.filter { it.role == role }
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surfaceContainer
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
                                                    Text(role.label, style = MaterialTheme.typography.titleSmall)
                                                    Text(
                                                        text = "${group.size} Sr • ${youthInRole.size} U19",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (group.isEmpty()) AlertCrimson else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (group.isEmpty()) {
                                                    Text(
                                                        text = "Thin coverage — scout a ${role.label} or promote from Academy.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = CautionOchre
                                                    )
                                                } else {
                                                    group.forEach { p ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { onOpenPlayer(p.id) }
                                                                .padding(vertical = 3.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "${p.name} (${p.age}y)",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Text(
                                                                text = "OVR ${p.overall}",
                                                                style = TelemetryTextStyles.statSmall,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (columns == 2 && roleChunk.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            item {
                                SquadDepthTable(
                                    seniorPlayers = seniorPlayers,
                                    youthPlayers = youthPlayers,
                                    onOpenPlayer = onOpenPlayer
                                )
                            }
                        }
                    }
                }

                SquadSubTab.ACADEMY -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = "Academy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Youth Academy • Level ${career.budgets.academyLevel}/5",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Invest in Academy Budget inside Club Office to attract higher-potential U-19 prospects.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    SquadViewModeToggle(
                                        mode = academyViewMode,
                                        onModeChange = { academyViewMode = it },
                                        testTagPrefix = "academy"
                                    )
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
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        } else if (academyViewMode == SquadViewMode.TILE) {
                            val columns = if (isWide) 2 else 1
                            items(youthPlayers.chunked(columns), key = { row -> "acad_${row.first().id}" }) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { prospect ->
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
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (columns == 2 && pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            item {
                                SquadAcademyTable(
                                    youthPlayers = youthPlayers,
                                    onOpenPlayer = onOpenPlayer,
                                    onPromoteYouth = onPromoteYouth
                                )
                            }
                        }
                    }
                }

                SquadSubTab.FORM_STATS -> {
                    SquadFormAndStatsSection(
                        seniorPlayers = seniorPlayers,
                        selectedXIIds = career.selectedXIIds,
                        viewMode = formStatsViewMode,
                        onViewModeChange = { formStatsViewMode = it },
                        isWide = isWide,
                        onOpenPlayer = onOpenPlayer
                    )
                }

                SquadSubTab.TRAINING_REHAB -> {
                    CombinedTrainingAndMedicalSection(
                        career = career,
                        seniorPlayers = seniorPlayers,
                        onOpenPlayer = onOpenPlayer,
                        onSetTraining = onSetTraining,
                        onSetTrainingIntensity = onSetTrainingIntensity,
                        onSetRehab = onSetRehab,
                        onToggleDelegation = onToggleDelegation
                    )
                }
            }
        }
    }
}

@Composable
private fun SquadRosterTable(
    players: List<Player>,
    selectedXIIds: List<String>,
    impactSubId: String?,
    onOpenPlayer: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("squad_roster_table"),
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
                TableHeaderCell("Player", Modifier.weight(2.1f), TextAlign.Start)
                TableHeaderCell("Role", Modifier.weight(0.75f), TextAlign.Center)
                TableHeaderCell("OVR", Modifier.weight(0.65f), TextAlign.End)
                TableHeaderCell("Fit", Modifier.weight(0.75f), TextAlign.End)
                TableHeaderCell("Frm", Modifier.weight(0.65f), TextAlign.End)
                TableHeaderCell("Wage", Modifier.weight(0.9f), TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            players.forEachIndexed { idx, player ->
                val rowBg = if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f) else Color.Transparent
                val isXI = selectedXIIds.contains(player.id)
                val isSub = impactSubId == player.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { onOpenPlayer(player.id) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("roster_table_row_${player.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(2.1f),
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
                        if (isXI) {
                            SquadTagPill("XI", MaterialTheme.colorScheme.primary)
                        } else if (isSub) {
                            SquadTagPill("SUB", MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Box(modifier = Modifier.weight(0.75f), contentAlignment = Alignment.Center) {
                        RoleChip(player.role)
                    }
                    Text(
                        text = "${player.overall}",
                        style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.65f)
                    )
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
                        modifier = Modifier.weight(0.75f)
                    )
                    Text(
                        text = "${player.form}",
                        style = TelemetryTextStyles.statSmall.copy(color = formBandColor(player.form)),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.65f)
                    )
                    Text(
                        text = formatCompactCurrency(player.contract.weeklyWage),
                        style = TelemetryTextStyles.statSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                if (idx < players.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun SquadDepthTable(
    seniorPlayers: List<Player>,
    youthPlayers: List<Player>,
    onOpenPlayer: (String) -> Unit
) {
    val rows = remember(seniorPlayers, youthPlayers) {
        T20Role.entries.flatMap { role ->
            val sr = seniorPlayers.filter { it.role == role }
            val u19 = youthPlayers.filter { it.role == role }
            if (sr.isEmpty() && u19.isEmpty()) {
                listOf(Triple(role, null as Player?, "Thin Coverage"))
            } else {
                sr.map { Triple(role, it, "Senior") } + u19.map { Triple(role, it, "U-19") }
            }
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("squad_depth_table"),
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
                TableHeaderCell("Role", Modifier.weight(0.8f), TextAlign.Start)
                TableHeaderCell("Player", Modifier.weight(2.0f), TextAlign.Start)
                TableHeaderCell("Age", Modifier.weight(0.6f), TextAlign.End)
                TableHeaderCell("OVR", Modifier.weight(0.65f), TextAlign.End)
                TableHeaderCell("Pool", Modifier.weight(0.95f), TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            rows.forEachIndexed { idx, (role, player, tierTag) ->
                val rowBg = if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable(enabled = player != null) { player?.id?.let(onOpenPlayer) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(0.8f)) {
                        RoleChip(role)
                    }
                    Text(
                        text = player?.name ?: "No ${role.label}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (player != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (player != null) MaterialTheme.colorScheme.onSurface else CautionOchre
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2.0f)
                    )
                    Text(
                        text = player?.let { "${it.age}" } ?: "-",
                        style = TelemetryTextStyles.statSmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = player?.let { "${it.overall}" } ?: "-",
                        style = TelemetryTextStyles.statSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (player != null) MaterialTheme.colorScheme.primary else CautionOchre
                        ),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.65f)
                    )
                    Text(
                        text = tierTag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = when (tierTag) {
                                "Senior" -> PitchMint
                                "U-19" -> TelemetryCyan80
                                else -> AlertCrimson
                            }
                        ),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.weight(0.95f)
                    )
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun SquadAcademyTable(
    youthPlayers: List<Player>,
    onOpenPlayer: (String) -> Unit,
    onPromoteYouth: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("squad_academy_table"),
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
                TableHeaderCell("Prospect", Modifier.weight(1.9f), TextAlign.Start)
                TableHeaderCell("Role", Modifier.weight(0.75f), TextAlign.Center)
                TableHeaderCell("OVR", Modifier.weight(0.6f), TextAlign.End)
                TableHeaderCell("Pot", Modifier.weight(0.6f), TextAlign.End)
                TableHeaderCell("Action", Modifier.weight(1.35f), TextAlign.End)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            youthPlayers.forEachIndexed { idx, prospect ->
                val rowBg = if (idx % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { onOpenPlayer(prospect.id) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.9f)) {
                        Text(
                            text = prospect.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${prospect.age}y • ${prospect.nationality}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Box(modifier = Modifier.weight(0.75f), contentAlignment = Alignment.Center) {
                        RoleChip(prospect.role)
                    }
                    Text(
                        text = "${prospect.overall}",
                        style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = "${prospect.potential}",
                        style = TelemetryTextStyles.statSmall.copy(color = PitchMint, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.6f)
                    )
                    Box(modifier = Modifier.weight(1.35f), contentAlignment = Alignment.CenterEnd) {
                        FilledTonalButton(
                            onClick = { onPromoteYouth(prospect.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("promote_youth_${prospect.id}")
                        ) {
                            Text("Promote", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (idx < youthPlayers.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                }
            }
        }
    }
}

@Composable
private fun SquadTagPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun SquadFormAndStatsSection(
    seniorPlayers: List<Player>,
    selectedXIIds: List<String>,
    viewMode: SquadViewMode,
    onViewModeChange: (SquadViewMode) -> Unit,
    isWide: Boolean,
    onOpenPlayer: (String) -> Unit
) {
    var sortBy by remember { mutableStateOf(SquadStatsSort.FORM) }
    val chartPlayers = remember(seniorPlayers) {
        seniorPlayers.sortedByDescending { it.form }.take(12)
    }
    val tablePlayers = remember(seniorPlayers, sortBy) {
        when (sortBy) {
            SquadStatsSort.FORM -> seniorPlayers.sortedByDescending { it.form }
            SquadStatsSort.RUNS -> seniorPlayers.sortedByDescending { it.seasonStats.runs }
            SquadStatsSort.FATIGUE -> seniorPlayers.sortedByDescending { 100 - it.fitness }
            SquadStatsSort.OVERS -> seniorPlayers.sortedByDescending { it.seasonStats.ballsBowled }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("squad_form_stats_section"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Visual Squad Form Chart
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("squad_form_chart_card"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Squad Form Chart",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Current match form ratings (0–99) across active squad",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FormLegendChip("Hot ≥75", PitchMint)
                                FormLegendChip("Steady 55+", CautionOchre)
                                FormLegendChip("Dip <55", AlertCrimson)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Squad Form Chart",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Match form ratings (0–99) across active squad",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FormLegendChip("Hot ≥75", PitchMint)
                                FormLegendChip("Steady 55+", CautionOchre)
                                FormLegendChip("Dip <55", AlertCrimson)
                            }
                        }
                    }

                    if (chartPlayers.isEmpty()) {
                        Text(
                            text = "No senior squad players available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SquadFormCanvasChart(
                            players = chartPlayers,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            chartPlayers.forEach { p ->
                                val shortName = p.name.substringAfterLast(' ').take(4)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onOpenPlayer(p.id) }
                                        .padding(vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${p.form}",
                                        style = TelemetryTextStyles.statSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = formBandColor(p.form)
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = shortName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Individual Playing Stats Header + Sort + Tile/Table Toggle
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Individual Playing Stats",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tap a player to inspect full telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SquadViewModeToggle(
                        mode = viewMode,
                        onModeChange = onViewModeChange,
                        testTagPrefix = "form_stats"
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SquadStatsSort.entries.forEach { sort ->
                        FilterChip(
                            selected = sortBy == sort,
                            onClick = { sortBy = sort },
                            label = { Text(sort.label) },
                            modifier = Modifier.testTag("stats_sort_${sort.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (viewMode == SquadViewMode.TABLE) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("squad_individual_stats_table"),
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
                            TableHeaderCell("Player", Modifier.weight(2.0f), TextAlign.Start)
                            TableHeaderCell("Form", Modifier.weight(0.75f), TextAlign.End)
                            TableHeaderCell("Fatigue", Modifier.weight(0.9f), TextAlign.End)
                            TableHeaderCell("Runs", Modifier.weight(0.8f), TextAlign.End)
                            TableHeaderCell("Overs", Modifier.weight(0.8f), TextAlign.End)
                            TableHeaderCell("Wkts", Modifier.weight(0.65f), TextAlign.End)
                            TableHeaderCell("Mat", Modifier.weight(0.55f), TextAlign.End)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        tablePlayers.forEachIndexed { idx, player ->
                            val fatiguePct = (100 - player.fitness).coerceIn(0, 100)
                            val fatigueColor = when {
                                player.injury.isInjured -> AlertCrimson
                                fatiguePct >= 35 -> AlertCrimson
                                fatiguePct >= 20 -> CautionOchre
                                else -> PitchMint
                            }
                            val rowBg = if (idx % 2 == 1) {
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
                            } else {
                                Color.Transparent
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable { onOpenPlayer(player.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("stats_row_${player.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.0f)) {
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
                                        if (selectedXIIds.contains(player.id)) {
                                            SquadTagPill("XI", MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Text(
                                        text = player.role.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${player.form}",
                                    style = TelemetryTextStyles.statSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = formBandColor(player.form)
                                    ),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.75f)
                                )
                                Text(
                                    text = if (player.injury.isInjured) "INJ ${player.injury.weeksRemaining}w" else "$fatiguePct%",
                                    style = TelemetryTextStyles.statSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = fatigueColor
                                    ),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = "${player.seasonStats.runs}",
                                    style = TelemetryTextStyles.statSmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = player.seasonStats.oversFormatted,
                                    style = TelemetryTextStyles.statSmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = "${player.seasonStats.wickets}",
                                    style = TelemetryTextStyles.statSmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.65f)
                                )
                                Text(
                                    text = "${player.seasonStats.matches}",
                                    style = TelemetryTextStyles.statSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.55f)
                                )
                            }
                            if (idx < tablePlayers.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            }
                        }
                    }
                }
            }
        } else {
            val columns = if (isWide) 2 else 1
            items(tablePlayers.chunked(columns), key = { row -> "fs_tile_${row.first().id}" }) { chunk ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("squad_form_stats_tiles"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chunk.forEach { player ->
                        val fatiguePct = (100 - player.fitness).coerceIn(0, 100)
                        val fatigueColor = when {
                            player.injury.isInjured -> AlertCrimson
                            fatiguePct >= 35 -> AlertCrimson
                            fatiguePct >= 20 -> CautionOchre
                            else -> PitchMint
                        }
                        ElevatedCard(
                            onClick = { onOpenPlayer(player.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stats_tile_${player.id}"),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PlayerAvatar(player = player, sizeDp = 40, showOvrBadge = true)
                                        Column {
                                            Text(
                                                text = player.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                RoleChip(player.role)
                                                if (selectedXIIds.contains(player.id)) {
                                                    SquadTagPill("XI", MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = formBandColor(player.form).copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "FRM ${player.form}",
                                            style = TelemetryTextStyles.statSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = formBandColor(player.form)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Runs: ${player.seasonStats.runs}", style = TelemetryTextStyles.statSmall)
                                    Text("Overs: ${player.seasonStats.oversFormatted}", style = TelemetryTextStyles.statSmall)
                                    Text("Wkts: ${player.seasonStats.wickets}", style = TelemetryTextStyles.statSmall)
                                    Text(
                                        text = if (player.injury.isInjured) "INJ ${player.injury.weeksRemaining}w" else "Fat: $fatiguePct%",
                                        style = TelemetryTextStyles.statSmall.copy(color = fatigueColor, fontWeight = FontWeight.Bold)
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

@Composable
private fun TableHeaderCell(
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
private fun FormLegendChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formBandColor(form: Int): Color = when {
    form >= 75 -> PitchMint
    form >= 55 -> CautionOchre
    else -> AlertCrimson
}

@Composable
private fun SquadFormCanvasChart(
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val trendColor = TelemetryCyan80
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (players.isEmpty() || w <= 0f || h <= 0f) return@Canvas

        // Reference dashed lines for 75 (Hot) and 55 (Steady)
        val yHot = h * (1f - 0.75f)
        val ySteady = h * (1f - 0.55f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        drawLine(
            color = PitchMint.copy(alpha = 0.45f),
            start = Offset(0f, yHot),
            end = Offset(w, yHot),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = dashEffect
        )
        drawLine(
            color = CautionOchre.copy(alpha = 0.40f),
            start = Offset(0f, ySteady),
            end = Offset(w, ySteady),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = dashEffect
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, h - 1.dp.toPx()),
            end = Offset(w, h - 1.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        val slotW = w / players.size.toFloat()
        val barW = (slotW * 0.52f).coerceIn(8.dp.toPx(), 26.dp.toPx())
        val trendPath = Path()

        players.forEachIndexed { idx, player ->
            val ratio = (player.form / 100f).coerceIn(0.05f, 1f)
            val barH = (h - 8.dp.toPx()) * ratio
            val cx = slotW * idx + slotW / 2f
            val topY = h - barH

            drawRoundRect(
                color = formBandColor(player.form).copy(alpha = 0.78f),
                topLeft = Offset(cx - barW / 2f, topY),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            if (idx == 0) {
                trendPath.moveTo(cx, topY)
            } else {
                trendPath.lineTo(cx, topY)
            }
        }

        drawPath(
            path = trendPath,
            color = trendColor.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        players.forEachIndexed { idx, player ->
            val ratio = (player.form / 100f).coerceIn(0.05f, 1f)
            val barH = (h - 8.dp.toPx()) * ratio
            val cx = slotW * idx + slotW / 2f
            val topY = h - barH
            drawCircle(
                color = trendColor,
                radius = 3.2.dp.toPx(),
                center = Offset(cx, topY)
            )
        }
    }
}

@Composable
private fun CombinedTrainingAndMedicalSection(
    career: CareerState,
    seniorPlayers: List<Player>,
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    onSetRehab: (String, RehabMode) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit
) {
    val headCoachName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name
    var mobilePane by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 680.dp) {
            // Landscape 2-column split: Training Drills (left) + Medical Room & Rehab (right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrainingDrillsColumn(
                    career = career,
                    seniorPlayers = seniorPlayers,
                    headCoachName = headCoachName,
                    onOpenPlayer = onOpenPlayer,
                    onSetTraining = onSetTraining,
                    onSetTrainingIntensity = onSetTrainingIntensity,
                    onToggleDelegation = onToggleDelegation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                MedicalRoomSection(
                    career = career,
                    onSetTrainingIntensity = onSetTrainingIntensity,
                    onSetRehab = onSetRehab,
                    onRestPlayerRecovery = { playerId -> onSetTraining(playerId, TrainingFocus.FITNESS_RECOVERY) },
                    onOpenPlayer = onOpenPlayer,
                    onToggleDelegation = onToggleDelegation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mobilePane == 0,
                        onClick = { mobilePane = 0 },
                        label = { Text("Training Drills") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mobilePane == 1,
                        onClick = { mobilePane = 1 },
                        label = { Text("Medical & Rehab") },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (mobilePane == 0) {
                    TrainingDrillsColumn(
                        career = career,
                        seniorPlayers = seniorPlayers,
                        headCoachName = headCoachName,
                        onOpenPlayer = onOpenPlayer,
                        onSetTraining = onSetTraining,
                        onSetTrainingIntensity = onSetTrainingIntensity,
                        onToggleDelegation = onToggleDelegation,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MedicalRoomSection(
                        career = career,
                        onSetTrainingIntensity = onSetTrainingIntensity,
                        onSetRehab = onSetRehab,
                        onRestPlayerRecovery = { playerId -> onSetTraining(playerId, TrainingFocus.FITNESS_RECOVERY) },
                        onOpenPlayer = onOpenPlayer,
                        onToggleDelegation = onToggleDelegation,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrainingDrillsColumn(
    career: CareerState,
    seniorPlayers: List<Player>,
    headCoachName: String?,
    onOpenPlayer: (String) -> Unit,
    onSetTraining: (String, TrainingFocus) -> Unit,
    onSetTrainingIntensity: (TrainingIntensity) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("training_drills_column"),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Weekly Squad Training Intensity",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TrainingIntensity.entries.forEach { level ->
                            FilterChip(
                                selected = career.trainingIntensity == level,
                                onClick = { onSetTrainingIntensity(level) },
                                label = { Text(level.label) }
                            )
                        }
                    }
                    Text(
                        text = career.trainingIntensity.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
