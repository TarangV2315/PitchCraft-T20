package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.abs

enum class DashboardSquadFilter(val label: String) {
    ALL("All Squad"),
    STARTING_XI("Starting XI"),
    BATTERS("Batters"),
    ALL_ROUNDERS("All-Rounders"),
    BOWLERS("Bowlers")
}

val PLAYER_PORTRAIT_DRAWABLES: List<Int> = listOf(
    R.drawable.img_portrait_south_asian_batter,
    R.drawable.img_portrait_caribbean_finisher,
    R.drawable.img_portrait_aussie_pacer,
    R.drawable.img_portrait_english_keeper,
    R.drawable.img_portrait_veteran_spinner,
    R.drawable.img_player_batter,
    R.drawable.img_player_pace,
    R.drawable.img_player_spinner
)

@DrawableRes
fun playerPortraitDrawable(player: Player): Int {
    val variant = player.portraitVariant
    if (variant in PLAYER_PORTRAIT_DRAWABLES.indices) {
        return PLAYER_PORTRAIT_DRAWABLES[variant]
    }
    val fallbackIndex = abs((player.id + player.name).hashCode()) % PLAYER_PORTRAIT_DRAWABLES.size
    return PLAYER_PORTRAIT_DRAWABLES[fallbackIndex]
}

@Composable
fun PlayerAvatar(
    player: Player,
    modifier: Modifier = Modifier,
    sizeDp: Int = 52,
    showOvrBadge: Boolean = true
) {
    val ringColor = when {
        player.injury.isInjured -> AlertCrimson
        player.fitness < 72 -> CautionOchre
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier.size(sizeDp.dp)
    ) {
        Image(
            painter = painterResource(id = playerPortraitDrawable(player)),
            contentDescription = "${player.name} portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, ringColor.copy(alpha = 0.75f), CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )
        if (showOvrBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "${player.overall}",
                    style = TelemetryTextStyles.statSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun SquadPlayerDashboardCard(
    player: Player,
    isSelectedInXI: Boolean = false,
    isImpactSub: Boolean = false,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val fitnessColor = when {
        player.injury.isInjured -> AlertCrimson
        player.fitness >= 78 -> PitchMint
        player.fitness >= 62 -> CautionOchre
        else -> AlertCrimson
    }
    val fitnessProgress = if (player.injury.isInjured) {
        (player.fitness / 100f).coerceIn(0.1f, 0.6f)
    } else {
        (player.fitness / 100f).coerceIn(0f, 1f)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("squad_dashboard_card_${player.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelectedInXI) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Photo + Identity + Market Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerAvatar(player = player, sizeDp = 52, showOvrBadge = true)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${player.nationality} • ${player.age}y • ${player.battingHand.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelectedInXI) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "XI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isImpactSub) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "SUB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                if (trailingAction != null) {
                    trailingAction()
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = formatCompactCurrency(player.marketValue),
                                style = TelemetryTextStyles.statValue.copy(
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                            Text(
                                text = "VALUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Individual Player Stats Strip (Matches/Batting/Bowling or Core Attributes)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val hasMatches = player.seasonStats.matches > 0
                    StatCell(
                        label = "BAT",
                        value = if (hasMatches) {
                            "${player.seasonStats.runs}R"
                        } else {
                            "${player.attributes.battingTiming}/${player.attributes.powerHitting}"
                        },
                        sub = if (hasMatches) "SR ${"%.0f".format(player.seasonStats.strikeRate)}" else "TIM/PWR",
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "BOWL",
                        value = if (hasMatches) {
                            "${player.seasonStats.wickets}W"
                        } else if (player.canBowl) {
                            "${maxOf(player.attributes.paceBowling, player.attributes.spinVariation)}"
                        } else {
                            "—"
                        },
                        sub = if (hasMatches) {
                            if (player.seasonStats.ballsBowled > 0) "EC ${"%.1f".format(player.seasonStats.economyRate)}" else "${player.seasonStats.matches} MAT"
                        } else if (player.canBowl) {
                            "DTH ${player.attributes.deathExecution}"
                        } else {
                            "FLD ${player.attributes.fielding}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "FORM",
                        value = "${player.form}",
                        sub = "POT ${player.potential}",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(0.85f)
                    )
                    StatCell(
                        label = "WAGE",
                        value = formatCompactCurrency(player.contract.weeklyWage),
                        sub = "${player.contract.weeksRemaining}w left",
                        accent = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(0.95f)
                    )
                }
            }

            // Fitness Bar & Injury / Training Status Footer
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (player.injury.isInjured) Icons.Filled.MedicalServices else Icons.Filled.Favorite,
                            contentDescription = "Fitness",
                            tint = fitnessColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (player.injury.isInjured) {
                                "${player.injury.injuryName} (${player.injury.weeksRemaining}w)"
                            } else {
                                "Match Fitness"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (player.injury.isInjured) AlertCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${player.fitness}%",
                        style = TelemetryTextStyles.statSmall.copy(
                            color = fitnessColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                LinearProgressIndicator(
                    progress = { fitnessProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = fitnessColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    sub: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TelemetryTextStyles.statValue.copy(color = accent),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SquadDashboardHeaderCard(
    seniorSquad: List<Player>,
    selectedFilter: DashboardSquadFilter,
    onSelectFilter: (DashboardSquadFilter) -> Unit,
    onViewFullSquad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMarketValue = remember(seniorSquad) { seniorSquad.sumOf { it.marketValue } }
    val avgFitness = remember(seniorSquad) {
        if (seniorSquad.isEmpty()) 0 else seniorSquad.sumOf { it.fitness } / seniorSquad.size
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Club Squad (${seniorSquad.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Squad Value ${formatCompactCurrency(totalMarketValue)} • Avg Fitness $avgFitness%",
                    style = TelemetryTextStyles.statSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TextButton(
                onClick = onViewFullSquad,
                modifier = Modifier.testTag("hq_view_full_squad_btn")
            ) {
                Text("Manage All")
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DashboardSquadFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.testTag("hq_squad_filter_${filter.name.lowercase()}")
                )
            }
        }
    }
}

fun filterDashboardSquad(
    seniorSquad: List<Player>,
    selectedXIIds: List<String>,
    filter: DashboardSquadFilter
): List<Player> {
    val sorted = seniorSquad.sortedByDescending { it.overall }
    return when (filter) {
        DashboardSquadFilter.ALL -> sorted
        DashboardSquadFilter.STARTING_XI -> sorted.filter { selectedXIIds.contains(it.id) }
        DashboardSquadFilter.BATTERS -> sorted.filter {
            it.role in listOf(
                T20Role.OPENER, T20Role.TOP_ORDER, T20Role.MIDDLE_ORDER,
                T20Role.FINISHER, T20Role.WICKETKEEPER
            )
        }
        DashboardSquadFilter.ALL_ROUNDERS -> sorted.filter { it.role == T20Role.ALL_ROUNDER }
        DashboardSquadFilter.BOWLERS -> sorted.filter {
            it.role in listOf(T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER, T20Role.SPINNER)
        }
    }
}
