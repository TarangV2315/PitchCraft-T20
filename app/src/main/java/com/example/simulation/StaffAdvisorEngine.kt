package com.example.simulation

import com.example.model.*

object StaffAdvisorEngine {

    fun buildPlayerRecommendations(
        player: Player,
        career: CareerState
    ): List<StaffRecommendation> {
        val recs = mutableListOf<StaffRecommendation>()
        val headCoach = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }
        val batCoach = career.staff.firstOrNull { it.role == StaffRole.BATTING_COACH }
        val bowlCoach = career.staff.firstOrNull { it.role == StaffRole.BOWLING_COACH }
        val physio = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }
        val scout = career.staff.firstOrNull { it.role == StaffRole.CHIEF_SCOUT }

        // 1. Physio Rehab Recommendation
        if (player.injury.isInjured && physio != null) {
            val recommendedRehab = when {
                player.overall >= 76 && player.injury.weeksRemaining >= 2 -> RehabMode.CONSERVATIVE
                player.injury.weeksRemaining == 1 -> RehabMode.BALANCED
                else -> RehabMode.BALANCED
            }
            if (player.injury.rehabMode != recommendedRehab) {
                recs.add(
                    StaffRecommendation(
                        id = "rec_rehab_${player.id}",
                        staffRole = StaffRole.PHYSIO,
                        staffName = physio.name,
                        staffRating = physio.rating,
                        title = "Switch to ${recommendedRehab.label}",
                        rationale = "${player.name} has ${player.injury.injuryName} (${player.injury.weeksRemaining}w). ${recommendedRehab.label} keeps setback risk at ${recommendedRehab.setbackRiskPercent}%.",
                        actionType = RecommendationActionType.SET_REHAB_MODE,
                        actionLabel = "Apply Rehab",
                        targetRehab = recommendedRehab,
                        priorityHigh = true
                    )
                )
            }
        } else if (player.fitness < 74 && player.trainingFocus != TrainingFocus.FITNESS_RECOVERY && physio != null) {
            recs.add(
                StaffRecommendation(
                    id = "rec_fit_${player.id}",
                    staffRole = StaffRole.PHYSIO,
                    staffName = physio.name,
                    staffRating = physio.rating,
                    title = "Schedule Recovery Session",
                    rationale = "Match fitness dropped to ${player.fitness}%. Switching to Recovery restores sharpness and prevents soft-tissue injury.",
                    actionType = RecommendationActionType.SET_TRAINING_DRILL,
                    actionLabel = "Set Recovery",
                    targetDrill = TrainingFocus.FITNESS_RECOVERY,
                    priorityHigh = true
                )
            )
        }

        // 2. Youth Promotion Recommendation
        if (player.isYouthAcademy && scout != null && player.overall >= 58) {
            recs.add(
                StaffRecommendation(
                    id = "rec_youth_${player.id}",
                    staffRole = StaffRole.CHIEF_SCOUT,
                    staffName = scout.name,
                    staffRating = scout.rating,
                    title = "Promote Prospect to Senior Squad",
                    rationale = "${player.name} (${player.overall} OVR / ${player.potential} POT) is outgrowing U-19 cricket and ready for senior T20 minutes.",
                    actionType = RecommendationActionType.PROMOTE_YOUTH,
                    actionLabel = "Promote Now",
                    priorityHigh = player.overall >= 63
                )
            )
        }

        // 3. Contract Renewal Recommendation
        if (!player.isYouthAcademy && player.contract.weeksRemaining <= 16 && scout != null) {
            val fairWage = ((player.overall * player.overall) / 2 + 650).coerceAtLeast(player.contract.weeklyWage)
            recs.add(
                StaffRecommendation(
                    id = "rec_contract_${player.id}",
                    staffRole = StaffRole.CHIEF_SCOUT,
                    staffName = scout.name,
                    staffRating = scout.rating,
                    title = "Extend Expiring Contract (${player.contract.weeksRemaining}w left)",
                    rationale = "Avoid losing ${player.name} on a free transfer. Fair benchmark is ~$${fairWage}/wk.",
                    actionType = RecommendationActionType.OFFER_CONTRACT_RENEWAL,
                    actionLabel = "Open Renewal",
                    suggestedWage = fairWage,
                    priorityHigh = true
                )
            )
        }

        // 4. Role-Specific Coaching Drill Recommendation
        if (!player.injury.isInjured && player.fitness >= 74) {
            val recommendedDrill = when (player.role) {
                T20Role.OPENER, T20Role.TOP_ORDER -> {
                    if (player.attributes.battingTiming <= player.attributes.powerHitting) {
                        TrainingFocus.ANCHOR_TECHNIQUE
                    } else {
                        TrainingFocus.POWER_HITTING
                    }
                }
                T20Role.MIDDLE_ORDER, T20Role.FINISHER, T20Role.WICKETKEEPER -> TrainingFocus.POWER_HITTING
                T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER -> TrainingFocus.DEATH_YORKERS
                T20Role.SPINNER -> TrainingFocus.SPIN_VARIATIONS
                T20Role.ALL_ROUNDER -> {
                    if (player.bowlingStyle.isPace) TrainingFocus.DEATH_YORKERS else TrainingFocus.POWER_HITTING
                }
            }
            val coach = when (player.role) {
                T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER, T20Role.SPINNER -> bowlCoach ?: headCoach
                else -> batCoach ?: headCoach
            }
            if (coach != null && player.trainingFocus != recommendedDrill) {
                val statSummary = if (player.seasonStats.innings > 0) {
                    "Current season SR ${"%.1f".format(player.seasonStats.strikeRate)}, Econ ${"%.2f".format(player.seasonStats.economyRate)}."
                } else {
                    "Tailored to ${player.role.label} T20 phase demands."
                }
                recs.add(
                    StaffRecommendation(
                        id = "rec_drill_${player.id}",
                        staffRole = coach.role,
                        staffName = coach.name,
                        staffRating = coach.rating,
                        title = "Assign ${recommendedDrill.label}",
                        rationale = "$statSummary ${recommendedDrill.description}.",
                        actionType = RecommendationActionType.SET_TRAINING_DRILL,
                        actionLabel = "Assign Drill",
                        targetDrill = recommendedDrill
                    )
                )
            }
        }

        // 5. Matchday XI Inclusion or Transfer Listing Recommendation
        if (headCoach != null && !player.isYouthAcademy && !player.injury.isInjured) {
            val inXI = career.selectedXIIds.contains(player.id)
            if (!inXI && player.form >= 72 && player.overall >= 66) {
                recs.add(
                    StaffRecommendation(
                        id = "rec_xi_${player.id}",
                        staffRole = StaffRole.HEAD_COACH,
                        staffName = headCoach.name,
                        staffRating = headCoach.rating,
                        title = "Promote into Matchday XI",
                        rationale = "${player.name} is peaking at ${player.form} form with ${player.fitness}% fitness—worth a starting berth.",
                        actionType = RecommendationActionType.INCLUDE_IN_XI,
                        actionLabel = "Add to XI"
                    )
                )
            } else if (!inXI && player.age >= 32 && player.overall < 66 && !player.isOnTransferList) {
                recs.add(
                    StaffRecommendation(
                        id = "rec_sell_${player.id}",
                        staffRole = StaffRole.HEAD_COACH,
                        staffName = headCoach.name,
                        staffRating = headCoach.rating,
                        title = "Consider Transfer Listing",
                        rationale = "Fringe squad depth at $${player.contract.weeklyWage}/wk. Listing frees wage budget for youth or prime signings.",
                        actionType = RecommendationActionType.TOGGLE_TRANSFER_LIST,
                        actionLabel = "List Player"
                    )
                )
            }
        }

        return recs
    }

    fun recommendPlayingXI(
        squad: List<Player>,
        pitchType: PitchType,
        headCoachRating: Int
    ): Pair<List<String>, String?> {
        val available = squad.filter { it.isAvailableForMatch }
        if (available.size <= 11) {
            return available.map { it.id } to null
        }

        fun scoreForPitch(p: Player): Double {
            val base = p.overall * 1.15 + p.form * 0.3 + p.fitness * 0.25
            val pitchBonus = when (pitchType) {
                PitchType.GREEN_SEAM -> if (p.bowlingStyle.isPace) 8.0 else 0.0
                PitchType.DUSTY_TURNER -> if (!p.bowlingStyle.isPace && p.bowlingStyle.canBowl) 9.0 else 0.0
                PitchType.BATTING_PARADISE -> if (!p.canBowl) 6.0 else 2.0
                PitchType.BALANCED_DECK -> 0.0
            }
            val coachNoise = if (headCoachRating < 65) ((p.id.hashCode() % 5) - 2).toDouble() else 0.0
            return base + pitchBonus + coachNoise
        }

        val picked = mutableListOf<Player>()

        // 1. At least 1 Wicketkeeper
        available.filter { it.role == T20Role.WICKETKEEPER }
            .maxByOrNull(::scoreForPitch)
            ?.let { picked.add(it) }

        // 2. At least 2 Openers / Top Order
        available.filter {
            it !in picked && (it.role == T20Role.OPENER || it.role == T20Role.TOP_ORDER)
        }.sortedByDescending(::scoreForPitch).take(3).forEach { picked.add(it) }

        // 3. At least 4 Bowlers (Pace / Death / Spinner / All-Rounder)
        val bowlersNeeded = (5 - picked.count { it.canBowl }).coerceAtLeast(0)
        available.filter { it !in picked && it.canBowl }
            .sortedByDescending(::scoreForPitch)
            .take(bowlersNeeded)
            .forEach { picked.add(it) }

        // 4. Fill remaining spots up to 11 by highest match score
        available.filter { it !in picked }
            .sortedByDescending(::scoreForPitch)
            .take((11 - picked.size).coerceAtLeast(0))
            .forEach { picked.add(it) }

        // Order batting lineup logically (Openers -> Top -> WK/Middle -> Finishers/All-Rounders -> Bowlers)
        val orderedXI = picked.sortedBy { p ->
            when (p.role) {
                T20Role.OPENER -> 1
                T20Role.TOP_ORDER -> 2
                T20Role.MIDDLE_ORDER -> 3
                T20Role.WICKETKEEPER -> 4
                T20Role.ALL_ROUNDER -> 5
                T20Role.FINISHER -> 6
                T20Role.SPINNER -> 7
                T20Role.PACE_SPEARHEAD -> 8
                T20Role.DEATH_BOWLER -> 9
            }
        }

        val impactSub = available.filter { it !in orderedXI }
            .maxByOrNull(::scoreForPitch)?.id

        return orderedXI.map { it.id } to impactSub
    }

    fun recommendLiveTactics(
        pitchType: PitchType,
        phase: MatchPhase,
        isUserBatting: Boolean,
        wicketsLost: Int,
        requiredRate: Double?,
        headCoachRating: Int
    ): Triple<BattingTempo?, BowlingPlan?, FieldSpread?> {
        return if (isUserBatting) {
            val tempo = when {
                wicketsLost >= 5 && phase != MatchPhase.DEATH -> BattingTempo.ANCHOR
                requiredRate != null && requiredRate >= 10.5 -> BattingTempo.ALL_OUT
                phase == MatchPhase.DEATH && wicketsLost < 6 -> BattingTempo.ALL_OUT
                phase == MatchPhase.POWERPLAY && wicketsLost <= 1 -> BattingTempo.AGGRESSIVE
                else -> BattingTempo.BALANCED
            }
            Triple(tempo, null, null)
        } else {
            val plan = when {
                phase == MatchPhase.DEATH -> BowlingPlan.DEATH_YORKERS
                pitchType == PitchType.DUSTY_TURNER || phase == MatchPhase.MIDDLE -> BowlingPlan.SPIN_TRAP
                pitchType == PitchType.GREEN_SEAM && phase == MatchPhase.POWERPLAY -> BowlingPlan.LINE_AND_LENGTH
                headCoachRating >= 72 -> BowlingPlan.PACE_VARIATIONS
                else -> BowlingPlan.LINE_AND_LENGTH
            }
            val field = when {
                phase == MatchPhase.POWERPLAY -> FieldSpread.ATTACKING
                phase == MatchPhase.DEATH -> FieldSpread.DEFENSIVE
                else -> FieldSpread.BALANCED
            }
            Triple(null, plan, field)
        }
    }
}
