package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CareerRepository
import com.example.data.local.PitchCraftDatabase
import com.example.data.local.WelcomeRewardsStore
import com.example.model.CareerState
import com.example.model.LiveMatchState
import com.example.ui.CareerViewModel
import com.example.ui.MainTab
import com.example.ui.components.InGameTopContextBar
import com.example.ui.components.ScreenGuideDialog
import com.example.ui.components.ScreenGuideId
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

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
    val rewardsStore = remember {
        WelcomeRewardsStore(context)
    }
    val vm: CareerViewModel = viewModel(factory = CareerViewModel.factory(repository, rewardsStore))

    var showInitialLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2200L)
        showInitialLoading = false
    }

    val savedCareers by vm.savedCareers.collectAsStateWithLifecycle()
    val welcomeRewards by vm.welcomeRewards.collectAsStateWithLifecycle()
    val activeCareer by vm.activeCareer.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val officeSubTab by vm.officeSubTab.collectAsStateWithLifecycle()
    val squadSubTab by vm.squadSubTab.collectAsStateWithLifecycle()
    val trainingMedicalSubTab by vm.trainingMedicalSubTab.collectAsStateWithLifecycle()
    val inspectedPlayerId by vm.inspectedPlayerId.collectAsStateWithLifecycle()
    val negotiation by vm.negotiation.collectAsStateWithLifecycle()
    val sponsorNegotiation by vm.sponsorNegotiation.collectAsStateWithLifecycle()
    val liveMatch by vm.liveMatch.collectAsStateWithLifecycle()
    val isAutoStreaming by vm.isAutoStreaming.collectAsStateWithLifecycle()
    val streamSpeedMs by vm.streamSpeedMs.collectAsStateWithLifecycle()
    val snackbarMsg by vm.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        val msg = snackbarMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            vm.clearSnackbar()
        }
    }

    AnimatedContent(
        targetState = showInitialLoading,
        transitionSpec = {
            fadeIn(tween(420)) togetherWith fadeOut(tween(260))
        },
        label = "splashToApp"
    ) { loading ->
        if (loading) {
            CricketLoadingScreen(
                onSkip = { showInitialLoading = false }
            )
        } else {
            val hasCareer = activeCareer != null
            AnimatedContent(
                targetState = hasCareer,
                transitionSpec = {
                    (fadeIn(tween(420)) + scaleIn(initialScale = 0.94f, animationSpec = tween(420))) togetherWith
                        fadeOut(tween(220))
                },
                label = "launcherToCareer"
            ) { inCareer ->
                val career = activeCareer
                if (!inCareer || career == null) {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { innerPadding ->
                        CareerLauncherScreen(
                            savedCareers = savedCareers,
                            welcomeRewards = welcomeRewards,
                            onLoadCareer = vm::loadCareer,
                            onDeleteCareer = vm::deleteCareer,
                            onCreateCareer = vm::createNewCareer,
                            onWatchAdReward = vm::recordRewardedAdWatched,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                } else {
                    InCareerRoot(
                        career = career,
                        vm = vm,
                        selectedTab = selectedTab,
                        officeSubTab = officeSubTab,
                        squadSubTab = squadSubTab,
                        trainingMedicalSubTab = trainingMedicalSubTab,
                        inspectedPlayerId = inspectedPlayerId,
                        negotiation = negotiation,
                        sponsorNegotiation = sponsorNegotiation,
                        liveMatch = liveMatch,
                        isAutoStreaming = isAutoStreaming,
                        streamSpeedMs = streamSpeedMs,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}

@Composable
private fun InCareerRoot(
    career: CareerState,
    vm: CareerViewModel,
    selectedTab: MainTab,
    officeSubTab: OfficeSubTab,
    squadSubTab: SquadSubTab,
    trainingMedicalSubTab: TrainingMedicalSubTab,
    inspectedPlayerId: String?,
    negotiation: com.example.model.NegotiationSession?,
    sponsorNegotiation: com.example.model.SponsorNegotiationSession?,
    liveMatch: LiveMatchState?,
    isAutoStreaming: Boolean,
    streamSpeedMs: Long,
    snackbarHostState: SnackbarHostState
) {
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
                session = negotiation,
                clubBalance = career.bankBalance,
                onUpdateOffer = vm::updateNegotiationOffer,
                onSubmit = vm::submitNegotiationOffer,
                onDismiss = vm::closeNegotiation
            )
        }
        return
    }

    var activeGuideForScreen by remember { mutableStateOf<ScreenGuideId?>(null) }
    val currentGuideId = resolveScreenGuideId(
        selectedTab = selectedTab,
        squadSubTab = squadSubTab,
        trainingMedicalSubTab = trainingMedicalSubTab,
        officeSubTab = officeSubTab,
        liveMatch = liveMatch,
        isPreSeason = career.isPreSeason
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            InGameTopContextBar(
                guideId = currentGuideId,
                clubShortCode = career.clubShortCode,
                divisionTier = career.divisionTier,
                season = career.season,
                week = career.week,
                bankBalance = career.bankBalance,
                isPreSeason = career.isPreSeason,
                clubName = career.clubName,
                onSaveClick = vm::saveCurrentCareer,
                onExitToLauncher = vm::exitToLauncher,
                onInfoClick = { activeGuideForScreen = currentGuideId }
            )
        },
        bottomBar = {
            if (liveMatch == null) {
                LandscapeBottomCommandBar(
                    selectedTab = selectedTab,
                    isPreSeason = career.isPreSeason,
                    isTransferWindowOpen = career.isTransferWindowOpen,
                    onSelectTab = vm::selectTab,
                    onStartSeason = vm::startSeason
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            when (selectedTab) {
                MainTab.HQ -> HqDashboardScreen(
                    career = career,
                    onPrepareMatchday = { vm.selectTab(MainTab.MATCHDAY) },
                    onStartNextSeason = vm::startNextSeason,
                    onAdvanceWeek = vm::advanceWeekOrPlay,
                    onToggleDelegation = vm::toggleDelegation,
                    onOpenPlayer = vm::openPlayerActivity,
                    onNavigateTab = vm::selectTab,
                    onOpenFinances = vm::openOfficeFinances,
                    onOpenStaffOffice = vm::openStaffOffice,
                    onOpenCalendar = vm::openSeasonCalendar,
                    onOpenMedicalRoom = vm::openMedicalRoom,
                    onOpenStandings = vm::openLeagueStandings,
                    onExitToLauncher = vm::exitToLauncher,
                    onStartSeason = vm::startSeason,
                    onSaveGame = vm::saveCurrentCareer
                )
                MainTab.SQUAD -> SquadScreen(
                    career = career,
                    activeSubTab = squadSubTab,
                    onSelectSubTab = vm::selectSquadSubTab,
                    onOpenPlayer = vm::openPlayerActivity,
                    onSetTraining = vm::setPlayerTraining,
                    onSetTrainingIntensity = vm::setTrainingIntensity,
                    onSetRehab = vm::setPlayerRehab,
                    onPromoteYouth = vm::promoteYouthPlayer,
                    onToggleDelegation = vm::toggleDelegation
                )
                MainTab.MATCHDAY -> MatchdayScreen(
                    career = career,
                    liveMatch = liveMatch,
                    isAutoStreaming = isAutoStreaming,
                    streamSpeedMs = streamSpeedMs,
                    onToggleDelegation = vm::toggleDelegation,
                    onTogglePlayerInXI = vm::togglePlayerInXI,
                    onSetImpactSub = vm::setImpactSub,
                    onAutoPickXI = vm::autoPickXI,
                    onSetDefaultTactics = vm::setDefaultTactics,
                    onSetPreMatchTeamTalk = vm::setPreMatchTeamTalk,
                    onStartMatch = vm::startNextMatch,
                    onToggleAutoStream = vm::toggleAutoStream,
                    onCycleStreamSpeed = vm::cycleStreamSpeed,
                    onStepBalls = vm::stepLiveBalls,
                    onAdvanceToMoment = vm::advanceToNextKeyMoment,
                    onApplyLiveTactics = vm::applyLiveTactics,
                    onSubmitKeyMoment = vm::submitKeyMomentDecision,
                    onDeliverPostMatchTalk = vm::deliverPostMatchTeamTalk,
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
                MainTab.TRAINING_MEDICAL -> TrainingMedicalScreen(
                    career = career,
                    activeSubTab = trainingMedicalSubTab,
                    onSelectSubTab = vm::selectTrainingMedicalSubTab,
                    onOpenPlayer = vm::openPlayerActivity,
                    onSetTraining = vm::setPlayerTraining,
                    onSetTrainingIntensity = vm::setTrainingIntensity,
                    onSetRehab = vm::setPlayerRehab,
                    onToggleDelegation = vm::toggleDelegation
                )
                MainTab.OFFICE -> ClubOfficeScreen(
                    career = career,
                    activeTab = officeSubTab,
                    onSelectTab = vm::selectOfficeSubTab,
                    onToggleDelegation = vm::toggleDelegation,
                    onAssignCoachFocus = vm::assignCoachTrainingFocus,
                    onHireStaff = vm::hireStaffMember,
                    onSignSponsor = vm::signSponsorDeal,
                    onNegotiateSponsor = vm::openSponsorNegotiation,
                    activeSponsorNegotiation = sponsorNegotiation,
                    onUpdateSponsorOffer = vm::updateSponsorNegotiationTerms,
                    onSubmitSponsorOffer = vm::submitSponsorNegotiationOffer,
                    onDismissSponsorNegotiation = vm::closeSponsorNegotiation,
                    onSetTicketTier = vm::setTicketTier,
                    onAdjustBudget = { a, s, m -> vm.adjustBudgetLevel(a, s, m) },
                    onOpenTransfers = { vm.selectTab(MainTab.TRANSFERS) },
                    onOpenMatchday = { vm.selectTab(MainTab.MATCHDAY) },
                    onOpenSquadTraining = {
                        vm.selectTrainingMedicalSubTab(TrainingMedicalSubTab.TRAINING)
                        vm.selectTab(MainTab.TRAINING_MEDICAL)
                    }
                )
            }
        }
    }

    activeGuideForScreen?.let { guideId ->
        ScreenGuideDialog(
            initialScreenId = guideId,
            onDismiss = { activeGuideForScreen = null }
        )
    }
}

@Composable
internal fun LandscapeBottomCommandBar(
    selectedTab: MainTab,
    isPreSeason: Boolean,
    isTransferWindowOpen: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onStartSeason: () -> Unit
) {
    val leftTabs = MainTab.bottomNavForPhase(isPreSeason)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_command_bar"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        ) {
            val isPortraitCompact = maxWidth < 600.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isPortraitCompact) 6.dp else 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Standard Navigation Tabs on Left/Center
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if (isPortraitCompact) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftTabs.forEach { tab ->
                        val selected = selectedTab == tab
                        val enabled = tab != MainTab.TRANSFERS || isTransferWindowOpen
                        val bgColor = when {
                            !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f)
                            selected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        }
                        val contentColor = when {
                            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            selected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val compactLabel = when {
                            isPortraitCompact && tab == MainTab.TRAINING_MEDICAL -> "Train/Med"
                            else -> tab.label
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 200.dp)
                                .heightIn(min = 50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = enabled) { onSelectTab(tab) }
                                .testTag("nav_tab_${tab.name.lowercase()}")
                        ) {
                            if (isPortraitCompact) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (tab == MainTab.TRANSFERS && !enabled) {
                                            Icons.Outlined.Lock
                                        } else {
                                            tabIcon(tab, selected)
                                        },
                                        contentDescription = if (tab == MainTab.TRANSFERS && !enabled) {
                                            "${tab.label} (Window Closed)"
                                        } else {
                                            tab.label
                                        },
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = compactLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = contentColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (tab == MainTab.TRANSFERS && !enabled) {
                                            Icons.Outlined.Lock
                                        } else {
                                            tabIcon(tab, selected)
                                        },
                                        contentDescription = if (tab == MainTab.TRANSFERS && !enabled) {
                                            "${tab.label} (Window Closed)"
                                        } else {
                                            tab.label
                                        },
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(if (isPortraitCompact) 6.dp else 12.dp))

                // Bottom Right-Most Corner Button: "Start Season" (Pre-Season) or "MatchDay" (Ongoing Season)
                val cornerLabel = if (isPreSeason) "Start Season" else "MatchDay"
                val cornerSelected = !isPreSeason && selectedTab == MainTab.MATCHDAY
                Button(
                    onClick = {
                        if (isPreSeason) onStartSeason() else onSelectTab(MainTab.MATCHDAY)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isPreSeason -> FloodlightGold80
                            cornerSelected -> TurfEmerald80
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = when {
                            isPreSeason -> OnFloodlightGold20
                            cornerSelected -> OnTurfEmerald20
                            else -> MaterialTheme.colorScheme.onPrimary
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = if (isPortraitCompact) 10.dp else 18.dp,
                        vertical = if (isPortraitCompact) 6.dp else 10.dp
                    ),
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("nav_tab_matchday")
                ) {
                    if (isPortraitCompact) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isPreSeason) Icons.Filled.PlayArrow else Icons.Filled.SportsCricket,
                                contentDescription = cornerLabel,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = cornerLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isPreSeason) Icons.Filled.PlayArrow else Icons.Filled.SportsCricket,
                            contentDescription = cornerLabel,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = cornerLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun resolveScreenGuideId(
    selectedTab: MainTab,
    squadSubTab: SquadSubTab,
    trainingMedicalSubTab: TrainingMedicalSubTab,
    officeSubTab: OfficeSubTab,
    liveMatch: LiveMatchState?,
    isPreSeason: Boolean
): ScreenGuideId = when (selectedTab) {
    MainTab.HQ -> ScreenGuideId.HQ_DASHBOARD
    MainTab.SQUAD -> {
        val allowed = SquadSubTab.forPhase(isPreSeason)
        val effective = if (squadSubTab in allowed) squadSubTab else allowed.first()
        when (effective) {
            SquadSubTab.ROSTER -> ScreenGuideId.SQUAD_ROSTER
            SquadSubTab.DEPTH -> ScreenGuideId.SQUAD_DEPTH
            SquadSubTab.ACADEMY -> ScreenGuideId.SQUAD_ACADEMY
            SquadSubTab.FORM_STATS -> ScreenGuideId.SQUAD_FORM_STATS
            SquadSubTab.TRAINING_REHAB -> ScreenGuideId.SQUAD_TRAINING
        }
    }
    MainTab.MATCHDAY -> when {
        liveMatch?.finishedRecord != null -> ScreenGuideId.MATCHDAY_SCORECARD
        liveMatch != null -> ScreenGuideId.MATCHDAY_LIVE
        else -> ScreenGuideId.MATCHDAY_PREP
    }
    MainTab.TRANSFERS -> ScreenGuideId.TRANSFERS
    MainTab.TRAINING_MEDICAL -> when (trainingMedicalSubTab) {
        TrainingMedicalSubTab.TRAINING -> ScreenGuideId.SQUAD_TRAINING
        TrainingMedicalSubTab.MEDICAL -> ScreenGuideId.SQUAD_MEDICAL
    }
    MainTab.OFFICE -> {
        val allowed = OfficeSubTab.forPhase(isPreSeason)
        val effective = if (officeSubTab in allowed) officeSubTab else allowed.first()
        when (effective) {
            OfficeSubTab.STAFF_DELEGATION -> ScreenGuideId.OFFICE_STAFF
            OfficeSubTab.FINANCE_SPONSORS -> ScreenGuideId.OFFICE_FINANCES
            OfficeSubTab.SEASON_CALENDAR -> ScreenGuideId.OFFICE_CALENDAR
            OfficeSubTab.PYRAMID -> ScreenGuideId.OFFICE_STANDINGS
        }
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.HQ -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    MainTab.SQUAD -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    MainTab.TRANSFERS -> if (selected) Icons.Filled.SwapHoriz else Icons.Outlined.SwapHoriz
    MainTab.TRAINING_MEDICAL -> if (selected) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter
    MainTab.OFFICE -> if (selected) Icons.Filled.BusinessCenter else Icons.Outlined.BusinessCenter
    MainTab.MATCHDAY -> if (selected) Icons.Filled.SportsCricket else Icons.Outlined.SportsCricket
}
