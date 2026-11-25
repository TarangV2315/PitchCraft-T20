package com.example.simulation

import com.example.model.*
import kotlin.math.roundToInt
import kotlin.random.Random

class ActiveMatchRuntime(
    val careerSnapshot: CareerState,
    val fixture: Fixture,
    val userXI: List<Player>,
    var impactSub: Player?,
    val opponentXI: List<Player>,
    val userBatFirst: Boolean,
    val random: Random = Random(System.currentTimeMillis())
) {
    var currentInnings: Int = 1
    var ballsBowledInInnings: Int = 0
    var runsInInnings: Int = 0
    var wicketsInInnings: Int = 0
    var target: Int? = null
    var currentTempo: BattingTempo = careerSnapshot.defaultTempo
    var currentBowlingPlan: BowlingPlan = careerSnapshot.defaultBowlingPlan
    var currentFieldSpread: FieldSpread = careerSnapshot.defaultFieldSpread
    var preMatchTeamTalk: TeamTalkTone = careerSnapshot.preMatchTeamTalk
    var inningsBreakTeamTalk: TeamTalkTone? = null
    var postMatchTeamTalkDelivered: TeamTalkTone? = null
    var impactSubUsed: Boolean = false

    private var battingLineup: MutableList<Player> =
        if (userBatFirst) userXI.toMutableList() else opponentXI.toMutableList()
    private var bowlingLineup: MutableList<Player> =
        if (userBatFirst) opponentXI.toMutableList() else userXI.toMutableList()

    private var strikerIdx: Int = 0
    private var nonStrikerIdx: Int = 1
    private var nextBatterIdx: Int = 2

    private var batterMap: MutableMap<String, BatterScoreEntry> = initBatterMap(battingLineup)
    private var bowlerMap: MutableMap<String, BowlerScoreEntry> = mutableMapOf()
    private var bowlerOverCounts: MutableMap<String, Int> = mutableMapOf()
    private var currentBowlerId: String = pickBowlerForOver(0, null).id
    private var currentOverRuns: Int = 0

    private val recentBalls = mutableListOf<String>()
    private val commentaryLog = mutableListOf<String>()
    private val ballFeed = mutableListOf<BallEvent>()
    private val runRateHistory = mutableListOf<RunRatePoint>()
    private val keyHighlights = mutableListOf<String>()
    private val matchFitnessMap = (userXI + opponentXI + listOfNotNull(impactSub))
        .associate { it.id to it.fitness }
        .toMutableMap()

    var firstInningsScorecard: InningsScorecard? = null
    var secondInningsScorecard: InningsScorecard? = null

    private val momentCheckpoints = setOf(18, 36, 66, 96, 111)
    private val triggeredMoments = mutableSetOf<Int>()
    private var ballSeq = 0

    init {
        val firstBatName = if (userBatFirst) careerSnapshot.clubName else opponentName()
        commentaryLog.add("Toss: $firstBatName bat first on a ${fixture.pitchType.label} surface.")
        commentaryLog.add("Team Talk (${preMatchTeamTalk.label}): ${preMatchTeamTalk.speechLine(TeamTalkStage.PRE_MATCH)}")
    }

    private fun activeTeamTalk(): TeamTalkTone =
        if (currentInnings == 2 && inningsBreakTeamTalk != null) inningsBreakTeamTalk!! else preMatchTeamTalk

    private fun opponentName(): String =
        if (fixture.homeClubId == careerSnapshot.clubId) fixture.awayClubName else fixture.homeClubName

    private fun userIsBattingNow(): Boolean =
        (currentInnings == 1 && userBatFirst) || (currentInnings == 2 && !userBatFirst)

    private fun initBatterMap(lineup: List<Player>): MutableMap<String, BatterScoreEntry> {
        val map = linkedMapOf<String, BatterScoreEntry>()
        lineup.forEach { p ->
            map[p.id] = BatterScoreEntry(
                playerId = p.id,
                playerName = p.name,
                roleLabel = p.role.shortLabel
            )
        }
        return map
    }

    private fun pickBowlerForOver(overIndex: Int, lastBowlerId: String?): Player {
        val phase = phaseForBalls(overIndex * 6)
        val candidates = bowlingLineup.filter {
            it.id != lastBowlerId && (bowlerOverCounts[it.id] ?: 0) < 4
        }.ifEmpty {
            bowlingLineup.filter { (bowlerOverCounts[it.id] ?: 0) < 4 }.ifEmpty { bowlingLineup }
        }
        return candidates.maxByOrNull { p ->
            val bowlSkill = if (p.bowlingStyle.isPace) p.attributes.paceBowling else p.attributes.spinVariation
            val phaseBonus = when (phase) {
                MatchPhase.POWERPLAY -> if (p.bowlingStyle.isPace) 18 else 4
                MatchPhase.MIDDLE -> if (!p.bowlingStyle.isPace && p.bowlingStyle.canBowl) 18 else 6
                MatchPhase.DEATH -> p.attributes.deathExecution / 3
            }
            val roleBonus = if (p.canBowl) 35 else -25
            bowlSkill + phaseBonus + roleBonus + random.nextInt(0, 7)
        } ?: bowlingLineup.last()
    }

    fun applyIntervention(
        tempo: BattingTempo?,
        bowlingPlan: BowlingPlan?,
        fieldSpread: FieldSpread?,
        impactPlayerId: String?,
        teamTalkTone: TeamTalkTone? = null
    ) {
        if (tempo != null) currentTempo = tempo
        if (bowlingPlan != null) currentBowlingPlan = bowlingPlan
        if (fieldSpread != null) currentFieldSpread = fieldSpread
        if (teamTalkTone != null) {
            inningsBreakTeamTalk = teamTalkTone
            commentaryLog.add(0, "Innings Break Talk (${teamTalkTone.label}): ${teamTalkTone.speechLine(TeamTalkStage.INNINGS_BREAK)}")
            keyHighlights.add("Innings Break Huddle: ${teamTalkTone.label}")
        }
        if (impactPlayerId != null && !impactSubUsed && impactSub?.id == impactPlayerId) {
            val sub = impactSub!!
            impactSubUsed = true
            if (userIsBattingNow()) {
                // Insert impact sub as next batter if slots remain
                if (nextBatterIdx < battingLineup.size) {
                    battingLineup[nextBatterIdx] = sub
                    batterMap[sub.id] = BatterScoreEntry(sub.id, sub.name, sub.role.shortLabel)
                    commentaryLog.add(0, "IMPACT SUB: ${sub.name} enters the batting order!")
                    keyHighlights.add("Impact Sub ${sub.name} brought into the chase")
                }
            } else {
                // Replace the least-used non-bowling or weakest bowler
                val replaceIdx = bowlingLineup.indexOfLast { it.id != currentBowlerId && (bowlerOverCounts[it.id] ?: 0) < 2 }
                    .takeIf { it >= 0 } ?: bowlingLineup.lastIndex
                bowlingLineup[replaceIdx] = sub
                commentaryLog.add(0, "IMPACT SUB: ${sub.name} joins the bowling attack!")
                keyHighlights.add("Impact Sub ${sub.name} deployed with the ball")
            }
        }
    }

    fun applyLiveTactics(
        tempo: BattingTempo?,
        bowlingPlan: BowlingPlan?,
        fieldSpread: FieldSpread?,
        impactPlayerId: String?,
        teamTalkTone: TeamTalkTone? = null
    ): LiveMatchState {
        applyIntervention(tempo, bowlingPlan, fieldSpread, impactPlayerId, teamTalkTone)
        return toUiState()
    }

    private fun recommendedInningsBreakTalk(): TeamTalkTone {
        val tgt = target ?: 160
        return when {
            userIsBattingNow() && tgt >= 175 -> TeamTalkTone.AGGRESSIVE
            userIsBattingNow() -> TeamTalkTone.ENCOURAGING
            tgt <= 145 -> TeamTalkTone.CALM
            else -> TeamTalkTone.ASSERTIVE
        }
    }

    fun stepBalls(maxBalls: Int): LiveMatchState {
        val delegateTactics = careerSnapshot.delegation.tacticsDelegated
        val headCoachRating = careerSnapshot.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
        var remaining = maxBalls.coerceAtLeast(1)

        while (remaining > 0) {
            val targetReached = target != null && runsInInnings >= target!!
            if (ballsBowledInInnings >= 120 || wicketsInInnings >= 10 || targetReached) {
                val scorecard = buildInningsScorecard()
                if (currentInnings == 1) {
                    firstInningsScorecard = scorecard
                    target = runsInInnings + 1
                    commentaryLog.add(
                        0,
                        "End of 1st Innings: ${scorecard.battingClubName} ${scorecard.runs}/${scorecard.wickets} (${scorecard.oversFormatted}). Target: $target."
                    )
                    keyHighlights.add("${scorecard.battingClubName} posted ${scorecard.runs}/${scorecard.wickets}")
                    switchToSecondInnings()
                    if (!delegateTactics) {
                        return toUiState(
                            pendingMoment = KeyMomentPrompt(
                                momentIndex = 99,
                                title = "Innings Break • Target $target",
                                description = "Deliver your Innings Break Team Talk and set your opening ${if (userIsBattingNow()) "chase tempo" else "defensive bowling blueprint"} for the 2nd innings.",
                                phase = MatchPhase.POWERPLAY,
                                isUserBatting = userIsBattingNow(),
                                staffTip = buildStaffTip(MatchPhase.POWERPLAY, headCoachRating),
                                recommendedTempo = if (userIsBattingNow()) BattingTempo.AGGRESSIVE else null,
                                recommendedBowlingPlan = if (!userIsBattingNow()) BowlingPlan.LINE_AND_LENGTH else null,
                                recommendedField = if (!userIsBattingNow()) FieldSpread.ATTACKING else null,
                                isInningsBreak = true,
                                recommendedTeamTalk = recommendedInningsBreakTalk()
                            )
                        )
                    }
                    continue
                } else {
                    secondInningsScorecard = scorecard
                    val record = finalizeMatchRecord()
                    return toUiState(pendingMoment = null, finishedRecord = record)
                }
            }

            if (delegateTactics && ballsBowledInInnings % 18 == 0) {
                val phase = phaseForBalls(ballsBowledInInnings)
                val reqRate = target?.let { t ->
                    val rem = (120 - ballsBowledInInnings).coerceAtLeast(1)
                    ((t - runsInInnings).coerceAtLeast(0) * 6.0) / rem
                }
                val (recT, recB, recF) = StaffAdvisorEngine.recommendLiveTactics(
                    pitchType = fixture.pitchType,
                    phase = phase,
                    isUserBatting = userIsBattingNow(),
                    wicketsLost = wicketsInInnings,
                    requiredRate = reqRate,
                    headCoachRating = headCoachRating
                )
                if (recT != null) currentTempo = recT
                if (recB != null) currentBowlingPlan = recB
                if (recF != null) currentFieldSpread = recF
            }

            val checkpointKey = currentInnings * 1000 + ballsBowledInInnings
            if (!delegateTactics && ballsBowledInInnings in momentCheckpoints && triggeredMoments.add(checkpointKey)) {
                return toUiState(pendingMoment = buildKeyMomentPrompt(checkpointKey, headCoachRating))
            }

            simulateSingleBall()
            remaining -= 1
        }
        return toUiState()
    }

    private fun buildKeyMomentPrompt(checkpointKey: Int, headCoachRating: Int): KeyMomentPrompt {
        val phase = phaseForBalls(ballsBowledInInnings)
        val reqRate = target?.let { t ->
            val rem = (120 - ballsBowledInInnings).coerceAtLeast(1)
            ((t - runsInInnings).coerceAtLeast(0) * 6.0) / rem
        }
        val (recT, recB, recF) = StaffAdvisorEngine.recommendLiveTactics(
            pitchType = fixture.pitchType,
            phase = phase,
            isUserBatting = userIsBattingNow(),
            wicketsLost = wicketsInInnings,
            requiredRate = reqRate,
            headCoachRating = headCoachRating
        )
        val title = when (ballsBowledInInnings) {
            18 -> "Powerplay Pulse (3.0 Ov)"
            36 -> "End of Powerplay (6.0 Ov)"
            66 -> "Middle-Overs Turning Point (11.0 Ov)"
            96 -> "Death Overs Launch (16.0 Ov)"
            else -> "Final-Overs Crunch (18.3 Ov)"
        }
        val desc = if (target != null) {
            val need = (target!! - runsInInnings).coerceAtLeast(0)
            val ballsLeft = 120 - ballsBowledInInnings
            "Score $runsInInnings/$wicketsInInnings • Need $need runs off $ballsLeft balls."
        } else {
            "Score $runsInInnings/$wicketsInInnings after ${ballsBowledInInnings / 6}.${ballsBowledInInnings % 6} overs."
        }
        return KeyMomentPrompt(
            momentIndex = checkpointKey,
            title = title,
            description = desc,
            phase = phase,
            isUserBatting = userIsBattingNow(),
            staffTip = buildStaffTip(phase, headCoachRating),
            recommendedTempo = recT,
            recommendedBowlingPlan = recB,
            recommendedField = recF
        )
    }

    fun advanceUntilNextPause(instantFinishAll: Boolean): LiveMatchState {
        val delegateTactics = careerSnapshot.delegation.tacticsDelegated || instantFinishAll
        val headCoachRating = careerSnapshot.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65

        while (true) {
            // Check if innings ended
            val targetReached = target != null && runsInInnings >= target!!
            if (ballsBowledInInnings >= 120 || wicketsInInnings >= 10 || targetReached) {
                val scorecard = buildInningsScorecard()
                if (currentInnings == 1) {
                    firstInningsScorecard = scorecard
                    target = runsInInnings + 1
                    commentaryLog.add(
                        0,
                        "End of 1st Innings: ${scorecard.battingClubName} ${scorecard.runs}/${scorecard.wickets} (${scorecard.oversFormatted}). Target: $target."
                    )
                    keyHighlights.add("${scorecard.battingClubName} posted ${scorecard.runs}/${scorecard.wickets}")
                    switchToSecondInnings()
                    if (!delegateTactics) {
                        return toUiState(
                            pendingMoment = KeyMomentPrompt(
                                momentIndex = 99,
                                title = "Innings Break • Target $target",
                                description = "Deliver your Innings Break Team Talk and set your opening ${if (userIsBattingNow()) "chase tempo" else "defensive bowling blueprint"} for the 2nd innings.",
                                phase = MatchPhase.POWERPLAY,
                                isUserBatting = userIsBattingNow(),
                                staffTip = buildStaffTip(MatchPhase.POWERPLAY, headCoachRating),
                                recommendedTempo = if (userIsBattingNow()) BattingTempo.AGGRESSIVE else null,
                                recommendedBowlingPlan = if (!userIsBattingNow()) BowlingPlan.LINE_AND_LENGTH else null,
                                recommendedField = if (!userIsBattingNow()) FieldSpread.ATTACKING else null,
                                isInningsBreak = true,
                                recommendedTeamTalk = recommendedInningsBreakTalk()
                            )
                        )
                    }
                    continue
                } else {
                    secondInningsScorecard = scorecard
                    val record = finalizeMatchRecord()
                    return toUiState(pendingMoment = null, finishedRecord = record)
                }
            }

            // Delegated tactics auto-adjust every 3 overs
            if (delegateTactics && ballsBowledInInnings % 18 == 0) {
                val phase = phaseForBalls(ballsBowledInInnings)
                val reqRate = target?.let { t ->
                    val rem = (120 - ballsBowledInInnings).coerceAtLeast(1)
                    ((t - runsInInnings).coerceAtLeast(0) * 6.0) / rem
                }
                val (recT, recB, recF) = StaffAdvisorEngine.recommendLiveTactics(
                    pitchType = fixture.pitchType,
                    phase = phase,
                    isUserBatting = userIsBattingNow(),
                    wicketsLost = wicketsInInnings,
                    requiredRate = reqRate,
                    headCoachRating = headCoachRating
                )
                if (recT != null) currentTempo = recT
                if (recB != null) currentBowlingPlan = recB
                if (recF != null) currentFieldSpread = recF
            }

            // Key-moment pause check before simulating the ball
            val checkpointKey = currentInnings * 1000 + ballsBowledInInnings
            if (!delegateTactics && ballsBowledInInnings in momentCheckpoints && triggeredMoments.add(checkpointKey)) {
                return toUiState(
                    pendingMoment = buildKeyMomentPrompt(checkpointKey, headCoachRating)
                )
            }

            simulateSingleBall()
        }
    }

    private fun buildStaffTip(phase: MatchPhase, coachRating: Int): String {
        val coachName = careerSnapshot.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.name ?: "Head Coach"
        return if (userIsBattingNow()) {
            val req = target?.let { ((it - runsInInnings).coerceAtLeast(0) * 6.0) / (120 - ballsBowledInInnings).coerceAtLeast(1) }
            when {
                req != null && req > 11.0 -> "$coachName ($coachRating): Required rate is ${"%.1f".format(req)} — shift to All-Out Attack and target boundaries."
                wicketsInInnings >= 5 && ballsBowledInInnings < 90 -> "$coachName ($coachRating): Five down early — Anchor for 2 overs to protect the tail."
                phase == MatchPhase.DEATH -> "$coachName ($coachRating): Death overs unlocked — push Aggressive or All-Out if set batters are in."
                else -> "$coachName ($coachRating): Pitch is ${fixture.pitchType.label} — Balanced or Aggressive pacing suits this phase."
            }
        } else {
            when (phase) {
                MatchPhase.POWERPLAY -> "$coachName ($coachRating): Keep Attacking Ring inside the 30-yard circle for early breakthroughs."
                MatchPhase.MIDDLE -> "$coachName ($coachRating): Choke singles in the middle overs with Spin Trap or Cutters."
                MatchPhase.DEATH -> "$coachName ($coachRating): Switch to Yorker Pinpoint with Boundary Riders to cut off sixes."
            }
        }
    }

    private fun switchToSecondInnings() {
        currentInnings = 2
        ballsBowledInInnings = 0
        runsInInnings = 0
        wicketsInInnings = 0
        battingLineup = if (userBatFirst) opponentXI.toMutableList() else userXI.toMutableList()
        bowlingLineup = if (userBatFirst) userXI.toMutableList() else opponentXI.toMutableList()
        strikerIdx = 0
        nonStrikerIdx = 1
        nextBatterIdx = 2
        batterMap = initBatterMap(battingLineup)
        bowlerMap = mutableMapOf()
        bowlerOverCounts = mutableMapOf()
        currentBowlerId = pickBowlerForOver(0, null).id
        currentOverRuns = 0
        recentBalls.clear()
        runRateHistory.clear()
        currentTempo = careerSnapshot.defaultTempo
        currentBowlingPlan = careerSnapshot.defaultBowlingPlan
        currentFieldSpread = careerSnapshot.defaultFieldSpread
    }

    private fun simulateSingleBall() {
        val striker = battingLineup[strikerIdx]
        val bowler = bowlingLineup.firstOrNull { it.id == currentBowlerId } ?: bowlingLineup.last()
        val phase = phaseForBalls(ballsBowledInInnings)

        val liveStrikerFit = matchFitnessMap[striker.id] ?: striker.fitness
        val liveBowlerFit = matchFitnessMap[bowler.id] ?: bowler.fitness
        val fieldingAvgFit = bowlingLineup.map { matchFitnessMap[it.id] ?: it.fitness }.average().roundToInt()

        val talkMod = T20MatchEngine.teamTalkModifiers(activeTeamTalk())
        val effStrikerFit = (liveStrikerFit + if (userIsBattingNow()) talkMod.composureFitnessBonus else 0).coerceIn(45, 100)
        val effBowlerFit = (liveBowlerFit + if (!userIsBattingNow()) talkMod.composureFitnessBonus else 0).coerceIn(45, 100)

        val matchup = T20MatchEngine.calculateMatchupFactors(
            striker = striker.copy(fitness = effStrikerFit),
            bowler = bowler.copy(fitness = effBowlerFit),
            pitchType = fixture.pitchType,
            phase = phase,
            fieldingAvgFitness = fieldingAvgFit
        )

        val tempoMult = if (userIsBattingNow()) currentTempo.runBoost else aiTempoForState(phase).runBoost
        val tempoWktMult = if (userIsBattingNow()) currentTempo.wicketRisk else aiTempoForState(phase).wicketRisk
        val bowlEconMult = if (!userIsBattingNow()) currentBowlingPlan.economyFactor else 1.0
        val fieldBoundaryMult = if (!userIsBattingNow()) currentFieldSpread.boundaryFactor else 1.0
        val bowlWktMult = if (!userIsBattingNow()) {
            currentBowlingPlan.wicketFactor * currentFieldSpread.wicketFactor
        } else 1.0
        val talkBatRunMult = if (userIsBattingNow()) talkMod.battingRunMult else 1.0
        val talkBatWktMult = if (userIsBattingNow()) talkMod.battingWicketMult else 1.0
        val talkBowlEconMult = if (!userIsBattingNow()) talkMod.bowlingEconMult else 1.0
        val talkBowlWktMult = if (!userIsBattingNow()) talkMod.bowlingWktMult else 1.0

        val phaseRunMult = when (phase) {
            MatchPhase.POWERPLAY -> 1.08
            MatchPhase.MIDDLE -> 0.94
            MatchPhase.DEATH -> 1.24
        }

        // Wide ball check increases slightly if bowler is fatigued
        val wideProb = (0.030 + ((100 - effBowlerFit).coerceAtLeast(0) * 0.0005)).coerceAtMost(0.055)
        if (random.nextDouble() < wideProb) {
            runsInInnings += 1
            val bEntry = bowlerMap[bowler.id] ?: BowlerScoreEntry(bowler.id, bowler.name)
            bowlerMap[bowler.id] = bEntry.copy(runsConceded = bEntry.runsConceded + 1)
            recentBalls.add("Wd")
            if (recentBalls.size > 12) recentBalls.removeAt(0)
            val ovLabel = "${ballsBowledInInnings / 6}.${ballsBowledInInnings % 6}"
            val wideNote = if (liveBowlerFit < 74) {
                "${bowler.name} (Fit $liveBowlerFit%) strays down leg under fatigue — 1 wide."
            } else {
                "${bowler.name} pushes it wide outside off — 1 extra."
            }
            pushBallEvent(
                overBall = ovLabel,
                symbol = "Wd",
                batterName = striker.name,
                bowlerName = bowler.name,
                runsScored = 1,
                isWicket = false,
                isBoundary = false,
                commentary = wideNote
            )
            recordRunRatePoint()
            return
        }

        val skillEdge = matchup.skillEdge
        val wicketProb = (0.044 * tempoWktMult * bowlWktMult * talkBatWktMult * talkBowlWktMult * (1.0 - skillEdge * 0.85)).coerceIn(0.015, 0.145)
        val boundaryScale = (fixture.pitchType.runBias * phaseRunMult * tempoMult * bowlEconMult * fieldBoundaryMult * talkBatRunMult * talkBowlEconMult * (1.0 + skillEdge)).coerceIn(0.55, 1.88)
        val sixProb = (0.062 * boundaryScale).coerceIn(0.02, 0.21)
        val fourProb = (0.125 * boundaryScale).coerceIn(0.05, 0.29)
        val twoProb = (0.095 * matchup.strikerFitnessFactor).coerceIn(0.06, 0.12)
        val oneProb = (0.345 * (1.0 / fieldBoundaryMult.coerceAtLeast(0.8))).coerceIn(0.24, 0.44)

        ballsBowledInInnings += 1
        val bPrev = bowlerMap[bowler.id] ?: BowlerScoreEntry(bowler.id, bowler.name)
        val sPrev = batterMap[striker.id] ?: BatterScoreEntry(striker.id, striker.name, striker.role.shortLabel)

        val ovBall = oversLabel()
        val roll = random.nextDouble()
        val strikerLast = striker.name.split(" ").last()
        val bowlerLast = bowler.name.split(" ").last()

        when {
            roll < wicketProb -> {
                wicketsInInnings += 1
                val dismissal = listOf(
                    "c & b $bowlerLast",
                    "b $bowlerLast",
                    "lbw b $bowlerLast",
                    "caught deep"
                ).random(random)
                batterMap[striker.id] = sPrev.copy(balls = sPrev.balls + 1, isOut = true, dismissalText = dismissal)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, wickets = bPrev.wickets + 1, dotBalls = bPrev.dotBalls + 1)
                recordBallSymbol("W")
                val fitCue = if (liveStrikerFit < 75) " (fatigue at $liveStrikerFit% fitness)" else ""
                val msg = "WICKET! $bowlerLast removes ${striker.name} ($dismissal) for ${sPrev.runs} (${sPrev.balls})$fitCue!"
                commentaryLog.add(0, "$ovBall $msg")
                pushBallEvent(ovBall, "W", striker.name, bowler.name, 0, isWicket = true, isBoundary = false, commentary = msg)
                if (nextBatterIdx < battingLineup.size) {
                    strikerIdx = nextBatterIdx
                    nextBatterIdx += 1
                }
            }
            roll < wicketProb + sixProb -> {
                runsInInnings += 6
                currentOverRuns += 6
                batterMap[striker.id] = sPrev.copy(runs = sPrev.runs + 6, balls = sPrev.balls + 1, sixes = sPrev.sixes + 1)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, runsConceded = bPrev.runsConceded + 6)
                recordBallSymbol("6")
                val shot = if (bowler.bowlingStyle.isPace) "dispatches the pace into the stands" else "launches the spinner clean over long-on"
                val msg = "SIX! $strikerLast (PWR ${striker.attributes.powerHitting}) $shot off $bowlerLast!"
                pushBallEvent(ovBall, "6", striker.name, bowler.name, 6, isWicket = false, isBoundary = true, commentary = msg)
                checkBatterMilestone(batterMap.getValue(striker.id))
            }
            roll < wicketProb + sixProb + fourProb -> {
                runsInInnings += 4
                currentOverRuns += 4
                batterMap[striker.id] = sPrev.copy(runs = sPrev.runs + 4, balls = sPrev.balls + 1, fours = sPrev.fours + 1)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, runsConceded = bPrev.runsConceded + 4)
                recordBallSymbol("4")
                val zone = listOf("through extra cover", "past backward point", "down the ground", "fine past short fine leg").random(random)
                val msg = "FOUR! $strikerLast threads $bowlerLast $zone."
                pushBallEvent(ovBall, "4", striker.name, bowler.name, 4, isWicket = false, isBoundary = true, commentary = msg)
                checkBatterMilestone(batterMap.getValue(striker.id))
            }
            roll < wicketProb + sixProb + fourProb + twoProb -> {
                runsInInnings += 2
                currentOverRuns += 2
                batterMap[striker.id] = sPrev.copy(runs = sPrev.runs + 2, balls = sPrev.balls + 1)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, runsConceded = bPrev.runsConceded + 2)
                recordBallSymbol("2")
                val msg = "$strikerLast tucks $bowlerLast into the gap and sprints hard for TWO (Fit $liveStrikerFit%)."
                pushBallEvent(ovBall, "2", striker.name, bowler.name, 2, isWicket = false, isBoundary = false, commentary = msg)
            }
            roll < wicketProb + sixProb + fourProb + twoProb + oneProb -> {
                runsInInnings += 1
                currentOverRuns += 1
                batterMap[striker.id] = sPrev.copy(runs = sPrev.runs + 1, balls = sPrev.balls + 1)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, runsConceded = bPrev.runsConceded + 1)
                recordBallSymbol("1")
                val msg = "$strikerLast rotates strike with a crisp single off $bowlerLast."
                pushBallEvent(ovBall, "1", striker.name, bowler.name, 1, isWicket = false, isBoundary = false, commentary = msg)
                swapStrike()
            }
            else -> {
                batterMap[striker.id] = sPrev.copy(balls = sPrev.balls + 1)
                bowlerMap[bowler.id] = bPrev.copy(ballsBowled = bPrev.ballsBowled + 1, dotBalls = bPrev.dotBalls + 1)
                recordBallSymbol("0")
                val delivery = if (phase == MatchPhase.DEATH) "nailing a sharp death yorker" else if (bowler.bowlingStyle.isPace) "hitting a heavy seam length" else "deceiving with subtle drift"
                val msg = "Dot ball — $bowlerLast beats the bat $delivery."
                pushBallEvent(ovBall, "0", striker.name, bowler.name, 0, isWicket = false, isBoundary = false, commentary = msg)
            }
        }

        // Subtle in-match stamina drain every 6 balls for active bowler & striker
        if (ballsBowledInInnings % 6 == 0) {
            matchFitnessMap[bowler.id] = (liveBowlerFit - 1).coerceAtLeast(45)
            matchFitnessMap[striker.id] = (liveStrikerFit - 1).coerceAtLeast(45)
            if (currentOverRuns == 0) {
                val finishedBowler = bowlerMap[bowler.id] ?: BowlerScoreEntry(bowler.id, bowler.name)
                bowlerMap[bowler.id] = finishedBowler.copy(maidens = finishedBowler.maidens + 1)
            }
            currentOverRuns = 0
        }

        // End of over
        if (ballsBowledInInnings % 6 == 0 && ballsBowledInInnings < 120) {
            bowlerOverCounts[bowler.id] = (bowlerOverCounts[bowler.id] ?: 0) + 1
            swapStrike()
            currentBowlerId = pickBowlerForOver(ballsBowledInInnings / 6, bowler.id).id
        }

        recordRunRatePoint()
    }

    private fun recordRunRatePoint() {
        val ballNum = ballsBowledInInnings.coerceAtLeast(1)
        val crr = if (ballsBowledInInnings > 0) {
            (runsInInnings * 6.0) / ballsBowledInInnings
        } else {
            runsInInnings * 6.0
        }
        val rrr = target?.let { t ->
            val remainingBalls = (120 - ballsBowledInInnings).coerceAtLeast(1)
            val needed = (t - runsInInnings).coerceAtLeast(0)
            (needed * 6.0) / remainingBalls
        }
        val point = RunRatePoint(ball = ballNum, crr = crr, rrr = rrr)
        if (runRateHistory.lastOrNull()?.ball == ballNum) {
            runRateHistory[runRateHistory.lastIndex] = point
        } else {
            runRateHistory.add(point)
        }
    }

    private fun pushBallEvent(
        overBall: String,
        symbol: String,
        batterName: String,
        bowlerName: String,
        runsScored: Int,
        isWicket: Boolean,
        isBoundary: Boolean,
        commentary: String
    ) {
        ballSeq += 1
        val event = BallEvent(
            id = "b_${currentInnings}_$ballSeq",
            innings = currentInnings,
            overBallText = overBall,
            symbol = symbol,
            batterName = batterName,
            bowlerName = bowlerName,
            runsScored = runsScored,
            isWicket = isWicket,
            isBoundary = isBoundary,
            commentary = commentary,
            scoreAfterBall = "$runsInInnings/$wicketsInInnings"
        )
        ballFeed.add(0, event)
        if (ballFeed.size > 120) {
            ballFeed.removeAt(ballFeed.lastIndex)
        }
    }

    private fun checkBatterMilestone(entry: BatterScoreEntry) {
        if (entry.runs in 50..55 && commentaryLog.none { it.contains("${entry.playerName} reaches 50") }) {
            commentaryLog.add(0, "${oversLabel()} ${entry.playerName} reaches 50 off ${entry.balls} balls!")
            keyHighlights.add("${entry.playerName} smashed ${entry.runs} (${entry.balls})")
        }
    }

    private fun recordBallSymbol(sym: String) {
        recentBalls.add(sym)
        if (recentBalls.size > 12) recentBalls.removeAt(0)
    }

    private fun swapStrike() {
        val temp = strikerIdx
        strikerIdx = nonStrikerIdx
        nonStrikerIdx = temp
    }

    private fun oversLabel(): String = "${ballsBowledInInnings / 6}.${ballsBowledInInnings % 6}"

    private fun aiTempoForState(phase: MatchPhase): BattingTempo {
        val req = target?.let { ((it - runsInInnings).coerceAtLeast(0) * 6.0) / (120 - ballsBowledInInnings).coerceAtLeast(1) }
        return when {
            req != null && req > 10.5 -> BattingTempo.ALL_OUT
            phase == MatchPhase.DEATH && wicketsInInnings < 6 -> BattingTempo.ALL_OUT
            phase == MatchPhase.POWERPLAY -> BattingTempo.AGGRESSIVE
            wicketsInInnings >= 5 -> BattingTempo.ANCHOR
            else -> BattingTempo.BALANCED
        }
    }

    private fun phaseForBalls(balls: Int): MatchPhase = when {
        balls < 36 -> MatchPhase.POWERPLAY
        balls < 90 -> MatchPhase.MIDDLE
        else -> MatchPhase.DEATH
    }

    private fun buildInningsScorecard(): InningsScorecard {
        val batName = if (userIsBattingNow()) careerSnapshot.clubName else opponentName()
        val bowlName = if (userIsBattingNow()) opponentName() else careerSnapshot.clubName
        return InningsScorecard(
            battingClubName = batName,
            bowlingClubName = bowlName,
            runs = runsInInnings,
            wickets = wicketsInInnings,
            balls = ballsBowledInInnings,
            batters = batterMap.values.filter { it.balls > 0 || it.isOut }.ifEmpty { batterMap.values.take(2) },
            bowlers = bowlerMap.values.toList()
        )
    }

    private fun finalizeMatchRecord(): MatchRecord {
        val first = firstInningsScorecard!!
        val second = secondInningsScorecard!!
        val firstBatClubIsUser = userBatFirst
        val userScore = if (firstBatClubIsUser) first else second
        val oppScore = if (firstBatClubIsUser) second else first
        val userWon = userScore.runs > oppScore.runs ||
            (userScore.runs == oppScore.runs && userScore.wickets <= oppScore.wickets)

        val resultText = if (firstBatClubIsUser) {
            if (userWon) {
                val margin = (first.runs - second.runs).coerceAtLeast(1)
                "${careerSnapshot.clubName} won by $margin runs"
            } else {
                val wktLeft = (10 - second.wickets).coerceAtLeast(1)
                "${opponentName()} won by $wktLeft wickets"
            }
        } else {
            if (userWon) {
                val wktLeft = (10 - second.wickets).coerceAtLeast(1)
                "${careerSnapshot.clubName} won by $wktLeft wickets"
            } else {
                val margin = (first.runs - second.runs).coerceAtLeast(1)
                "${opponentName()} won by $margin runs"
            }
        }

        val allBatters = first.batters + second.batters
        val allBowlers = first.bowlers + second.bowlers
        val topBat = allBatters.maxByOrNull { it.runs + it.sixes * 2 }
        val topBowl = allBowlers.maxByOrNull { it.wickets * 24 - it.runsConceded }
        val motm = if ((topBat?.runs ?: 0) >= ((topBowl?.wickets ?: 0) * 22)) {
            "${topBat?.playerName ?: "MVP"} (${topBat?.runs ?: 0}*)"
        } else {
            "${topBowl?.playerName ?: "MVP"} (${topBowl?.wickets ?: 0}/${topBowl?.runsConceded ?: 0})"
        }

        val isHome = fixture.homeClubId == careerSnapshot.clubId
        val attendance = if (isHome) {
            (careerSnapshot.stadiumCapacity * careerSnapshot.budgets.ticketTier.attendanceFactor).roundToInt()
        } else 0
        val gateRevenue = attendance * careerSnapshot.budgets.ticketTier.pricePerSeat
        val userBowlers = if (firstBatClubIsUser) second.bowlers else first.bowlers
        val sponsorBreakdown = SponsorNegotiationEngine.evaluateMatchSponsorPayout(
            sponsors = careerSnapshot.sponsors,
            userWon = userWon,
            userBatters = userScore.batters,
            userBowlers = userBowlers
        )
        val combinedHighlights = keyHighlights.toMutableList()
        if (sponsorBreakdown.individualMilestoneBonus > 0) {
            combinedHighlights.add(
                0,
                "Sponsor Milestones: +$${sponsorBreakdown.individualMilestoneBonus} (${sponsorBreakdown.fiftiesCount}x50s, ${sponsorBreakdown.centuriesCount}x100s, ${sponsorBreakdown.threeWicketHaulsCount}x3W)"
            )
        }

        return MatchRecord(
            matchId = fixture.id,
            season = careerSnapshot.season,
            round = fixture.round,
            label = if (fixture.isPlayoff) fixture.playoffLabel else "Round ${fixture.round}",
            pitchType = fixture.pitchType,
            userClubName = careerSnapshot.clubName,
            opponentName = opponentName(),
            userWon = userWon,
            resultText = resultText,
            manOfTheMatch = motm,
            firstInnings = first,
            secondInnings = second,
            keyHighlights = combinedHighlights.take(5).ifEmpty { listOf(resultText, "MOTM: $motm") },
            revenueEarned = gateRevenue + sponsorBreakdown.totalBonus,
            ticketSalesRevenue = gateRevenue,
            sponsorBonusEarned = sponsorBreakdown.totalBonus,
            winSponsorBonus = sponsorBreakdown.winBonus,
            individualMilestoneBonus = sponsorBreakdown.individualMilestoneBonus,
            attendance = attendance
        )
    }

    fun toUiState(
        pendingMoment: KeyMomentPrompt? = null,
        finishedRecord: MatchRecord? = null
    ): LiveMatchState {
        val strikerPlayer = battingLineup.getOrNull(strikerIdx)
        val nonStrikerPlayer = battingLineup.getOrNull(nonStrikerIdx)
        val activeBowlerPlayer = bowlingLineup.firstOrNull { it.id == currentBowlerId } ?: bowlingLineup.last()
        val activeB = listOfNotNull(
            strikerPlayer?.let { batterMap[it.id] },
            nonStrikerPlayer?.let { batterMap[it.id] }
        )
        val activeBowl = bowlerMap[currentBowlerId] ?: bowlingLineup.firstOrNull { it.id == currentBowlerId }?.let {
            BowlerScoreEntry(it.id, it.name)
        }
        val fitnessMap = buildMap {
            strikerPlayer?.let { put(it.id, matchFitnessMap[it.id] ?: it.fitness) }
            nonStrikerPlayer?.let { put(it.id, matchFitnessMap[it.id] ?: it.fitness) }
        }
        val bowlerFit = matchFitnessMap[activeBowlerPlayer.id] ?: activeBowlerPlayer.fitness
        val phase = phaseForBalls(ballsBowledInInnings)
        val matchupText = if (strikerPlayer != null) {
            val mf = T20MatchEngine.calculateMatchupFactors(
                striker = strikerPlayer.copy(fitness = matchFitnessMap[strikerPlayer.id] ?: strikerPlayer.fitness),
                bowler = activeBowlerPlayer.copy(fitness = bowlerFit),
                pitchType = fixture.pitchType,
                phase = phase
            )
            val edgeStr = "%+.1f".format(mf.batRating - mf.bowlRating)
            "${phase.label}: ${strikerPlayer.name.split(" ").last()} (${mf.batRating.roundToInt()}) vs ${activeBowlerPlayer.name.split(" ").last()} (${mf.bowlRating.roundToInt()}) • Edge $edgeStr"
        } else ""

        return LiveMatchState(
            fixture = fixture,
            userBatFirst = userBatFirst,
            currentInnings = currentInnings,
            userIsBattingNow = userIsBattingNow(),
            runs = runsInInnings,
            wickets = wicketsInInnings,
            balls = ballsBowledInInnings,
            target = target,
            recentBalls = recentBalls.toList(),
            activeBatters = activeB,
            activeBowler = activeBowl,
            firstInningsSummary = firstInningsScorecard,
            secondInningsSummary = secondInningsScorecard,
            commentaryLog = commentaryLog.take(18),
            ballFeed = ballFeed.toList(),
            runRateHistory = runRateHistory.toList(),
            activeBatterFitness = fitnessMap,
            activeBowlerFitness = bowlerFit,
            matchupSummary = matchupText,
            currentTempo = currentTempo,
            currentBowlingPlan = currentBowlingPlan,
            currentFieldSpread = currentFieldSpread,
            preMatchTeamTalk = preMatchTeamTalk,
            inningsBreakTeamTalk = inningsBreakTeamTalk,
            postMatchTeamTalkDelivered = postMatchTeamTalkDelivered,
            availableImpactSubs = if (!impactSubUsed && impactSub != null) listOf(impactSub!!) else emptyList(),
            impactSubUsed = impactSubUsed,
            pendingKeyMoment = pendingMoment,
            finishedRecord = finishedRecord
        )
    }
}

data class MatchupFactors(
    val batRating: Double,
    val bowlRating: Double,
    val strikerFitnessFactor: Double,
    val bowlerFitnessFactor: Double,
    val skillEdge: Double
)

data class TeamTalkModifier(
    val battingRunMult: Double,
    val battingWicketMult: Double,
    val bowlingEconMult: Double,
    val bowlingWktMult: Double,
    val composureFitnessBonus: Int
)

object T20MatchEngine {
    fun teamTalkModifiers(tone: TeamTalkTone?): TeamTalkModifier {
        if (tone == null) return TeamTalkModifier(1.0, 1.0, 1.0, 1.0, 0)
        return TeamTalkModifier(
            battingRunMult = (1.0 + tone.runRateDelta).coerceIn(0.85, 1.25),
            battingWicketMult = (1.0 + tone.wicketRiskDelta).coerceIn(0.80, 1.25),
            bowlingEconMult = (1.0 + tone.bowlingEconDelta).coerceIn(0.85, 1.20),
            bowlingWktMult = (1.0 + tone.bowlingWktDelta).coerceIn(0.85, 1.25),
            composureFitnessBonus = tone.composureFitnessBonus
        )
    }

    fun calculateMatchupFactors(
        striker: Player,
        bowler: Player,
        pitchType: PitchType,
        phase: MatchPhase,
        fieldingAvgFitness: Int = 85
    ): MatchupFactors {
        val deathPenaltyScale = if (phase == MatchPhase.DEATH) 1.25 else 1.0
        val strikerFitDelta = ((striker.fitness - 82) / 260.0) * deathPenaltyScale
        val strikerFitFactor = (1.0 + strikerFitDelta).coerceIn(0.80, 1.05)

        val bowlerFitDelta = ((bowler.fitness - 82) / 240.0) * deathPenaltyScale
        val bowlerFitFactor = (1.0 + bowlerFitDelta).coerceIn(0.80, 1.05)

        val baseBat = striker.attributes.battingTiming * 0.55 + striker.attributes.powerHitting * 0.45
        val batRating = baseBat * (0.90 + striker.form / 500.0) * strikerFitFactor

        val rawBowl = if (bowler.bowlingStyle.isPace) {
            bowler.attributes.paceBowling * pitchType.paceBias
        } else if (bowler.bowlingStyle.canBowl) {
            bowler.attributes.spinVariation * pitchType.spinBias
        } else {
            38.0
        }
        val baseBowl = rawBowl * 0.72 + bowler.attributes.deathExecution * 0.28
        val fieldFactor = (0.96 + fieldingAvgFitness / 2000.0).coerceIn(0.95, 1.02)
        val bowlRating = baseBowl * (0.90 + bowler.form / 500.0) * bowlerFitFactor * fieldFactor

        val skillEdge = ((batRating - bowlRating) / 135.0).coerceIn(-0.30, 0.30)
        return MatchupFactors(
            batRating = batRating,
            bowlRating = bowlRating,
            strikerFitnessFactor = strikerFitFactor,
            bowlerFitnessFactor = bowlerFitFactor,
            skillEdge = skillEdge
        )
    }
    fun createMatchSession(career: CareerState, fixture: Fixture): ActiveMatchRuntime {
        val headCoachRating = career.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
        val resolvedXIIds = if (career.delegation.selectionDelegated || career.selectedXIIds.size < 11) {
            StaffAdvisorEngine.recommendPlayingXI(career.squad, fixture.pitchType, headCoachRating).first
        } else {
            career.selectedXIIds
        }
        val xiPlayers = resolvedXIIds.mapNotNull { id ->
            career.squad.firstOrNull { it.id == id && it.isAvailableForMatch }
        }.ifEmpty {
            career.squad.filter { it.isAvailableForMatch }.take(11)
        }
        val finalXI = if (xiPlayers.size < 11) {
            (xiPlayers + career.squad.filter { it.isAvailableForMatch && it !in xiPlayers }).take(11)
        } else xiPlayers

        val impactSub = career.impactSubId?.let { id ->
            career.squad.firstOrNull { it.id == id && it.isAvailableForMatch && it !in finalXI }
        } ?: career.squad.firstOrNull { it.isAvailableForMatch && it !in finalXI }

        val opponentClubId = if (fixture.homeClubId == career.clubId) fixture.awayClubId else fixture.homeClubId
        val oppStrength = career.divisions.flatMap { it.clubs }
            .firstOrNull { it.clubId == opponentClubId }?.strength ?: 70

        val oppXI = buildOpponentXI(opponentClubId, oppStrength, career.divisionTier)
        val userBatFirst = Random.nextBoolean()
        return ActiveMatchRuntime(
            careerSnapshot = career,
            fixture = fixture,
            userXI = finalXI,
            impactSub = impactSub,
            opponentXI = oppXI,
            userBatFirst = userBatFirst
        )
    }

    private fun buildOpponentXI(clubId: String, strength: Int, tier: Int): List<Player> {
        val roles = listOf(
            T20Role.OPENER, T20Role.OPENER, T20Role.TOP_ORDER, T20Role.MIDDLE_ORDER,
            T20Role.WICKETKEEPER, T20Role.ALL_ROUNDER, T20Role.FINISHER,
            T20Role.ALL_ROUNDER, T20Role.SPINNER, T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER
        )
        val seed = clubId.hashCode().toLong()
        val rng = Random(seed)
        return roles.map { role ->
            WorldGenerator.createPlayer(
                role = role,
                targetOvr = (strength + rng.nextInt(-5, 6)).coerceIn(52, 88),
                age = rng.nextInt(21, 33),
                isYouth = false,
                tier = tier,
                random = rng
            )
        }
    }
}
