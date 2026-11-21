package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.PitchMint
import com.example.ui.theme.TelemetryTextStyles

@Composable
fun TransfersScreen(
    career: CareerState,
    activeNegotiation: NegotiationSession?,
    onToggleDelegation: (DelegationArea) -> Unit,
    onToggleShortlist: (String) -> Unit,
    onOpenNegotiation: (Player) -> Unit,
    onUpdateOffer: (Int, Int, Int, Int) -> Unit,
    onSubmitOffer: () -> Unit,
    onCloseNegotiation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showShortlistOnly by remember { mutableStateOf(false) }
    var roleFilter by remember { mutableStateOf<T20Role?>(null) }

    val scoutName = career.staff.firstOrNull { it.role == StaffRole.CHIEF_SCOUT }?.name
    val marketList = remember(career.transferMarket, showShortlistOnly, roleFilter) {
        career.transferMarket
            .filter { (!showShortlistOnly || it.isShortlisted) && (roleFilter == null || it.role == roleFilter) }
            .sortedByDescending { it.overall }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DelegationBanner(
                area = DelegationArea.TRANSFERS,
                isDelegated = career.delegation.transfersDelegated,
                staffOwnerName = scoutName,
                onToggle = { onToggleDelegation(DelegationArea.TRANSFERS) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Global T20 Transfer Market", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Available Cash: ${formatCurrency(career.bankBalance)} • Scout Lvl ${career.budgets.scoutingLevel}/5",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                FilterChip(
                    selected = showShortlistOnly,
                    onClick = { showShortlistOnly = !showShortlistOnly },
                    label = { Text("Shortlist (${career.transferMarket.count { it.isShortlisted }})") },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = roleFilter == null,
                        onClick = { roleFilter = null },
                        label = { Text("All Roles") }
                    )
                }
                items(T20Role.entries) { role ->
                    FilterChip(
                        selected = roleFilter == role,
                        onClick = { roleFilter = if (roleFilter == role) null else role },
                        label = { Text(role.shortLabel) }
                    )
                }
            }
        }

        if (marketList.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No transfer targets match your filter", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        } else {
            items(marketList, key = { it.id }) { target ->
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(target.name, style = MaterialTheme.typography.titleMedium)
                                    RoleChip(target.role)
                                }
                                Text(
                                    text = "${target.nationality} • ${target.age}y • OVR ${target.overall} / POT ${target.potential}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onToggleShortlist(target.id) }) {
                                Icon(
                                    imageVector = if (target.isShortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = "Shortlist",
                                    tint = if (target.isShortlisted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Est. Fee ${formatCompactCurrency(target.marketValue)} • ${formatCompactCurrency(target.contract.weeklyWage)}/wk",
                                style = TelemetryTextStyles.statSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = { onOpenNegotiation(target) },
                                modifier = Modifier.testTag("negotiate_${target.id}")
                            ) {
                                Text("Negotiate")
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeNegotiation != null) {
        NegotiationDialog(
            session = activeNegotiation,
            clubBalance = career.bankBalance,
            onUpdateOffer = onUpdateOffer,
            onSubmit = onSubmitOffer,
            onDismiss = onCloseNegotiation
        )
    }
}

@Composable
fun NegotiationDialog(
    session: NegotiationSession,
    clubBalance: Int,
    onUpdateOffer: (Int, Int, Int, Int) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (session.isRenewal) "Contract Renewal" else "Transfer Negotiation",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${session.player.name} (${session.player.role.label} • OVR ${session.player.overall})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Agent Patience", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${session.agentPatience}%",
                                style = TelemetryTextStyles.statSmall,
                                color = if (session.agentPatience < 35) AlertCrimson else PitchMint
                            )
                        }
                        LinearProgressIndicator(
                            progress = { session.agentPatience / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = session.agentMessage,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (!session.isAccepted && !session.isCollapsed) {
                    if (!session.isRenewal) {
                        StepperRow(
                            label = "Transfer Fee",
                            valueText = formatCurrency(session.offeredFee),
                            onMinus = { onUpdateOffer((session.offeredFee - 5000).coerceAtLeast(0), session.offeredWage, session.offeredSigningBonus, session.offeredYears) },
                            onPlus = { onUpdateOffer(session.offeredFee + 5000, session.offeredWage, session.offeredSigningBonus, session.offeredYears) }
                        )
                    }
                    StepperRow(
                        label = "Weekly Wage",
                        valueText = "${formatCurrency(session.offeredWage)}/wk",
                        onMinus = { onUpdateOffer(session.offeredFee, (session.offeredWage - 250).coerceAtLeast(800), session.offeredSigningBonus, session.offeredYears) },
                        onPlus = { onUpdateOffer(session.offeredFee, session.offeredWage + 250, session.offeredSigningBonus, session.offeredYears) }
                    )
                    StepperRow(
                        label = "Signing Bonus",
                        valueText = formatCurrency(session.offeredSigningBonus),
                        onMinus = { onUpdateOffer(session.offeredFee, session.offeredWage, (session.offeredSigningBonus - 1000).coerceAtLeast(0), session.offeredYears) },
                        onPlus = { onUpdateOffer(session.offeredFee, session.offeredWage, session.offeredSigningBonus + 1000, session.offeredYears) }
                    )
                    StepperRow(
                        label = "Contract Length",
                        valueText = "${session.offeredYears} Season(s)",
                        onMinus = { onUpdateOffer(session.offeredFee, session.offeredWage, session.offeredSigningBonus, (session.offeredYears - 1).coerceAtLeast(1)) },
                        onPlus = { onUpdateOffer(session.offeredFee, session.offeredWage, session.offeredSigningBonus, (session.offeredYears + 1).coerceAtMost(4)) }
                    )
                    Text(
                        text = "Upfront Cost: ${formatCurrency(session.offeredFee + session.offeredSigningBonus)} (Cash: ${formatCompactCurrency(clubBalance)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (session.isAccepted || session.isCollapsed) {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.testTag("submit_offer_button")
                ) {
                    Text("Submit Offer")
                }
            }
        },
        dismissButton = {
            if (!session.isAccepted && !session.isCollapsed) {
                TextButton(onClick = onDismiss) {
                    Text("Walk Away")
                }
            }
        }
    )
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = TelemetryTextStyles.statValue)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(onClick = onMinus) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
            }
            FilledTonalIconButton(onClick = onPlus) {
                Icon(Icons.Filled.Add, contentDescription = "Increase $label")
            }
        }
    }
}
