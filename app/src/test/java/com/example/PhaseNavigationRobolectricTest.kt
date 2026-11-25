package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CareerRepository
import com.example.data.local.PitchCraftDatabase
import com.example.model.CricketNation
import com.example.model.DelegationArea
import com.example.model.InjuryState
import com.example.model.PlayerStats
import com.example.model.RehabMode
import com.example.model.TrainingFocus
import com.example.model.TrainingIntensity
import com.example.simulation.WorldGenerator
import com.example.ui.CareerViewModel
import com.example.ui.MainTab
import com.example.ui.components.InGameTopContextBar
import com.example.ui.components.ScreenGuideId
import com.example.ui.screens.ClubOfficeScreen
import com.example.ui.screens.HqDashboardScreen
import com.example.ui.screens.OfficeSubTab
import com.example.ui.screens.SquadScreen
import com.example.ui.screens.SquadSubTab
import com.example.ui.screens.TrainingMedicalScreen
import com.example.ui.screens.TrainingMedicalSubTab
import com.example.ui.theme.PitchCraftTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "port-xxhdpi")
class PhaseNavigationRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `main activity is configured for vertical portrait orientation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val info = pm.getActivityInfo(
            android.content.ComponentName(context, MainActivity::class.java),
            0
        )
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, info.screenOrientation)
    }

    @Test
    fun `pre-season HQ retains stats and quick links while ongoing HQ removes stats, save button, and club operations`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val preSeasonCareer = WorldGenerator.createNewCareer(template, "Gaffer", Random(91))
        val ongoingCareer = preSeasonCareer.copy(isPreSeason = false)

        var isPreSeasonState by mutableStateOf(true)
        var startSeasonClicked = false

        composeRule.setContent {
            PitchCraftTheme {
                HqDashboardScreen(
                    career = if (isPreSeasonState) preSeasonCareer else ongoingCareer,
                    onPrepareMatchday = {},
                    onStartNextSeason = {},
                    onAdvanceWeek = {},
                    onToggleDelegation = {},
                    onOpenPlayer = {},
                    onNavigateTab = {},
                    onOpenFinances = {},
                    onOpenStaffOffice = {},
                    onExitToLauncher = {},
                    onStartSeason = { startSeasonClicked = true },
                    onSaveGame = {}
                )
            }
        }

        // Pre-Season HQ checks
        composeRule.onNodeWithTag("hq_preseason_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_stat_cash").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_stat_board").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_stat_sponsor").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_stat_fitness").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_go_squad_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hq_go_transfers_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hq_go_office_btn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hq_proceed_season_btn").performScrollTo().performClick()
        assertTrue(startSeasonClicked)

        // Switch to Ongoing Season HQ: Save & Team Name moved to Top Ribbon; Club Operations removed
        isPreSeasonState = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("hq_ongoing_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_stat_cash").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_stat_board").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_stat_sponsor").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_stat_fitness").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_save_game_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_go_squad_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_go_matchday_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_go_office_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("hq_matchday_cta").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("hq_delegate_${DelegationArea.TRAINING.name.lowercase()}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `top ribbon displays team name and save button during ongoing season`() {
        var isPreSeason by mutableStateOf(true)
        var saveClicked = false

        composeRule.setContent {
            PitchCraftTheme {
                InGameTopContextBar(
                    guideId = ScreenGuideId.HQ_DASHBOARD,
                    clubShortCode = "MUM",
                    divisionTier = 1,
                    season = 1,
                    week = 1,
                    bankBalance = 250000,
                    onInfoClick = {},
                    isPreSeason = isPreSeason,
                    clubName = "Mumbai Mariners",
                    onSaveClick = { saveClicked = true },
                    onExitToLauncher = {}
                )
            }
        }

        // Pre-season hides ongoing Save button & team name in top ribbon
        composeRule.onNodeWithTag("top_ribbon_save_button").assertDoesNotExist()
        composeRule.onNodeWithTag("top_ribbon_team_name").assertDoesNotExist()

        // Ongoing Season shows Team Name and Save button on top ribbon
        isPreSeason = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("top_ribbon_team_name").assertIsDisplayed()
        composeRule.onNodeWithText("Mumbai Mariners").assertIsDisplayed()
        composeRule.onNodeWithTag("top_ribbon_save_button").assertIsDisplayed().performClick()
        assertTrue(saveClicked)
    }

    @Test
    fun `bottom navigation replaces Transfers with Training & Medical during ongoing season`() {
        var isPreSeason by mutableStateOf(true)
        var selectedTab by mutableStateOf(MainTab.HQ)

        composeRule.setContent {
            PitchCraftTheme {
                LandscapeBottomCommandBar(
                    selectedTab = selectedTab,
                    isPreSeason = isPreSeason,
                    isTransferWindowOpen = true,
                    onSelectTab = { selectedTab = it },
                    onStartSeason = { isPreSeason = false }
                )
            }
        }

        // Pre-Season bottom bar
        composeRule.onNodeWithTag("nav_tab_hq").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_squad").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_transfers").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tab_training_medical").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_office").assertIsDisplayed()
        composeRule.onNodeWithText("Start Season").assertIsDisplayed().performClick()

        composeRule.waitForIdle()

        // Ongoing Season bottom bar: Transfers replaced by Training & Medical, Start Season becomes MatchDay
        composeRule.onNodeWithTag("nav_tab_transfers").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_training_medical").assertIsDisplayed().performClick()
        assertEquals(MainTab.TRAINING_MEDICAL, selectedTab)
        composeRule.onNodeWithText("MatchDay").assertIsDisplayed()
    }

    @Test
    fun `squad screen switches tabs between pre-season and ongoing season with form chart and stats table`() {
        val template = WorldGenerator.clubsFor(CricketNation.ENGLAND, 1).first()
        val base = WorldGenerator.createNewCareer(template, "Coach", Random(92))
        val updatedSquad = base.squad.mapIndexed { idx, p ->
            if (idx == 0) p.copy(
                form = 88,
                fitness = 76,
                seasonStats = PlayerStats(matches = 4, runs = 164, ballsBowled = 30, wickets = 3)
            ) else p
        }
        val preSeasonCareer = base.copy(squad = updatedSquad, isPreSeason = true)
        val ongoingCareer = preSeasonCareer.copy(isPreSeason = false)

        var preSeasonMode by mutableStateOf(true)

        composeRule.setContent {
            PitchCraftTheme {
                SquadScreen(
                    career = if (preSeasonMode) preSeasonCareer else ongoingCareer,
                    activeSubTab = if (preSeasonMode) SquadSubTab.ROSTER else SquadSubTab.FORM_STATS,
                    onSelectSubTab = {},
                    onOpenPlayer = {},
                    onSetTraining = { _, _ -> },
                    onSetTrainingIntensity = {},
                    onSetRehab = { _, _ -> },
                    onPromoteYouth = {},
                    onToggleDelegation = {}
                )
            }
        }

        // Pre-Season Squad: Roster, Depth Chart, Youth Academy ONLY
        composeRule.onNodeWithTag("squad_tab_roster").assertIsDisplayed()
        composeRule.onNodeWithTag("squad_tab_depth").assertIsDisplayed()
        composeRule.onNodeWithTag("squad_tab_academy").assertIsDisplayed()
        composeRule.onNodeWithTag("squad_tab_training_rehab").assertDoesNotExist()
        composeRule.onNodeWithTag("squad_tab_form_stats").assertDoesNotExist()

        // Ongoing Season Squad: Form & Stats (Training & Medical is now its own top-level tab)
        preSeasonMode = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("squad_tab_roster").assertDoesNotExist()
        composeRule.onNodeWithTag("squad_tab_depth").assertDoesNotExist()
        composeRule.onNodeWithTag("squad_tab_academy").assertDoesNotExist()
        composeRule.onNodeWithTag("squad_tab_training_rehab").assertDoesNotExist()
        composeRule.onNodeWithTag("squad_tab_form_stats").assertIsDisplayed()
        composeRule.onNodeWithTag("squad_form_chart_card").assertIsDisplayed()
        composeRule.onNodeWithTag("squad_form_stats_section")
            .performScrollToNode(hasTestTag("squad_individual_stats_table"))
        composeRule.onNodeWithTag("squad_individual_stats_table").assertIsDisplayed()
        composeRule.onAllNodesWithText("Fatigue").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Runs").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Overs").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("164").assertIsDisplayed()
        composeRule.onNodeWithText("5.0").assertIsDisplayed()
    }

    @Test
    fun `training and medical screen renders interactive tabular views on both tabs`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val base = WorldGenerator.createNewCareer(template, "Trainer", Random(95))
        val seniorList = base.squad.filter { !it.isYouthAcademy }.sortedByDescending { it.overall }
        val injuredPlayer = seniorList[0].copy(
            fitness = 54,
            injury = InjuryState(
                isInjured = true,
                injuryName = "Side Strain",
                weeksRemaining = 2,
                rehabMode = RehabMode.BALANCED
            )
        )
        val fitPlayer = seniorList[1].copy(
            fitness = 82,
            trainingFocus = TrainingFocus.BALANCED
        )
        val tweakedSquad = base.squad.map { p ->
            when (p.id) {
                injuredPlayer.id -> injuredPlayer
                fitPlayer.id -> fitPlayer
                else -> p
            }
        }

        var careerState by mutableStateOf(base.copy(isPreSeason = false, squad = tweakedSquad))
        var activeSubTab by mutableStateOf(TrainingMedicalSubTab.TRAINING)

        composeRule.setContent {
            PitchCraftTheme {
                TrainingMedicalScreen(
                    career = careerState,
                    activeSubTab = activeSubTab,
                    onSelectSubTab = { activeSubTab = it },
                    onOpenPlayer = {},
                    onSetTraining = { playerId, focus ->
                        careerState = careerState.copy(
                            squad = careerState.squad.map {
                                if (it.id == playerId) it.copy(trainingFocus = focus) else it
                            }
                        )
                    },
                    onSetTrainingIntensity = { intensity ->
                        careerState = careerState.copy(trainingIntensity = intensity)
                    },
                    onSetRehab = { playerId, mode ->
                        careerState = careerState.copy(
                            squad = careerState.squad.map {
                                if (it.id == playerId && it.injury.isInjured) {
                                    it.copy(injury = it.injury.copy(rehabMode = mode))
                                } else it
                            }
                        )
                    },
                    onToggleDelegation = {}
                )
            }
        }

        // 1. Training tab tabular view & interactive edits
        composeRule.onNodeWithTag("training_tabular_view").assertIsDisplayed()
        composeRule.onNodeWithTag("tm_intensity_high").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(TrainingIntensity.HIGH, careerState.trainingIntensity)

        composeRule.onNodeWithTag("training_tabular_view").performScrollToNode(hasTestTag("training_table"))
        composeRule.onNodeWithTag("training_table").assertIsDisplayed()

        composeRule.onNodeWithTag("training_focus_btn_${fitPlayer.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("training_drill_option_${fitPlayer.id}_power_hitting").performClick()
        composeRule.waitForIdle()
        assertEquals(
            TrainingFocus.POWER_HITTING,
            careerState.squad.first { it.id == fitPlayer.id }.trainingFocus
        )

        // 2. Medical tab tabular view & interactive rehab + rest edits
        composeRule.onNodeWithTag("tm_tab_medical").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("medical_tabular_view").assertIsDisplayed()
        composeRule.onNodeWithTag("medical_tabular_view").performScrollToNode(hasTestTag("medical_table"))
        composeRule.onNodeWithTag("medical_table").assertIsDisplayed()

        // Edit injured player's rehab mode inline
        composeRule.onNodeWithTag("medical_rehab_btn_${injuredPlayer.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("medical_rehab_option_${injuredPlayer.id}_accelerated").performClick()
        composeRule.waitForIdle()
        assertEquals(
            RehabMode.ACCELERATED,
            careerState.squad.first { it.id == injuredPlayer.id }.injury.rehabMode
        )

        // Toggle fit player's recovery/rest directly in the Medical table
        composeRule.onNodeWithTag("medical_rest_btn_${fitPlayer.id}").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(
            TrainingFocus.FITNESS_RECOVERY,
            careerState.squad.first { it.id == fitPlayer.id }.trainingFocus
        )
    }

    @Test
    fun `office screen exposes staff & finances in pre-season and calendar & standings in ongoing season`() {
        val template = WorldGenerator.clubsFor(CricketNation.AUSTRALIA, 1).first()
        val preSeason = WorldGenerator.createNewCareer(template, "Exec", Random(93))
        val ongoing = preSeason.copy(isPreSeason = false)
        var isPreSeason by mutableStateOf(true)

        composeRule.setContent {
            PitchCraftTheme {
                ClubOfficeScreen(
                    career = if (isPreSeason) preSeason else ongoing,
                    activeTab = if (isPreSeason) OfficeSubTab.STAFF_DELEGATION else OfficeSubTab.PYRAMID,
                    onSelectTab = {},
                    onToggleDelegation = {},
                    onAssignCoachFocus = { _, _ -> },
                    onHireStaff = { _, _ -> },
                    onSignSponsor = {},
                    onSetTicketTier = {},
                    onAdjustBudget = { _, _, _ -> }
                )
            }
        }

        // Pre-Season Office
        composeRule.onNodeWithTag("office_tab_staff_delegation").assertIsDisplayed()
        composeRule.onNodeWithTag("office_tab_finance_sponsors").assertIsDisplayed()
        composeRule.onNodeWithTag("office_tab_season_calendar").assertDoesNotExist()
        composeRule.onNodeWithTag("office_tab_pyramid").assertDoesNotExist()

        // Ongoing Season Office
        isPreSeason = false
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("office_tab_staff_delegation").assertDoesNotExist()
        composeRule.onNodeWithTag("office_tab_finance_sponsors").assertDoesNotExist()
        composeRule.onNodeWithTag("office_tab_season_calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("office_tab_pyramid").assertIsDisplayed()
    }

    @Test
    fun `view model startSeason transitions bottom nav and openMedicalRoom routes to Training & Medical`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PitchCraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = CareerRepository(db.careerDao())
        val vm = CareerViewModel(repo)

        val template = WorldGenerator.clubsFor(CricketNation.SOUTH_AFRICA, 1).first()
        val preSeason = WorldGenerator.createNewCareer(template, "Boss", Random(94))
        runBlocking { repo.saveCareer(preSeason) }
        vm.loadCareer(preSeason.careerId)

        val start = System.currentTimeMillis()
        while (vm.activeCareer.value == null && System.currentTimeMillis() - start < 3000L) {
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }

        assertTrue(vm.activeCareer.value!!.isPreSeason)
        assertEquals(MainTab.MATCHDAY, MainTab.entries.last())
        assertTrue(MainTab.TRANSFERS in MainTab.bottomNavForPhase(isPreSeason = true))
        assertFalse(MainTab.TRAINING_MEDICAL in MainTab.bottomNavForPhase(isPreSeason = true))

        vm.startSeason()
        assertFalse(vm.activeCareer.value!!.isPreSeason)
        assertEquals(SquadSubTab.FORM_STATS, vm.squadSubTab.value)
        assertEquals(OfficeSubTab.PYRAMID, vm.officeSubTab.value)
        assertFalse(MainTab.TRANSFERS in MainTab.bottomNavForPhase(isPreSeason = false))
        assertTrue(MainTab.TRAINING_MEDICAL in MainTab.bottomNavForPhase(isPreSeason = false))

        vm.openMedicalRoom()
        assertEquals(MainTab.TRAINING_MEDICAL, vm.selectedTab.value)
        assertEquals(TrainingMedicalSubTab.MEDICAL, vm.trainingMedicalSubTab.value)

        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        Thread.sleep(100)
        db.close()
    }
}
