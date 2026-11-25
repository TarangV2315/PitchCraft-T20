package com.example.simulation

import com.example.model.*
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

data class CoachingBoostSummary(
    val battingBonusPct: Int,
    val bowlingBonusPct: Int,
    val fitnessBonusPct: Int,
    val battingCoaches: Int,
    val bowlingCoaches: Int,
    val fitnessCoaches: Int
)

object SeasonProgressionEngine {

    fun calculateCoachingBoosts(staff: List<StaffMember>): CoachingBoostSummary {
        var batPct = 0
        var bowlPct = 0
        var fitPct = 0
        var batCount = 0
        var bowlCount = 0
        var fitCount = 0

        staff.filter { it.isCoach }.forEach { coach ->
            val area = coach.effectiveTrainingArea ?: return@forEach
            val base = ((coach.rating - 40).coerceAtLeast(10)) / 3
            val synergy = if (coach.role == StaffRole.HEAD_COACH || coach.role.defaultTrainingArea == area) 4 else 0
            val total = base + synergy
            when (area) {
                CoachTrainingArea.BATTING -> {
                    batPct += total
                    batCount++
                }
                CoachTrainingArea.BOWLING -> {
                    bowlPct += total
                    bowlCount++
                }
                CoachTrainingArea.FITNESS -> {
                    fitPct += total
                    fitCount++
                }
            }
        }
        return CoachingBoostSummary(
            battingBonusPct = batPct,
            bowlingBonusPct = bowlPct,
            fitnessBonusPct = fitPct,
            battingCoaches = batCount,
            bowlingCoaches = bowlCount,
            fitnessCoaches = fitCount
        )
    }

    fun commitUserMatchAndSimulateRound(
        career: CareerState,
        fixture: Fixture,
        record: MatchRecord,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        val userBattingCard = if (record.firstInnings.battingClubName == career.clubName) {
            record.firstInnings
        } else {
            record.secondInnings
        }
        val userBowlingCard = if (record.firstInnings.bowlingClubName == career.clubName) {
            record.firstInnings
        } else {
            record.secondInnings
        }

        val coachingBoosts = calculateCoachingBoosts(career.staff)
        val physioRating = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.rating ?: 65
        val physioName = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name ?: "Head Physio"
        val postMatchInbox = mutableListOf<InboxMessage>()

        val updatedSquad = career.squad.map { player ->
            val bat = userBattingCard.batters.firstOrNull { it.playerId == player.id }
            val bowl = userBowlingCard.bowlers.firstOrNull { it.playerId == player.id }
            if (bat == null && bowl == null) {
                player
            } else {
                val updatedSeason = mergeStats(player.seasonStats, bat, bowl)
                val updatedCareer = mergeStats(player.careerStats, bat, bowl)
                val perfDelta = ((bat?.runs ?: 0) / 15) + ((bowl?.wickets ?: 0) * 3) - 1
                val newForm = (player.form + perfDelta).coerceIn(38, 99)
                val newFitness = (player.fitness - random.nextInt(5, 11)).coerceIn(45, 100)
                val newMorale = (player.morale + if (record.userWon) 3 else -2).coerceIn(35, 99)
                val newWorkload = MedicalRoomEngine.computePostMatchWorkload(
                    previousWorkload = player.recentMatchWorkload,
                    ballsFaced = bat?.balls ?: 0,
                    ballsBowled = bowl?.ballsBowled ?: 0
                )

                val candidatePlayer = player.copy(
                    form = newForm,
                    fitness = newFitness,
                    morale = newMorale,
                    recentMatchWorkload = newWorkload,
                    seasonStats = updatedSeason,
                    careerStats = updatedCareer
                )
                val matchInjuryProbPct = MedicalRoomEngine.calculateInjuryProbabilityPct(
                    player = candidatePlayer,
                    intensity = career.trainingIntensity,
                    isSelectedInXI = true,
                    medicalLevel = career.budgets.medicalLevel,
                    physioRating = physioRating,
                    fitnessCoachBonusPct = coachingBoosts.fitnessBonusPct
                )
                val injuryRoll = if (!player.injury.isInjured && (newFitness < 78 || newWorkload >= 55) && random.nextInt(100) < matchInjuryProbPct) {
                    val injuryName = listOf("Hamstring Tightness", "Shoulder Strain", "Ankle Twist", "Side Strain", "Calf Overload").random(random)
                    val weeksOut = if (matchInjuryProbPct >= 30 || newWorkload >= 75) random.nextInt(2, 4) else random.nextInt(1, 3)
                    postMatchInbox.add(
                        InboxMessage(
                            id = "msg_inj_match_${player.id}_${career.week}",
                            season = career.season,
                            week = career.week,
                            senderRole = StaffRole.PHYSIO,
                            senderName = physioName,
                            title = "Matchday Injury: ${player.name}",
                            body = "${player.name} suffered $injuryName ($weeksOut wk${if (weeksOut > 1) "s" else ""}) after $newWorkload% match workload ($matchInjuryProbPct% strain risk).",
                            relatedPlayerId = player.id,
                            isUrgent = true
                        )
                    )
                    InjuryState(
                        isInjured = true,
                        injuryName = injuryName,
                        weeksRemaining = weeksOut,
                        rehabMode = RehabMode.BALANCED
                    )
                } else player.injury

                val summary = buildString {
                    if (bat != null && bat.balls > 0) append("${bat.runs}(${bat.balls})")
                    if (bowl != null && bowl.ballsBowled > 0) {
                        if (isNotEmpty()) append(" • ")
                        append("${bowl.wickets}/${bowl.runsConceded}")
                    }
                    if (isEmpty()) append("Played in XI")
                }

                candidatePlayer.copy(
                    injury = injuryRoll,
                    lastMatchNote = summary
                )
            }
        }

        // Update standings for user match (league stage only)
        var updatedDivisions = career.divisions
        if (!fixture.isPlayoff) {
            val oppClubId = if (fixture.homeClubId == career.clubId) fixture.awayClubId else fixture.homeClubId
            val userScore = userBattingCard
            val oppScore = userBowlingCard
            updatedDivisions = applyMatchToStandings(
                divisions = updatedDivisions,
                tier = fixture.tier,
                clubAId = career.clubId,
                runsA = userScore.runs,
                ballsA = if (userScore.wickets >= 10) 120 else userScore.balls.coerceAtLeast(1),
                clubBId = oppClubId,
                runsB = oppScore.runs,
                ballsB = if (oppScore.wickets >= 10) 120 else oppScore.balls.coerceAtLeast(1)
            )
        }

        // Simulate all other fixtures of the same round across all divisions
        val roundFixtures = career.fixtures.filter { it.round == fixture.round && it.id != fixture.id && !it.isCompleted }
        val completedSimMap = mutableMapOf<String, Fixture>()
        val aiRoundResults = mutableListOf<Triple<Fixture, Pair<Int, Int>, Pair<Int, Int>>>()
        for (fx in roundFixtures) {
            val res = simulateAiFixture(fx, updatedDivisions, random)
            completedSimMap[fx.id] = res.first
            aiRoundResults.add(res)
            if (!fx.isPlayoff) {
                updatedDivisions = applyMatchToStandings(
                    divisions = updatedDivisions,
                    tier = fx.tier,
                    clubAId = fx.homeClubId,
                    runsA = res.second.first,
                    ballsA = res.second.second,
                    clubBId = fx.awayClubId,
                    runsB = res.third.first,
                    ballsB = res.third.second
                )
            }
        }

        val updatedTournamentStats = updateTournamentStatsAfterRound(
            existingStats = career.tournamentStats,
            userFixture = fixture,
            userClubId = career.clubId,
            oppBatters = userBowlingCard.batters,
            oppBowlers = userBattingCard.bowlers,
            aiRoundResults = aiRoundResults,
            random = random
        )

        val updatedFixtures = career.fixtures.map { fx ->
            when {
                fx.id == fixture.id -> fx.copy(isCompleted = true, resultSummary = record.resultText)
                completedSimMap.containsKey(fx.id) -> completedSimMap.getValue(fx.id)
                else -> fx
            }
        }

        val newBank = career.bankBalance + record.revenueEarned
        val matchLedgerEntries = buildList {
            if (record.ticketSalesRevenue > 0) {
                add(
                    FinanceLedgerEntry(
                        id = "ld_tk_${UUID.randomUUID().toString().take(8)}",
                        season = career.season,
                        week = career.week,
                        category = "Ticket Sales",
                        description = "Home gate (${record.attendance} fans) vs ${record.opponentName}",
                        amount = record.ticketSalesRevenue
                    )
                )
            }
            if (record.sponsorBonusEarned > 0) {
                val desc = buildString {
                    append("Sponsor bonus vs ${record.opponentName}")
                    if (record.winSponsorBonus > 0 && record.individualMilestoneBonus > 0) {
                        append(" (Win + Player Milestones)")
                    } else if (record.individualMilestoneBonus > 0) {
                        append(" (50s / 100s / 3W Milestones)")
                    } else {
                        append(" (Win Bonus)")
                    }
                }
                add(
                    FinanceLedgerEntry(
                        id = "ld_wb_${UUID.randomUUID().toString().take(8)}",
                        season = career.season,
                        week = career.week,
                        category = "Sponsorship",
                        description = desc,
                        amount = record.sponsorBonusEarned
                    )
                )
            }
            if (isEmpty() && record.revenueEarned > 0) {
                add(
                    FinanceLedgerEntry(
                        id = "ld_${UUID.randomUUID().toString().take(8)}",
                        season = career.season,
                        week = career.week,
                        category = "Matchday",
                        description = "Gate & win bonus vs ${record.opponentName}",
                        amount = record.revenueEarned
                    )
                )
            }
        }
        val newLedger = (matchLedgerEntries + career.ledger).take(40)

        val newConfidence = (career.boardConfidence + if (record.userWon) 4 else -3).coerceIn(15, 100)
        val newRep = (career.managerReputation + if (record.userWon) 2 else 0).coerceIn(10, 100)

        var nextState = career.copy(
            squad = updatedSquad,
            divisions = updatedDivisions,
            tournamentStats = updatedTournamentStats,
            fixtures = updatedFixtures,
            matchHistory = (listOf(record) + career.matchHistory).take(30),
            bankBalance = newBank,
            boardConfidence = newConfidence,
            managerReputation = newRep,
            ledger = newLedger,
            inbox = (postMatchInbox + career.inbox).take(25),
            seasonTicketIncome = career.seasonTicketIncome + record.ticketSalesRevenue,
            seasonSponsorIncome = career.seasonSponsorIncome + record.sponsorBonusEarned,
            homeMatchesHosted = career.homeMatchesHosted + if (record.ticketSalesRevenue > 0) 1 else 0
        )

        // Check playoff progression or end of regular season
        nextState = evaluatePlayoffScheduling(nextState, random)
        return advanceWeeklyTick(nextState, random)
    }

    fun applyPostMatchTeamTalk(
        career: CareerState,
        record: MatchRecord,
        tone: TeamTalkTone
    ): CareerState {
        val won = record.userWon
        val updatedSquad = career.squad.map { player ->
            if (player.isYouthAcademy) {
                player
            } else {
                val moraleShift = when {
                    !won && tone == TeamTalkTone.AGGRESSIVE -> tone.moraleDelta - 2
                    !won && tone == TeamTalkTone.ENCOURAGING -> tone.moraleDelta + 1
                    else -> tone.moraleDelta
                }
                val formShift = if (!player.injury.isInjured) tone.formDelta else 0
                player.copy(
                    morale = (player.morale + moraleShift).coerceIn(35, 99),
                    form = (player.form + formShift).coerceIn(38, 99)
                )
            }
        }
        val msg = InboxMessage(
            id = "msg_talk_${record.matchId}_${tone.name.lowercase()}",
            season = career.season,
            week = career.week,
            senderRole = StaffRole.HEAD_COACH,
            senderName = career.managerName,
            title = "Post-Match Debrief (${tone.label})",
            body = "${tone.speechLine(TeamTalkStage.POST_MATCH, won)} (${tone.summary})",
            isUrgent = false
        )
        return career.copy(
            squad = updatedSquad,
            inbox = (listOf(msg) + career.inbox.filterNot { it.id.startsWith("msg_talk_${record.matchId}_") }).take(25)
        )
    }

    fun advanceWeeklyTick(
        career: CareerState,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        // 1. Weekly Finances
        val sponsorIncome = career.weeklySponsorIncome
        val playerWagesOut = career.weeklyPlayerWages
        val staffWagesOut = career.weeklyStaffWages
        val wagesOut = playerWagesOut + staffWagesOut
        val facilityUpkeep = career.budgets.weeklyUpkeep
        val netCash = sponsorIncome - wagesOut - facilityUpkeep
        val newBank = career.bankBalance + netCash

        val newLedgerEntries = mutableListOf<FinanceLedgerEntry>()
        if (sponsorIncome > 0) {
            newLedgerEntries.add(
                FinanceLedgerEntry("ld_sp_${career.season}_${career.week}", career.season, career.week, "Sponsorship", "Weekly sponsorship retainers", sponsorIncome)
            )
        }
        if (playerWagesOut > 0) {
            newLedgerEntries.add(
                FinanceLedgerEntry("ld_pw_${career.season}_${career.week}", career.season, career.week, "Player Wages", "Senior squad weekly payroll", -playerWagesOut)
            )
        }
        if (staffWagesOut > 0) {
            newLedgerEntries.add(
                FinanceLedgerEntry("ld_sw_${career.season}_${career.week}", career.season, career.week, "Staff Wages", "Management staff salaries", -staffWagesOut)
            )
        }
        newLedgerEntries.add(
            FinanceLedgerEntry("ld_op_${career.season}_${career.week}", career.season, career.week, "Operations", "Academy, scouting & medical facilities", -facilityUpkeep)
        )

        // 2. Staff bonuses & coach focus progression multipliers
        val headCoachRating = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
        val physioRating = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.rating ?: 65
        val batCoachRating = career.staff.firstOrNull { it.role == StaffRole.BATTING_COACH }?.rating ?: 65
        val bowlCoachRating = career.staff.firstOrNull { it.role == StaffRole.BOWLING_COACH }?.rating ?: 65
        val coachingBoosts = calculateCoachingBoosts(career.staff)

        val newInbox = mutableListOf<InboxMessage>()

        // 3. Squad Training, Rehab & Contract Week Tick
        val progressedSquad = career.squad.map { player ->
            val updatedContract = player.contract.copy(weeksRemaining = (player.contract.weeksRemaining - 1).coerceAtLeast(1))

            if (player.injury.isInjured) {
                val effectiveRehab = if (career.delegation.rehabDelegated) RehabMode.BALANCED else player.injury.rehabMode
                val setbackChance = MedicalRoomEngine.calculateSetbackRiskPct(
                    rehabMode = effectiveRehab,
                    medicalLevel = career.budgets.medicalLevel,
                    physioRating = physioRating,
                    fitnessCoachBonusPct = coachingBoosts.fitnessBonusPct
                )
                val setback = random.nextInt(100) < setbackChance
                if (setback) {
                    newInbox.add(
                        InboxMessage(
                            id = "msg_setback_${player.id}_${career.week}",
                            season = career.season,
                            week = career.week,
                            senderRole = StaffRole.PHYSIO,
                            senderName = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name ?: "Head Physio",
                            title = "Rehab Setback: ${player.name}",
                            body = "${player.name} suffered a minor flare-up during ${effectiveRehab.label}. Added 1 week recovery.",
                            relatedPlayerId = player.id,
                            isUrgent = true
                        )
                    )
                    return@map player.copy(
                        contract = updatedContract,
                        recentMatchWorkload = (player.recentMatchWorkload * 0.50).roundToInt(),
                        injury = player.injury.copy(weeksRemaining = player.injury.weeksRemaining + 1)
                    )
                }
                val step = if (random.nextFloat() < (effectiveRehab.speedMultiplier - 0.8f + coachingBoosts.fitnessBonusPct / 150f + (career.budgets.medicalLevel - 2) * 0.08f)) 2 else 1
                val weeksLeft = (player.injury.weeksRemaining - step).coerceAtLeast(0)
                return@map if (weeksLeft == 0) {
                    player.copy(
                        fitness = (player.fitness + 14 + coachingBoosts.fitnessBonusPct / 4).coerceIn(72, 98),
                        contract = updatedContract,
                        recentMatchWorkload = 0,
                        injury = InjuryState(isInjured = false, injuryName = "", weeksRemaining = 0, rehabMode = RehabMode.NONE),
                        lastMatchNote = "Cleared for T20 selection"
                    )
                } else {
                    player.copy(
                        fitness = (player.fitness + 5 + coachingBoosts.fitnessBonusPct / 5).coerceAtMost(88),
                        contract = updatedContract,
                        recentMatchWorkload = (player.recentMatchWorkload * 0.50).roundToInt(),
                        injury = player.injury.copy(weeksRemaining = weeksLeft)
                    )
                }
            }

            // Active Training
            val activeDrill = if (career.delegation.trainingDelegated) {
                when {
                    player.fitness < 74 || player.recentMatchWorkload >= 70 -> TrainingFocus.FITNESS_RECOVERY
                    player.role == T20Role.SPINNER -> TrainingFocus.SPIN_VARIATIONS
                    player.role == T20Role.PACE_SPEARHEAD || player.role == T20Role.DEATH_BOWLER -> TrainingFocus.DEATH_YORKERS
                    else -> TrainingFocus.POWER_HITTING
                }
            } else player.trainingFocus

            val workloadDecay = if (activeDrill == TrainingFocus.FITNESS_RECOVERY || career.trainingIntensity == TrainingIntensity.LIGHT) 0.50 else 0.68
            val decayedWorkload = (player.recentMatchWorkload * workloadDecay).roundToInt().coerceAtLeast(0)

            val fitnessCoachBonus = coachingBoosts.fitnessBonusPct / 3
            val fitnessGain = if (activeDrill == TrainingFocus.FITNESS_RECOVERY) {
                15 + fitnessCoachBonus
            } else {
                6 + career.budgets.medicalLevel + (fitnessCoachBonus / 2)
            }
            val newFitness = (player.fitness + fitnessGain + career.trainingIntensity.fitnessDelta).coerceIn(45, 100)

            val focusBonus = when (activeDrill) {
                TrainingFocus.POWER_HITTING, TrainingFocus.ANCHOR_TECHNIQUE, TrainingFocus.BALANCED ->
                    coachingBoosts.battingBonusPct / 100f
                TrainingFocus.DEATH_YORKERS, TrainingFocus.SPIN_VARIATIONS ->
                    coachingBoosts.bowlingBonusPct / 100f
                TrainingFocus.FIELDING_AGILITY, TrainingFocus.FITNESS_RECOVERY ->
                    coachingBoosts.fitnessBonusPct / 100f
            }

            val baseGrowthChance = when {
                player.age <= 21 -> 0.40f + (career.budgets.academyLevel * 0.05f)
                player.age <= 26 -> 0.22f + ((batCoachRating + bowlCoachRating) / 700f)
                else -> 0.08f
            }
            val growthChance = ((baseGrowthChance + focusBonus) * career.trainingIntensity.growthMultiplier).coerceAtMost(0.95f)
            val attrStep = if (focusBonus >= 0.22f && random.nextFloat() < 0.35f) 2 else 1
            var attrs = player.attributes
            var ovr = player.overall
            if (ovr < player.potential && random.nextFloat() < growthChance) {
                attrs = when (activeDrill) {
                    TrainingFocus.POWER_HITTING -> attrs.copy(powerHitting = (attrs.powerHitting + attrStep).coerceAtMost(99))
                    TrainingFocus.ANCHOR_TECHNIQUE -> attrs.copy(battingTiming = (attrs.battingTiming + attrStep).coerceAtMost(99))
                    TrainingFocus.DEATH_YORKERS -> attrs.copy(
                        deathExecution = (attrs.deathExecution + attrStep).coerceAtMost(99),
                        paceBowling = (attrs.paceBowling + attrStep).coerceAtMost(99)
                    )
                    TrainingFocus.SPIN_VARIATIONS -> attrs.copy(spinVariation = (attrs.spinVariation + attrStep).coerceAtMost(99))
                    TrainingFocus.FIELDING_AGILITY -> attrs.copy(fielding = (attrs.fielding + attrStep).coerceAtMost(99))
                    TrainingFocus.BALANCED -> attrs.copy(
                        battingTiming = (attrs.battingTiming + attrStep).coerceAtMost(99),
                        fielding = (attrs.fielding + 1).coerceAtMost(99)
                    )
                    TrainingFocus.FITNESS_RECOVERY -> attrs.copy(fielding = (attrs.fielding + 1).coerceAtMost(99))
                }
                val ovrBoostChance = (0.45f + focusBonus * 0.6f).coerceAtMost(0.85f)
                if (activeDrill != TrainingFocus.FITNESS_RECOVERY && random.nextFloat() < ovrBoostChance) {
                    ovr = (ovr + 1).coerceAtMost(player.potential)
                }
            }

            val updatedPlayer = player.copy(
                attributes = attrs,
                overall = ovr,
                fitness = newFitness,
                trainingFocus = activeDrill,
                contract = updatedContract,
                recentMatchWorkload = decayedWorkload
            )

            val trainingInjuryProbPct = MedicalRoomEngine.calculateInjuryProbabilityPct(
                player = updatedPlayer,
                intensity = career.trainingIntensity,
                isSelectedInXI = career.selectedXIIds.contains(player.id),
                medicalLevel = career.budgets.medicalLevel,
                physioRating = physioRating,
                fitnessCoachBonusPct = coachingBoosts.fitnessBonusPct
            )
            val weeklyTrainRollThreshold = (trainingInjuryProbPct * 0.28).roundToInt().coerceAtLeast(0)
            val trainingInjury = if (!player.isYouthAcademy &&
                (career.trainingIntensity == TrainingIntensity.HIGH ||
                    career.trainingIntensity == TrainingIntensity.INTENSE ||
                    newFitness < 70 ||
                    decayedWorkload >= 60) &&
                random.nextInt(100) < weeklyTrainRollThreshold
            ) {
                val injuryName = listOf("Groin Strain", "Back Stiffness", "Quadriceps Pull", "Rotator Cuff Fatigue").random(random)
                val weeksOut = if (career.trainingIntensity == TrainingIntensity.INTENSE && random.nextBoolean()) 2 else 1
                newInbox.add(
                    InboxMessage(
                        id = "msg_inj_train_${player.id}_${career.week}",
                        season = career.season,
                        week = career.week,
                        senderRole = StaffRole.PHYSIO,
                        senderName = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.name ?: "Head Physio",
                        title = "Training Injury: ${player.name}",
                        body = "${player.name} sustained $injuryName during ${career.trainingIntensity.label} training ($trainingInjuryProbPct% risk). Out ~$weeksOut wk${if (weeksOut > 1) "s" else ""}.",
                        relatedPlayerId = player.id,
                        isUrgent = true
                    )
                )
                InjuryState(
                    isInjured = true,
                    injuryName = injuryName,
                    weeksRemaining = weeksOut,
                    rehabMode = RehabMode.BALANCED
                )
            } else updatedPlayer.injury

            updatedPlayer.copy(injury = trainingInjury)
        }

        // 4. Delegated Transfers / Expiring Contract Auto-Renewal
        val finalSquad = if (career.delegation.transfersDelegated) {
            progressedSquad.map { p ->
                if (!p.isYouthAcademy && p.contract.weeksRemaining <= 6 && p.overall >= 68 && newBank > 150000) {
                    newInbox.add(
                        InboxMessage(
                            id = "msg_auto_ren_${p.id}_${career.week}",
                            season = career.season,
                            week = career.week,
                            senderRole = StaffRole.CHIEF_SCOUT,
                            senderName = career.staff.firstOrNull { it.role == StaffRole.CHIEF_SCOUT }?.name ?: "Chief Scout",
                            title = "Delegated Renewal: ${p.name}",
                            body = "Extended ${p.name} by 52 weeks at $${p.contract.weeklyWage}/wk under your delegated transfer mandate.",
                            relatedPlayerId = p.id
                        )
                    )
                    p.copy(contract = p.contract.copy(weeksRemaining = 52))
                } else p
            }
        } else progressedSquad

        // 5. Auto-refresh XI if Selection is delegated
        val nextFixture = career.fixtures.firstOrNull { !it.isCompleted && (it.homeClubId == career.clubId || it.awayClubId == career.clubId) }
        val (newXI, newSub) = if (career.delegation.selectionDelegated) {
            StaffAdvisorEngine.recommendPlayingXI(
                squad = finalSquad,
                pitchType = nextFixture?.pitchType ?: PitchType.BALANCED_DECK,
                headCoachRating = headCoachRating
            )
        } else {
            // Filter out injured players from current XI automatically or keep user picks
            career.selectedXIIds.filter { id -> finalSquad.any { it.id == id && it.isAvailableForMatch } } to career.impactSubId
        }

        val nextWeek = career.week + 1
        val chiefScoutName = career.staff.firstOrNull { it.role == StaffRole.CHIEF_SCOUT }?.name ?: "Chief Scout"
        val updatedMarket = if (nextWeek == career.midSeasonTransferWeek && !career.seasonComplete) {
            val keptShortlist = career.transferMarket.filter { it.isShortlisted }
            val freshMarket = WorldGenerator.generateTransferMarket(career.divisionTier, 26, random, career.country)
            (keptShortlist + freshMarket.filter { fresh -> keptShortlist.none { it.name == fresh.name } }).take(26)
        } else {
            career.transferMarket
        }

        if (!career.seasonComplete) {
            when {
                nextWeek == career.midSeasonTransferWeek -> {
                    newInbox.add(
                        InboxMessage(
                            id = "msg_tw_mid_${career.season}_$nextWeek",
                            season = career.season,
                            week = nextWeek,
                            senderRole = StaffRole.CHIEF_SCOUT,
                            senderName = chiefScoutName,
                            title = "Mid-Season Transfer Window Open (1 Week)",
                            body = "The 1-week mid-season window is active during Week $nextWeek. Visit Transfers to bid on refreshed targets before the next match.",
                            isUrgent = true
                        )
                    )
                }
                career.isTransferWindowOpen && nextWeek != 1 && nextWeek != career.midSeasonTransferWeek -> {
                    newInbox.add(
                        InboxMessage(
                            id = "msg_tw_closed_${career.season}_$nextWeek",
                            season = career.season,
                            week = nextWeek,
                            senderRole = StaffRole.CHIEF_SCOUT,
                            senderName = chiefScoutName,
                            title = "Transfer Window Closed",
                            body = "The 1-week transfer window has shut. The Transfers tab is locked until the next scheduled window.",
                            isUrgent = false
                        )
                    )
                }
            }
            if (career.week == 1 && nextWeek == 2) {
                val commDir = career.staff.firstOrNull { it.role == StaffRole.COMMERCIAL_DIRECTOR }?.name ?: "Commercial Director"
                newInbox.add(
                    InboxMessage(
                        id = "msg_sp_frozen_${career.season}",
                        season = career.season,
                        week = nextWeek,
                        senderRole = StaffRole.COMMERCIAL_DIRECTOR,
                        senderName = commDir,
                        title = "Sponsorship Portfolio Frozen",
                        body = "Pre-season sponsorship negotiations are now locked for Season ${career.season}. Active contracts & milestone payouts remain active until Season ${career.season + 1}.",
                        isUrgent = false
                    )
                )
            }
        }

        return career.copy(
            week = nextWeek,
            isPreSeason = false,
            bankBalance = newBank,
            lastWeekNetCashflow = netCash,
            squad = finalSquad,
            transferMarket = updatedMarket,
            selectedXIIds = newXI,
            impactSubId = newSub,
            ledger = (newLedgerEntries + career.ledger).take(40),
            inbox = (newInbox + career.inbox).take(25),
            seasonSponsorIncome = career.seasonSponsorIncome + sponsorIncome,
            seasonPlayerWagesPaid = career.seasonPlayerWagesPaid + playerWagesOut,
            seasonStaffWagesPaid = career.seasonStaffWagesPaid + staffWagesOut,
            seasonUpkeepPaid = career.seasonUpkeepPaid + facilityUpkeep
        )
    }

    private fun evaluatePlayoffScheduling(career: CareerState, random: Random): CareerState {
        val tierFixtures = career.fixtures.filter { it.tier == career.divisionTier }
        val leagueAllDone = tierFixtures.filter { !it.isPlayoff }.all { it.isCompleted }
        if (!leagueAllDone) return career

        val existingPlayoffs = tierFixtures.filter { it.isPlayoff }
        val sortedTable = sortDivision(career.divisions.first { it.tier == career.divisionTier }.clubs)

        // Schedule Semi-Finals (Qualifier 1 & Eliminator)
        if (existingPlayoffs.isEmpty() && sortedTable.size >= 4) {
            val c1 = sortedTable[0]
            val c2 = sortedTable[1]
            val c3 = sortedTable[2]
            val c4 = sortedTable[3]
            val q1 = Fixture("po_q1_${career.season}", 15, career.divisionTier, c1.clubId, c1.name, c2.clubId, c2.name, PitchType.BALANCED_DECK, true, "Qualifier 1")
            val el = Fixture("po_el_${career.season}", 15, career.divisionTier, c3.clubId, c3.name, c4.clubId, c4.name, PitchType.BATTING_PARADISE, true, "Eliminator")

            val userInPlayoffs = listOf(c1.clubId, c2.clubId, c3.clubId, c4.clubId).contains(career.clubId)
            val msg = InboxMessage(
                id = "msg_po_${career.season}",
                season = career.season,
                week = career.week,
                senderRole = StaffRole.HEAD_COACH,
                senderName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name ?: "Head Coach",
                title = if (userInPlayoffs) "Playoffs Qualified!" else "League Stage Concluded",
                body = if (userInPlayoffs) "We finished inside the Top 4! Prepare the squad for knockout T20 cricket." else "We missed the Top 4 playoffs this year. Finish out the season calendar to kick off off-season planning.",
                isUrgent = userInPlayoffs
            )
            return if (userInPlayoffs) {
                career.copy(fixtures = career.fixtures + listOf(q1, el), inbox = (listOf(msg) + career.inbox).take(25))
            } else {
                // Simulate playoffs for AI (storing completed Grand Final) and mark season complete
                val (finalBanner, completedFinals) = simulateAiPlayoffsWinner(career, q1, el, random)
                career.copy(
                    fixtures = career.fixtures + completedFinals,
                    seasonComplete = true,
                    seasonSummaryBanner = finalBanner,
                    inbox = (listOf(msg) + career.inbox).take(25)
                )
            }
        }

        // Check Qualifier 2 / Final progression if user is in playoffs
        val q1 = existingPlayoffs.firstOrNull { it.playoffLabel == "Qualifier 1" }
        val el = existingPlayoffs.firstOrNull { it.playoffLabel == "Eliminator" }
        val q2 = existingPlayoffs.firstOrNull { it.playoffLabel == "Qualifier 2" }
        val finalFx = existingPlayoffs.firstOrNull { it.playoffLabel == "Grand Final" }

        if (q1 != null && el != null && q1.isCompleted && el.isCompleted && q2 == null) {
            val q1Winner = winnerIdOf(q1)
            val q1Loser = loserIdOf(q1)
            val elWinner = winnerIdOf(el)
            val clubs = career.divisions.first { it.tier == career.divisionTier }.clubs
            val loserClub = clubs.first { it.clubId == q1Loser }
            val elClub = clubs.first { it.clubId == elWinner }
            val newQ2 = Fixture("po_q2_${career.season}", 16, career.divisionTier, loserClub.clubId, loserClub.name, elClub.clubId, elClub.name, PitchType.GREEN_SEAM, true, "Qualifier 2")

            // If user already won Q1, wait for Q2 to finish or auto-sim Q2 immediately
            return if (q1Winner == career.clubId) {
                val simQ2 = simulateAiFixture(newQ2, career.divisions, random).first
                val q2WinClub = clubs.first { it.clubId == winnerIdOf(simQ2) }
                val userClub = clubs.first { it.clubId == career.clubId }
                val newFinal = Fixture("po_fin_${career.season}", 17, career.divisionTier, userClub.clubId, userClub.name, q2WinClub.clubId, q2WinClub.name, PitchType.BATTING_PARADISE, true, "Grand Final")
                career.copy(fixtures = career.fixtures + listOf(simQ2, newFinal))
            } else if (q1Loser == career.clubId || elWinner == career.clubId) {
                career.copy(fixtures = career.fixtures + newQ2)
            } else {
                // User eliminated in Eliminator — finish AI Q2 & Grand Final so league winner is known
                val simQ2 = simulateAiFixture(newQ2, career.divisions, random).first
                val cA = clubs.first { it.clubId == q1Winner }
                val cB = clubs.first { it.clubId == winnerIdOf(simQ2) }
                val newFinal = Fixture("po_fin_${career.season}", 17, career.divisionTier, cA.clubId, cA.name, cB.clubId, cB.name, PitchType.BATTING_PARADISE, true, "Grand Final")
                val simFinal = simulateAiFixture(newFinal, career.divisions, random).first
                career.copy(
                    fixtures = career.fixtures + listOf(simQ2, simFinal),
                    seasonComplete = true,
                    seasonSummaryBanner = "Eliminated in the Playoffs — ${clubs.first { it.clubId == winnerIdOf(simFinal) }.name} won the championship."
                )
            }
        }

        if (q2 != null && q2.isCompleted && finalFx == null && q1 != null) {
            val q1Winner = winnerIdOf(q1)
            val q2Winner = winnerIdOf(q2)
            val clubs = career.divisions.first { it.tier == career.divisionTier }.clubs
            val cA = clubs.first { it.clubId == q1Winner }
            val cB = clubs.first { it.clubId == q2Winner }
            val newFinal = Fixture("po_fin_${career.season}", 17, career.divisionTier, cA.clubId, cA.name, cB.clubId, cB.name, PitchType.BATTING_PARADISE, true, "Grand Final")
            return if (q1Winner == career.clubId || q2Winner == career.clubId) {
                career.copy(fixtures = career.fixtures + newFinal)
            } else {
                val simFinal = simulateAiFixture(newFinal, career.divisions, random).first
                career.copy(
                    fixtures = career.fixtures + simFinal,
                    seasonComplete = true,
                    seasonSummaryBanner = "Playoff run ended in Qualifier 2 — ${clubs.first { it.clubId == winnerIdOf(simFinal) }.name} won the championship."
                )
            }
        }

        if (finalFx != null && finalFx.isCompleted) {
            val championId = winnerIdOf(finalFx)
            val wonTitle = championId == career.clubId
            val trophyBonus = if (wonTitle) career.sponsors.filter { it.isActive }.sumOf { it.trophyBonus } else 0
            val trophyLedger = if (trophyBonus > 0) {
                (listOf(
                    FinanceLedgerEntry(
                        id = "ld_trp_${career.season}",
                        season = career.season,
                        week = career.week,
                        category = "Sponsorship",
                        description = "Championship title sponsor bonus",
                        amount = trophyBonus
                    )
                ) + career.ledger).take(40)
            } else career.ledger
            return career.copy(
                seasonComplete = true,
                trophiesWon = career.trophiesWon + if (wonTitle) 1 else 0,
                bankBalance = career.bankBalance + trophyBonus,
                seasonSponsorIncome = career.seasonSponsorIncome + trophyBonus,
                ledger = trophyLedger,
                boardConfidence = (career.boardConfidence + if (wonTitle) 18 else 6).coerceAtMost(100),
                managerReputation = (career.managerReputation + if (wonTitle) 12 else 4).coerceAtMost(100),
                seasonSummaryBanner = if (wonTitle) {
                    "CHAMPIONS! ${career.clubName} lifted the ${WorldGenerator.divisionNameFor(career.country, career.divisionTier)} Trophy (+$$trophyBonus bonus)!"
                } else {
                    "Runners-Up in the Grand Final! Great campaign by ${career.clubName}."
                }
            )
        }

        return career
    }

    fun startNextSeason(
        career: CareerState,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        val (updatedDivisions, newTier, swapSummary) = buildNextSeasonDivisions(career)
        val newFixtures = WorldGenerator.generateDivisionFixtures(updatedDivisions, random)

        // Age players, reset season stats, add 2 new youth academy prospects
        val nextSquad = career.squad.map { p ->
            val agedOvr = if (p.age >= 33) (p.overall - 1).coerceAtLeast(48) else p.overall
            p.copy(
                age = p.age + 1,
                overall = agedOvr,
                fitness = 96,
                recentMatchWorkload = 0,
                seasonStats = PlayerStats()
            )
        } + (1..2).map {
            WorldGenerator.createPlayer(
                role = T20Role.entries.random(random),
                targetOvr = (54 + career.budgets.academyLevel * 3 + random.nextInt(-2, 4)).coerceIn(50, 74),
                age = random.nextInt(16, 18),
                isYouth = true,
                tier = newTier,
                random = random,
                nation = career.country
            )
        }

        val currentDivSorted = career.currentDivision?.let { sortDivision(it.clubs) }.orEmpty()
        val finishRank = currentDivSorted.indexOfFirst { it.clubId == career.clubId }.let { if (it >= 0) it + 1 else 4 }
        val userStandingNow = currentDivSorted.firstOrNull { it.clubId == career.clubId }
        val completedFinal = career.fixtures.firstOrNull {
            it.tier == career.divisionTier && it.playoffLabel == "Grand Final" && it.isCompleted
        }
        val wonSeasonTitle = completedFinal != null && winnerIdOf(completedFinal) == career.clubId
        val finishedRecord = SeasonFinishRecord(
            season = career.season,
            tier = career.divisionTier,
            leagueRank = finishRank,
            points = userStandingNow?.points ?: 0,
            netRunRate = userStandingNow?.netRunRate ?: 0.0,
            wonTitle = wonSeasonTitle
        )
        val nextHistory = (career.seasonHistory + finishedRecord).takeLast(12)

        val seasonPrepCareer = career.copy(
            season = career.season + 1,
            divisionTier = newTier,
            squad = nextSquad,
            seasonHistory = nextHistory
        )
        val freshPortfolio = SponsorNegotiationEngine.generateSeasonSponsorPortfolio(
            tier = newTier,
            careerContext = seasonPrepCareer,
            country = career.country,
            cityTag = SponsorNegotiationEngine.resolveCityName(career),
            random = random
        )
        val activeMultiYear = career.sponsors
            .filter { it.isActive && it.seasonsRemaining > 1 }
            .associateBy { it.category }

        val updatedSponsors = freshPortfolio.map { offer ->
            val carry = activeMultiYear[offer.category]
            if (carry != null && carry.id == offer.id) {
                carry.copy(seasonsRemaining = carry.seasonsRemaining - 1, isActive = true)
            } else if (carry != null && carry.category == offer.category) {
                offer.copy(isActive = false)
            } else {
                offer
            }
        }.let { mapped ->
            // Ensure carried active deals not matched by ID still remain active while preserving portfolio variety
            val missingCarried = activeMultiYear.values.filter { carried -> mapped.none { it.id == carried.id } }
                .map { it.copy(seasonsRemaining = it.seasonsRemaining - 1, isActive = true) }
            val adjusted = if (missingCarried.isEmpty()) mapped else {
                mapped.filterNot { m -> missingCarried.any { it.category == m.category && it.id == m.id } } + missingCarried
            }
            adjusted
        }

        val promoNote = when {
            newTier < career.divisionTier -> "Promoted to ${WorldGenerator.divisionNameFor(career.country, newTier)}! $swapSummary"
            newTier > career.divisionTier -> "Relegated to ${WorldGenerator.divisionNameFor(career.country, newTier)}. $swapSummary"
            else -> "Starting Season ${career.season + 1} in ${WorldGenerator.divisionNameFor(career.country, newTier)}. $swapSummary"
        }.trim()
        val refreshedTournamentStats = WorldGenerator.generateTournamentRosters(updatedDivisions, career.clubId, career.country, random)

        return career.copy(
            season = career.season + 1,
            week = 1,
            isPreSeason = true,
            seasonComplete = false,
            divisionTier = newTier,
            squad = nextSquad,
            transferMarket = WorldGenerator.generateTransferMarket(newTier, 26, random, career.country),
            sponsors = updatedSponsors,
            divisions = updatedDivisions,
            tournamentStats = refreshedTournamentStats,
            fixtures = newFixtures,
            seasonHistory = nextHistory,
            seasonSummaryBanner = null,
            seasonTicketIncome = 0,
            seasonSponsorIncome = 0,
            seasonPlayerWagesPaid = 0,
            seasonStaffWagesPaid = 0,
            seasonUpkeepPaid = 0,
            homeMatchesHosted = 0,
            inbox = (listOf(
                InboxMessage(
                    id = "msg_new_s_${career.season + 1}",
                    season = career.season + 1,
                    week = 1,
                    senderRole = StaffRole.HEAD_COACH,
                    senderName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name ?: "Head Coach",
                    title = "Season ${career.season + 1} Pre-Season & Sponsors Open",
                    body = "$promoNote Pre-season sponsorship offers & negotiations are open during Week 1 before locking for the campaign.",
                    isUrgent = true
                )
            ) + career.inbox).take(25)
        )
    }

    private fun buildNextSeasonDivisions(career: CareerState): Triple<List<DivisionTable>, Int, String> {
        fun resetStanding(c: ClubStanding, tier: Int): ClubStanding = c.copy(
            tier = tier,
            played = 0,
            won = 0,
            lost = 0,
            runsFor = 0,
            ballsFaced = 0,
            runsAgainst = 0,
            ballsBowled = 0,
            points = 0
        )

        if (!career.country.supportsPromotionRelegation || career.divisions.size < 2) {
            val singleClubs = sortDivision(career.divisions.first().clubs).map { resetStanding(it, 1) }
            val nextDiv = DivisionTable(1, WorldGenerator.divisionNameFor(career.country, 1), singleClubs)
            return Triple(listOf(nextDiv), 1, "")
        }

        val sortedT1 = sortDivision(career.divisions.first { it.tier == 1 }.clubs)
        val sortedT2 = sortDivision(career.divisions.first { it.tier == 2 }.clubs)

        // Last place in Tier 1 (Major Cities) relegates to Tier 2 (Minor Cities)
        val relegatedFromT1 = sortedT1.last()

        // Winner of Tier 2 (Minor Cities) — Grand Final winner if played, else #1 in Tier 2 table — promotes to Tier 1
        val t2FinalFx = career.fixtures.firstOrNull {
            it.tier == 2 && it.playoffLabel == "Grand Final" && it.isCompleted
        }
        val t2WinnerId = if (t2FinalFx != null) winnerIdOf(t2FinalFx) else sortedT2.first().clubId
        val promotedFromT2 = sortedT2.firstOrNull { it.clubId == t2WinnerId } ?: sortedT2.first()

        val nextT1Clubs = (sortedT1.filterNot { it.clubId == relegatedFromT1.clubId } + promotedFromT2)
            .map { resetStanding(it, 1) }
        val nextT2Clubs = (sortedT2.filterNot { it.clubId == promotedFromT2.clubId } + relegatedFromT1)
            .map { resetStanding(it, 2) }

        val userNewTier = if (nextT1Clubs.any { it.clubId == career.clubId }) 1 else 2
        val swapSummary = "${promotedFromT2.name} promoted to Major Cities; ${relegatedFromT1.name} relegated to Minor Cities."

        val nextDivisions = listOf(
            DivisionTable(1, WorldGenerator.divisionNameFor(career.country, 1), nextT1Clubs),
            DivisionTable(2, WorldGenerator.divisionNameFor(career.country, 2), nextT2Clubs)
        )
        return Triple(nextDivisions, userNewTier, swapSummary)
    }

    private fun simulateAiPlayoffsWinner(
        career: CareerState,
        q1: Fixture,
        el: Fixture,
        random: Random
    ): Pair<String, List<Fixture>> {
        val sq1 = simulateAiFixture(q1, career.divisions, random).first
        val sel = simulateAiFixture(el, career.divisions, random).first
        val clubs = career.divisions.first { it.tier == career.divisionTier }.clubs
        val loserQ1 = clubs.first { it.clubId == loserIdOf(sq1) }
        val winEl = clubs.first { it.clubId == winnerIdOf(sel) }
        val q2 = Fixture("sim_q2_${career.season}", 16, career.divisionTier, loserQ1.clubId, loserQ1.name, winEl.clubId, winEl.name, PitchType.BALANCED_DECK, true, "Qualifier 2")
        val sq2 = simulateAiFixture(q2, career.divisions, random).first
        val winQ1 = clubs.first { it.clubId == winnerIdOf(sq1) }
        val winQ2 = clubs.first { it.clubId == winnerIdOf(sq2) }
        val fin = Fixture("sim_fin_${career.season}", 17, career.divisionTier, winQ1.clubId, winQ1.name, winQ2.clubId, winQ2.name, PitchType.BATTING_PARADISE, true, "Grand Final")
        val sFin = simulateAiFixture(fin, career.divisions, random).first
        val champ = clubs.first { it.clubId == winnerIdOf(sFin) }
        return "${champ.name} won the Season ${career.season} Championship Final." to listOf(sq1, sel, sq2, sFin)
    }

    private fun winnerIdOf(fx: Fixture): String {
        return if (fx.resultSummary.startsWith(fx.homeClubName)) fx.homeClubId else fx.awayClubId
    }

    private fun loserIdOf(fx: Fixture): String {
        return if (winnerIdOf(fx) == fx.homeClubId) fx.awayClubId else fx.homeClubId
    }

    private fun simulateAiFixture(
        fx: Fixture,
        divisions: List<DivisionTable>,
        random: Random
    ): Triple<Fixture, Pair<Int, Int>, Pair<Int, Int>> {
        val clubs = divisions.flatMap { it.clubs }
        val homeStr = clubs.firstOrNull { it.clubId == fx.homeClubId }?.strength ?: 70
        val awayStr = clubs.firstOrNull { it.clubId == fx.awayClubId }?.strength ?: 70
        val pitchAvg = (162 * fx.pitchType.runBias).roundToInt()
        val homeRuns = (pitchAvg + (homeStr - awayStr) + random.nextInt(-26, 28)).coerceIn(105, 232)
        var awayRuns = (pitchAvg + (awayStr - homeStr) + random.nextInt(-28, 28)).coerceIn(102, 234)
        if (awayRuns == homeRuns) awayRuns += if (random.nextBoolean()) 1 else -1

        val homeBalls = 120
        val awayBalls = if (awayRuns > homeRuns) random.nextInt(104, 120) else 120
        val winnerName = if (homeRuns > awayRuns) fx.homeClubName else fx.awayClubName
        val summary = "$winnerName won (${homeRuns} vs ${awayRuns})"

        return Triple(
            fx.copy(isCompleted = true, resultSummary = summary),
            homeRuns to homeBalls,
            awayRuns to awayBalls
        )
    }

    private fun applyMatchToStandings(
        divisions: List<DivisionTable>,
        tier: Int,
        clubAId: String,
        runsA: Int,
        ballsA: Int,
        clubBId: String,
        runsB: Int,
        ballsB: Int
    ): List<DivisionTable> {
        val aWon = runsA > runsB
        return divisions.map { div ->
            if (div.tier != tier) div
            else {
                val updatedClubs = div.clubs.map { c ->
                    when (c.clubId) {
                        clubAId -> c.copy(
                            played = c.played + 1,
                            won = c.won + if (aWon) 1 else 0,
                            lost = c.lost + if (!aWon) 1 else 0,
                            runsFor = c.runsFor + runsA,
                            ballsFaced = c.ballsFaced + ballsA,
                            runsAgainst = c.runsAgainst + runsB,
                            ballsBowled = c.ballsBowled + ballsB,
                            points = c.points + if (aWon) 2 else 0
                        )
                        clubBId -> c.copy(
                            played = c.played + 1,
                            won = c.won + if (!aWon) 1 else 0,
                            lost = c.lost + if (aWon) 1 else 0,
                            runsFor = c.runsFor + runsB,
                            ballsFaced = c.ballsFaced + ballsB,
                            runsAgainst = c.runsAgainst + runsA,
                            ballsBowled = c.ballsBowled + ballsA,
                            points = c.points + if (!aWon) 2 else 0
                        )
                        else -> c
                    }
                }
                div.copy(clubs = sortDivision(updatedClubs))
            }
        }
    }

    fun sortDivision(clubs: List<ClubStanding>): List<ClubStanding> =
        clubs.sortedWith(
            compareByDescending<ClubStanding> { it.points }
                .thenByDescending { it.won }
                .thenByDescending { it.netRunRate }
        )

    private fun updateTournamentStatsAfterRound(
        existingStats: List<TournamentPlayerStat>,
        userFixture: Fixture,
        userClubId: String,
        oppBatters: List<BatterScoreEntry>,
        oppBowlers: List<BowlerScoreEntry>,
        aiRoundResults: List<Triple<Fixture, Pair<Int, Int>, Pair<Int, Int>>>,
        random: Random
    ): List<TournamentPlayerStat> {
        if (existingStats.isEmpty()) return existingStats

        val mutable = existingStats.associateBy { it.playerId }.toMutableMap()
        val byClub = existingStats.groupBy { it.clubId }

        fun applyClubAiPerformances(
            clubId: String,
            runsScored: Int,
            ballsFaced: Int,
            runsConceded: Int,
            ballsBowled: Int
        ) {
            val roster = byClub[clubId] ?: return
            val batters = roster.filter {
                it.role != T20Role.PACE_SPEARHEAD && it.role != T20Role.DEATH_BOWLER && it.role != T20Role.SPINNER
            }
            val bowlers = roster.filter {
                it.role == T20Role.PACE_SPEARHEAD || it.role == T20Role.DEATH_BOWLER || it.role == T20Role.SPINNER
            }

            if (batters.isNotEmpty()) {
                val shares = listOf(0.32, 0.25, 0.18)
                var remainingRuns = (runsScored * 0.75).roundToInt()
                var remainingBalls = (ballsFaced * 0.74).roundToInt().coerceAtLeast(batters.size * 6)
                batters.forEachIndexed { idx, tp ->
                    val current = mutable[tp.playerId] ?: tp
                    val share = shares.getOrElse(idx) { 0.16 }
                    val r = if (idx == batters.lastIndex) {
                        remainingRuns.coerceIn(4, 92)
                    } else {
                        ((runsScored * share).roundToInt() + random.nextInt(-6, 12)).coerceIn(6, 98)
                    }
                    remainingRuns = (remainingRuns - r).coerceAtLeast(0)
                    val b = if (idx == batters.lastIndex) {
                        remainingBalls.coerceIn(6, 54)
                    } else {
                        ((ballsFaced * share).roundToInt() + random.nextInt(-4, 6)).coerceIn(6, 58)
                    }
                    remainingBalls = (remainingBalls - b).coerceAtLeast(0)
                    val dismissed = idx < 2 || random.nextFloat() < 0.65f
                    val batEntry = BatterScoreEntry(
                        playerId = current.playerId,
                        playerName = current.playerName,
                        roleLabel = current.role.shortLabel,
                        runs = r,
                        balls = b,
                        fours = (r / 10).coerceAtLeast(0),
                        sixes = (r / 22).coerceAtLeast(0),
                        isOut = dismissed
                    )
                    mutable[current.playerId] = current.copy(
                        stats = mergeStats(current.stats, batEntry, null)
                    )
                }
            }

            if (bowlers.isNotEmpty()) {
                val wicketsPool = random.nextInt(4, 9)
                val wktWeights = listOf(0.42, 0.32, 0.26)
                var wktAssigned = 0
                bowlers.forEachIndexed { idx, tp ->
                    val current = mutable[tp.playerId] ?: tp
                    val balls = if (ballsBowled >= 96) 24 else 18
                    val conceded = ((runsConceded / 5.0).roundToInt() + random.nextInt(-5, 7)).coerceIn(12, 48)
                    val wkts = if (idx == bowlers.lastIndex) {
                        (wicketsPool - wktAssigned).coerceIn(0, 4)
                    } else {
                        ((wicketsPool * wktWeights.getOrElse(idx) { 0.25 }).roundToInt() + if (random.nextFloat() < 0.25f) 1 else 0).coerceIn(0, 4)
                    }
                    wktAssigned += wkts
                    val dots = (balls * random.nextDouble(0.28, 0.46)).roundToInt().coerceIn(2, balls)
                    val maiden = if (conceded <= 15 && dots >= 9 && random.nextFloat() < 0.28f) 1 else 0
                    val bowlEntry = BowlerScoreEntry(
                        playerId = current.playerId,
                        playerName = current.playerName,
                        ballsBowled = balls,
                        runsConceded = conceded,
                        wickets = wkts,
                        dotBalls = dots,
                        maidens = maiden
                    )
                    mutable[current.playerId] = current.copy(
                        stats = mergeStats(current.stats, null, bowlEntry)
                    )
                }
            }
        }

        // Apply opponent performances from the user's real match card
        if (!userFixture.isPlayoff) {
            val oppClubId = if (userFixture.homeClubId == userClubId) userFixture.awayClubId else userFixture.homeClubId
            val oppRoster = byClub[oppClubId].orEmpty()
            val oppBatSlots = oppRoster.filter {
                it.role != T20Role.PACE_SPEARHEAD && it.role != T20Role.DEATH_BOWLER && it.role != T20Role.SPINNER
            }
            val oppBowlSlots = oppRoster.filter {
                it.role == T20Role.PACE_SPEARHEAD || it.role == T20Role.DEATH_BOWLER || it.role == T20Role.SPINNER
            }

            val sortedBatters = oppBatters.filter { it.balls > 0 || it.isOut }.sortedByDescending { it.runs }
            oppBatSlots.forEachIndexed { idx, tp ->
                val bat = sortedBatters.getOrNull(idx)
                if (bat != null) {
                    val current = mutable[tp.playerId] ?: tp
                    mutable[tp.playerId] = current.copy(stats = mergeStats(current.stats, bat, null))
                }
            }

            val sortedBowlers = oppBowlers.filter { it.ballsBowled > 0 }.sortedByDescending { it.wickets * 20 - it.runsConceded }
            oppBowlSlots.forEachIndexed { idx, tp ->
                val bowl = sortedBowlers.getOrNull(idx)
                if (bowl != null) {
                    val current = mutable[tp.playerId] ?: tp
                    mutable[tp.playerId] = current.copy(stats = mergeStats(current.stats, null, bowl))
                }
            }
        }

        // Apply AI vs AI match performances across all divisions
        for ((fx, homeScore, awayScore) in aiRoundResults) {
            if (fx.isPlayoff) continue
            applyClubAiPerformances(
                clubId = fx.homeClubId,
                runsScored = homeScore.first,
                ballsFaced = homeScore.second,
                runsConceded = awayScore.first,
                ballsBowled = awayScore.second
            )
            applyClubAiPerformances(
                clubId = fx.awayClubId,
                runsScored = awayScore.first,
                ballsFaced = awayScore.second,
                runsConceded = homeScore.first,
                ballsBowled = homeScore.second
            )
        }

        return existingStats.map { mutable[it.playerId] ?: it }
    }

    fun mergeStats(prev: PlayerStats, bat: BatterScoreEntry?, bowl: BowlerScoreEntry?): PlayerStats {
        val playedBat = bat != null && (bat.balls > 0 || bat.isOut)
        val runs = bat?.runs ?: 0
        val newBestWkt = bowl?.wickets ?: 0
        val newBestRuns = bowl?.runsConceded ?: 999
        val isBetterBowl = bowl != null && (newBestWkt > prev.bestWickets || (newBestWkt == prev.bestWickets && newBestRuns < prev.bestRuns))

        return prev.copy(
            matches = prev.matches + 1,
            innings = prev.innings + if (playedBat) 1 else 0,
            runs = prev.runs + runs,
            ballsFaced = prev.ballsFaced + (bat?.balls ?: 0),
            notOuts = prev.notOuts + if (playedBat && !bat!!.isOut) 1 else 0,
            fours = prev.fours + (bat?.fours ?: 0),
            sixes = prev.sixes + (bat?.sixes ?: 0),
            thirties = prev.thirties + if (runs in 30..49) 1 else 0,
            fifties = prev.fifties + if (runs in 50..99) 1 else 0,
            hundreds = prev.hundreds + if (runs >= 100) 1 else 0,
            highScore = maxOf(prev.highScore, runs),
            ballsBowled = prev.ballsBowled + (bowl?.ballsBowled ?: 0),
            runsConceded = prev.runsConceded + (bowl?.runsConceded ?: 0),
            wickets = prev.wickets + (bowl?.wickets ?: 0),
            dotBalls = prev.dotBalls + (bowl?.dotBalls ?: 0),
            maidens = prev.maidens + (bowl?.maidens ?: 0),
            threeWicketHauls = prev.threeWicketHauls + if ((bowl?.wickets ?: 0) >= 3) 1 else 0,
            bestWickets = if (isBetterBowl) newBestWkt else prev.bestWickets,
            bestRuns = if (isBetterBowl) newBestRuns else prev.bestRuns
        )
    }

    fun evaluateNegotiationOffer(
        session: NegotiationSession,
        career: CareerState
    ): NegotiationSession {
        val scoutRating = career.staff.firstOrNull { it.role == StaffRole.CHIEF_SCOUT }?.rating ?: 65
        val repFactor = 1.0 + ((career.managerReputation - 50) / 350.0) + ((scoutRating - 65) / 500.0)

        val feeRatio = if (session.isRenewal || session.askingFee == 0) 1.0 else {
            (session.offeredFee.toDouble() / session.askingFee.toDouble()) * repFactor
        }
        val wageValue = session.offeredWage + (session.offeredSigningBonus / (session.offeredYears * 52).coerceAtLeast(26))
        val wageRatio = (wageValue.toDouble() / session.askingWage.toDouble()) * repFactor

        val compositeScore = feeRatio * 0.45 + wageRatio * 0.55

        return when {
            compositeScore >= 0.98 -> session.copy(
                isAccepted = true,
                agentMessage = "Deal agreed! ${session.player.name} is excited to sign for ${career.clubName} on $${session.offeredWage}/wk."
            )
            session.agentPatience <= 22 || compositeScore < 0.65 -> {
                val nextPatience = (session.agentPatience - 32).coerceAtLeast(0)
                if (nextPatience <= 0) {
                    session.copy(
                        agentPatience = 0,
                        isCollapsed = true,
                        agentMessage = "Talks collapsed. The agent felt your offer was too far below market valuation."
                    )
                } else {
                    val counterFee = if (session.isRenewal) 0 else ((session.askingFee * 0.94).roundToInt())
                    val counterWage = (session.askingWage * 0.96).roundToInt()
                    session.copy(
                        askingFee = counterFee,
                        askingWage = counterWage,
                        agentPatience = nextPatience,
                        round = session.round + 1,
                        agentMessage = "That's well short. We need around ${if (counterFee > 0) "$$counterFee fee and " else ""}$$counterWage/wk."
                    )
                }
            }
            else -> {
                val nextPatience = (session.agentPatience - 18).coerceAtLeast(10)
                val counterFee = if (session.isRenewal) 0 else ((session.askingFee + session.offeredFee) / 2)
                val counterWage = ((session.askingWage + session.offeredWage) / 2)
                session.copy(
                    askingFee = counterFee,
                    askingWage = counterWage,
                    agentPatience = nextPatience,
                    round = session.round + 1,
                    agentMessage = "Getting closer! Meet us at ${if (counterFee > 0) "$$counterFee fee & " else ""}$$counterWage/wk and we have a deal."
                )
            }
        }
    }
}
