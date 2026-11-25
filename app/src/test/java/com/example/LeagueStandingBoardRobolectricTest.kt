package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.T20MatchEngine
import com.example.simulation.WorldGenerator
import com.example.ui.components.BattingSortMetric
import com.example.ui.components.BowlingSortMetric
import com.example.ui.components.LeagueStandingBoardSection
import com.example.ui.components.buildLeagueBattingLeaders
import com.example.ui.components.buildLeagueBowlingLeaders
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
class LeagueStandingBoardRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `mergeStats tracks 30s 50s dot balls maidens and 3w hauls`() {
        val bat30 = BatterScoreEntry("p1", "Batter A", "OPN", runs = 38, balls = 22, fours = 4, sixes = 2, isOut = true)
        val bat50 = BatterScoreEntry("p1", "Batter A", "OPN", runs = 64, balls = 36, fours = 6, sixes = 3, isOut = false)
        val bowlHaul = BowlerScoreEntry("p2", "Bowler B", ballsBowled = 24, runsConceded = 18, wickets = 3, dotBalls = 13, maidens = 1)

        val afterMatch1 = SeasonProgressionEngine.mergeStats(PlayerStats(), bat30, bowlHaul)
        assertEquals(1, afterMatch1.thirties)
        assertEquals(0, afterMatch1.fifties)
        assertEquals(1, afterMatch1.maidens)
        assertEquals(1, afterMatch1.threeWicketHauls)
        assertEquals(13, afterMatch1.dotBalls)
        assertEquals(3, afterMatch1.wickets)

        val afterMatch2 = SeasonProgressionEngine.mergeStats(afterMatch1, bat50, null)
        assertEquals(1, afterMatch2.thirties)
        assertEquals(1, afterMatch2.fifties)
        assertEquals(102, afterMatch2.runs)
        assertEquals(58, afterMatch2.ballsFaced)
        assertTrue(afterMatch2.strikeRate > 170.0)
    }

    @Test
    fun `match commit updates league points NRR squad stats and tournament leaderboards`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val initialCareer = WorldGenerator.createNewCareer(template, "Captain", Random(42))
        val firstFixture = initialCareer.nextUserFixture!!

        val engine = T20MatchEngine.createMatchSession(initialCareer, firstFixture)
        val liveResult = engine.advanceUntilNextPause(instantFinishAll = true)
        val record = liveResult.finishedRecord!!

        val afterRound = SeasonProgressionEngine.commitUserMatchAndSimulateRound(
            career = initialCareer,
            fixture = firstFixture,
            record = record,
            random = Random(42)
        )

        val tier1Standings = afterRound.divisions.first { it.tier == 1 }.clubs
        val userStanding = tier1Standings.first { it.clubId == initialCareer.clubId }
        assertEquals(1, userStanding.played)
        assertEquals(if (record.userWon) 2 else 0, userStanding.points)
        assertTrue(tier1Standings.all { it.played == 1 })

        val batLeaders = buildLeagueBattingLeaders(afterRound, tier = 1, sortBy = BattingSortMetric.MOST_RUNS)
        val bowlLeaders = buildLeagueBowlingLeaders(afterRound, tier = 1, sortBy = BowlingSortMetric.WICKETS)
        assertTrue(batLeaders.isNotEmpty())
        assertTrue(bowlLeaders.isNotEmpty())
        assertTrue(batLeaders.first().stats.runs >= batLeaders.last().stats.runs)
        assertTrue(bowlLeaders.first().stats.wickets >= bowlLeaders.last().stats.wickets)
    }

    @Test
    fun `league standing board UI shows points NRR promo rel and batting bowling leaderboards`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val baseCareer = WorldGenerator.createNewCareer(template, "Boss", Random(77))

        val customSquad = baseCareer.squad.mapIndexed { idx, p ->
            when (idx) {
                0 -> p.copy(
                    name = "Aarav Sharma",
                    seasonStats = PlayerStats(
                        matches = 5,
                        innings = 5,
                        runs = 248,
                        ballsFaced = 142,
                        notOuts = 1,
                        thirties = 2,
                        fifties = 2,
                        highScore = 84
                    )
                )
                11 -> p.copy(
                    name = "Kagiso Rabada",
                    seasonStats = PlayerStats(
                        matches = 5,
                        ballsBowled = 114,
                        runsConceded = 132,
                        wickets = 11,
                        dotBalls = 49,
                        maidens = 2,
                        threeWicketHauls = 2
                    )
                )
                else -> p
            }
        }
        val t1WithPoints = baseCareer.divisions.first { it.tier == 1 }.clubs.mapIndexed { idx, c ->
            if (idx == 0) c.copy(played = 5, won = 5, lost = 0, runsFor = 920, ballsFaced = 580, runsAgainst = 760, ballsBowled = 600, points = 10)
            else if (idx == 7) c.copy(played = 5, won = 0, lost = 5, runsFor = 680, ballsFaced = 600, runsAgainst = 890, ballsBowled = 570, points = 0)
            else c.copy(played = 5, won = 3, lost = 2, runsFor = 810, ballsFaced = 600, runsAgainst = 805, ballsBowled = 600, points = 6)
        }
        val t2WithPoints = baseCareer.divisions.first { it.tier == 2 }.clubs.mapIndexed { idx, c ->
            if (idx == 0) c.copy(played = 5, won = 5, lost = 0, runsFor = 880, ballsFaced = 575, runsAgainst = 740, ballsBowled = 600, points = 10)
            else c.copy(played = 5, won = 2, lost = 3, runsFor = 770, ballsFaced = 600, runsAgainst = 810, ballsBowled = 600, points = 4)
        }
        val populatedCareer = baseCareer.copy(
            squad = customSquad,
            divisions = listOf(
                baseCareer.divisions.first { it.tier == 1 }.copy(clubs = t1WithPoints),
                baseCareer.divisions.first { it.tier == 2 }.copy(clubs = t2WithPoints)
            )
        )

        var selectedTier by mutableIntStateOf(1)
        composeRule.setContent {
            PitchCraftTheme {
                LeagueStandingBoardSection(
                    career = populatedCareer,
                    selectedTier = selectedTier,
                    onSelectTier = { selectedTier = it }
                )
            }
        }

        // Verify Standings Table + (REL) indicator in Tier 1
        composeRule.onNodeWithText("NRR").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("8. Pune Challengers (REL)").performScrollTo().assertIsDisplayed()

        // Scroll to Tournament Stats Board and verify Batting columns & leader
        composeRule.onNodeWithTag("pyramid_standings_list")
            .performScrollToNode(hasTestTag("tournament_stats_board"))
        composeRule.onNodeWithTag("tournament_stats_board").assertIsDisplayed()
        composeRule.onNodeWithText("Runs").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("SR").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("50s").onFirst().performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("30s").onFirst().performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1. Aarav Sharma").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("248").performScrollTo().assertIsDisplayed()

        // Switch Batting sort metric to 30s & Strike Rate
        composeRule.onNodeWithTag("bat_sort_thirties").performScrollTo().performClick()
        composeRule.onNodeWithTag("bat_sort_strike_rate").performScrollTo().performClick()
        composeRule.onNodeWithText("1. Aarav Sharma").performScrollTo().assertIsDisplayed()

        // Switch to Bowling Stats leaderboard and verify Wkt, Dots, Mdn, 3W columns + leader
        composeRule.onNodeWithTag("leaderboard_bowling_tab").performScrollTo().performClick()
        composeRule.onNodeWithText("Wkt").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Dots").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mdn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3W").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1. Kagiso Rabada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("49").performScrollTo().assertIsDisplayed()

        // Switch Bowling sort metrics
        composeRule.onNodeWithTag("bowl_sort_maidens").performScrollTo().performClick()
        composeRule.onNodeWithTag("bowl_sort_three_wk_hauls").performScrollTo().performClick()
        composeRule.onNodeWithText("1. Kagiso Rabada").performScrollTo().assertIsDisplayed()

        // Switch to Tier 2 and verify (PROMO) indicator
        composeRule.onNodeWithTag("pyramid_standings_list")
            .performScrollToNode(hasTestTag("pyramid_tier_2"))
        composeRule.onNodeWithTag("pyramid_tier_2").performClick()
        composeRule.onNodeWithText("1. Jaipur Panthers (PROMO)").performScrollTo().assertIsDisplayed()
    }
}
