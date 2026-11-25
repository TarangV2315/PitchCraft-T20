package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.simulation.CoachingBoostSummary
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.theme.*

enum class StaffMarketFilter(val label: String) {
    ALL("All Staff"),
    BATTING("Batting"),
    BOWLING("Bowling"),
    FITNESS("Fitness"),
    EXECUTIVE("Executive")
}

@Composable
fun StaffOfficeSection(
    career: CareerState,
    onAssignCoachFocus: (String, CoachTrainingArea) -> Unit,
    onHireStaff: (StaffMember, CoachTrainingArea?) -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    modifier: Modifier = Modifier
) {
    var marketFilter by remember { mutableStateOf(StaffMarketFilter.ALL) }
    val boosts = remember(career.staff) { SeasonProgressionEngine.calculateCoachingBoosts(career.staff) }
    val coaches = remember(career.staff) { career.staff.filter { it.isCoach } }
    val executiveStaff = remember(career.staff) { career.staff.filter { !it.isCoach } }

    val filteredCandidates = remember(career.availableStaff, marketFilter) {
        career.availableStaff
            .filter { cand ->
                when (marketFilter) {
                    StaffMarketFilter.ALL -> true
                    StaffMarketFilter.BATTING -> cand.role.defaultTrainingArea == CoachTrainingArea.BATTING || cand.role == StaffRole.HEAD_COACH
                    StaffMarketFilter.BOWLING -> cand.role.defaultTrainingArea == CoachTrainingArea.BOWLING
                    StaffMarketFilter.FITNESS -> cand.role.defaultTrainingArea == CoachTrainingArea.FITNESS
                    StaffMarketFilter.EXECUTIVE -> !cand.isCoach
                }
            }
            .sortedByDescending { it.rating }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_office_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Player Progression Boost Matrix
        item {
            ProgressionBoostMatrixCard(
                boosts = boosts,
                weeklyStaffPayroll = career.weeklyStaffWages,
                bankBalance = career.bankBalance
            )
        }

        // 2. Coaching Staff & Training Focus Assignments
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Coaching Roster & Training Focus (${coaches.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Assign each coach to Batting, Bowling, or Fitness to accelerate weekly player progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(coaches, key = { it.id }) { coach ->
            HiredCoachAssignmentCard(
                coach = coach,
                onAssignFocus = { area -> onAssignCoachFocus(coach.id, area) }
            )
        }

        // 3. Executive & Advisory Staff
        if (executiveStaff.isNotEmpty()) {
            item {
                Text(
                    text = "Scouting & Commercial Directors (${executiveStaff.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(executiveStaff, key = { it.id }) { member ->
                ExecutiveStaffCard(member = member)
            }
        }

        // 4. Coach Recruitment Market (Hire Coaches)
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Coach Recruitment Market",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Hire upgraded coaches and assign their training focus immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "Cash ${formatCompactCurrency(career.bankBalance)}",
                            style = TelemetryTextStyles.statSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StaffMarketFilter.entries) { filter ->
                        FilterChip(
                            selected = marketFilter == filter,
                            onClick = { marketFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("staff_market_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filteredCandidates.isEmpty()) {
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No coaching candidates match this filter right now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredCandidates, key = { it.id }) { candidate ->
                val incumbent = career.staff.firstOrNull { it.role == candidate.role }
                CoachCandidateHireCard(
                    candidate = candidate,
                    incumbent = incumbent,
                    clubBalance = career.bankBalance,
                    onHire = { chosenFocus -> onHireStaff(candidate, chosenFocus) }
                )
            }
        }

        // 5. Department Delegation Controls
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Department Delegation",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Keep manual control or delegate daily execution to your hired specialists.",
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
    }
}

@Composable
private fun ProgressionBoostMatrixCard(
    boosts: CoachingBoostSummary,
    weeklyStaffPayroll: Int,
    bankBalance: Int
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_progression_matrix"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Staff Office • Progression Impact",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Payroll ${formatCurrency(weeklyStaffPayroll)}/wk • Available ${formatCompactCurrency(bankBalance)}",
                        style = TelemetryTextStyles.statSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PitchMint.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "+${boosts.battingBonusPct + boosts.bowlingBonusPct + boosts.fitnessBonusPct}% TOTAL",
                        style = TelemetryTextStyles.statSmall.copy(
                            color = PitchMint,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusBoostPill(
                    area = CoachTrainingArea.BATTING,
                    bonusPct = boosts.battingBonusPct,
                    coachCount = boosts.battingCoaches,
                    accent = PitchMint,
                    icon = Icons.Filled.SportsCricket,
                    modifier = Modifier.weight(1f)
                )
                FocusBoostPill(
                    area = CoachTrainingArea.BOWLING,
                    bonusPct = boosts.bowlingBonusPct,
                    coachCount = boosts.bowlingCoaches,
                    accent = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Filled.Adjust,
                    modifier = Modifier.weight(1f)
                )
                FocusBoostPill(
                    area = CoachTrainingArea.FITNESS,
                    bonusPct = boosts.fitnessBonusPct,
                    coachCount = boosts.fitnessCoaches,
                    accent = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Filled.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FocusBoostPill(
    area: CoachTrainingArea,
    bonusPct: Int,
    coachCount: Int,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = (bonusPct / 45f).coerceIn(0.06f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "focus_${area.name}"
    )

    Surface(
        modifier = modifier.testTag("boost_pill_${area.name.lowercase()}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (coachCount > 0) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = area.label,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = area.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "+$bonusPct%",
                style = TelemetryTextStyles.statValue.copy(color = accent)
            )
            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(
                text = "$coachCount coach${if (coachCount == 1) "" else "es"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HiredCoachAssignmentCard(
    coach: StaffMember,
    onAssignFocus: (CoachTrainingArea) -> Unit
) {
    val currentFocus = coach.effectiveTrainingArea ?: CoachTrainingArea.BATTING
    val hasSynergy = coach.role == StaffRole.HEAD_COACH || coach.role.defaultTrainingArea == currentFocus
    val baseGain = ((coach.rating - 40).coerceAtLeast(10)) / 3
    val effectiveGain = baseGain + if (hasSynergy) 4 else 0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hired_coach_card_${coach.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${coach.rating}",
                            style = TelemetryTextStyles.statValue.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "RTG",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = coach.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasSynergy) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PitchMint.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "SYNERGY +$effectiveGain%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchMint,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CautionOchre.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "+$effectiveGain%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CautionOchre,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${coach.role.label} • ${coach.specialty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatCurrency(coach.weeklySalary)}/wk • ${currentFocus.boostSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Training Focus Selector (Batting / Bowling / Fitness)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ASSIGN TRAINING FOCUS AREA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CoachTrainingArea.entries.forEachIndexed { idx, area ->
                        val selected = currentFocus == area
                        SegmentedButton(
                            selected = selected,
                            onClick = { onAssignFocus(area) },
                            shape = SegmentedButtonDefaults.itemShape(idx, CoachTrainingArea.entries.size),
                            icon = {
                                Icon(
                                    imageVector = focusIconFor(area),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            modifier = Modifier.testTag("assign_focus_${coach.id}_${area.name.lowercase()}")
                        ) {
                            Text(area.label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveStaffCard(member: StaffMember) {
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun CoachCandidateHireCard(
    candidate: StaffMember,
    incumbent: StaffMember?,
    clubBalance: Int,
    onHire: (CoachTrainingArea?) -> Unit
) {
    val hireCost = candidate.weeklySalary * 4
    val canAfford = clubBalance >= hireCost
    val ratingDelta = incumbent?.let { candidate.rating - it.rating } ?: 0
    var selectedHireFocus by remember(candidate.id) {
        mutableStateOf(candidate.effectiveTrainingArea ?: CoachTrainingArea.BATTING)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("candidate_card_${candidate.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
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
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${candidate.rating}",
                            style = TelemetryTextStyles.statValue.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "RTG",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = candidate.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (incumbent != null) {
                            val deltaColor = when {
                                ratingDelta > 0 -> PitchMint
                                ratingDelta < 0 -> AlertCrimson
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = deltaColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${if (ratingDelta >= 0) "+" else ""}$ratingDelta vs current",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = deltaColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${candidate.role.label} • ${candidate.specialty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatCurrency(candidate.weeklySalary)}/wk • Signing Fee ${formatCompactCurrency(hireCost)}",
                        style = TelemetryTextStyles.statSmall,
                        color = if (canAfford) MaterialTheme.colorScheme.secondary else AlertCrimson
                    )
                }
            }

            if (candidate.isCoach) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CoachTrainingArea.entries.forEach { area ->
                            FilterChip(
                                selected = selectedHireFocus == area,
                                onClick = { selectedHireFocus = area },
                                label = { Text(area.shortLabel) },
                                modifier = Modifier.testTag("hire_focus_${candidate.id}_${area.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onHire(selectedHireFocus) },
                        enabled = canAfford,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hire_staff_${candidate.id}")
                    ) {
                        Text(if (canAfford) "Hire" else "Low Cash")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = { onHire(null) },
                        enabled = canAfford,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("hire_staff_${candidate.id}")
                    ) {
                        Text(if (canAfford) "Hire Director" else "Insufficient Cash")
                    }
                }
            }
        }
    }
}

private fun focusIconFor(area: CoachTrainingArea): ImageVector = when (area) {
    CoachTrainingArea.BATTING -> Icons.Filled.SportsCricket
    CoachTrainingArea.BOWLING -> Icons.Filled.Adjust
    CoachTrainingArea.FITNESS -> Icons.Filled.FitnessCenter
}
