package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CareerSummary
import com.example.simulation.ClubTemplate
import com.example.simulation.WorldGenerator
import com.example.ui.components.ClubBadge
import com.example.ui.components.formatCompactCurrency
import com.example.ui.theme.TelemetryTextStyles

@Composable
fun CareerLauncherScreen(
    savedCareers: List<CareerSummary>,
    onLoadCareer: (String) -> Unit,
    onDeleteCareer: (String) -> Unit,
    onCreateCareer: (ClubTemplate, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTier by remember { mutableIntStateOf(3) }
    var managerName by remember { mutableStateOf("Alex Mercer") }
    var selectedClub by remember(selectedTier) {
        mutableStateOf(WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == selectedTier })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_stadium_hero),
                    contentDescription = "Floodlit T20 Cricket Stadium",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "LOCAL CAREER STORAGE • T20 PYRAMID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "PitchCraft T20",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Executive club cricket management, staff delegation & live tactical matchdays.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (savedCareers.isNotEmpty()) {
            item {
                Text(
                    text = "Resume Local Career (${savedCareers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(savedCareers, key = { it.careerId }) { save ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onLoadCareer(save.careerId) }
                        .testTag("resume_career_${save.careerId}"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClubBadge(save.clubShortCode, save.divisionTier)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(save.clubName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Mgr: ${save.managerName} • Tier ${save.divisionTier} • S${save.season} W${save.week}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Cash ${formatCompactCurrency(save.bankBalance)} • ${save.trophiesWon} Trophies",
                                style = TelemetryTextStyles.statSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCareer(save.careerId) },
                            modifier = Modifier.testTag("delete_career_${save.careerId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete career",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        FilledTonalIconButton(onClick = { onLoadCareer(save.careerId) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume career")
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Launch New T20 Franchise",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = managerName,
                    onValueChange = { managerName = it },
                    label = { Text("Manager Name") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manager_name_input")
                )

                Text(
                    text = "Select Competition Tier",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        3 to "Tier 3 Challenger",
                        2 to "Tier 2 Champ.",
                        1 to "Tier 1 Premier"
                    ).forEachIndexed { idx, (tier, label) ->
                        SegmentedButton(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier },
                            shape = SegmentedButtonDefaults.itemShape(idx, 3),
                            modifier = Modifier.testTag("tier_select_$tier")
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }

        val clubs = WorldGenerator.ALL_CLUB_TEMPLATES.filter { it.tier == selectedTier }
        items(clubs, key = { it.clubId }) { club ->
            val isPicked = selectedClub.clubId == club.clubId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { selectedClub = club }
                    .testTag("club_pick_${club.clubId}"),
                shape = MaterialTheme.shapes.medium,
                color = if (isPicked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = if (isPicked) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClubBadge(club.shortCode, club.tier)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(club.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${club.cityTag} • ${club.stadiumCapacity / 1000}k Arena • OVR ${club.baseStrength}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCompactCurrency(club.startingBudget),
                            style = TelemetryTextStyles.statValue.copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onCreateCareer(selectedClub, managerName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(54.dp)
                    .testTag("start_career_button")
            ) {
                Icon(Icons.Filled.SportsCricket, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Take Charge of ${selectedClub.name}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
