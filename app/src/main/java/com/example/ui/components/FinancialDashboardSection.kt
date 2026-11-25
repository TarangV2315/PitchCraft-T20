package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CareerState
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class FinanceViewMode(val label: String) {
    WEEKLY_RATE("Weekly Rate"),
    SEASON_ACTUALS("Season P&L")
}

data class FinancialDashboardSnapshot(
    val ticketIncome: Int,
    val sponsorIncome: Int,
    val playerWages: Int,
    val staffWages: Int,
    val facilityUpkeep: Int
) {
    val totalIncome: Int get() = ticketIncome + sponsorIncome
    val totalWages: Int get() = playerWages + staffWages
    val netVsWages: Int get() = totalIncome - totalWages
    val netAfterUpkeep: Int get() = totalIncome - totalWages - facilityUpkeep
    val wageCoveragePercent: Int
        get() = if (totalWages <= 0) 100 else ((totalIncome * 100f) / totalWages).roundToInt()
}

fun buildFinancialSnapshot(
    career: CareerState,
    mode: FinanceViewMode
): FinancialDashboardSnapshot {
    return when (mode) {
        FinanceViewMode.WEEKLY_RATE -> FinancialDashboardSnapshot(
            ticketIncome = career.projectedWeeklyGateAverage,
            sponsorIncome = career.weeklySponsorIncome,
            playerWages = career.weeklyPlayerWages,
            staffWages = career.weeklyStaffWages,
            facilityUpkeep = career.budgets.weeklyUpkeep
        )
        FinanceViewMode.SEASON_ACTUALS -> FinancialDashboardSnapshot(
            ticketIncome = career.seasonTicketIncome,
            sponsorIncome = career.seasonSponsorIncome,
            playerWages = career.seasonPlayerWagesPaid,
            staffWages = career.seasonStaffWagesPaid,
            facilityUpkeep = career.seasonUpkeepPaid
        )
    }
}

@Composable
fun FinancialDashboardSection(
    career: CareerState,
    onOpenFinanceOffice: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasSeasonActuals = career.seasonTrackedIncome > 0 || career.seasonWageExpenses > 0
    var viewMode by remember(career.careerId) {
        mutableStateOf(
            if (hasSeasonActuals) FinanceViewMode.SEASON_ACTUALS else FinanceViewMode.WEEKLY_RATE
        )
    }

    val snapshot = remember(career, viewMode) { buildFinancialSnapshot(career, viewMode) }
    val seniorCount = remember(career.squad) { career.squad.count { !it.isYouthAcademy } }
    val activeSponsorCount = remember(career.sponsors) { career.sponsors.count { it.isActive } }
    val openSponsorCount = (3 - activeSponsorCount).coerceAtLeast(0)

    val periodSuffix = if (viewMode == FinanceViewMode.WEEKLY_RATE) "/wk" else " S${career.season}"
    val netColor = if (snapshot.netVsWages >= 0) PitchMint else AlertCrimson
    val coverageColor = when {
        snapshot.wageCoveragePercent >= 100 -> PitchMint
        snapshot.wageCoveragePercent >= 75 -> CautionOchre
        else -> AlertCrimson
    }

    val maxBarValue = maxOf(snapshot.totalIncome, snapshot.totalWages, 1).toFloat()
    val incomeBarRatio by animateFloatAsState(
        targetValue = (snapshot.totalIncome / maxBarValue).coerceIn(0.03f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "incomeBar"
    )
    val wageBarRatio by animateFloatAsState(
        targetValue = (snapshot.totalWages / maxBarValue).coerceIn(0.03f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "wageBar"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_dashboard_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header + Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Financial Dashboard",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Tickets & Sponsors vs. Wage Bill",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onOpenFinanceOffice != null) {
                TextButton(
                    onClick = onOpenFinanceOffice,
                    modifier = Modifier.testTag("finance_dashboard_manage_btn")
                ) {
                    Text("Controls")
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("finance_mode_toggle")
        ) {
            FinanceViewMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = viewMode == mode,
                    onClick = { viewMode = mode },
                    shape = SegmentedButtonDefaults.itemShape(idx, FinanceViewMode.entries.size),
                    modifier = Modifier.testTag("finance_mode_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = if (mode == FinanceViewMode.SEASON_ACTUALS) {
                            "Season ${career.season} P&L"
                        } else {
                            "Weekly Run Rate"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Primary Comparison Hero Card: Income vs Wage Expenses
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (viewMode == FinanceViewMode.WEEKLY_RATE) "NET VS. WAGES (EST.)" else "SEASON OPERATING NET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (snapshot.netVsWages >= 0) "+" else ""}${formatCurrency(snapshot.netVsWages)}$periodSuffix",
                            style = TelemetryTextStyles.scoreboardLarge.copy(fontSize = 22.sp, color = netColor)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = coverageColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, coverageColor.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${snapshot.wageCoveragePercent}%",
                                style = TelemetryTextStyles.statValue.copy(color = coverageColor)
                            )
                            Text(
                                text = "WAGES COVERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = coverageColor
                            )
                        }
                    }
                }

                // Dual Head-to-Head Proportional Comparison Bars
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComparisonBarRow(
                        label = "Club Income (Tickets + Sponsors)",
                        amountText = "+${formatCompactCurrency(snapshot.totalIncome)}$periodSuffix",
                        ratio = incomeBarRatio,
                        barColors = listOf(PitchMint, MaterialTheme.colorScheme.primary),
                        valueColor = PitchMint
                    )
                    ComparisonBarRow(
                        label = "Wage Expenses (Players + Staff)",
                        amountText = "-${formatCompactCurrency(snapshot.totalWages)}$periodSuffix",
                        ratio = wageBarRatio,
                        barColors = listOf(AlertCrimson, CautionOchre),
                        valueColor = AlertCrimson
                    )
                }

                if (viewMode == FinanceViewMode.SEASON_ACTUALS && !hasSeasonActuals) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Season ledger records after Round 1 kicks off. Switch to Weekly Run Rate for projected cashflow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Breakdown Cards: Income Sources vs Wage Categories
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sideBySide = maxWidth >= 580.dp
            if (sideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IncomeBreakdownCard(
                        career = career,
                        snapshot = snapshot,
                        viewMode = viewMode,
                        activeSponsorCount = activeSponsorCount,
                        periodSuffix = periodSuffix,
                        modifier = Modifier.weight(1f)
                    )
                    ExpenseBreakdownCard(
                        career = career,
                        snapshot = snapshot,
                        viewMode = viewMode,
                        seniorCount = seniorCount,
                        periodSuffix = periodSuffix,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IncomeBreakdownCard(
                        career = career,
                        snapshot = snapshot,
                        viewMode = viewMode,
                        activeSponsorCount = activeSponsorCount,
                        periodSuffix = periodSuffix
                    )
                    ExpenseBreakdownCard(
                        career = career,
                        snapshot = snapshot,
                        viewMode = viewMode,
                        seniorCount = seniorCount,
                        periodSuffix = periodSuffix
                    )
                }
            }
        }

        // Actionable Commercial Advisory Banner
        val adviceText = when {
            openSponsorCount > 0 && career.isSponsorshipWindowOpen ->
                "Pre-Season Tip: $openSponsorCount sponsorship slot(s) still open in Week 1 — negotiate & sign deals in Office before they freeze."
            openSponsorCount > 0 && !career.isSponsorshipWindowOpen ->
                "Sponsorships Frozen (Wk ${career.week}): $openSponsorCount slot(s) locked until Season ${career.season + 1} Pre-Season."
            snapshot.netVsWages < 0 ->
                "Payroll Alert: Wages exceed current ticket & sponsor income by ${formatCompactCurrency(-snapshot.netVsWages)}$periodSuffix. Review ticket tier or trim high-wage squad backups."
            else ->
                "Healthy Margins: Ticket gates and commercial retainers cover 100% of player and staff payroll."
        }
        val highlightWarning = (openSponsorCount > 0 && career.isSponsorshipWindowOpen) || snapshot.netVsWages < 0
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = if (highlightWarning) {
                    CautionOchre.copy(alpha = 0.45f)
                } else {
                    PitchMint.copy(alpha = 0.35f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (snapshot.netVsWages < 0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                    contentDescription = "Financial insight",
                    tint = if (openSponsorCount > 0 || snapshot.netVsWages < 0) CautionOchre else PitchMint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = adviceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    amountText: String,
    ratio: Float,
    barColors: List<Color>,
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                style = TelemetryTextStyles.statSmall.copy(
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(barColors))
            )
        }
    }
}

@Composable
private fun IncomeBreakdownCard(
    career: CareerState,
    snapshot: FinancialDashboardSnapshot,
    viewMode: FinanceViewMode,
    activeSponsorCount: Int,
    periodSuffix: String,
    modifier: Modifier = Modifier
) {
    val totalIncome = snapshot.totalIncome.coerceAtLeast(1)
    val ticketShare = snapshot.ticketIncome.toFloat() / totalIncome
    val sponsorShare = snapshot.sponsorIncome.toFloat() / totalIncome

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("finance_income_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Filled.ArrowCircleUp,
                        contentDescription = null,
                        tint = PitchMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Club Income", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = "+${formatCurrency(snapshot.totalIncome)}$periodSuffix",
                    style = TelemetryTextStyles.statValue.copy(color = PitchMint)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BreakdownStreamRow(
                icon = Icons.Filled.ConfirmationNumber,
                title = "Ticket Sales",
                subtitle = if (viewMode == FinanceViewMode.WEEKLY_RATE) {
                    "${formatCompactCurrency(career.projectedHomeTicketRevenue)}/home gate • ${career.projectedAttendance} fans @ $${career.budgets.ticketTier.pricePerSeat}"
                } else {
                    "${career.homeMatchesHosted} home fixture(s) • ${career.budgets.ticketTier.label}"
                },
                amount = "+${formatCurrency(snapshot.ticketIncome)}",
                shareRatio = ticketShare,
                accentColor = PitchMint
            )

            BreakdownStreamRow(
                icon = Icons.Filled.Handshake,
                title = "Sponsorships",
                subtitle = if (viewMode == FinanceViewMode.WEEKLY_RATE) {
                    "$activeSponsorCount of 3 active commercial deals"
                } else {
                    "Weekly retainers + match win/cup bonuses"
                },
                amount = "+${formatCurrency(snapshot.sponsorIncome)}",
                shareRatio = sponsorShare,
                accentColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ExpenseBreakdownCard(
    career: CareerState,
    snapshot: FinancialDashboardSnapshot,
    viewMode: FinanceViewMode,
    seniorCount: Int,
    periodSuffix: String,
    modifier: Modifier = Modifier
) {
    val totalWages = snapshot.totalWages.coerceAtLeast(1)
    val playerShare = snapshot.playerWages.toFloat() / totalWages
    val staffShare = snapshot.staffWages.toFloat() / totalWages

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("finance_expenses_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Filled.ArrowCircleDown,
                        contentDescription = null,
                        tint = AlertCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Wage Expenses", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = "-${formatCurrency(snapshot.totalWages)}$periodSuffix",
                    style = TelemetryTextStyles.statValue.copy(color = AlertCrimson)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BreakdownStreamRow(
                icon = Icons.Filled.Groups,
                title = "Player Wages",
                subtitle = "$seniorCount senior squad contracts",
                amount = "-${formatCurrency(snapshot.playerWages)}",
                shareRatio = playerShare,
                accentColor = AlertCrimson
            )

            BreakdownStreamRow(
                icon = Icons.Filled.Badge,
                title = "Staff Wages",
                subtitle = "${career.staff.size} coaches, scouts & directors",
                amount = "-${formatCurrency(snapshot.staffWages)}",
                shareRatio = staffShare,
                accentColor = CautionOchre
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operations Upkeep (Academy/Scout/Med)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-${formatCompactCurrency(snapshot.facilityUpkeep)}$periodSuffix",
                    style = TelemetryTextStyles.statSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BreakdownStreamRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amount: String,
    shareRatio: Float,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amount,
                style = TelemetryTextStyles.statValue.copy(color = accentColor)
            )
        }
        LinearProgressIndicator(
            progress = { shareRatio.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}
