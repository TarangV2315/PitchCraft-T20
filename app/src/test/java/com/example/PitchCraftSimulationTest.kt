package com.example

import com.example.model.*
import com.example.simulation.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PitchCraftSimulationTest {

    @Test
    fun `new career generates domestic divisions, balanced squad, staff, and 11-player XI`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.country == CricketNation.INDIA && it.tier == 2 }
        val career = WorldGenerator.createNewCareer(template, "Test Manager", Random(42))

        assertEquals(2, career.divisions.size)
        assertTrue(career.divisions.all { it.clubs.size == 8 })
        assertEquals(17, career.squad.count { !it.isYouthAcademy })
        assertEquals(4, career.squad.count { it.isYouthAcademy })
        assertEquals(6, career.staff.size)
        assertEquals(11, career.selectedXIIds.size)
        assertNotNull(career.nextUserFixture)
    }

    @Test
    fun `staff advisor produces recommendations for injured and expiring players`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        val career = WorldGenerator.createNewCareer(template, "Coach", Random(99))

        val injuredPlayer = career.squad.first { it.injury.isInjured }.copy(
            injury = InjuryState(true, "Side Strain", 3, RehabMode.ACCELERATED)
        )
        val recs = StaffAdvisorEngine.buildPlayerRecommendations(injuredPlayer, career)
        assertTrue(recs.any { it.actionType == RecommendationActionType.SET_REHAB_MODE })

        val expiringPlayer = career.squad.first { !it.isYouthAcademy && !it.injury.isInjured && it.contract.weeksRemaining <= 16 }
        val expRecs = StaffAdvisorEngine.buildPlayerRecommendations(expiringPlayer, career)
        assertTrue(expRecs.any { it.actionType == RecommendationActionType.OFFER_CONTRACT_RENEWAL })
    }

    @Test
    fun `key moment match simulation completes and updates standings and stats`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(123))
        val fixture = career.nextUserFixture!!

        val runtime = T20MatchEngine.createMatchSession(career, fixture)
        var state = runtime.advanceUntilNextPause(instantFinishAll = false)
        var safety = 0
        while (state.finishedRecord == null && safety < 20) {
            assertNotNull(state.pendingKeyMoment)
            runtime.applyIntervention(
                tempo = BattingTempo.AGGRESSIVE,
                bowlingPlan = BowlingPlan.DEATH_YORKERS,
                fieldSpread = FieldSpread.BALANCED,
                impactPlayerId = null
            )
            state = runtime.advanceUntilNextPause(instantFinishAll = false)
            safety++
        }

        val record = state.finishedRecord
        assertNotNull(record)
        assertTrue(record!!.firstInnings.runs > 0)
        assertTrue(record.secondInnings.runs > 0)

        val updatedCareer = SeasonProgressionEngine.commitUserMatchAndSimulateRound(career, fixture, record, Random(123))
        val userStanding = updatedCareer.currentDivision!!.clubs.first { it.isUserClub }
        assertEquals(1, userStanding.played)
        assertEquals(2, updatedCareer.week)
        assertEquals(1, updatedCareer.matchHistory.size)
    }

    @Test
    fun `negotiation evaluates fair offer as accepted`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Director", Random(77))
        val target = career.transferMarket.first()
        val session = NegotiationSession(
            player = target,
            isRenewal = false,
            askingFee = target.marketValue,
            askingWage = target.contract.weeklyWage,
            askingYears = 2,
            offeredFee = target.marketValue,
            offeredWage = target.contract.weeklyWage,
            offeredSigningBonus = 5000,
            offeredYears = 2,
            agentPatience = 100,
            round = 1,
            agentMessage = "Initial"
        )
        val result = SeasonProgressionEngine.evaluateNegotiationOffer(session, career)
        assertTrue(result.isAccepted)
    }

    @Test
    fun `dashboard squad filter isolates starting XI, batters, all-rounders, and bowlers`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(314))
        val senior = career.squad.filter { !it.isYouthAcademy }

        val all = com.example.ui.components.filterDashboardSquad(
            senior, career.selectedXIIds, com.example.ui.components.DashboardSquadFilter.ALL
        )
        assertEquals(senior.size, all.size)
        assertTrue(all.zipWithNext().all { (a, b) -> a.overall >= b.overall })

        val xi = com.example.ui.components.filterDashboardSquad(
            senior, career.selectedXIIds, com.example.ui.components.DashboardSquadFilter.STARTING_XI
        )
        assertEquals(11, xi.size)

        val bowlers = com.example.ui.components.filterDashboardSquad(
            senior, career.selectedXIIds, com.example.ui.components.DashboardSquadFilter.BOWLERS
        )
        assertTrue(bowlers.isNotEmpty())
        assertTrue(bowlers.all {
            it.role in listOf(T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER, T20Role.SPINNER)
        })
    }

    @Test
    fun `financial tracking records ticket sales, sponsorships, player wages, and staff wages`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val baseCareer = WorldGenerator.createNewCareer(template, "Treasurer", Random(2025))
        // Pick a guaranteed home fixture so ticket sales > 0
        val homeFixture = baseCareer.fixtures.first { it.homeClubId == baseCareer.clubId }

        val runtime = T20MatchEngine.createMatchSession(baseCareer, homeFixture)
        val finished = runtime.advanceUntilNextPause(instantFinishAll = true).finishedRecord!!
        assertTrue(finished.ticketSalesRevenue > 0)
        assertTrue(finished.attendance > 0)

        val updated = SeasonProgressionEngine.commitUserMatchAndSimulateRound(baseCareer, homeFixture, finished, Random(2025))
        assertEquals(finished.ticketSalesRevenue, updated.seasonTicketIncome)
        assertEquals(1, updated.homeMatchesHosted)
        assertEquals(baseCareer.weeklySponsorIncome + finished.sponsorBonusEarned, updated.seasonSponsorIncome)
        assertEquals(baseCareer.weeklyPlayerWages, updated.seasonPlayerWagesPaid)
        assertEquals(baseCareer.weeklyStaffWages, updated.seasonStaffWagesPaid)

        assertTrue(updated.ledger.any { it.category == "Ticket Sales" && it.amount == finished.ticketSalesRevenue })
        assertTrue(updated.ledger.any { it.category == "Player Wages" && it.amount == -baseCareer.weeklyPlayerWages })
        assertTrue(updated.ledger.any { it.category == "Staff Wages" && it.amount == -baseCareer.weeklyStaffWages })

        val seasonSnapshot = com.example.ui.components.buildFinancialSnapshot(
            updated, com.example.ui.components.FinanceViewMode.SEASON_ACTUALS
        )
        assertEquals(updated.seasonTicketIncome + updated.seasonSponsorIncome, seasonSnapshot.totalIncome)
        assertEquals(updated.seasonPlayerWagesPaid + updated.seasonStaffWagesPaid, seasonSnapshot.totalWages)

        val weeklySnapshot = com.example.ui.components.buildFinancialSnapshot(
            updated, com.example.ui.components.FinanceViewMode.WEEKLY_RATE
        )
        assertEquals(updated.projectedWeeklyGateAverage, weeklySnapshot.ticketIncome)
        assertEquals(updated.weeklySponsorIncome, weeklySnapshot.sponsorIncome)
        assertEquals(updated.weeklyPlayerWages, weeklySnapshot.playerWages)
        assertEquals(updated.weeklyStaffWages, weeklySnapshot.staffWages)
    }

    @Test
    fun `assigning coaches to batting, bowling, and fitness updates progression boosts`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Director", Random(808))

        val initialBoosts = SeasonProgressionEngine.calculateCoachingBoosts(career.staff)
        assertEquals(2, initialBoosts.battingCoaches)
        assertEquals(1, initialBoosts.bowlingCoaches)
        assertEquals(1, initialBoosts.fitnessCoaches)
        assertTrue(initialBoosts.battingBonusPct > 0)
        assertTrue(initialBoosts.bowlingBonusPct > 0)
        assertTrue(initialBoosts.fitnessBonusPct > 0)

        // Reassign all coaches to Fitness and verify fitness boost climbs significantly
        val allFitnessStaff = career.staff.map { member ->
            if (member.isCoach) member.copy(assignedFocus = CoachTrainingArea.FITNESS) else member
        }
        val fitnessBoosts = SeasonProgressionEngine.calculateCoachingBoosts(allFitnessStaff)
        assertEquals(0, fitnessBoosts.battingCoaches)
        assertEquals(0, fitnessBoosts.bowlingCoaches)
        assertEquals(4, fitnessBoosts.fitnessCoaches)
        assertTrue(fitnessBoosts.fitnessBonusPct > initialBoosts.fitnessBonusPct)
    }

    @Test
    fun `matchup calculation reflects player skill stats and fitness levels with steeper death penalty`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Analyst", Random(55))
        val striker = career.squad.first { it.isAvailableForMatch }
        val bowler = career.squad.first { it.canBowl && it.isAvailableForMatch }

        val fitMatchup = T20MatchEngine.calculateMatchupFactors(
            striker = striker.copy(fitness = 96),
            bowler = bowler.copy(fitness = 85),
            pitchType = PitchType.BALANCED_DECK,
            phase = MatchPhase.MIDDLE
        )
        val fatiguedMatchup = T20MatchEngine.calculateMatchupFactors(
            striker = striker.copy(fitness = 55),
            bowler = bowler.copy(fitness = 85),
            pitchType = PitchType.BALANCED_DECK,
            phase = MatchPhase.MIDDLE
        )
        assertTrue(fitMatchup.batRating > fatiguedMatchup.batRating)
        assertTrue(fitMatchup.strikerFitnessFactor > fatiguedMatchup.strikerFitnessFactor)
        assertTrue(fitMatchup.skillEdge > fatiguedMatchup.skillEdge)

        val deathFatigued = T20MatchEngine.calculateMatchupFactors(
            striker = striker.copy(fitness = 55),
            bowler = bowler.copy(fitness = 85),
            pitchType = PitchType.BALANCED_DECK,
            phase = MatchPhase.DEATH
        )
        assertTrue(deathFatigued.strikerFitnessFactor < fatiguedMatchup.strikerFitnessFactor)
    }

    @Test
    fun `ball-by-ball step simulation populates real-time ball feed and live fitness state`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(909))
        val fixture = career.nextUserFixture!!

        val runtime = T20MatchEngine.createMatchSession(career, fixture)
        val stateAfter6 = runtime.stepBalls(6)

        assertTrue(stateAfter6.ballFeed.isNotEmpty())
        assertTrue(stateAfter6.ballFeed.size >= 6)
        assertTrue(stateAfter6.matchupSummary.isNotBlank())
        assertTrue(stateAfter6.activeBatterFitness.isNotEmpty())
        assertTrue(stateAfter6.activeBowlerFitness in 45..100)
        assertTrue(stateAfter6.ballFeed.all { it.commentary.isNotBlank() && it.symbol.isNotBlank() })

        val afterTactic = runtime.applyLiveTactics(
            tempo = BattingTempo.ALL_OUT,
            bowlingPlan = BowlingPlan.DEATH_YORKERS,
            fieldSpread = FieldSpread.DEFENSIVE,
            impactPlayerId = null
        )
        assertEquals(BattingTempo.ALL_OUT, afterTactic.currentTempo)
        assertEquals(BowlingPlan.DEATH_YORKERS, afterTactic.currentBowlingPlan)
        assertEquals(FieldSpread.DEFENSIVE, afterTactic.currentFieldSpread)
    }

    @Test
    fun `generated squad and market players receive diverse randomized portrait variants`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.country == CricketNation.INDIA && it.tier == 1 }
        val careerA = WorldGenerator.createNewCareer(template, "Mgr A", Random(101))
        val careerB = WorldGenerator.createNewCareer(template, "Mgr B", Random(202))

        val variantsA = careerA.squad.map { it.portraitVariant }
        assertTrue(variantsA.all { it in 0 until WorldGenerator.PORTRAIT_VARIANT_COUNT })
        assertEquals(
            "Every club squad should display all ${WorldGenerator.PORTRAIT_VARIANT_COUNT} portrait styles",
            WorldGenerator.PORTRAIT_VARIANT_COUNT,
            variantsA.toSet().size
        )

        val marketVariants = careerA.transferMarket.map { it.portraitVariant }
        assertTrue(marketVariants.all { it in 0 until WorldGenerator.PORTRAIT_VARIANT_COUNT })
        assertTrue(marketVariants.toSet().size >= 5)

        val variantsB = careerB.squad.map { it.portraitVariant }
        assertNotEquals("Different seeds should randomize portrait order", variantsA, variantsB)
    }
}


