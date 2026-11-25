package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.model.CricketNation
import com.example.model.Fixture
import com.example.model.PitchType
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.WorldGenerator
import com.example.ui.screens.ClubOfficeScreen
import com.example.ui.screens.OfficeSubTab
import com.example.ui.theme.PitchCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DomesticPyramidRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `india and england have 2 domestic tiers while australia south africa and caribbean have single league`() {
        listOf(CricketNation.INDIA, CricketNation.ENGLAND).forEach { nation ->
            val t1Clubs = WorldGenerator.clubsFor(nation, 1)
            val t2Clubs = WorldGenerator.clubsFor(nation, 2)
            assertEquals(8, t1Clubs.size)
            assertEquals(8, t2Clubs.size)

            val career = WorldGenerator.createNewCareer(t1Clubs.first(), "Skipper", Random(101))
            assertEquals(nation, career.country)
            assertEquals(2, career.divisions.size)
            assertTrue(career.divisions.all { it.clubs.size == 8 })

            // Every fixture in a domestic career only features clubs from that domestic pyramid
            val validClubIds = (t1Clubs + t2Clubs).map { it.clubId }.toSet()
            assertTrue(career.fixtures.all { it.homeClubId in validClubIds && it.awayClubId in validClubIds })
        }

        listOf(CricketNation.AUSTRALIA, CricketNation.SOUTH_AFRICA, CricketNation.CARIBBEAN).forEach { nation ->
            val leagueClubs = WorldGenerator.clubsFor(nation, 1)
            assertEquals(8, leagueClubs.size)

            val career = WorldGenerator.createNewCareer(leagueClubs.first(), "Boss", Random(202))
            assertEquals(nation, career.country)
            assertEquals(1, career.divisions.size)
            assertEquals(1, career.divisionTier)
            assertEquals(8, career.divisions.first().clubs.size)

            val validClubIds = leagueClubs.map { it.clubId }.toSet()
            assertTrue(career.fixtures.all { it.homeClubId in validClubIds && it.awayClubId in validClubIds })
        }
    }

    @Test
    fun `last place in india or england major city league relegates and minor city winner promotes`() {
        // Test relegation from England Tier 1 (Major Cities -> Minor Cities)
        val engMajorClubs = WorldGenerator.clubsFor(CricketNation.ENGLAND, 1)
        val userMajorTemplate = engMajorClubs.first()
        val engCareer = WorldGenerator.createNewCareer(userMajorTemplate, "Manager", Random(303))

        val t1WithPoints = engCareer.divisions.first { it.tier == 1 }.clubs.mapIndexed { idx, c ->
            if (c.clubId == userMajorTemplate.clubId) c.copy(played = 14, won = 1, lost = 13, points = 2)
            else c.copy(played = 14, won = 10 - idx.coerceAtMost(7), lost = 4, points = 20 - idx * 2)
        }
        val t2Clubs = engCareer.divisions.first { it.tier == 2 }.clubs
        val t2WinnerClub = t2Clubs[1] // Pick index 1 via Grand Final win to verify Grand Final winner promotes
        val t2RunnerUp = t2Clubs[0]
        val completedT2Final = Fixture(
            id = "po_fin_1",
            round = 17,
            tier = 2,
            homeClubId = t2WinnerClub.clubId,
            homeClubName = t2WinnerClub.name,
            awayClubId = t2RunnerUp.clubId,
            awayClubName = t2RunnerUp.name,
            pitchType = PitchType.BATTING_PARADISE,
            isPlayoff = true,
            playoffLabel = "Grand Final",
            isCompleted = true,
            resultSummary = "${t2WinnerClub.name} won (182 vs 170)"
        )

        val finishedEngCareer = engCareer.copy(
            seasonComplete = true,
            divisions = listOf(
                engCareer.divisions.first { it.tier == 1 }.copy(clubs = t1WithPoints),
                engCareer.divisions.first { it.tier == 2 }.copy(clubs = t2Clubs)
            ),
            fixtures = engCareer.fixtures + completedT2Final
        )

        val nextEngSeason = SeasonProgressionEngine.startNextSeason(finishedEngCareer, Random(303))
        assertEquals(2, nextEngSeason.divisionTier)
        val nextT1Ids = nextEngSeason.divisions.first { it.tier == 1 }.clubs.map { it.clubId }
        val nextT2Ids = nextEngSeason.divisions.first { it.tier == 2 }.clubs.map { it.clubId }

        assertEquals(8, nextT1Ids.size)
        assertEquals(8, nextT2Ids.size)
        assertFalse(userMajorTemplate.clubId in nextT1Ids)
        assertTrue(userMajorTemplate.clubId in nextT2Ids)
        assertTrue(t2WinnerClub.clubId in nextT1Ids)
        assertFalse(t2WinnerClub.clubId in nextT2Ids)

        // Test promotion from India Tier 2 (Minor Cities -> Major Cities) when winning Minor City League
        val indMinorTemplate = WorldGenerator.clubsFor(CricketNation.INDIA, 2).first()
        val indCareer = WorldGenerator.createNewCareer(indMinorTemplate, "Coach", Random(404))
        val indT1Clubs = indCareer.divisions.first { it.tier == 1 }.clubs.mapIndexed { idx, c ->
            c.copy(played = 14, won = 12 - idx, points = 24 - idx * 2)
        }
        val expectedRelegatedFromIndT1 = SeasonProgressionEngine.sortDivision(indT1Clubs).last()
        val indT2Opponent = indCareer.divisions.first { it.tier == 2 }.clubs.first { it.clubId != indMinorTemplate.clubId }
        val indT2Final = Fixture(
            id = "po_fin_ind",
            round = 17,
            tier = 2,
            homeClubId = indMinorTemplate.clubId,
            homeClubName = indMinorTemplate.name,
            awayClubId = indT2Opponent.clubId,
            awayClubName = indT2Opponent.name,
            pitchType = PitchType.BATTING_PARADISE,
            isPlayoff = true,
            playoffLabel = "Grand Final",
            isCompleted = true,
            resultSummary = "${indMinorTemplate.name} won (190 vs 168)"
        )
        val nextIndSeason = SeasonProgressionEngine.startNextSeason(
            indCareer.copy(
                seasonComplete = true,
                divisions = listOf(
                    indCareer.divisions.first { it.tier == 1 }.copy(clubs = indT1Clubs),
                    indCareer.divisions.first { it.tier == 2 }
                ),
                fixtures = indCareer.fixtures + indT2Final
            ),
            Random(404)
        )
        assertEquals(1, nextIndSeason.divisionTier)
        assertTrue(indMinorTemplate.clubId in nextIndSeason.divisions.first { it.tier == 1 }.clubs.map { it.clubId })
        assertTrue(expectedRelegatedFromIndT1.clubId in nextIndSeason.divisions.first { it.tier == 2 }.clubs.map { it.clubId })
    }

    @Test
    fun `single league countries never relegate bottom clubs across seasons`() {
        val ausClub = WorldGenerator.clubsFor(CricketNation.AUSTRALIA, 1).last()
        val ausCareer = WorldGenerator.createNewCareer(ausClub, "Manager", Random(505))
        val bottomFinish = ausCareer.divisions.first().clubs.mapIndexed { idx, c ->
            if (c.clubId == ausClub.clubId) c.copy(played = 14, won = 0, lost = 14, points = 0)
            else c.copy(played = 14, won = 10 - idx.coerceAtMost(6), points = 20 - idx * 2)
        }

        val nextSeason = SeasonProgressionEngine.startNextSeason(
            ausCareer.copy(
                seasonComplete = true,
                divisions = listOf(ausCareer.divisions.first().copy(clubs = bottomFinish))
            ),
            Random(505)
        )

        assertEquals(1, nextSeason.divisionTier)
        assertEquals(1, nextSeason.divisions.size)
        assertTrue(ausClub.clubId in nextSeason.divisions.first().clubs.map { it.clubId })
    }

    @Test
    fun `pyramid standings tab shows 2 tiers with rel and promo badges for india and single league for south africa`() {
        val indTemplate = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val indCareer = WorldGenerator.createNewCareer(indTemplate, "Skipper", Random(606))

        composeRule.setContent {
            PitchCraftTheme {
                ClubOfficeScreen(
                    career = indCareer,
                    activeTab = OfficeSubTab.PYRAMID,
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

        composeRule.onNodeWithTag("pyramid_tier_1").assertIsDisplayed()
        composeRule.onNodeWithTag("pyramid_tier_2").assertIsDisplayed()
        composeRule.onNodeWithText("India Domestic T20 (India)").assertIsDisplayed()

        // Switch to Tier 2 Minor Cities standings
        composeRule.onNodeWithTag("pyramid_tier_2").performClick()
        composeRule.onNodeWithTag("pyramid_standings_list")
            .performScrollToNode(hasTestTag("pyramid_tier_2"))
        composeRule.onNodeWithText("India Tier 2 • Minor Cities League").assertIsDisplayed()
    }

    @Test
    fun `pyramid standings tab hides tier 2 toggle for single league countries`() {
        val rsaTemplate = WorldGenerator.clubsFor(CricketNation.SOUTH_AFRICA, 1).first()
        val rsaCareer = WorldGenerator.createNewCareer(rsaTemplate, "Coach", Random(707))

        composeRule.setContent {
            PitchCraftTheme {
                ClubOfficeScreen(
                    career = rsaCareer,
                    activeTab = OfficeSubTab.PYRAMID,
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

        composeRule.onNodeWithText("South Africa T20 League (South Africa)").assertIsDisplayed()
        composeRule.onNodeWithTag("pyramid_tier_2").assertDoesNotExist()
    }
}
