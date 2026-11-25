package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.components.*
import com.example.ui.theme.*

enum class OfficeSubTab(val label: String) {
    STAFF_DELEGATION("Staff Office"),
    FINANCE_SPONSORS("Finances & Sponsors"),
    SEASON_CALENDAR("Calendar"),
    PYRAMID("Standings & Stats");

    companion object {
        fun forPhase(isPreSeason: Boolean): List<OfficeSubTab> = if (isPreSeason) {
            listOf(STAFF_DELEGATION, FINANCE_SPONSORS)
        } else {
            listOf(SEASON_CALENDAR, PYRAMID)
        }
    }
}

@Composable
fun ClubOfficeScreen(
    career: CareerState,
    activeTab: OfficeSubTab,
    onSelectTab: (OfficeSubTab) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    onAssignCoachFocus: (String, CoachTrainingArea) -> Unit,
    onHireStaff: (StaffMember, CoachTrainingArea?) -> Unit,
    onSignSponsor: (String) -> Unit,
    onNegotiateSponsor: (String) -> Unit = {},
    activeSponsorNegotiation: SponsorNegotiationSession? = null,
    onUpdateSponsorOffer: (Int, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    onSubmitSponsorOffer: () -> Unit = {},
    onDismissSponsorNegotiation: () -> Unit = {},
    onSetTicketTier: (TicketTier) -> Unit,
    onAdjustBudget: (Int?, Int?, Int?) -> Unit,
    onOpenTransfers: () -> Unit = {},
    onOpenMatchday: () -> Unit = {},
    onOpenSquadTraining: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedDivisionTier by remember(career.divisionTier) { mutableIntStateOf(career.divisionTier) }
    var selectedSponsorCategory by remember { mutableStateOf<SponsorCategory?>(null) }

    val visibleTabs = remember(career.isPreSeason) {
        OfficeSubTab.forPhase(career.isPreSeason)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("club_office_screen")
    ) {
        TabRow(
            selectedTabIndex = visibleTabs.indexOf(activeTab).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            visibleTabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = { Text(tab.label) },
                    modifier = Modifier.testTag("office_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeTab) {
            OfficeSubTab.STAFF_DELEGATION -> {
                StaffOfficeSection(
                    career = career,
                    onAssignCoachFocus = onAssignCoachFocus,
                    onHireStaff = onHireStaff,
                    onToggleDelegation = onToggleDelegation
                )
            }

            OfficeSubTab.SEASON_CALENDAR -> {
                SeasonCalendarSection(
                    career = career,
                    onOpenTransfers = onOpenTransfers,
                    onOpenMatchday = onOpenMatchday,
                    onOpenSquadTraining = onOpenSquadTraining
                )
            }

            OfficeSubTab.FINANCE_SPONSORS -> {
                LazyColumn(
                    modifier = Modifier.testTag("finance_sponsors_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        FinancialDashboardSection(career = career)
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
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(TicketTier.entries) { tier ->
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
                        Text("Commercial Sponsorships & Negotiations", style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        SponsorWindowStatusBanner(career = career)
                    }
                    item {
                        val leverage = remember(career) {
                            com.example.simulation.SponsorNegotiationEngine.evaluateClubLeverage(career)
                        }
                        ClubCommercialLeverageCard(leverage = leverage)
                    }
                    item {
                        ActiveSponsorPortfolioStrip(sponsors = career.sponsors)
                    }
                    item {
                        SponsorCategoryFilterBar(
                            selectedCategory = selectedSponsorCategory,
                            onSelectCategory = { selectedSponsorCategory = it }
                        )
                    }
                    val visibleSponsors = if (selectedSponsorCategory == null) {
                        career.sponsors
                    } else {
                        career.sponsors.filter { it.category == selectedSponsorCategory }
                    }
                    items(visibleSponsors, key = { it.id }) { sp ->
                        SponsorBrandOfferCard(
                            deal = sp,
                            isWindowOpen = career.isSponsorshipWindowOpen,
                            onSignDirect = { onSignSponsor(sp.id) },
                            onNegotiate = { onNegotiateSponsor(sp.id) }
                        )
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
                LeagueStandingBoardSection(
                    career = career,
                    selectedTier = selectedDivisionTier,
                    onSelectTier = { selectedDivisionTier = it }
                )
            }
        }
    }

    if (activeSponsorNegotiation != null) {
        SponsorNegotiationDialog(
            session = activeSponsorNegotiation,
            onUpdateTerms = onUpdateSponsorOffer,
            onSubmitOffer = onSubmitSponsorOffer,
            onDismiss = onDismissSponsorNegotiation
        )
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
