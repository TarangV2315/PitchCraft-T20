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
import com.example.simulation.T20MatchEngine
import com.example.simulation.WorldGenerator
import com.example.ui.screens.LiveMatchWarRoomView
import com.example.ui.theme.PitchCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "port-xxhdpi")
class RunRateChartRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `match engine records ball-by-ball CRR and RRR points across both innings`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(template, "Analyst", Random(2026))
        val fixture = career.nextUserFixture!!
        val runtime = T20MatchEngine.createMatchSession(career, fixture)

        // Step 6 balls in 1st innings
        var state = runtime.stepBalls(6)
        assertEquals(1, state.currentInnings)
        assertTrue(state.runRateHistory.isNotEmpty())
        assertTrue(state.runRateHistory.all { it.rrr == null })
        assertEquals(state.currentRunRate, state.runRateHistory.last().crr, 0.01)

        // Advance through innings 1 into innings 2 chase
        while (runtime.currentInnings == 1) {
            state = runtime.advanceUntilNextPause(instantFinishAll = false)
            if (state.pendingKeyMoment != null) {
                runtime.applyIntervention(null, null, null, null)
                state = runtime.stepBalls(1)
            }
        }
        // Step a full over in 2nd innings chase
        state = runtime.stepBalls(6)
        assertEquals(2, state.currentInnings)
        assertNotNull(state.target)
        assertTrue(state.runRateHistory.isNotEmpty())
        assertTrue(state.runRateHistory.all { it.rrr != null })
        assertEquals(state.requiredRunRate!!, state.runRateHistory.last().rrr!!, 0.01)
    }

    @Test
    fun `live war room updates CRR vs RRR graph in real time as deliveries progress`() {
        val template = WorldGenerator.clubsFor(CricketNation.AUSTRALIA, 1).first()
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(77))
        val fixture = career.nextUserFixture!!
        val runtime = T20MatchEngine.createMatchSession(career, fixture)

        var liveState by mutableStateOf(runtime.stepBalls(1))

        composeRule.setContent {
            PitchCraftTheme {
                LiveMatchWarRoomView(
                    live = liveState,
                    isAutoStreaming = false,
                    streamSpeedMs = 750L,
                    onToggleAutoStream = {},
                    onCycleStreamSpeed = {},
                    onStepBalls = { count -> liveState = runtime.stepBalls(count) },
                    onAdvanceToMoment = {},
                    onApplyLiveTactics = { _, _, _, _, _ -> },
                    onSubmitDecision = { _, _, _, _, _, _ ->
                        runtime.applyIntervention(null, null, null, null, null)
                        liveState = runtime.stepBalls(1)
                    }
                )
            }
        }

        // First innings shows graph card, canvas, live CRR pill, and RRR Inn 2 pending pill
        composeRule.onNodeWithTag("live_war_room_list")
            .performScrollToNode(hasTestTag("live_run_rate_graph_card"))
        composeRule.onNodeWithTag("live_run_rate_graph_card").assertIsDisplayed()
        composeRule.onNodeWithTag("live_run_rate_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("graph_crr_value").assertIsDisplayed()
        composeRule.onNodeWithTag("graph_rrr_pending").assertIsDisplayed()
        assertNull(liveState.requiredRunRate)

        // Advance to 2nd innings chase
        while (runtime.currentInnings == 1 || runtime.ballsBowledInInnings < 6) {
            val next = runtime.advanceUntilNextPause(instantFinishAll = false)
            if (next.pendingKeyMoment != null) {
                runtime.applyIntervention(null, null, null, null)
            }
            liveState = runtime.stepBalls(6)
        }
        composeRule.waitForIdle()

        // Second innings shows live RRR pill and updates as +1 Over is clicked
        composeRule.onNodeWithTag("live_war_room_list")
            .performScrollToNode(hasTestTag("live_run_rate_graph_card"))
        composeRule.onNodeWithTag("graph_rrr_value").assertIsDisplayed()
        val beforeBalls = liveState.balls
        val beforeCrrText = "CRR ${"%.2f".format(liveState.currentRunRate)}"
        composeRule.onNodeWithText(beforeCrrText).assertIsDisplayed()

        if (liveState.pendingKeyMoment == null) {
            composeRule.onNodeWithTag("live_war_room_list")
                .performScrollToNode(hasTestTag("step_one_over_btn"))
            composeRule.onNodeWithTag("step_one_over_btn").performClick()
            composeRule.waitForIdle()
            assertTrue(liveState.balls > beforeBalls || liveState.finishedRecord != null)
            if (liveState.finishedRecord == null) {
                composeRule.onNodeWithTag("live_war_room_list")
                    .performScrollToNode(hasTestTag("live_run_rate_graph_card"))
                val updatedCrrText = "CRR ${"%.2f".format(liveState.currentRunRate)}"
                val updatedRrrText = "RRR ${"%.2f".format(liveState.requiredRunRate!!)}"
                composeRule.onNodeWithText(updatedCrrText).assertIsDisplayed()
                composeRule.onNodeWithText(updatedRrrText).assertIsDisplayed()
            }
        }
    }
}
