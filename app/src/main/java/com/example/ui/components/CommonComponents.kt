package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val prefix = if (amount < 0) "-$" else "$"
    return "$prefix${formatter.format(abs(amount))}"
}

fun formatCompactCurrency(amount: Int): String {
    val sign = if (amount < 0) "-" else ""
    val a = abs(amount)
    return when {
        a >= 1_000_000 -> "$sign$${"%.2f".format(a / 1_000_000.0)}M"
        a >= 1_000 -> "$sign$${"%.1f".format(a / 1_000.0)}K"
        else -> "$sign$$a"
    }
}

@Composable
fun ClubBadge(
    shortCode: String,
    tier: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 44
) {
    val gradient = when (tier) {
        1 -> listOf(Color(0xFF0D6E4C), Color(0xFFB87A00))
        2 -> listOf(Color(0xFF005C73), Color(0xFF0D6E4C))
        else -> listOf(Color(0xFF26423A), Color(0xFF142923))
    }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortCode.take(3).uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = TelemetryTextStyles.statValue.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RoleChip(role: T20Role, modifier: Modifier = Modifier) {
    val color = when (role) {
        T20Role.OPENER, T20Role.TOP_ORDER, T20Role.MIDDLE_ORDER, T20Role.FINISHER ->
            MaterialTheme.colorScheme.primary
        T20Role.WICKETKEEPER -> MaterialTheme.colorScheme.secondary
        T20Role.ALL_ROUNDER -> MaterialTheme.colorScheme.tertiary
        T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER, T20Role.SPINNER ->
            Color(0xFF4FD8A2)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = role.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DelegationBanner(
    area: DelegationArea,
    isDelegated: Boolean,
    staffOwnerName: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isDelegated) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "delegationBg"
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDelegated) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isDelegated) Icons.Outlined.SmartToy else Icons.Filled.Person,
                    contentDescription = "Control mode",
                    tint = if (isDelegated) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = if (isDelegated) "Delegated to ${area.staffOwner.label}" else "Direct Manager Control",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (isDelegated) {
                            "${staffOwnerName ?: "Staff"} automates ${area.label.lowercase()}"
                        } else {
                            "You decide ${area.label.lowercase()} manually"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isDelegated,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("delegate_switch_${area.name.lowercase()}")
            )
        }
    }
}

@Composable
fun StaffRecommendationCard(
    rec: StaffRecommendation,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (rec.priorityHigh) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (rec.priorityHigh) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = rec.staffRole.label,
                        tint = if (rec.priorityHigh) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${rec.staffRole.label} • ${rec.staffName} (${rec.staffRating})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (rec.priorityHigh) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "PRIORITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = rec.title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = rec.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onApply,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("apply_rec_${rec.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(rec.actionLabel)
                }
            }
        }
    }
}

@Composable
fun AttributeBar(
    label: String,
    value: Int,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "$value",
                style = TelemetryTextStyles.statSmall.copy(color = accent, fontWeight = FontWeight.Bold)
            )
        }
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
fun PlayerSummaryCard(
    player: Player,
    isSelectedInXI: Boolean = false,
    isImpactSub: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("player_card_${player.id}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelectedInXI) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (isSelectedInXI) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PlayerAvatar(
                player = player,
                sizeDp = 46,
                showOvrBadge = true
            )

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
                        overflow = TextOverflow.Ellipsis
                    )
                    RoleChip(player.role)
                    if (isImpactSub) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "SUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${player.nationality} • ${player.age}y",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (player.injury.isInjured) {
                        Text(
                            text = "INJ (${player.injury.weeksRemaining}w)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertCrimson
                        )
                    } else {
                        Text(
                            text = "FIT ${player.fitness}% • FRM ${player.form}",
                            style = TelemetryTextStyles.statSmall,
                            color = if (player.fitness < 72) CautionOchre else PitchMint
                        )
                    }
                }
                Text(
                    text = if (player.seasonStats.matches > 0) {
                        "${player.seasonStats.matches}M • ${player.seasonStats.runs}R (SR ${"%.0f".format(player.seasonStats.strikeRate)}) • ${player.seasonStats.wickets}W"
                    } else {
                        "Drill: ${player.trainingFocus.label} • ${formatCompactCurrency(player.contract.weeklyWage)}/wk"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailingAction != null) {
                trailingAction()
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Inspect player",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SquadViewModeToggle(
    mode: SquadViewMode,
    onModeChange: (SquadViewMode) -> Unit,
    testTagPrefix: String = "squad",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SquadViewMode.entries.forEach { entry ->
                val selected = mode == entry
                val tagSuffix = entry.name.lowercase()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeChange(entry) }
                        .testTag("${testTagPrefix}_view_toggle_$tagSuffix")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (entry == SquadViewMode.TILE) Icons.Filled.GridView else Icons.Filled.TableRows,
                            contentDescription = "${entry.label} view",
                            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

