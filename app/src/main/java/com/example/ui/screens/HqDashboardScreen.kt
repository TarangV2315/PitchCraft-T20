package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.ui.MainTab
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HqDashboardScreen(
    career: CareerState,
    onPrepareMatchday: () -> Unit,
    onStartNextSeason: () -> Unit,
    onAdvanceWeek: () -> Unit,
    onToggleDelegation: (DelegationArea) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onExitToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val divClubs = career.currentDivision?.clubs.orEmpty()
    val sortedClubs = SeasonProgressionEngine.sortDivision(divClubs)
    val userRank = sortedClubs.indexOfFirst { it.clubId == career.clubId }.let { if (it >= 0) it + 1 else 1 }
    val nextFixture = career.nextUserFixture
    val netWeekly = career.weeklySponsorIncome - (career.weeklyPlayerWages + career.weeklyStaffWages + career.budgets.weeklyUpkeep)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Club Header Banner
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_stadium_hero),
                        contentDescription = "Club stadium",
                        modifier = Modifier
                            .matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ClubBadge(career.clubShortCode, career.divisionTier, sizeDp = 48)
                                Column {
                                    Text(
                                        text = career.clubName,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = "Season ${career.season} • Week ${career.week} • Tier ${career.divisionTier} (#$userRank)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onExitToLauncher,
                                modifier = Modifier.testTag("exit_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderShared,
                                    contentDescription = "Switch Career Save",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Next Matchday or Season Complete CTA Card
                        if (career.seasonComplete) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = career.seasonSummaryBanner ?: "Season ${career.season} Complete",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Button(
                                        onClick = onStartNextSeason,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_next_season_button")
                                    ) {
                                        Icon(Icons.Filled.EmojiEvents, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Launch Season ${career.season + 1}")
                                    }
                                }
                            }
                        } else if (nextFixture != null) {
                            val opponent = if (nextFixture.homeClubId == career.clubId) {
                                "vs ${nextFixture.awayClubName} (Home)"
                            } else {
                                "@ ${nextFixture.homeClubName} (Away)"
                            }
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
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
                                        Text(
                                            text = if (nextFixture.isPlayoff) nextFixture.playoffLabel.uppercase()
                                            else "ROUND ${nextFixture.round} OF ${career.totalRounds}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = nextFixture.pitchType.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    Text(
                                        text = opponent,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = nextFixture.pitchType.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onPrepareMatchday,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("hq_matchday_cta")
                                    ) {
                                        Icon(Icons.Filled.SportsCricket, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Matchday Prep & Live Tactics")
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = onAdvanceWeek,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("advance_week_button")
                            ) {
                                Text("Advance Calendar Week")
                            }
                        }
                    }
                }
            }
        }

        // Executive Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Club Cash",
                        value = formatCompactCurrency(career.bankBalance),
                        subtitle = "${if (netWeekly >= 0) "+" else ""}${formatCompactCurrency(netWeekly)}/wk net",
                        icon = Icons.Filled.AccountBalanceWallet,
                        accentColor = if (netWeekly >= 0) PitchMint else CautionOchre,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateTab(MainTab.OFFICE) }
                    )
                    MetricTile(
                        label = "Board Confidence",
                        value = "${career.boardConfidence}%",
                        subtitle = "Rep ${career.managerReputation} • ${career.trophiesWon} Cups",
                        icon = Icons.Filled.VerifiedUser,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val injuredCount = career.squad.count { it.injury.isInjured }
                    val expiringCount = career.squad.count { !it.isYouthAcademy && it.contract.weeksRemaining <= 14 }
                    MetricTile(
                        label = "Squad & Rehab",
                        value = "${career.squad.count { !it.isYouthAcademy }} Senior",
                        subtitle = if (injuredCount > 0) "$injuredCount injured • $expiringCount expiring" else "Full fitness • $expiringCount expiring",
                        icon = Icons.Filled.Groups,
                        accentColor = if (injuredCount > 0) CautionOchre else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateTab(MainTab.SQUAD) }
                    )
                    MetricTile(
                        label = "Sponsor Retainer",
                        value = "${formatCompactCurrency(career.weeklySponsorIncome)}/wk",
                        subtitle = "${career.sponsors.count { it.isActive }} of 3 active deals",
                        icon = Icons.Filled.Handshake,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateTab(MainTab.OFFICE) }
                    )
                }
            }
        }

        // Delegation Quick Control Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Staff Delegation Controls",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { onNavigateTab(MainTab.OFFICE) }) {
                        Text("Hire Staff")
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DelegationArea.entries) { area ->
                        val isDelegated = career.delegation.isDelegated(area)
                        FilterChip(
                            selected = isDelegated,
                            onClick = { onToggleDelegation(area) },
                            label = {
                                Text(
                                    text = "${area.label}: ${if (isDelegated) "Staff" else "You"}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isDelegated) Icons.Filled.AutoAwesome else Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("hq_delegate_${area.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Staff Briefing Inbox
        item {
            Text(
                text = "Staff Briefing & Activity Feed",
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(career.inbox, key = { it.id }) { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(enabled = msg.relatedPlayerId != null) {
                        msg.relatedPlayerId?.let(onOpenPlayer)
                    },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = if (msg.isUrgent) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${msg.senderRole.label} • ${msg.senderName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (msg.isUrgent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "S${msg.season} W${msg.week}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(msg.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
