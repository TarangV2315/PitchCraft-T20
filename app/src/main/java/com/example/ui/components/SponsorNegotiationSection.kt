package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.simulation.SponsorNegotiationEngine
import com.example.ui.theme.*

@Composable
fun SponsorWindowStatusBanner(
    career: CareerState,
    modifier: Modifier = Modifier
) {
    val isOpen = career.isSponsorshipWindowOpen
    val accent = if (isOpen) PitchMint else AlertCrimson
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsor_window_banner"),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = if (isOpen) "Sponsorship Window Open" else "Sponsorship Window Frozen",
                tint = accent
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (isOpen) {
                        "Pre-Season Sponsorship Window Open (Week 1)"
                    } else {
                        "Sponsorship Selection Frozen for Season ${career.season}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = accent
                )
                Text(
                    text = if (isOpen) {
                        "Choose or negotiate your Title, Kit & Stadium contracts now. Portfolio freezes after Week 1."
                    } else {
                        "Brand selection is locked mid-season (Week ${career.week}). Re-opens at Season ${career.season + 1} Pre-Season."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ClubCommercialLeverageCard(
    leverage: SponsorLeverageProfile,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsor_leverage_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Commercial Negotiation Leverage",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${leverage.clubCity} Market • Max +${leverage.maxNegotiationLiftPct}% Brand Uplift",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${leverage.overallLeverageScore}/100",
                        style = TelemetryTextStyles.statValue.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LeveragePillarRow(
                label = "City Demography",
                score = leverage.cityDemographyScore,
                summary = leverage.cityDemographySummary,
                accent = MaterialTheme.colorScheme.primary
            )
            LeveragePillarRow(
                label = "Fan Buying Pattern",
                score = leverage.buyingPatternScore,
                summary = leverage.buyingPatternSummary,
                accent = MaterialTheme.colorScheme.tertiary
            )
            LeveragePillarRow(
                label = "Tournament History",
                score = leverage.performanceHistoryScore,
                summary = leverage.performanceHistorySummary,
                accent = MaterialTheme.colorScheme.secondary
            )
            LeveragePillarRow(
                label = "Calibre of Players",
                score = leverage.playerCalibreScore,
                summary = leverage.playerCalibreSummary,
                accent = PitchMint
            )
        }
    }
}

@Composable
private fun LeveragePillarRow(
    label: String,
    score: Int,
    summary: String,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "$score/100",
                style = TelemetryTextStyles.statSmall.copy(color = accent, fontWeight = FontWeight.Bold)
            )
        }
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActiveSponsorPortfolioStrip(
    sponsors: List<SponsorDeal>,
    modifier: Modifier = Modifier
) {
    val active = sponsors.filter { it.isActive }
    val weeklyBase = active.sumOf { it.weeklyPayout }
    val weeklyBranding = active.sumOf { it.playerBrandingWeekly }
    val winSum = active.sumOf { it.winBonus }
    val fiftySum = active.sumOf { it.perFiftyBonus }
    val centSum = active.sumOf { it.perCenturyBonus }
    val haulSum = active.sumOf { it.perThreeWicketHaulBonus }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_sponsor_portfolio_strip"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
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
                Text("Active Commercial Portfolio (${active.size}/3)", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${formatCompactCurrency(weeklyBase + weeklyBranding)}/wk",
                    style = TelemetryTextStyles.statValue.copy(color = PitchMint)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortfolioStatPill("Base", "${formatCompactCurrency(weeklyBase)}/wk", Modifier.weight(1f))
                PortfolioStatPill("Branding", "${formatCompactCurrency(weeklyBranding)}/wk", Modifier.weight(1f))
                PortfolioStatPill("Per Win", "+${formatCompactCurrency(winSum)}", Modifier.weight(1f))
            }
            Text(
                text = "Match Milestones: +${formatCompactCurrency(fiftySum)} / 50  •  +${formatCompactCurrency(centSum)} / 100  •  +${formatCompactCurrency(haulSum)} / 3W Haul",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PortfolioStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun SponsorCategoryFilterBar(
    selectedCategory: SponsorCategory?,
    onSelectCategory: (SponsorCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All Brands") },
                modifier = Modifier.testTag("sponsor_filter_all")
            )
        }
        items(SponsorCategory.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = { Text(category.label) },
                modifier = Modifier.testTag("sponsor_filter_${category.name.lowercase()}")
            )
        }
    }
}

@Composable
fun SponsorBrandOfferCard(
    deal: SponsorDeal,
    isWindowOpen: Boolean,
    onSignDirect: () -> Unit,
    onNegotiate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val structureColor = when (deal.structureType) {
        SponsorStructureType.GUARANTEED_ANCHOR -> MaterialTheme.colorScheme.tertiary
        SponsorStructureType.BALANCED -> MaterialTheme.colorScheme.primary
        SponsorStructureType.PERFORMANCE_ESCALATOR -> MaterialTheme.colorScheme.secondary
        SponsorStructureType.STAR_BRANDING -> PitchMint
    }
    val displayedDeal = remember(deal) {
        if (deal.projectedSeasonMax > 0) deal else SponsorNegotiationEngine.withProjectedTotals(deal)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sponsor_card_${deal.id}"),
        shape = MaterialTheme.shapes.medium,
        color = if (deal.isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (deal.isActive) {
            BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: Brand, Category, Archetype tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(deal.brandName, style = MaterialTheme.typography.titleSmall)
                        if (deal.negotiatedPct != 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PitchMint.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "${if (deal.negotiatedPct > 0) "+" else ""}${deal.negotiatedPct}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PitchMint,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${deal.category.label} • ${deal.industry} • ${deal.seasonsRemaining} season(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = structureColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = deal.structureType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = structureColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            if (deal.contractSummary.isNotBlank()) {
                Text(
                    text = deal.contractSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contract Financials Breakdown: Base + Player Branding vs Performance Bonuses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("GUARANTEED + BRANDING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Base ${formatCompactCurrency(deal.weeklyPayout)}/wk",
                            style = TelemetryTextStyles.statSmall.copy(color = PitchMint, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Player Brand +${formatCompactCurrency(deal.playerBrandingWeekly)}/wk",
                            style = TelemetryTextStyles.statSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Signing ${formatCompactCurrency(deal.signingBonus)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("TEAM & PLAYER BONUSES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Win +${formatCompactCurrency(deal.winBonus)} • Cup +${formatCompactCurrency(deal.trophyBonus)}",
                            style = TelemetryTextStyles.statSmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "50: +${formatCompactCurrency(deal.perFiftyBonus)} • 100: +${formatCompactCurrency(deal.perCenturyBonus)}",
                            style = TelemetryTextStyles.statSmall
                        )
                        Text(
                            text = "3W Haul: +${formatCompactCurrency(deal.perThreeWicketHaulBonus)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Projected Season Total Deviation + City Fit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Est. Season Total: ${formatCompactCurrency(displayedDeal.projectedSeasonMin)} – ${formatCompactCurrency(displayedDeal.projectedSeasonMax)}",
                    style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = deal.cityFitSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Controls: Respect Week 1 Window vs Frozen State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (deal.isActive) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (isWindowOpen) "ACTIVE (PRE-SEASON)" else "LOCKED ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Text(
                        text = deal.structureType.riskTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isWindowOpen) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onNegotiate,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("negotiate_sponsor_${deal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (deal.isActive) "Renegotiate" else "Negotiate")
                        }
                        if (!deal.isActive) {
                            FilledTonalButton(
                                onClick = onSignDirect,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sign_sponsor_${deal.id}")
                            ) {
                                Text("Sign")
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.testTag("frozen_sponsor_${deal.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Frozen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Frozen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SponsorNegotiationDialog(
    session: SponsorNegotiationSession,
    onUpdateTerms: (
        baseWeekly: Int,
        brandingWeekly: Int,
        winBonus: Int,
        fiftyBonus: Int,
        centuryBonus: Int,
        haulBonus: Int
    ) -> Unit,
    onSubmitOffer: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val previewDeal = remember(
        session.deal,
        session.offeredBaseWeekly,
        session.offeredBrandingWeekly,
        session.offeredWinBonus,
        session.offeredFiftyBonus,
        session.offeredCenturyBonus,
        session.offeredHaulBonus
    ) {
        SponsorNegotiationEngine.withProjectedTotals(
            session.deal.copy(
                weeklyPayout = session.offeredBaseWeekly,
                playerBrandingWeekly = session.offeredBrandingWeekly,
                winBonus = session.offeredWinBonus,
                perFiftyBonus = session.offeredFiftyBonus,
                perCenturyBonus = session.offeredCenturyBonus,
                perThreeWicketHaulBonus = session.offeredHaulBonus
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .testTag("sponsor_negotiation_dialog"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Negotiate: ${session.deal.brandName}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${session.deal.category.label} • ${session.deal.structureType.label} • Round ${session.round}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close negotiation")
                    }
                }

                // Brand Patience Meter & Leverage Snapshot
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Brand Patience", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${session.brandPatience}%",
                                style = TelemetryTextStyles.statSmall.copy(
                                    color = if (session.brandPatience > 45) PitchMint else AlertCrimson,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        LinearProgressIndicator(
                            progress = { session.brandPatience / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (session.brandPatience > 45) PitchMint else AlertCrimson,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        Text(
                            text = "Leverage ${session.leverage.overallLeverageScore}/100 (City ${session.leverage.cityDemographyScore} • Buying ${session.leverage.buyingPatternScore} • History ${session.leverage.performanceHistoryScore} • Calibre ${session.leverage.playerCalibreScore})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Brand Representative Feedback
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = session.brandMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (!session.isAccepted && !session.isWalkedAway) {
                    TermStepperRow(
                        label = "Weekly Base Retainer",
                        askValue = session.askBaseWeekly,
                        offeredValue = session.offeredBaseWeekly,
                        step = 500,
                        minVal = (session.deal.weeklyPayout * 0.6).toInt().coerceAtLeast(1000),
                        suffix = "/wk",
                        tag = "neg_base",
                        onChange = { next ->
                            onUpdateTerms(
                                next,
                                session.offeredBrandingWeekly,
                                session.offeredWinBonus,
                                session.offeredFiftyBonus,
                                session.offeredCenturyBonus,
                                session.offeredHaulBonus
                            )
                        }
                    )
                    TermStepperRow(
                        label = "Player Branding Revenue",
                        askValue = session.askBrandingWeekly,
                        offeredValue = session.offeredBrandingWeekly,
                        step = 300,
                        minVal = 0,
                        suffix = "/wk",
                        tag = "neg_branding",
                        onChange = { next ->
                            onUpdateTerms(
                                session.offeredBaseWeekly,
                                next,
                                session.offeredWinBonus,
                                session.offeredFiftyBonus,
                                session.offeredCenturyBonus,
                                session.offeredHaulBonus
                            )
                        }
                    )
                    TermStepperRow(
                        label = "Per-Win Team Bonus",
                        askValue = session.askWinBonus,
                        offeredValue = session.offeredWinBonus,
                        step = 500,
                        minVal = 0,
                        suffix = "/win",
                        tag = "neg_win",
                        onChange = { next ->
                            onUpdateTerms(
                                session.offeredBaseWeekly,
                                session.offeredBrandingWeekly,
                                next,
                                session.offeredFiftyBonus,
                                session.offeredCenturyBonus,
                                session.offeredHaulBonus
                            )
                        }
                    )
                    TermStepperRow(
                        label = "Per-Fifty Bonus",
                        askValue = session.askFiftyBonus,
                        offeredValue = session.offeredFiftyBonus,
                        step = 300,
                        minVal = 0,
                        suffix = "/50",
                        tag = "neg_fifty",
                        onChange = { next ->
                            onUpdateTerms(
                                session.offeredBaseWeekly,
                                session.offeredBrandingWeekly,
                                session.offeredWinBonus,
                                next,
                                session.offeredCenturyBonus,
                                session.offeredHaulBonus
                            )
                        }
                    )
                    TermStepperRow(
                        label = "Per-Century Bonus",
                        askValue = session.askCenturyBonus,
                        offeredValue = session.offeredCenturyBonus,
                        step = 1000,
                        minVal = 0,
                        suffix = "/100",
                        tag = "neg_century",
                        onChange = { next ->
                            onUpdateTerms(
                                session.offeredBaseWeekly,
                                session.offeredBrandingWeekly,
                                session.offeredWinBonus,
                                session.offeredFiftyBonus,
                                next,
                                session.offeredHaulBonus
                            )
                        }
                    )
                    TermStepperRow(
                        label = "Per-3W Haul Bonus",
                        askValue = session.askHaulBonus,
                        offeredValue = session.offeredHaulBonus,
                        step = 300,
                        minVal = 0,
                        suffix = "/3W",
                        tag = "neg_haul",
                        onChange = { next ->
                            onUpdateTerms(
                                session.offeredBaseWeekly,
                                session.offeredBrandingWeekly,
                                session.offeredWinBonus,
                                session.offeredFiftyBonus,
                                session.offeredCenturyBonus,
                                next
                            )
                        }
                    )
                }

                // Live Season Value Preview
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Projected Season Return", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${formatCompactCurrency(previewDeal.projectedSeasonMin)} – ${formatCompactCurrency(previewDeal.projectedSeasonMax)}",
                            style = TelemetryTextStyles.statValue.copy(color = PitchMint)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("close_sponsor_negotiation_btn")
                    ) {
                        Text(if (session.isAccepted || session.isWalkedAway) "Done" else "Cancel")
                    }
                    if (!session.isAccepted && !session.isWalkedAway) {
                        Button(
                            onClick = onSubmitOffer,
                            modifier = Modifier
                                .weight(1.4f)
                                .minimumInteractiveComponentSize()
                                .testTag("submit_sponsor_offer_btn")
                        ) {
                            Text("Propose Terms")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermStepperRow(
    label: String,
    askValue: Int,
    offeredValue: Int,
    step: Int,
    minVal: Int,
    suffix: String,
    tag: String,
    onChange: (Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text(
                    text = "Offer ${formatCurrency(offeredValue)}$suffix (Ask ${formatCurrency(askValue)})",
                    style = TelemetryTextStyles.statSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = { onChange((offeredValue - step).coerceAtLeast(minVal)) },
                    enabled = offeredValue > minVal,
                    modifier = Modifier.testTag("${tag}_minus")
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
                }
                FilledTonalIconButton(
                    onClick = { onChange(offeredValue + step) },
                    modifier = Modifier.testTag("${tag}_plus")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase $label")
                }
            }
        }
    }
}
