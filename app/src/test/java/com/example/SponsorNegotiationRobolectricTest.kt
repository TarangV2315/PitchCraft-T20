package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CareerRepository
import com.example.data.local.PitchCraftDatabase
import com.example.model.*
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.SponsorNegotiationEngine
import com.example.simulation.WorldGenerator
import com.example.ui.CareerViewModel
import com.example.ui.screens.ClubOfficeScreen
import com.example.ui.screens.OfficeSubTab
import com.example.ui.theme.PitchCraftTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
@Config(sdk = [36])
class SponsorNegotiationRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `sponsor portfolio offers diverse structures and significant revenue deviation`() {
        val mumbai = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(mumbai, "Director", Random(11))

        assertEquals(9, career.sponsors.size)
        val titleDeals = career.sponsors.filter { it.category == SponsorCategory.TITLE }
        assertEquals(3, titleDeals.size)

        val anchor = titleDeals.first { it.structureType == SponsorStructureType.GUARANTEED_ANCHOR }
        val escalator = titleDeals.first { it.structureType == SponsorStructureType.PERFORMANCE_ESCALATOR }

        // Anchor gives noticeably higher guaranteed base weekly + signing cash
        assertTrue(anchor.weeklyPayout > escalator.weeklyPayout * 2)
        assertTrue(anchor.signingBonus > escalator.signingBonus)
        assertTrue(anchor.projectedSeasonMin > escalator.projectedSeasonMin)

        // Escalator offers much higher performance payouts & higher peak ceiling
        assertTrue(escalator.winBonus > anchor.winBonus * 3)
        assertTrue(escalator.perCenturyBonus > anchor.perCenturyBonus * 4)
        assertTrue(escalator.perFiftyBonus > anchor.perFiftyBonus * 4)
        assertTrue(escalator.perThreeWicketHaulBonus > anchor.perThreeWicketHaulBonus * 4)
        assertTrue(escalator.projectedSeasonMax > anchor.projectedSeasonMax)

        // Ensure all 4 structure archetypes are represented
        val types = career.sponsors.map { it.structureType }.toSet()
        assertTrue(types.contains(SponsorStructureType.GUARANTEED_ANCHOR))
        assertTrue(types.contains(SponsorStructureType.BALANCED))
        assertTrue(types.contains(SponsorStructureType.PERFORMANCE_ESCALATOR))
        assertTrue(types.contains(SponsorStructureType.STAR_BRANDING))
    }

    @Test
    fun `leverage reflects city demography buying pattern tournament history and player calibre`() {
        val metroClub = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first { it.clubId == "in_t1_1" } // Mumbai
        val regionalClub = WorldGenerator.clubsFor(CricketNation.INDIA, 2).last { it.clubId == "in_t2_8" } // Guwahati

        val metroCareer = WorldGenerator.createNewCareer(metroClub, "Boss", Random(21))
        val regionalCareer = WorldGenerator.createNewCareer(regionalClub, "Boss", Random(21))

        val metroLev = SponsorNegotiationEngine.evaluateClubLeverage(metroCareer)
        val regionalLev = SponsorNegotiationEngine.evaluateClubLeverage(regionalCareer)

        assertTrue(metroLev.cityDemographyScore > regionalLev.cityDemographyScore)
        assertTrue(metroLev.overallLeverageScore > regionalLev.overallLeverageScore)

        // Buying pattern reacts to ticket tier change
        val economyLev = SponsorNegotiationEngine.evaluateClubLeverage(
            metroCareer.copy(budgets = metroCareer.budgets.copy(ticketTier = TicketTier.ECONOMY))
        )
        val vipLev = SponsorNegotiationEngine.evaluateClubLeverage(
            metroCareer.copy(budgets = metroCareer.budgets.copy(ticketTier = TicketTier.PREMIUM))
        )
        assertTrue(vipLev.buyingPatternScore > economyLev.buyingPatternScore)

        // Tournament history & star player calibre lift leverage and max negotiation headroom
        val boostedSquad = regionalCareer.squad.mapIndexed { i, p ->
            if (i < 4) p.copy(overall = 90, potential = 94) else p
        }
        val dynastyRegional = regionalCareer.copy(
            trophiesWon = 2,
            managerReputation = 88,
            boardConfidence = 94,
            squad = boostedSquad,
            seasonHistory = listOf(
                SeasonFinishRecord(1, 2, 1, 24, 1.45, true),
                SeasonFinishRecord(2, 1, 2, 20, 0.92, true)
            )
        )
        val dynastyLev = SponsorNegotiationEngine.evaluateClubLeverage(dynastyRegional)
        assertTrue(dynastyLev.performanceHistoryScore > regionalLev.performanceHistoryScore)
        assertTrue(dynastyLev.playerCalibreScore > regionalLev.playerCalibreScore)
        assertTrue(dynastyLev.maxNegotiationLiftPct > regionalLev.maxNegotiationLiftPct)
    }

    @Test
    fun `negotiation accepts fair uplift within leverage ceiling and walks away on excessive demand`() {
        val mumbai = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(mumbai, "Boss", Random(31))
        val target = career.sponsors.first { it.id == "sp_kit_1" }

        val session = SponsorNegotiationEngine.startNegotiation(target, career)
        val modestOffer = session.copy(
            offeredBaseWeekly = (target.weeklyPayout * 1.08).toInt(),
            offeredBrandingWeekly = (target.playerBrandingWeekly * 1.10).toInt(),
            offeredWinBonus = (target.winBonus * 1.05).toInt()
        )
        val acceptedResult = SponsorNegotiationEngine.evaluateCounterOffer(modestOffer)
        assertTrue(acceptedResult.isAccepted)
        assertTrue(acceptedResult.deal.negotiatedPct > 0)
        assertTrue(acceptedResult.deal.totalWeeklyGuaranteed > target.totalWeeklyGuaranteed)

        // Excessive counter-offers drain patience and walk away
        var greedySession = session.copy(
            offeredBaseWeekly = target.weeklyPayout * 4,
            offeredBrandingWeekly = target.playerBrandingWeekly * 4,
            offeredWinBonus = target.winBonus * 4
        )
        repeat(4) {
            if (!greedySession.isWalkedAway) {
                greedySession = SponsorNegotiationEngine.evaluateCounterOffer(greedySession)
            }
        }
        assertTrue(greedySession.isWalkedAway)
        assertEquals(0, greedySession.brandPatience)
    }

    @Test
    fun `match payout computes win bonus plus individual 50 century and 3w haul bonuses`() {
        val mumbai = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(mumbai, "Skipper", Random(41))
        val kitMilestone = career.sponsors.first { it.id == "sp_kit_2" }.copy(isActive = true)
        val titleBalanced = career.sponsors.first { it.id == "sp_title_1" }.copy(isActive = true)
        val sponsors = listOf(titleBalanced, kitMilestone)

        val batters = listOf(
            BatterScoreEntry("b1", "Star Opener", "OPN", runs = 104, balls = 54, fours = 10, sixes = 6),
            BatterScoreEntry("b2", "Mid Bat", "MID", runs = 62, balls = 36, fours = 6, sixes = 2),
            BatterScoreEntry("b3", "Finisher", "FIN", runs = 24, balls = 12, fours = 2, sixes = 2)
        )
        val bowlers = listOf(
            BowlerScoreEntry("w1", "Pace Ace", ballsBowled = 24, runsConceded = 22, wickets = 4, dotBalls = 11),
            BowlerScoreEntry("w2", "Spinner", ballsBowled = 24, runsConceded = 26, wickets = 2, dotBalls = 9)
        )

        val breakdown = SponsorNegotiationEngine.evaluateMatchSponsorPayout(
            sponsors = sponsors,
            userWon = true,
            userBatters = batters,
            userBowlers = bowlers
        )

        val expectedWin = titleBalanced.winBonus + kitMilestone.winBonus
        val expectedMilestones = (titleBalanced.perCenturyBonus + kitMilestone.perCenturyBonus) +
            (titleBalanced.perFiftyBonus + kitMilestone.perFiftyBonus) +
            (titleBalanced.perThreeWicketHaulBonus + kitMilestone.perThreeWicketHaulBonus)

        assertEquals(1, breakdown.centuriesCount)
        assertEquals(1, breakdown.fiftiesCount)
        assertEquals(1, breakdown.threeWicketHaulsCount)
        assertEquals(expectedWin, breakdown.winBonus)
        assertEquals(expectedMilestones, breakdown.individualMilestoneBonus)
        assertEquals(expectedWin + expectedMilestones, breakdown.totalBonus)
    }

    @Test
    fun `sponsorship selection is open only at start of season week 1 and frozen afterwards`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PitchCraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = CareerRepository(db.careerDao())
        val vm = CareerViewModel(repo)

        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val week1Career = WorldGenerator.createNewCareer(template, "Exec", Random(51))
        runBlocking { repo.saveCareer(week1Career) }
        vm.loadCareer(week1Career.careerId)
        waitForCondition { vm.activeCareer.value?.week == 1 }

        // Week 1: selection and negotiation succeed
        assertTrue(vm.activeCareer.value!!.isSponsorshipWindowOpen)
        vm.signSponsorDeal("sp_kit_1")
        waitForCondition {
            runBlocking {
                repo.loadCareer(week1Career.careerId)?.sponsors?.any { it.id == "sp_kit_1" && it.isActive } == true
            }
        }

        vm.openSponsorNegotiation("sp_stad_2")
        assertNotNull(vm.sponsorNegotiation.value)
        vm.closeSponsorNegotiation()

        // Advance Week 1 -> Week 2: sponsorship portfolio freezes
        val week2Career = SeasonProgressionEngine.advanceWeeklyTick(vm.activeCareer.value!!, Random(51))
        runBlocking { repo.saveCareer(week2Career) }
        vm.loadCareer(week2Career.careerId)
        waitForCondition { vm.activeCareer.value?.week == 2 }

        assertFalse(vm.activeCareer.value!!.isSponsorshipWindowOpen)
        vm.signSponsorDeal("sp_stad_3")
        assertFalse(vm.activeCareer.value!!.sponsors.first { it.id == "sp_stad_3" }.isActive)
        vm.openSponsorNegotiation("sp_title_2")
        assertNull(vm.sponsorNegotiation.value)

        // Starting next season re-opens sponsorship window at Week 1 and records SeasonFinishRecord
        val nextSeason = SeasonProgressionEngine.startNextSeason(
            vm.activeCareer.value!!.copy(seasonComplete = true),
            Random(51)
        )
        assertEquals(1, nextSeason.week)
        assertTrue(nextSeason.isSponsorshipWindowOpen)
        assertEquals(1, nextSeason.seasonHistory.size)

        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Thread.sleep(100)
        db.close()
    }

    @Test
    fun `office sponsorship UI renders leverage pillars negotiation modal in week 1 and frozen lock in week 2`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val week1 = WorldGenerator.createNewCareer(template, "Gaffer", Random(61))
        var negotiatedId: String? = null

        composeRule.setContent {
            PitchCraftTheme {
                ClubOfficeScreen(
                    career = week1,
                    activeTab = OfficeSubTab.FINANCE_SPONSORS,
                    onSelectTab = {},
                    onToggleDelegation = {},
                    onAssignCoachFocus = { _, _ -> },
                    onHireStaff = { _, _ -> },
                    onSignSponsor = {},
                    onNegotiateSponsor = { negotiatedId = it },
                    onSetTicketTier = {},
                    onAdjustBudget = { _, _, _ -> }
                )
            }
        }

        val list = composeRule.onNodeWithTag("finance_sponsors_list")
        list.performScrollToNode(hasTestTag("sponsor_window_banner"))
        composeRule.onNodeWithTag("sponsor_window_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Pre-Season Sponsorship Window Open (Week 1)").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("sponsor_leverage_card"))
        composeRule.onNodeWithTag("sponsor_leverage_card").assertIsDisplayed()
        composeRule.onNodeWithText("City Demography").assertIsDisplayed()
        composeRule.onNodeWithText("Fan Buying Pattern").assertIsDisplayed()
        composeRule.onNodeWithText("Tournament History").assertIsDisplayed()
        composeRule.onNodeWithText("Calibre of Players").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("negotiate_sponsor_sp_title_2"))
        composeRule.onNodeWithTag("negotiate_sponsor_sp_title_2").performClick()
        assertEquals("sp_title_2", negotiatedId)
    }

    @Test
    fun `office sponsorship UI hides sign buttons and shows frozen state in week 2`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val week2 = SeasonProgressionEngine.advanceWeeklyTick(
            WorldGenerator.createNewCareer(template, "Gaffer", Random(62)),
            Random(62)
        )

        composeRule.setContent {
            PitchCraftTheme {
                ClubOfficeScreen(
                    career = week2,
                    activeTab = OfficeSubTab.FINANCE_SPONSORS,
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

        val list = composeRule.onNodeWithTag("finance_sponsors_list")
        list.performScrollToNode(hasTestTag("sponsor_window_banner"))
        composeRule.onNodeWithTag("sponsor_window_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Sponsorship Selection Frozen for Season 1").assertIsDisplayed()

        list.performScrollToNode(hasTestTag("frozen_sponsor_sp_title_1"))
        composeRule.onNodeWithTag("frozen_sponsor_sp_title_1").assertIsDisplayed()
        list.performScrollToNode(hasTestTag("sponsor_card_sp_title_2"))
        composeRule.onNodeWithTag("sign_sponsor_sp_title_2").assertDoesNotExist()
        composeRule.onNodeWithTag("negotiate_sponsor_sp_title_2").assertDoesNotExist()
    }

    private fun waitForCondition(timeoutMs: Long = 3000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        assertTrue("Timed out waiting for ViewModel state update", predicate())
    }
}
