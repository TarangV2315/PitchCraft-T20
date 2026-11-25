package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun TransferWindowBanner(
    status: TransferWindowStatus,
    modifier: Modifier = Modifier
) {
    val accent = if (status.isOpen) PitchMint else AlertCrimson
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transfer_window_banner"),
        shape = MaterialTheme.shapes.medium,
        color = if (status.isOpen) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)
        },
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = if (status.isOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = if (status.isOpen) "Transfer window open" else "Transfer window closed",
                    tint = accent,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (status.isOpen) "1-WEEK ACTIVE" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = status.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TransferMarketFilterHeader(
    bankBalance: Int,
    scoutingLevel: Int,
    shortlistCount: Int,
    availableCount: Int,
    showShortlistOnly: Boolean,
    onToggleShortlistOnly: () -> Unit,
    selectedRole: T20Role?,
    onSelectRole: (T20Role?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Transfer Market ($availableCount)",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Cash ${formatCurrency(bankBalance)} • Scout Lvl $scoutingLevel/5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilterChip(
                selected = showShortlistOnly,
                onClick = onToggleShortlistOnly,
                label = { Text("Shortlist ($shortlistCount)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("transfer_shortlist_filter")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedRole == null,
                    onClick = { onSelectRole(null) },
                    label = { Text("All Roles") }
                )
            }
            items(T20Role.entries) { role ->
                FilterChip(
                    selected = selectedRole == role,
                    onClick = { onSelectRole(if (selectedRole == role) null else role) },
                    label = { Text(role.shortLabel) }
                )
            }
        }
    }
}

@Composable
fun TransferMarketPlayerCard(
    player: Player,
    isWindowOpen: Boolean,
    onToggleShortlist: () -> Unit,
    onBid: () -> Unit,
    modifier: Modifier = Modifier
) {
    val attrs = player.attributes
    val primaryBowlLabel = if (player.bowlingStyle.isPace) "PAC" else if (player.bowlingStyle.canBowl) "SPN" else "DTH"
    val primaryBowlValue = if (player.bowlingStyle.isPace) attrs.paceBowling else if (player.bowlingStyle.canBowl) attrs.spinVariation else attrs.deathExecution

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transfer_card_${player.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Identity Row: Avatar + Name/Role/Age + Shortlist Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerAvatar(player = player, sizeDp = 52, showOvrBadge = true)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        RoleChip(role = player.role)
                    }
                    Text(
                        text = "${player.nationality} • Age ${player.age} • OVR ${player.overall} • POT ${player.potential} • FIT ${player.fitness}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleShortlist,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("shortlist_${player.id}")
                ) {
                    Icon(
                        imageVector = if (player.isShortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (player.isShortlisted) "Remove from shortlist" else "Add to shortlist",
                        tint = if (player.isShortlisted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Skill Ratings Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MarketSkillRatingChip(label = "OVR", value = player.overall, accent = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                MarketSkillRatingChip(label = "TIM", value = attrs.battingTiming, accent = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                MarketSkillRatingChip(label = "PWR", value = attrs.powerHitting, accent = BoundaryAmber, modifier = Modifier.weight(1f))
                MarketSkillRatingChip(label = primaryBowlLabel, value = primaryBowlValue, accent = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                MarketSkillRatingChip(label = "FLD", value = attrs.fielding, accent = PitchMint, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Base Value + Bid Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "BASE VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatCompactCurrency(player.marketValue),
                            style = TelemetryTextStyles.statValue.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• ${formatCompactCurrency(player.contract.weeklyWage)}/wk",
                            style = TelemetryTextStyles.statSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onBid,
                    enabled = isWindowOpen,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("bid_${player.id}")
                ) {
                    Icon(
                        imageVector = if (isWindowOpen) Icons.Filled.Gavel else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isWindowOpen) "Bid" else "Locked")
                }
            }
        }
    }
}

@Composable
private fun MarketSkillRatingChip(
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$value",
                style = TelemetryTextStyles.statSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
        }
    }
}
