package com.example.simulation

import com.example.model.*
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random

object SeasonProgressionEngine {

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

                // Small chance of post-match niggle if fitness drops below 65
                val medBonus = career.budgets.medicalLevel * 2
                val injuryRoll = if (newFitness < 65 && random.nextInt(100) < (14 - medBonus).coerceAtLeast(3)) {
                    InjuryState(
                        isInjured = true,
                        injuryName = listOf("Hamstring Tightness", "Shoulder Strain", "Ankle Twist").random(random),
                        weeksRemaining = random.nextInt(1, 3),
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

                player.copy(
                    form = newForm,
                    fitness = newFitness,
                    morale = newMorale,
                    injury = injuryRoll,
                    seasonStats = updatedSeason,
                    careerStats = updatedCareer,
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

        // Simulate all other fixtures of the same round across all 3 divisions
        val roundFixtures = career.fixtures.filter { it.round == fixture.round && it.id != fixture.id && !it.isCompleted }
        val completedSimMap = mutableMapOf<String, Fixture>()
        for (fx in roundFixtures) {
            val res = simulateAiFixture(fx, updatedDivisions, random)
            completedSimMap[fx.id] = res.first
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

        val updatedFixtures = career.fixtures.map { fx ->
            when {
                fx.id == fixture.id -> fx.copy(isCompleted = true, resultSummary = record.resultText)
                completedSimMap.containsKey(fx.id) -> completedSimMap.getValue(fx.id)
                else -> fx
            }
        }

        val newBank = career.bankBalance + record.revenueEarned
        val newLedger = if (record.revenueEarned > 0) {
            (listOf(
                FinanceLedgerEntry(
                    id = "ld_${UUID.randomUUID().toString().take(8)}",
                    season = career.season,
                    week = career.week,
                    category = "Matchday",
                    description = "Gate & win bonus vs ${record.opponentName}",
                    amount = record.revenueEarned
                )
            ) + career.ledger).take(40)
        } else career.ledger

        val newConfidence = (career.boardConfidence + if (record.userWon) 4 else -3).coerceIn(15, 100)
        val newRep = (career.managerReputation + if (record.userWon) 2 else 0).coerceIn(10, 100)

        var nextState = career.copy(
            squad = updatedSquad,
            divisions = updatedDivisions,
            fixtures = updatedFixtures,
            matchHistory = (listOf(record) + career.matchHistory).take(30),
            bankBalance = newBank,
            boardConfidence = newConfidence,
            managerReputation = newRep,
            ledger = newLedger
        )

        // Check playoff progression or end of regular season
        nextState = evaluatePlayoffScheduling(nextState, random)
        return advanceWeeklyTick(nextState, random)
    }

    fun advanceWeeklyTick(
        career: CareerState,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        // 1. Weekly Finances
        val sponsorIncome = career.weeklySponsorIncome
        val wagesOut = career.weeklyPlayerWages + career.weeklyStaffWages
        val facilityUpkeep = career.budgets.weeklyUpkeep
        val netCash = sponsorIncome - wagesOut - facilityUpkeep
        val newBank = career.bankBalance + netCash

        val newLedgerEntries = mutableListOf<FinanceLedgerEntry>()
        if (sponsorIncome > 0) {
            newLedgerEntries.add(
                FinanceLedgerEntry("ld_sp_${career.season}_${career.week}", career.season, career.week, "Sponsors", "Weekly sponsorship retainers", sponsorIncome)
            )
        }
        newLedgerEntries.add(
            FinanceLedgerEntry("ld_wg_${career.season}_${career.week}", career.season, career.week, "Payroll", "Squad & management wages", -wagesOut)
        )
        newLedgerEntries.add(
            FinanceLedgerEntry("ld_op_${career.season}_${career.week}", career.season, career.week, "Operations", "Academy, scouting & medical facilities", -facilityUpkeep)
        )

        // 2. Staff bonuses
        val headCoachRating = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
        val physioRating = career.staff.firstOrNull { it.role == StaffRole.PHYSIO }?.rating ?: 65
        val batCoachRating = career.staff.firstOrNull { it.role == StaffRole.BATTING_COACH }?.rating ?: 65
        val bowlCoachRating = career.staff.firstOrNull { it.role == StaffRole.BOWLING_COACH }?.rating ?: 65

        val newInbox = mutableListOf<InboxMessage>()

        // 3. Squad Training, Rehab & Contract Week Tick
        val progressedSquad = career.squad.map { player ->
            val updatedContract = player.contract.copy(weeksRemaining = (player.contract.weeksRemaining - 1).coerceAtLeast(1))

            if (player.injury.isInjured) {
                val effectiveRehab = if (career.delegation.rehabDelegated) RehabMode.BALANCED else player.injury.rehabMode
                val setbackChance = (effectiveRehab.setbackRiskPercent - (physioRating - 60) / 5).coerceAtLeast(1)
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
                        injury = player.injury.copy(weeksRemaining = player.injury.weeksRemaining + 1)
                    )
                }
                val step = if (random.nextFloat() < (effectiveRehab.speedMultiplier - 0.8f)) 2 else 1
                val weeksLeft = (player.injury.weeksRemaining - step).coerceAtLeast(0)
                return@map if (weeksLeft == 0) {
                    player.copy(
                        fitness = (player.fitness + 14).coerceIn(72, 95),
                        contract = updatedContract,
                        injury = InjuryState(isInjured = false, injuryName = "", weeksRemaining = 0, rehabMode = RehabMode.NONE),
                        lastMatchNote = "Cleared for T20 selection"
                    )
                } else {
                    player.copy(
                        fitness = (player.fitness + 5).coerceAtMost(85),
                        contract = updatedContract,
                        injury = player.injury.copy(weeksRemaining = weeksLeft)
                    )
                }
            }

            // Active Training
            val activeDrill = if (career.delegation.trainingDelegated) {
                when {
                    player.fitness < 74 -> TrainingFocus.FITNESS_RECOVERY
                    player.role == T20Role.SPINNER -> TrainingFocus.SPIN_VARIATIONS
                    player.role == T20Role.PACE_SPEARHEAD || player.role == T20Role.DEATH_BOWLER -> TrainingFocus.DEATH_YORKERS
                    else -> TrainingFocus.POWER_HITTING
                }
            } else player.trainingFocus

            val fitnessGain = if (activeDrill == TrainingFocus.FITNESS_RECOVERY) 15 else 6 + (career.budgets.medicalLevel)
            val newFitness = (player.fitness + fitnessGain).coerceIn(50, 100)

            val growthChance = when {
                player.age <= 21 -> 0.42f + (career.budgets.academyLevel * 0.05f)
                player.age <= 26 -> 0.25f + ((batCoachRating + bowlCoachRating) / 600f)
                else -> 0.08f
            }
            var attrs = player.attributes
            var ovr = player.overall
            if (ovr < player.potential && random.nextFloat() < growthChance) {
                attrs = when (activeDrill) {
                    TrainingFocus.POWER_HITTING -> attrs.copy(powerHitting = (attrs.powerHitting + 1).coerceAtMost(99))
                    TrainingFocus.ANCHOR_TECHNIQUE -> attrs.copy(battingTiming = (attrs.battingTiming + 1).coerceAtMost(99))
                    TrainingFocus.DEATH_YORKERS -> attrs.copy(deathExecution = (attrs.deathExecution + 1).coerceAtMost(99), paceBowling = (attrs.paceBowling + 1).coerceAtMost(99))
                    TrainingFocus.SPIN_VARIATIONS -> attrs.copy(spinVariation = (attrs.spinVariation + 1).coerceAtMost(99))
                    TrainingFocus.FIELDING_AGILITY -> attrs.copy(fielding = (attrs.fielding + 1).coerceAtMost(99))
                    TrainingFocus.BALANCED -> attrs.copy(
                        battingTiming = (attrs.battingTiming + 1).coerceAtMost(99),
                        fielding = (attrs.fielding + 1).coerceAtMost(99)
                    )
                    TrainingFocus.FITNESS_RECOVERY -> attrs
                }
                if (activeDrill != TrainingFocus.FITNESS_RECOVERY && random.nextFloat() < 0.48f) {
                    ovr = (ovr + 1).coerceAtMost(player.potential)
                }
            }

            player.copy(
                attributes = attrs,
                overall = ovr,
                fitness = newFitness,
                trainingFocus = activeDrill,
                contract = updatedContract
            )
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

        return career.copy(
            week = career.week + 1,
            bankBalance = newBank,
            lastWeekNetCashflow = netCash,
            squad = finalSquad,
            selectedXIIds = newXI,
            impactSubId = newSub,
            ledger = (newLedgerEntries + career.ledger).take(40),
            inbox = (newInbox + career.inbox).take(25)
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
                // Simulate playoffs for AI and mark season complete
                val finalStandings = simulateAiPlayoffsWinner(career, q1, el, random)
                career.copy(
                    seasonComplete = true,
                    seasonSummaryBanner = finalStandings,
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
                // User eliminated in Eliminator
                career.copy(
                    seasonComplete = true,
                    seasonSummaryBanner = "Eliminated in the Playoffs — ready for Season ${career.season + 1} rebuild."
                )
            }
        }

        if (q2 != null && q2.isCompleted && finalFx == null && q1 != null) {
            val q1Winner = winnerIdOf(q1)
            val q2Winner = winnerIdOf(q2)
            val clubs = career.divisions.first { it.tier == career.divisionTier }.clubs
            if (q1Winner == career.clubId || q2Winner == career.clubId) {
                val cA = clubs.first { it.clubId == q1Winner }
                val cB = clubs.first { it.clubId == q2Winner }
                val newFinal = Fixture("po_fin_${career.season}", 17, career.divisionTier, cA.clubId, cA.name, cB.clubId, cB.name, PitchType.BATTING_PARADISE, true, "Grand Final")
                return career.copy(fixtures = career.fixtures + newFinal)
            } else {
                return career.copy(
                    seasonComplete = true,
                    seasonSummaryBanner = "Playoff run ended in Qualifier 2 — Season ${career.season + 1} awaits."
                )
            }
        }

        if (finalFx != null && finalFx.isCompleted) {
            val championId = winnerIdOf(finalFx)
            val wonTitle = championId == career.clubId
            val trophyBonus = if (wonTitle) career.sponsors.filter { it.isActive }.sumOf { it.trophyBonus } else 0
            return career.copy(
                seasonComplete = true,
                trophiesWon = career.trophiesWon + if (wonTitle) 1 else 0,
                bankBalance = career.bankBalance + trophyBonus,
                boardConfidence = (career.boardConfidence + if (wonTitle) 18 else 6).coerceAtMost(100),
                managerReputation = (career.managerReputation + if (wonTitle) 12 else 4).coerceAtMost(100),
                seasonSummaryBanner = if (wonTitle) {
                    "CHAMPIONS! ${career.clubName} lifted the ${WorldGenerator.divisionNameForTier(career.divisionTier)} Trophy (+$$trophyBonus bonus)!"
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
        val currentDiv = sortDivision(career.divisions.first { it.tier == career.divisionTier }.clubs)
        val userRank = currentDiv.indexOfFirst { it.clubId == career.clubId } + 1
        val finalFx = career.fixtures.firstOrNull { it.tier == career.divisionTier && it.playoffLabel == "Grand Final" && it.isCompleted }
        val wonPlayoffs = finalFx != null && winnerIdOf(finalFx) == career.clubId

        // Promotion & Relegation across the 3-tier pyramid
        val newTier = when {
            career.divisionTier > 1 && (wonPlayoffs || userRank == 1) -> career.divisionTier - 1
            career.divisionTier < 3 && userRank == currentDiv.size -> career.divisionTier + 1
            else -> career.divisionTier
        }

        val updatedDivisions = buildNextSeasonDivisions(career, newTier)
        val newFixtures = WorldGenerator.generateDivisionFixtures(updatedDivisions, random)

        // Age players, reset season stats, add 2 new youth academy prospects
        val nextSquad = career.squad.map { p ->
            val agedOvr = if (p.age >= 33) (p.overall - 1).coerceAtLeast(48) else p.overall
            p.copy(
                age = p.age + 1,
                overall = agedOvr,
                fitness = 96,
                seasonStats = PlayerStats()
            )
        } + (1..2).map {
            WorldGenerator.createPlayer(
                role = T20Role.entries.random(random),
                targetOvr = (54 + career.budgets.academyLevel * 3 + random.nextInt(-2, 4)).coerceIn(50, 74),
                age = random.nextInt(16, 18),
                isYouth = true,
                tier = newTier,
                random = random
            )
        }

        val updatedSponsors = career.sponsors.map { sp ->
            if (sp.isActive) {
                val left = sp.seasonsRemaining - 1
                if (left <= 0) sp.copy(isActive = false, seasonsRemaining = 2) else sp.copy(seasonsRemaining = left)
            } else sp
        }

        val promoNote = when {
            newTier < career.divisionTier -> "Promoted to ${WorldGenerator.divisionNameForTier(newTier)}!"
            newTier > career.divisionTier -> "Relegated to ${WorldGenerator.divisionNameForTier(newTier)}."
            else -> "Starting Season ${career.season + 1} in ${WorldGenerator.divisionNameForTier(newTier)}."
        }

        return career.copy(
            season = career.season + 1,
            week = 1,
            seasonComplete = false,
            divisionTier = newTier,
            squad = nextSquad,
            transferMarket = WorldGenerator.generateTransferMarket(newTier, 26, random),
            sponsors = updatedSponsors,
            divisions = updatedDivisions,
            fixtures = newFixtures,
            seasonSummaryBanner = null,
            inbox = (listOf(
                InboxMessage(
                    id = "msg_new_s_${career.season + 1}",
                    season = career.season + 1,
                    week = 1,
                    senderRole = StaffRole.HEAD_COACH,
                    senderName = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name ?: "Head Coach",
                    title = "Season ${career.season + 1} Pre-Season",
                    body = "$promoNote Two new U-19 Academy prospects have joined the youth ranks.",
                    isUrgent = true
                )
            ) + career.inbox).take(25)
        )
    }

    private fun buildNextSeasonDivisions(career: CareerState, userNewTier: Int): List<DivisionTable> {
        val sortedByTier = career.divisions.associate { it.tier to sortDivision(it.clubs) }
        val t1 = sortedByTier[1].orEmpty().toMutableList()
        val t2 = sortedByTier[2].orEmpty().toMutableList()
        val t3 = sortedByTier[3].orEmpty().toMutableList()

        // Ensure user club sits in userNewTier
        val allClubs = (t1 + t2 + t3).map {
            it.copy(played = 0, won = 0, lost = 0, runsFor = 0, ballsFaced = 0, runsAgainst = 0, ballsBowled = 0, points = 0)
        }.toMutableList()

        val userClub = allClubs.first { it.clubId == career.clubId }.copy(tier = userNewTier)
        allClubs.removeAll { it.clubId == career.clubId }

        val newT1 = allClubs.filter { it.tier == 1 }.toMutableList()
        val newT2 = allClubs.filter { it.tier == 2 }.toMutableList()
        val newT3 = allClubs.filter { it.tier == 3 }.toMutableList()

        when (userNewTier) {
            1 -> newT1.add(0, userClub)
            2 -> newT2.add(0, userClub)
            else -> newT3.add(0, userClub)
        }
        // Rebalance 8 per division
        while (newT1.size > 8) {
            val drop = newT1.removeAt(newT1.lastIndex)
            newT2.add(drop.copy(tier = 2))
        }
        while (newT2.size > 8) {
            val drop = newT2.removeAt(newT2.lastIndex)
            newT3.add(drop.copy(tier = 3))
        }
        while (newT3.size > 8 && newT2.size < 8) {
            val up = newT3.removeAt(0)
            newT2.add(up.copy(tier = 2))
        }
        while (newT2.size > 8 && newT1.size < 8) {
            val up = newT2.removeAt(0)
            newT1.add(up.copy(tier = 1))
        }

        return listOf(
            DivisionTable(1, WorldGenerator.divisionNameForTier(1), newT1.take(8)),
            DivisionTable(2, WorldGenerator.divisionNameForTier(2), newT2.take(8)),
            DivisionTable(3, WorldGenerator.divisionNameForTier(3), newT3.take(8))
        )
    }

    private fun simulateAiPlayoffsWinner(
        career: CareerState,
        q1: Fixture,
        el: Fixture,
        random: Random
    ): String {
        val sq1 = simulateAiFixture(q1, career.divisions, random).first
        val sel = simulateAiFixture(el, career.divisions, random).first
        val clubs = career.divisions.first { it.tier == career.divisionTier }.clubs
        val loserQ1 = clubs.first { it.clubId == loserIdOf(sq1) }
        val winEl = clubs.first { it.clubId == winnerIdOf(sel) }
        val q2 = Fixture("sim_q2", 16, career.divisionTier, loserQ1.clubId, loserQ1.name, winEl.clubId, winEl.name, PitchType.BALANCED_DECK, true, "Qualifier 2")
        val sq2 = simulateAiFixture(q2, career.divisions, random).first
        val winQ1 = clubs.first { it.clubId == winnerIdOf(sq1) }
        val winQ2 = clubs.first { it.clubId == winnerIdOf(sq2) }
        val fin = Fixture("sim_fin", 17, career.divisionTier, winQ1.clubId, winQ1.name, winQ2.clubId, winQ2.name, PitchType.BATTING_PARADISE, true, "Grand Final")
        val sFin = simulateAiFixture(fin, career.divisions, random).first
        val champ = clubs.first { it.clubId == winnerIdOf(sFin) }
        return "${champ.name} won the Season ${career.season} Championship Final."
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

    private fun mergeStats(prev: PlayerStats, bat: BatterScoreEntry?, bowl: BowlerScoreEntry?): PlayerStats {
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
            fifties = prev.fifties + if (runs in 50..99) 1 else 0,
            hundreds = prev.hundreds + if (runs >= 100) 1 else 0,
            highScore = maxOf(prev.highScore, runs),
            ballsBowled = prev.ballsBowled + (bowl?.ballsBowled ?: 0),
            runsConceded = prev.runsConceded + (bowl?.runsConceded ?: 0),
            wickets = prev.wickets + (bowl?.wickets ?: 0),
            dotBalls = prev.dotBalls + (bowl?.dotBalls ?: 0),
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
