package com.example.simulation

import com.example.model.*
import kotlin.math.roundToInt

enum class InjuryRiskTier(val label: String) {
    LOW("Low Risk"),
    MODERATE("Moderate Risk"),
    HIGH("High Risk"),
    CRITICAL("Critical Risk")
}

data class PlayerMedicalAssessment(
    val player: Player,
    val injuryProbabilityPct: Int,
    val riskTier: InjuryRiskTier,
    val matchWorkloadScore: Int,
    val trainingLoadScore: Int,
    val estimatedRecoveryWeeks: Double,
    val setbackRiskPct: Int,
    val medicalNote: String
)

data class SquadMedicalSummary(
    val injuredCount: Int,
    val highRiskCount: Int,
    val avgWorkloadPct: Int,
    val avgFitnessPct: Int,
    val physioName: String,
    val physioRating: Int,
    val medicalLevel: Int,
    val fitnessCoachBonusPct: Int
)

object MedicalRoomEngine {

    fun assessSquad(career: CareerState): Pair<SquadMedicalSummary, List<PlayerMedicalAssessment>> {
        val senior = career.squad.filter { !it.isYouthAcademy }
        val physio = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }
        val physioName = physio?.name ?: "Head Physio"
        val physioRating = physio?.rating ?: 65
        val fitnessBoostPct = SeasonProgressionEngine.calculateCoachingBoosts(career.staff).fitnessBonusPct

        val assessments = senior.map { player ->
            assessPlayer(
                player = player,
                intensity = career.trainingIntensity,
                isSelectedInXI = career.selectedXIIds.contains(player.id),
                medicalLevel = career.budgets.medicalLevel,
                physioRating = physioRating,
                fitnessCoachBonusPct = fitnessBoostPct
            )
        }.sortedWith(
            compareByDescending<PlayerMedicalAssessment> { it.player.injury.isInjured }
                .thenByDescending { it.injuryProbabilityPct }
                .thenBy { it.player.fitness }
        )

        val activeAssessments = assessments.filter { !it.player.injury.isInjured }
        val summary = SquadMedicalSummary(
            injuredCount = assessments.count { it.player.injury.isInjured },
            highRiskCount = activeAssessments.count {
                it.riskTier == InjuryRiskTier.HIGH || it.riskTier == InjuryRiskTier.CRITICAL
            },
            avgWorkloadPct = activeAssessments.map { it.matchWorkloadScore }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0,
            avgFitnessPct = senior.map { it.fitness }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 90,
            physioName = physioName,
            physioRating = physioRating,
            medicalLevel = career.budgets.medicalLevel,
            fitnessCoachBonusPct = fitnessBoostPct
        )
        return summary to assessments
    }

    fun assessPlayer(
        player: Player,
        intensity: TrainingIntensity,
        isSelectedInXI: Boolean,
        medicalLevel: Int,
        physioRating: Int,
        fitnessCoachBonusPct: Int
    ): PlayerMedicalAssessment {
        val matchWorkload = calculateMatchWorkloadScore(player, isSelectedInXI)
        val trainingLoad = calculateTrainingLoadScore(player, intensity)
        val injuryProb = calculateInjuryProbabilityPct(
            player = player,
            intensity = intensity,
            isSelectedInXI = isSelectedInXI,
            medicalLevel = medicalLevel,
            physioRating = physioRating,
            fitnessCoachBonusPct = fitnessCoachBonusPct
        )
        val riskTier = when {
            injuryProb >= 30 -> InjuryRiskTier.CRITICAL
            injuryProb >= 18 -> InjuryRiskTier.HIGH
            injuryProb >= 9 -> InjuryRiskTier.MODERATE
            else -> InjuryRiskTier.LOW
        }
        val etaWeeks = calculateEstimatedRecoveryWeeks(
            player = player,
            medicalLevel = medicalLevel,
            physioRating = physioRating,
            fitnessCoachBonusPct = fitnessCoachBonusPct
        )
        val setbackRisk = if (player.injury.isInjured) {
            calculateSetbackRiskPct(
                rehabMode = player.injury.rehabMode,
                medicalLevel = medicalLevel,
                physioRating = physioRating,
                fitnessCoachBonusPct = fitnessCoachBonusPct
            )
        } else 0

        val note = when {
            player.injury.isInjured -> {
                val etaStr = "%.1f".format(etaWeeks)
                "${player.injury.injuryName}: Est. return in ~$etaStr wk(s) (${player.injury.rehabMode.label}, $setbackRisk% setback risk)."
            }
            riskTier == InjuryRiskTier.CRITICAL -> {
                "Severe overload (Workload $matchWorkload%, Fit ${player.fitness}%). Switch to Recovery drill or rest from XI."
            }
            riskTier == InjuryRiskTier.HIGH -> {
                "Elevated strain under ${intensity.label} training ($trainingLoad% load). Consider Light intensity or Recovery."
            }
            riskTier == InjuryRiskTier.MODERATE -> {
                "Manageable T20 workload. Monitor stamina after next match."
            }
            else -> {
                "Green light for full T20 match & training participation."
            }
        }

        return PlayerMedicalAssessment(
            player = player,
            injuryProbabilityPct = injuryProb,
            riskTier = riskTier,
            matchWorkloadScore = matchWorkload,
            trainingLoadScore = trainingLoad,
            estimatedRecoveryWeeks = etaWeeks,
            setbackRiskPct = setbackRisk,
            medicalNote = note
        )
    }

    fun calculateMatchWorkloadScore(player: Player, isSelectedInXI: Boolean): Int {
        val seasonMatches = player.seasonStats.matches.coerceAtLeast(1)
        val densityLoad = ((player.seasonStats.ballsBowled * 1.4 + player.seasonStats.ballsFaced * 0.45) / seasonMatches)
            .roundToInt()
        val xiAnticipationBonus = if (isSelectedInXI) 12 else 0
        val roleLoadBonus = if (player.canBowl) 8 else 0
        return (player.recentMatchWorkload + densityLoad + xiAnticipationBonus + roleLoadBonus)
            .coerceIn(5, 100)
    }

    fun calculateTrainingLoadScore(player: Player, intensity: TrainingIntensity): Int {
        if (player.injury.isInjured) return 15
        val raw = 46.0 * intensity.workloadFactor * player.trainingFocus.drillLoadFactor
        return raw.roundToInt().coerceIn(10, 100)
    }

    fun calculateInjuryProbabilityPct(
        player: Player,
        intensity: TrainingIntensity,
        isSelectedInXI: Boolean,
        medicalLevel: Int,
        physioRating: Int,
        fitnessCoachBonusPct: Int
    ): Int {
        if (player.injury.isInjured) return 0

        val matchWorkload = calculateMatchWorkloadScore(player, isSelectedInXI)
        val trainingLoad = calculateTrainingLoadScore(player, intensity)

        val fatigueComponent = ((100 - player.fitness).coerceAtLeast(0)) * 0.42
        val matchComponent = (matchWorkload * 0.16)
        val trainingComponent = (trainingLoad * 0.14)
        val ageComponent = if (player.age >= 31) (player.age - 30) * 1.1 else 0.0

        val medicalMitigation = (medicalLevel * 1.4) + ((physioRating - 60).coerceAtLeast(0) * 0.15) + (fitnessCoachBonusPct * 0.12)
        val recoveryBonus = if (player.trainingFocus == TrainingFocus.FITNESS_RECOVERY) 3.5 else 0.0

        val rawRisk = 1.5 + fatigueComponent + matchComponent + trainingComponent + ageComponent - medicalMitigation - recoveryBonus
        return rawRisk.roundToInt().coerceIn(1, 68)
    }

    fun calculateEstimatedRecoveryWeeks(
        player: Player,
        medicalLevel: Int,
        physioRating: Int,
        fitnessCoachBonusPct: Int
    ): Double {
        if (!player.injury.isInjured) return 0.0
        val baseWeeks = player.injury.weeksRemaining.toDouble().coerceAtLeast(1.0)
        val speedFactor = (player.injury.rehabMode.speedMultiplier +
            (medicalLevel - 2) * 0.08 +
            ((physioRating - 65).coerceAtLeast(0) * 0.006) +
            (fitnessCoachBonusPct * 0.004))
            .coerceIn(0.85, 2.35)
        return (baseWeeks / speedFactor).coerceAtLeast(0.5)
    }

    fun calculateSetbackRiskPct(
        rehabMode: RehabMode,
        medicalLevel: Int,
        physioRating: Int,
        fitnessCoachBonusPct: Int
    ): Int {
        val mitigation = (medicalLevel * 2) + ((physioRating - 60) / 5) + (fitnessCoachBonusPct / 4)
        return (rehabMode.setbackRiskPercent - mitigation).coerceIn(1, 45)
    }

    fun computePostMatchWorkload(
        previousWorkload: Int,
        ballsFaced: Int,
        ballsBowled: Int
    ): Int {
        val matchSpike = 18 + (ballsFaced / 3) + (ballsBowled * 2)
        return ((previousWorkload * 0.65).roundToInt() + matchSpike).coerceIn(15, 100)
    }
}
