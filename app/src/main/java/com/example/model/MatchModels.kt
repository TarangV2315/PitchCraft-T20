package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatterScoreEntry(
    val playerId: String,
    val playerName: String,
    val roleLabel: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalText: String = "not out"
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs * 100.0) / balls else 0.0
}

@JsonClass(generateAdapter = true)
data class BowlerScoreEntry(
    val playerId: String,
    val playerName: String,
    val ballsBowled: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val dotBalls: Int = 0,
    val maidens: Int = 0
) {
    val oversText: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    val economy: Double
        get() = if (ballsBowled > 0) (runsConceded * 6.0) / ballsBowled else 0.0
}

@JsonClass(generateAdapter = true)
data class InningsScorecard(
    val battingClubName: String,
    val bowlingClubName: String,
    val runs: Int,
    val wickets: Int,
    val balls: Int,
    val batters: List<BatterScoreEntry>,
    val bowlers: List<BowlerScoreEntry>
) {
    val oversFormatted: String
        get() = "${balls / 6}.${balls % 6}"

    val runRate: Double
        get() = if (balls > 0) (runs * 6.0) / balls else 0.0
}

@JsonClass(generateAdapter = true)
data class MatchRecord(
    val matchId: String,
    val season: Int,
    val round: Int,
    val label: String,
    val pitchType: PitchType,
    val userClubName: String,
    val opponentName: String,
    val userWon: Boolean,
    val resultText: String,
    val manOfTheMatch: String,
    val firstInnings: InningsScorecard,
    val secondInnings: InningsScorecard,
    val keyHighlights: List<String>,
    val revenueEarned: Int,
    val ticketSalesRevenue: Int = 0,
    val sponsorBonusEarned: Int = 0,
    val winSponsorBonus: Int = 0,
    val individualMilestoneBonus: Int = 0,
    val attendance: Int = 0
)

@JsonClass(generateAdapter = true)
data class BallEvent(
    val id: String,
    val innings: Int,
    val overBallText: String,
    val symbol: String,
    val batterName: String,
    val bowlerName: String,
    val runsScored: Int,
    val isWicket: Boolean,
    val isBoundary: Boolean,
    val commentary: String,
    val scoreAfterBall: String
)

data class KeyMomentPrompt(
    val momentIndex: Int,
    val title: String,
    val description: String,
    val phase: MatchPhase,
    val isUserBatting: Boolean,
    val staffTip: String,
    val recommendedTempo: BattingTempo? = null,
    val recommendedBowlingPlan: BowlingPlan? = null,
    val recommendedField: FieldSpread? = null,
    val isInningsBreak: Boolean = false,
    val recommendedTeamTalk: TeamTalkTone? = null
)

data class RunRatePoint(
    val ball: Int,
    val crr: Double,
    val rrr: Double? = null
)

data class LiveMatchState(
    val fixture: Fixture,
    val userBatFirst: Boolean,
    val currentInnings: Int, // 1 or 2
    val userIsBattingNow: Boolean,
    val runs: Int,
    val wickets: Int,
    val balls: Int,
    val target: Int?,
    val recentBalls: List<String>,
    val activeBatters: List<BatterScoreEntry>,
    val activeBowler: BowlerScoreEntry?,
    val firstInningsSummary: InningsScorecard?,
    val secondInningsSummary: InningsScorecard?,
    val commentaryLog: List<String>,
    val ballFeed: List<BallEvent> = emptyList(),
    val runRateHistory: List<RunRatePoint> = emptyList(),
    val activeBatterFitness: Map<String, Int> = emptyMap(),
    val activeBowlerFitness: Int = 100,
    val matchupSummary: String = "",
    val currentTempo: BattingTempo,
    val currentBowlingPlan: BowlingPlan,
    val currentFieldSpread: FieldSpread,
    val preMatchTeamTalk: TeamTalkTone = TeamTalkTone.ENCOURAGING,
    val inningsBreakTeamTalk: TeamTalkTone? = null,
    val postMatchTeamTalkDelivered: TeamTalkTone? = null,
    val availableImpactSubs: List<Player>,
    val impactSubUsed: Boolean,
    val pendingKeyMoment: KeyMomentPrompt?,
    val finishedRecord: MatchRecord?
) {
    val oversText: String
        get() = "${balls / 6}.${balls % 6}"

    val currentRunRate: Double
        get() = if (balls > 0) (runs * 6.0) / balls else 0.0

    val requiredRunRate: Double?
        get() {
            val t = target ?: return null
            val remainingBalls = (120 - balls).coerceAtLeast(1)
            val needed = (t - runs).coerceAtLeast(0)
            return (needed * 6.0) / remainingBalls
        }
}
