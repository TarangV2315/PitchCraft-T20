package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.simulation.WorldGenerator
import com.example.ui.components.SeasonCalendarPreviewCard
import com.example.ui.components.SeasonCalendarSection
import com.example.ui.components.buildSeasonCalendarWeeks
import com.example.ui.theme.PitchCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SeasonCalendarRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `calendar builder populates 17 weeks with transfer windows, training camps, and rival matches`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        val career = WorldGenerator.createNewCareer(template, "Planner", Random(404))
        val weeks = buildSeasonCalendarWeeks(career)

        assertEquals(17, weeks.size)
        assertTrue(weeks.first { it.week == 1 }.isCurrentWeek)
        assertTrue(weeks.first { it.week == 1 }.hasTransferWindow)
        assertTrue(weeks.first { it.week == 8 }.hasTransferWindow)
        assertEquals(2, weeks.count { it.hasTransferWindow })

        val campWeeks = weeks.filter { it.hasTrainingCamp }.map { it.week }
        assertEquals(listOf(1, 4, 7, 11, 14), campWeeks)

        val rivalWeeks = weeks.filter { it.isKeyRivalMatch }
        assertTrue("Expected rival & playoff highlights", rivalWeeks.size >= 5)
        assertTrue(weeks.take(14).all { it.fixture != null && it.opponentName != null })
    }

    @Test
    fun `season calendar section selects weeks and reveals transfer window and training camp details`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Tactician", Random(505))

        composeRule.setContent {
            PitchCraftTheme {
                SeasonCalendarSection(
                    career = career,
                    onOpenTransfers = {},
                    onOpenMatchday = {},
                    onOpenSquadTraining = {}
                )
            }
        }

        // Week 8 shows Mid-Season Transfer Window details when clicked
        composeRule.onNodeWithTag("season_calendar_section")
            .performScrollToNode(hasTestTag("calendar_week_8"))
        composeRule.onNodeWithTag("calendar_week_8").performClick()

        composeRule.onNodeWithTag("season_calendar_section")
            .performScrollToNode(hasTestTag("calendar_selected_detail"))
        composeRule.onNodeWithText("Mid-Season Transfer Window (1 Wk)").assertIsDisplayed()

        // Week 4 shows Powerplay Attack Camp details when clicked
        composeRule.onNodeWithTag("season_calendar_section")
            .performScrollToNode(hasTestTag("calendar_week_4"))
        composeRule.onNodeWithTag("calendar_week_4").performClick()

        composeRule.onNodeWithTag("season_calendar_section")
            .performScrollToNode(hasTestTag("calendar_selected_detail"))
        composeRule.onNodeWithText("Powerplay Attack Camp").assertIsDisplayed()
    }

    @Test
    fun `hq calendar preview card displays milestones and opens full calendar`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.country == com.example.model.CricketNation.AUSTRALIA && it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Manager", Random(606))
        var opened = false

        composeRule.setContent {
            PitchCraftTheme {
                SeasonCalendarPreviewCard(
                    career = career,
                    onOpenFullCalendar = { opened = true }
                )
            }
        }

        composeRule.onNodeWithTag("hq_calendar_preview_card").assertIsDisplayed()
        composeRule.onNodeWithText("Transfer: W1").assertIsDisplayed()
        composeRule.onNodeWithText("Camp: W1").assertIsDisplayed()
        composeRule.onNodeWithTag("hq_open_calendar_btn").performClick()
        assertTrue(opened)
    }
}
