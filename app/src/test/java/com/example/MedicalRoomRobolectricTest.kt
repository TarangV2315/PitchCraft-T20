package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.model.InjuryState
import com.example.model.RehabMode
import com.example.model.TrainingFocus
import com.example.model.TrainingIntensity
import com.example.simulation.MedicalRoomEngine
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.WorldGenerator
import com.example.ui.components.MedicalRoomSection
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
class MedicalRoomRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `injury probability scales with training intensity and match workload`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "PhysioLead", Random(701))
        val basePlayer = career.squad.first { !it.isYouthAcademy && !it.injury.isInjured }.copy(
            fitness = 72,
            recentMatchWorkload = 20,
            trainingFocus = TrainingFocus.POWER_HITTING
        )
        val heavyWorkloadPlayer = basePlayer.copy(recentMatchWorkload = 82, fitness = 62)

        val lightProb = MedicalRoomEngine.calculateInjuryProbabilityPct(
            player = basePlayer,
            intensity = TrainingIntensity.LIGHT,
            isSelectedInXI = true,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 8
        )
        val intenseProb = MedicalRoomEngine.calculateInjuryProbabilityPct(
            player = basePlayer,
            intensity = TrainingIntensity.INTENSE,
            isSelectedInXI = true,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 8
        )
        val heavyMatchIntenseProb = MedicalRoomEngine.calculateInjuryProbabilityPct(
            player = heavyWorkloadPlayer,
            intensity = TrainingIntensity.INTENSE,
            isSelectedInXI = true,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 8
        )

        val lightLoad = MedicalRoomEngine.calculateTrainingLoadScore(basePlayer, TrainingIntensity.LIGHT)
        val intenseLoad = MedicalRoomEngine.calculateTrainingLoadScore(basePlayer, TrainingIntensity.INTENSE)
        val baseMatchLoad = MedicalRoomEngine.calculateMatchWorkloadScore(basePlayer, true)
        val highMatchLoad = MedicalRoomEngine.calculateMatchWorkloadScore(heavyWorkloadPlayer, true)

        assertTrue(intenseLoad > lightLoad)
        assertTrue(highMatchLoad > baseMatchLoad)
        assertTrue(intenseProb > lightProb)
        assertTrue(heavyMatchIntenseProb > intenseProb)

        val recoveryProb = MedicalRoomEngine.calculateInjuryProbabilityPct(
            player = heavyWorkloadPlayer.copy(trainingFocus = TrainingFocus.FITNESS_RECOVERY),
            intensity = TrainingIntensity.LIGHT,
            isSelectedInXI = false,
            medicalLevel = 4,
            physioRating = 85,
            fitnessCoachBonusPct = 18
        )
        assertTrue(recoveryProb < heavyMatchIntenseProb)
    }

    @Test
    fun `recovery duration and setback risk respond to rehab mode and medical quality`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        val career = WorldGenerator.createNewCareer(template, "RehabLead", Random(702))
        val injuredConservative = career.squad.first { !it.isYouthAcademy }.copy(
            injury = InjuryState(
                isInjured = true,
                injuryName = "Hamstring Strain",
                weeksRemaining = 4,
                rehabMode = RehabMode.CONSERVATIVE
            )
        )
        val injuredAccelerated = injuredConservative.copy(
            injury = injuredConservative.injury.copy(rehabMode = RehabMode.ACCELERATED)
        )

        val conservativeEta = MedicalRoomEngine.calculateEstimatedRecoveryWeeks(
            player = injuredConservative,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 5
        )
        val acceleratedEta = MedicalRoomEngine.calculateEstimatedRecoveryWeeks(
            player = injuredAccelerated,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 5
        )
        val topFacilityEta = MedicalRoomEngine.calculateEstimatedRecoveryWeeks(
            player = injuredAccelerated,
            medicalLevel = 5,
            physioRating = 90,
            fitnessCoachBonusPct = 20
        )

        val conservativeSetback = MedicalRoomEngine.calculateSetbackRiskPct(
            rehabMode = RehabMode.CONSERVATIVE,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 5
        )
        val acceleratedSetback = MedicalRoomEngine.calculateSetbackRiskPct(
            rehabMode = RehabMode.ACCELERATED,
            medicalLevel = 2,
            physioRating = 70,
            fitnessCoachBonusPct = 5
        )

        assertTrue(acceleratedEta < conservativeEta)
        assertTrue(topFacilityEta <= acceleratedEta)
        assertTrue(acceleratedSetback > conservativeSetback)
    }

    @Test
    fun `weekly progression decays workload and reflects training intensity on fitness`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        val baseCareer = WorldGenerator.createNewCareer(template, "Trainer", Random(703))
        val targetId = baseCareer.squad.first { !it.isYouthAcademy }.id
        val loadedSquad = baseCareer.squad.map {
            if (it.id == targetId) it.copy(fitness = 75, recentMatchWorkload = 80, trainingFocus = TrainingFocus.POWER_HITTING)
            else it.copy(injury = InjuryState())
        }

        val lightAfter = SeasonProgressionEngine.advanceWeeklyTick(
            baseCareer.copy(squad = loadedSquad, trainingIntensity = TrainingIntensity.LIGHT),
            Random(11)
        )
        val intenseAfter = SeasonProgressionEngine.advanceWeeklyTick(
            baseCareer.copy(squad = loadedSquad, trainingIntensity = TrainingIntensity.INTENSE),
            Random(11)
        )

        val lightPlayer = lightAfter.squad.first { it.id == targetId }
        val intensePlayer = intenseAfter.squad.first { it.id == targetId }

        assertTrue(lightPlayer.recentMatchWorkload < 80)
        assertTrue(lightPlayer.recentMatchWorkload < intensePlayer.recentMatchWorkload)
        assertTrue(lightPlayer.fitness >= intensePlayer.fitness)
    }

    @Test
    fun `medical room component renders rehab ETA, training intensity, and workload probabilities`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val baseCareer = WorldGenerator.createNewCareer(template, "Boss", Random(704))
        val senior = baseCareer.squad.filter { !it.isYouthAcademy }
        val injuredPlayer = senior[0].copy(
            injury = InjuryState(
                isInjured = true,
                injuryName = "Calf Tear",
                weeksRemaining = 3,
                rehabMode = RehabMode.BALANCED
            ),
            fitness = 64
        )
        val strainedPlayer = senior[1].copy(
            injury = InjuryState(isInjured = false),
            fitness = 60,
            recentMatchWorkload = 78,
            trainingFocus = TrainingFocus.POWER_HITTING
        )
        val testCareer = baseCareer.copy(
            squad = listOf(injuredPlayer, strainedPlayer) + baseCareer.squad.drop(2),
            trainingIntensity = TrainingIntensity.MODERATE
        )

        var selectedIntensity: TrainingIntensity? = null
        var selectedRehab: Pair<String, RehabMode>? = null
        var recoveryAssignedTo: String? = null

        composeRule.setContent {
            PitchCraftTheme {
                MedicalRoomSection(
                    career = testCareer,
                    onSetTrainingIntensity = { selectedIntensity = it },
                    onSetRehab = { id, mode -> selectedRehab = id to mode },
                    onRestPlayerRecovery = { recoveryAssignedTo = it },
                    onOpenPlayer = {},
                    onToggleDelegation = {}
                )
            }
        }

        composeRule.onNodeWithTag("medical_room_overview").assertIsDisplayed()
        composeRule.onNodeWithText("Medical Room & Sports Science").assertIsDisplayed()

        // Switch training intensity to Intense
        composeRule.onNodeWithTag("medical_intensity_intense").performClick()
        assertEquals(TrainingIntensity.INTENSE, selectedIntensity)

        // Scroll to injured player's rehab card & switch to Accelerated
        composeRule.onNodeWithTag("medical_room_list")
            .performScrollToNode(hasTestTag("medical_rehab_card_${injuredPlayer.id}"))
        composeRule.onNodeWithTag("medical_rehab_card_${injuredPlayer.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("rehab_mode_${injuredPlayer.id}_accelerated").performClick()
        assertEquals(injuredPlayer.id to RehabMode.ACCELERATED, selectedRehab)

        // Scroll to healthy overloaded player's workload card & tap Recovery Drill
        composeRule.onNodeWithTag("medical_room_list")
            .performScrollToNode(hasTestTag("medical_card_${strainedPlayer.id}"))
        composeRule.onNodeWithTag("medical_card_${strainedPlayer.id}").assertIsDisplayed()
        composeRule.onNodeWithTag("medical_recovery_btn_${strainedPlayer.id}").performClick()
        assertEquals(strainedPlayer.id, recoveryAssignedTo)
    }
}
