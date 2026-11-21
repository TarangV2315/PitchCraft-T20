package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class OfficeSubTab(val label: String) {
    STAFF_DELEGATION("Staff & Delegation"),
    FINANCE_SPONSORS("Finances & Sponsors"),
    PYRAMID("T20 Pyramid")
}

@Composable
fun ClubOfficeScreen(
    career: CareerState,
    onToggleDelegation: (DelegationArea) -> Unit,
    onHireStaff: (StaffMember) -> Unit,
    onSignSponsor: (String) -> Unit,
    onSetTicketTier: (TicketTier) -> Unit,
    onAdjustBudget: (Int?, Int?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(OfficeSubTab.STAFF_DELEGATION) }
    var selectedDivisionTier by remember(career.divisionTier) { mutableIntStateOf(career.divisionTier) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            OfficeSubTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab.label) },
                    modifier = Modifier.testTag("office_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeTab) {
            OfficeSubTab.STAFF_DELEGATION -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Responsibility Delegation", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Manage every department yourself or delegate to your hired staff.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(DelegationArea.entries) { area ->
                        val owner = career.staff.firstOrNull { it.role == area.staffOwner }?.name
                        DelegationBanner(
                            area = area,
                            isDelegated = career.delegation.isDelegated(area),
                            staffOwnerName = owner,
                            onToggle = { onToggleDelegation(area) }
                        )
                    }

                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Current Management Staff", style = MaterialTheme.typography.titleMedium)
                    }
                    items(career.staff, key = { it.id }) { member ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${member.role.label} • ${member.name}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${member.specialty} • ${formatCurrency(member.weeklySalary)}/wk",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "RTG ${member.rating}",
                                        style = TelemetryTextStyles.statValue.copy(color = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Staff Recruitment Market", style = MaterialTheme.typography.titleMedium)
                    }
                    items(career.availableStaff.sortedByDescending { it.rating }, key = { it.id }) { candidate ->
                        val hireCost = candidate.weeklySalary * 4
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${candidate.name} (${candidate.rating})", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${candidate.role.label} • ${candidate.specialty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatCurrency(candidate.weeklySalary)}/wk • Fee ${formatCompactCurrency(hireCost)}",
                                        style = TelemetryTextStyles.statSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onHireStaff(candidate) },
                                    modifier = Modifier.testTag("hire_staff_${candidate.id}")
                                ) {
                                    Text("Hire")
                                }
                            }
                        }
                    }
                }
            }

            OfficeSubTab.FINANCE_SPONSORS -> {
                val weeklyOut = career.weeklyPlayerWages + career.weeklyStaffWages + career.budgets.weeklyUpkeep
                val weeklyNet = career.weeklySponsorIncome - weeklyOut
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Club P&L & Cashflow Control", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricTile(
                                        label = "Bank Balance",
                                        value = formatCompactCurrency(career.bankBalance),
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Weekly Base Net",
                                        value = "${if (weeklyNet >= 0) "+" else ""}${formatCompactCurrency(weeklyNet)}",
                                        subtitle = "Before home gates",
                                        accentColor = if (weeklyNet >= 0) PitchMint else CautionOchre,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Sponsor Income: +${formatCurrency(career.weeklySponsorIncome)}/wk  |  Wages & Upkeep: -${formatCurrency(weeklyOut)}/wk",
                                    style = TelemetryTextStyles.statSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Executive Ticket & Budget Allocation
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Ticket Pricing & Operations Budgets", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TicketTier.entries.forEach { tier ->
                                        FilterChip(
                                            selected = career.budgets.ticketTier == tier,
                                            onClick = { onSetTicketTier(tier) },
                                            label = { Text(tier.name.lowercase().replaceFirstChar { it.uppercase() } + " $${tier.pricePerSeat}") }
                                        )
                                    }
                                }
                                BudgetLevelRow(
                                    label = "Youth Academy Investment",
                                    level = career.budgets.academyLevel,
                                    costPerWeek = career.budgets.academyLevel * 3200,
                                    onDecrease = { onAdjustBudget(career.budgets.academyLevel - 1, null, null) },
                                    onIncrease = { onAdjustBudget(career.budgets.academyLevel + 1, null, null) }
                                )
                                BudgetLevelRow(
                                    label = "Global Scouting Network",
                                    level = career.budgets.scoutingLevel,
                                    costPerWeek = career.budgets.scoutingLevel * 2400,
                                    onDecrease = { onAdjustBudget(null, career.budgets.scoutingLevel - 1, null) },
                                    onIncrease = { onAdjustBudget(null, career.budgets.scoutingLevel + 1, null) }
                                )
                                BudgetLevelRow(
                                    label = "Sports Science & Medical",
                                    level = career.budgets.medicalLevel,
                                    costPerWeek = career.budgets.medicalLevel * 2600,
                                    onDecrease = { onAdjustBudget(null, null, career.budgets.medicalLevel - 1) },
                                    onIncrease = { onAdjustBudget(null, null, career.budgets.medicalLevel + 1) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Commercial Sponsorship Deals", style = MaterialTheme.typography.titleMedium)
                    }
                    items(career.sponsors, key = { it.id }) { sp ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (sp.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainer,
                            border = if (sp.isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${sp.brandName} • ${sp.category.label}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${formatCurrency(sp.weeklyPayout)}/wk • Win +${formatCompactCurrency(sp.winBonus)} • Cup +${formatCompactCurrency(sp.trophyBonus)}",
                                        style = TelemetryTextStyles.statSmall,
                                        color = PitchMint
                                    )
                                    Text(
                                        text = "Target: Top ${sp.minRankTarget} finish • ${sp.seasonsRemaining} season(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (sp.isActive) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = { onSignSponsor(sp.id) },
                                        modifier = Modifier.testTag("sign_sponsor_${sp.id}")
                                    ) {
                                        Text("Sign")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Recent Financial Ledger", style = MaterialTheme.typography.titleMedium)
                    }
                    items(career.ledger.take(15), key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.description, style = MaterialTheme.typography.bodySmall)
                                Text("S${entry.season} W${entry.week} • ${entry.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${if (entry.amount >= 0) "+" else ""}${formatCurrency(entry.amount)}",
                                style = TelemetryTextStyles.statValue.copy(
                                    color = if (entry.amount >= 0) PitchMint else AlertCrimson
                                )
                            )
                        }
                    }
                }
            }

            OfficeSubTab.PYRAMID -> {
                val div = career.divisions.firstOrNull { it.tier == selectedDivisionTier }
                val sorted = div?.let { SeasonProgressionEngine.sortDivision(it.clubs) }.orEmpty()

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(1 to "Tier 1 Premier", 2 to "Tier 2 Champ.", 3 to "Tier 3 Chall.").forEachIndexed { idx, (tier, label) ->
                                SegmentedButton(
                                    selected = selectedDivisionTier == tier,
                                    onClick = { selectedDivisionTier = tier },
                                    shape = SegmentedButtonDefaults.itemShape(idx, 3)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("#  Club", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2.2f))
                                    Text("P", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.4f))
                                    Text("W", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.4f))
                                    Text("NRR", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
                                    Text("Pts", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.5f))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                sorted.forEachIndexed { idx, club ->
                                    val pos = idx + 1
                                    val rowColor = when {
                                        club.isUserClub -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        pos <= 4 -> PitchMint.copy(alpha = 0.06f)
                                        pos == sorted.size && selectedDivisionTier < 3 -> AlertCrimson.copy(alpha = 0.08f)
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    }
                                    Surface(color = rowColor, shape = MaterialTheme.shapes.small) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$pos. ${club.name}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (club.isUserClub) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (club.isUserClub) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.weight(2.2f),
                                                maxLines = 1
                                            )
                                            Text("${club.played}", style = TelemetryTextStyles.statSmall, modifier = Modifier.weight(0.4f))
                                            Text("${club.won}", style = TelemetryTextStyles.statSmall, modifier = Modifier.weight(0.4f))
                                            Text("${"%+.2f".format(club.netRunRate)}", style = TelemetryTextStyles.statSmall, modifier = Modifier.weight(0.8f))
                                            Text("${club.points}", style = TelemetryTextStyles.statValue, modifier = Modifier.weight(0.5f))
                                        }
                                    }
                                }
                                Text(
                                    text = "Top 4 qualify for Playoffs • #1 promotes (Tier 2/3) • Last relegates (Tier 1/2)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

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
        }
    }
}

@Composable
private fun BudgetLevelRow(
    label: String,
    level: Int,
    costPerWeek: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Level $level / 5 • ${formatCurrency(costPerWeek)}/wk",
                style = TelemetryTextStyles.statSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(onClick = onDecrease, enabled = level > 1) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            FilledTonalIconButton(onClick = onIncrease, enabled = level < 5) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
    }
}
