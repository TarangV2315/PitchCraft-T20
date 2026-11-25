package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.model.CricketNation
import com.example.model.TeamTalkTone
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.T20MatchEngine
import com.example.simulation.WorldGenerator
import com.example.ui.screens.MatchdayScreen
import com.example.ui.screens.SquadScreen
import com.example.ui.screens.SquadSubTab
import com.example.ui.screens.TrainingMedicalScreen
import com.example.ui.screens.TrainingMedicalSubTab
import com.example.ui.theme.PitchCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "port-xxhdpi")
class TeamTalkAndViewToggleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `team talk modifiers affect simulation attributes and post-match morale and form`() {
        val agg = T20MatchEngine.teamTalkModifiers(TeamTalkTone.AGGRESSIVE)
        val calm = T20MatchEngine.teamTalkModifiers(TeamTalkTone.CALM)
        val enc = T20MatchEngine.teamTalkModifiers(TeamTalkTone.ENCOURAGING)
        val ass = T20MatchEngine.teamTalkModifiers(TeamTalkTone.ASSERTIVE)

        assertTrue(agg.battingRunMult > calm.battingRunMult)
        assertTrue(calm.battingWicketMult < agg.battingWicketMult)
        assertTrue(enc.composureFitnessBonus > ass.composureFitnessBonus)
        assertTrue(ass.bowlingWktMult > calm.bowlingWktMult)

        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(303))
            .copy(preMatchTeamTalk = TeamTalkTone.AGGRESSIVE, isPreSeason = false)
        val fixture = career.nextUserFixture!!
        val runtime = T20MatchEngine.createMatchSession(career, fixture)

        assertEquals(TeamTalkTone.AGGRESSIVE, runtime.preMatchTeamTalk)

        // Advance until innings break prompt appears (transition to innings 2)
        var state = runtime.toUiState()
        var guard = 0
        while (guard < 40 && state.finishedRecord == null && !(state.pendingKeyMoment?.isInningsBreak == true)) {
            state = runtime.advanceUntilNextPause(instantFinishAll = false)
            if (state.pendingKeyMoment != null && !state.pendingKeyMoment.isInningsBreak) {
                runtime.applyIntervention(null, null, null, null, null)
                state = runtime.stepBalls(1)
            }
            guard++
        }

        assertEquals(true, state.pendingKeyMoment?.isInningsBreak)
        assertNotNull(state.pendingKeyMoment?.recommendedTeamTalk)

        runtime.applyIntervention(null, null, null, null, TeamTalkTone.CALM)
        val afterBreak = runtime.stepBalls(1)
        assertEquals(TeamTalkTone.CALM, afterBreak.inningsBreakTeamTalk)

        val finished = runtime.advanceUntilNextPause(instantFinishAll = true)
        val record = finished.finishedRecord
        assertNotNull(record)

        val beforeAvgMorale = career.squad.filter { !it.isYouthAcademy }.map { it.morale }.average()
        val afterPostTalk = SeasonProgressionEngine.applyPostMatchTeamTalk(career, record!!, TeamTalkTone.ENCOURAGING)
        val afterAvgMorale = afterPostTalk.squad.filter { !it.isYouthAcademy }.map { it.morale }.average()

        assertTrue(afterAvgMorale > beforeAvgMorale)
        assertTrue(afterPostTalk.inbox.first().title.contains("Post-Match Debrief"))
    }

    @Test
    fun `matchday screen supports pre-match, innings-break, post-match team talks and XI tile-table toggle`() {
        val template = WorldGenerator.clubsFor(CricketNation.ENGLAND, 1).first()
        var career by mutableStateOf(WorldGenerator.createNewCareer(template, "Boss", Random(404)).copy(isPreSeason = false))
        val fixture = career.nextUserFixture!!
        val runtime = T20MatchEngine.createMatchSession(career, fixture)
        var liveMatchState by mutableStateOf<com.example.model.LiveMatchState?>(null)

        composeRule.setContent {
            PitchCraftTheme {
                MatchdayScreen(
                    career = career,
                    liveMatch = liveMatchState,
                    isAutoStreaming = false,
                    streamSpeedMs = 750L,
                    onToggleDelegation = {},
                    onTogglePlayerInXI = {},
                    onSetImpactSub = {},
                    onAutoPickXI = {},
                    onSetDefaultTactics = { _, _, _ -> },
                    onSetPreMatchTeamTalk = { tone -> career = career.copy(preMatchTeamTalk = tone) },
                    onStartMatch = {},
                    onToggleAutoStream = {},
                    onCycleStreamSpeed = {},
                    onStepBalls = { count -> liveMatchState = runtime.stepBalls(count) },
                    onAdvanceToMoment = {},
                    onApplyLiveTactics = { _, _, _, _, _ -> },
                    onSubmitKeyMoment = { tempo, plan, field, sub, finishAll, talk ->
                        runtime.applyIntervention(tempo, plan, field, sub, talk)
                        liveMatchState = if (finishAll) {
                            runtime.advanceUntilNextPause(instantFinishAll = true)
                        } else {
                            runtime.stepBalls(1)
                        }
                    },
                    onDeliverPostMatchTalk = { tone ->
                        runtime.postMatchTeamTalkDelivered = tone
                        liveMatchState = liveMatchState?.copy(postMatchTeamTalkDelivered = tone)
                    },
                    onFinishMatch = {},
                    onOpenPlayer = {}
                )
            }
        }

        // 1. Pre-Match Team Talk selection
        composeRule.onNodeWithTag("matchday_setup_list")
            .performScrollToNode(hasTestTag("team_talk_prematch_card"))
        composeRule.onNodeWithTag("prematch_talk_aggressive").performClick()
        composeRule.waitForIdle()
        assertEquals(TeamTalkTone.AGGRESSIVE, career.preMatchTeamTalk)

        // 2. Playing XI Tile <-> Table Toggle
        composeRule.onNodeWithTag("matchday_setup_list")
            .performScrollToNode(hasTestTag("xi_view_toggle_table"))
        composeRule.onNodeWithTag("xi_view_toggle_table").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("matchday_setup_list")
            .performScrollToNode(hasTestTag("matchday_xi_table"))
        composeRule.onNodeWithTag("matchday_xi_table").assertIsDisplayed()

        composeRule.onNodeWithTag("matchday_setup_list")
            .performScrollToNode(hasTestTag("xi_view_toggle_tile"))
        composeRule.onNodeWithTag("xi_view_toggle_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("matchday_xi_table").assertDoesNotExist()

        // 3. Innings-Break Team Talk in Live War Room
        var state = runtime.toUiState()
        var guard = 0
        while (guard < 40 && state.finishedRecord == null && !(state.pendingKeyMoment?.isInningsBreak == true)) {
            state = runtime.advanceUntilNextPause(instantFinishAll = false)
            if (state.pendingKeyMoment != null && !state.pendingKeyMoment.isInningsBreak) {
                runtime.applyIntervention(null, null, null, null, null)
                state = runtime.stepBalls(1)
            }
            guard++
        }
        liveMatchState = state
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("live_war_room_list")
            .performScrollToNode(hasTestTag("team_talk_innings_break_section"))
        composeRule.onNodeWithTag("innings_talk_calm").performClick()
        composeRule.onNodeWithTag("live_war_room_list")
            .performScrollToNode(hasTestTag("resume_match_button"))
        composeRule.onNodeWithTag("resume_match_button").performClick()
        composeRule.waitForIdle()

        assertEquals(TeamTalkTone.CALM, liveMatchState?.inningsBreakTeamTalk)
        composeRule.onNodeWithTag("live_war_room_list")
            .performScrollToNode(hasTestTag("live_active_team_talk_badge"))
        composeRule.onNodeWithText("Talk: Calm").assertIsDisplayed()

        // 4. Post-Match Team Talk delivery on Scorecard
        liveMatchState = runtime.advanceUntilNextPause(instantFinishAll = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("post_match_scorecard_list")
            .performScrollToNode(hasTestTag("team_talk_postmatch_card"))
        composeRule.onNodeWithTag("postmatch_talk_assertive").performClick()
        composeRule.onNodeWithTag("deliver_post_match_talk_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(TeamTalkTone.ASSERTIVE, liveMatchState?.postMatchTeamTalkDelivered)
        composeRule.onNodeWithText("Delivered: Assertive").assertIsDisplayed()
    }

    @Test
    fun `squad and training-medical screens toggle smoothly between tile and table views in portrait`() {
        val template = WorldGenerator.clubsFor(CricketNation.AUSTRALIA, 1).first()
        val preSeason = WorldGenerator.createNewCareer(template, "Gaffer", Random(505))
        val ongoing = preSeason.copy(isPreSeason = false)
        var squadSubTab by mutableStateOf(SquadSubTab.ROSTER)
        var isPreSeason by mutableStateOf(true)

        composeRule.setContent {
            PitchCraftTheme {
                SquadScreen(
                    career = if (isPreSeason) preSeason else ongoing,
                    activeSubTab = squadSubTab,
                    onSelectSubTab = { squadSubTab = it },
                    onOpenPlayer = {},
                    onSetTraining = { _, _ -> },
                    onSetTrainingIntensity = {},
                    onSetRehab = { _, _ -> },
                    onPromoteYouth = {},
                    onToggleDelegation = {}
                )
            }
        }

        // Pre-season Roster: default Tiles -> switch to Table -> back to Tiles
        composeRule.onNodeWithTag("roster_view_toggle_table").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_roster_table").assertIsDisplayed()
        composeRule.onNodeWithTag("roster_view_toggle_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_roster_table").assertDoesNotExist()

        // Depth Chart: default Tiles -> switch to Table
        squadSubTab = SquadSubTab.DEPTH
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("depth_view_toggle_table").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_depth_table").assertIsDisplayed()

        // Youth Academy: default Tiles -> switch to Table
        squadSubTab = SquadSubTab.ACADEMY
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("academy_view_toggle_table").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_academy_table").assertIsDisplayed()

        // Ongoing Form & Stats: default Table -> switch to Tiles
        isPreSeason = false
        squadSubTab = SquadSubTab.FORM_STATS
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_form_stats_section")
            .performScrollToNode(hasTestTag("form_stats_view_toggle_tile"))
        composeRule.onNodeWithTag("form_stats_view_toggle_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("squad_individual_stats_table").assertDoesNotExist()
    }

    @Test
    fun `training and medical screen toggles cleanly between table and tile views`() {
        val template = WorldGenerator.clubsFor(CricketNation.SOUTH_AFRICA, 1).first()
        val ongoing = WorldGenerator.createNewCareer(template, "Coach", Random(606)).copy(isPreSeason = false)
        var tmTab by mutableStateOf(TrainingMedicalSubTab.TRAINING)

        composeRule.setContent {
            PitchCraftTheme {
                TrainingMedicalScreen(
                    career = ongoing,
                    activeSubTab = tmTab,
                    onSelectSubTab = { tmTab = it },
                    onOpenPlayer = {},
                    onSetTraining = { _, _ -> },
                    onSetTrainingIntensity = {},
                    onSetRehab = { _, _ -> },
                    onToggleDelegation = {}
                )
            }
        }

        // Training tab starts as Table -> switch to Tiles
        composeRule.onNodeWithTag("training_table").assertIsDisplayed()
        composeRule.onNodeWithTag("training_view_toggle_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("training_table").assertDoesNotExist()

        // Medical tab starts as Table -> switch to Tiles
        tmTab = TrainingMedicalSubTab.MEDICAL
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("medical_table").assertIsDisplayed()
        composeRule.onNodeWithTag("medical_view_toggle_tile").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("medical_table").assertDoesNotExist()
    }
}
