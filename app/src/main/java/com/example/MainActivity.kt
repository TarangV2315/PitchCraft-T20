package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CareerRepository
import com.example.data.local.PitchCraftDatabase
import com.example.ui.CareerViewModel
import com.example.ui.MainTab
import com.example.ui.screens.*
import com.example.ui.theme.PitchCraftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PitchCraftTheme {
                PitchCraftApp()
            }
        }
    }
}

@Composable
fun PitchCraftApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember {
        CareerRepository(PitchCraftDatabase.getInstance(context).careerDao())
    }
    val vm: CareerViewModel = viewModel(factory = CareerViewModel.factory(repository))

    val savedCareers by vm.savedCareers.collectAsStateWithLifecycle()
    val activeCareer by vm.activeCareer.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val inspectedPlayerId by vm.inspectedPlayerId.collectAsStateWithLifecycle()
    val negotiation by vm.negotiation.collectAsStateWithLifecycle()
    val liveMatch by vm.liveMatch.collectAsStateWithLifecycle()
    val snackbarMsg by vm.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearSnackbar()
        }
    }

    val career = activeCareer
    if (career == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            CareerLauncherScreen(
                savedCareers = savedCareers,
                onLoadCareer = vm::loadCareer,
                onDeleteCareer = vm::deleteCareer,
                onCreateCareer = vm::createNewCareer,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    val inspectedPlayer = inspectedPlayerId?.let { id ->
        career.squad.firstOrNull { it.id == id }
    }
    if (inspectedPlayer != null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            PlayerActivityScreen(
                player = inspectedPlayer,
                career = career,
                onBack = vm::closePlayerActivity,
                onApplyRecommendation = vm::applyStaffRecommendation,
                onSetTraining = vm::setPlayerTraining,
                onSetRehab = vm::setPlayerRehab,
                onPromoteYouth = vm::promoteYouthPlayer,
                onOpenRenewal = { p -> vm.openNegotiation(p, isRenewal = true) },
                onToggleTransferList = vm::toggleTransferList,
                onSellPlayer = vm::sellListedPlayer,
                modifier = Modifier.padding(innerPadding)
            )
        }
        if (negotiation != null) {
            NegotiationDialog(
                session = negotiation!!,
                clubBalance = career.bankBalance,
                onUpdateOffer = vm::updateNegotiationOffer,
                onSubmit = vm::submitNegotiationOffer,
                onDismiss = vm::closeNegotiation
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail && liveMatch == null) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { vm.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && liveMatch == null) {
                    NavigationRail {
                        Spacer(Modifier.height(12.dp))
                        MainTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { vm.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        MainTab.HQ -> HqDashboardScreen(
                            career = career,
                            onPrepareMatchday = { vm.selectTab(MainTab.MATCHDAY) },
                            onStartNextSeason = vm::startNextSeason,
                            onAdvanceWeek = vm::advanceWeekOrPlay,
                            onToggleDelegation = vm::toggleDelegation,
                            onOpenPlayer = vm::openPlayerActivity,
                            onNavigateTab = vm::selectTab,
                            onExitToLauncher = vm::exitToLauncher
                        )
                        MainTab.SQUAD -> SquadScreen(
                            career = career,
                            onOpenPlayer = vm::openPlayerActivity,
                            onSetTraining = vm::setPlayerTraining,
                            onSetRehab = vm::setPlayerRehab,
                            onPromoteYouth = vm::promoteYouthPlayer,
                            onToggleDelegation = vm::toggleDelegation
                        )
                        MainTab.MATCHDAY -> MatchdayScreen(
                            career = career,
                            liveMatch = liveMatch,
                            onToggleDelegation = vm::toggleDelegation,
                            onTogglePlayerInXI = vm::togglePlayerInXI,
                            onSetImpactSub = vm::setImpactSub,
                            onAutoPickXI = vm::autoPickXI,
                            onSetDefaultTactics = vm::setDefaultTactics,
                            onStartMatch = vm::startNextMatch,
                            onSubmitKeyMoment = vm::submitKeyMomentDecision,
                            onFinishMatch = vm::dismissCompletedMatch,
                            onOpenPlayer = vm::openPlayerActivity
                        )
                        MainTab.TRANSFERS -> TransfersScreen(
                            career = career,
                            activeNegotiation = negotiation,
                            onToggleDelegation = vm::toggleDelegation,
                            onToggleShortlist = vm::toggleShortlist,
                            onOpenNegotiation = { p -> vm.openNegotiation(p, isRenewal = false) },
                            onUpdateOffer = vm::updateNegotiationOffer,
                            onSubmitOffer = vm::submitNegotiationOffer,
                            onCloseNegotiation = vm::closeNegotiation
                        )
                        MainTab.OFFICE -> ClubOfficeScreen(
                            career = career,
                            onToggleDelegation = vm::toggleDelegation,
                            onHireStaff = vm::hireStaffMember,
                            onSignSponsor = vm::signSponsorDeal,
                            onSetTicketTier = vm::setTicketTier,
                            onAdjustBudget = { a, s, m -> vm.adjustBudgetLevel(a, s, m) }
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.HQ -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    MainTab.SQUAD -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    MainTab.MATCHDAY -> if (selected) Icons.Filled.SportsCricket else Icons.Outlined.SportsCricket
    MainTab.TRANSFERS -> if (selected) Icons.Filled.SwapHoriz else Icons.Outlined.SwapHoriz
    MainTab.OFFICE -> if (selected) Icons.Filled.BusinessCenter else Icons.Outlined.BusinessCenter
}
