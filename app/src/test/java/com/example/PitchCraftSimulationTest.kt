package com.example

import com.example.model.*
import com.example.simulation.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class PitchCraftSimulationTest {

    @Test
    fun `new career generates 3 divisions, balanced squad, staff, and 11-player XI`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 3 }
        val career = WorldGenerator.createNewCareer(template, "Test Manager", Random(42))

        assertEquals(3, career.divisions.size)
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
}
