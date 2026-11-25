package com.example.model

import com.squareup.moshi.JsonClass
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class PlayerAttributes(
    val battingTiming: Int = 60,
    val powerHitting: Int = 60,
    val paceBowling: Int = 40,
    val spinVariation: Int = 40,
    val deathExecution: Int = 55,
    val fielding: Int = 65
)

@JsonClass(generateAdapter = true)
data class PlayerStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val notOuts: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val thirties: Int = 0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val highScore: Int = 0,
    val ballsBowled: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val dotBalls: Int = 0,
    val maidens: Int = 0,
    val threeWicketHauls: Int = 0,
    val bestWickets: Int = 0,
    val bestRuns: Int = 999
) {
    val strikeRate: Double
        get() = if (ballsFaced > 0) (runs * 100.0) / ballsFaced else 0.0

    val battingAverage: Double
        get() {
            val dismissals = (innings - notOuts).coerceAtLeast(1)
            return if (innings == 0) 0.0 else runs.toDouble() / dismissals
        }

    val economyRate: Double
        get() = if (ballsBowled > 0) (runsConceded * 6.0) / ballsBowled else 0.0

    val oversFormatted: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    val bestBowlingFormatted: String
        get() = if (bestWickets == 0 && bestRuns == 999) "-" else "$bestWickets/$bestRuns"
}

@JsonClass(generateAdapter = true)
data class InjuryState(
    val isInjured: Boolean = false,
    val injuryName: String = "",
    val weeksRemaining: Int = 0,
    val rehabMode: RehabMode = RehabMode.NONE
)

@JsonClass(generateAdapter = true)
data class PlayerContract(
    val weeklyWage: Int = 2500,
    val weeksRemaining: Int = 52,
    val releaseClause: Int = 75000
)

@JsonClass(generateAdapter = true)
data class Player(
    val id: String,
    val name: String,
    val nationality: String,
    val age: Int,
    val role: T20Role,
    val battingHand: BattingHand,
    val bowlingStyle: BowlingStyle,
    val attributes: PlayerAttributes,
    val overall: Int,
    val potential: Int,
    val form: Int,
    val fitness: Int,
    val morale: Int,
    val trainingFocus: TrainingFocus = TrainingFocus.BALANCED,
    val injury: InjuryState = InjuryState(),
    val contract: PlayerContract = PlayerContract(),
    val marketValue: Int = 45000,
    val isYouthAcademy: Boolean = false,
    val isShortlisted: Boolean = false,
    val isOnTransferList: Boolean = false,
    val isForeign: Boolean = false,
    val seasonStats: PlayerStats = PlayerStats(),
    val careerStats: PlayerStats = PlayerStats(),
    val lastMatchNote: String = "Ready for selection",
    val recentMatchWorkload: Int = 0,
    val portraitVariant: Int = -1
) {
    val canBowl: Boolean
        get() = bowlingStyle.canBowl || role == T20Role.ALL_ROUNDER

    val isAvailableForMatch: Boolean
        get() = !injury.isInjured && !isYouthAcademy
}

@JsonClass(generateAdapter = true)
data class StaffMember(
    val id: String,
    val name: String,
    val role: StaffRole,
    val rating: Int,
    val specialty: String,
    val weeklySalary: Int,
    val isHired: Boolean,
    val assignedFocus: CoachTrainingArea? = null
) {
    val isCoach: Boolean
        get() = role.defaultTrainingArea != null

    val effectiveTrainingArea: CoachTrainingArea?
        get() = if (isCoach) (assignedFocus ?: role.defaultTrainingArea) else null
}

@JsonClass(generateAdapter = true)
data class SponsorDeal(
    val id: String,
    val brandName: String,
    val category: SponsorCategory,
    val weeklyPayout: Int,
    val winBonus: Int,
    val trophyBonus: Int,
    val minRankTarget: Int,
    val seasonsRemaining: Int,
    val isActive: Boolean,
    val industry: String = "Commercial Partner",
    val structureType: SponsorStructureType = SponsorStructureType.BALANCED,
    val signingBonus: Int = 0,
    val perFiftyBonus: Int = 0,
    val perCenturyBonus: Int = 0,
    val perThreeWicketHaulBonus: Int = 0,
    val playerBrandingWeekly: Int = 0,
    val cityFitSummary: String = "Broad Regional Appeal",
    val negotiatedPct: Int = 0,
    val projectedSeasonMin: Int = 0,
    val projectedSeasonMax: Int = 0,
    val contractSummary: String = ""
) {
    val totalWeeklyGuaranteed: Int
        get() = weeklyPayout + playerBrandingWeekly
}

@JsonClass(generateAdapter = true)
data class SeasonFinishRecord(
    val season: Int,
    val tier: Int,
    val leagueRank: Int,
    val points: Int,
    val netRunRate: Double,
    val wonTitle: Boolean
)

data class SponsorLeverageProfile(
    val clubCity: String,
    val cityDemographyScore: Int,
    val cityDemographySummary: String,
    val buyingPatternScore: Int,
    val buyingPatternSummary: String,
    val performanceHistoryScore: Int,
    val performanceHistorySummary: String,
    val playerCalibreScore: Int,
    val playerCalibreSummary: String,
    val overallLeverageScore: Int,
    val maxNegotiationLiftPct: Int
)

data class SponsorNegotiationSession(
    val deal: SponsorDeal,
    val leverage: SponsorLeverageProfile,
    val askBaseWeekly: Int,
    val askBrandingWeekly: Int,
    val askWinBonus: Int,
    val askFiftyBonus: Int,
    val askCenturyBonus: Int,
    val askHaulBonus: Int,
    val offeredBaseWeekly: Int,
    val offeredBrandingWeekly: Int,
    val offeredWinBonus: Int,
    val offeredFiftyBonus: Int,
    val offeredCenturyBonus: Int,
    val offeredHaulBonus: Int,
    val brandPatience: Int = 100,
    val round: Int = 1,
    val brandMessage: String,
    val isAccepted: Boolean = false,
    val isWalkedAway: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ClubStanding(
    val clubId: String,
    val name: String,
    val shortCode: String,
    val tier: Int,
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val runsFor: Int = 0,
    val ballsFaced: Int = 0,
    val runsAgainst: Int = 0,
    val ballsBowled: Int = 0,
    val points: Int = 0,
    val strength: Int = 72,
    val isUserClub: Boolean = false
) {
    val netRunRate: Double
        get() {
            val forRate = if (ballsFaced > 0) (runsFor * 6.0) / ballsFaced else 0.0
            val againstRate = if (ballsBowled > 0) (runsAgainst * 6.0) / ballsBowled else 0.0
            return forRate - againstRate
        }
}

@JsonClass(generateAdapter = true)
data class DivisionTable(
    val tier: Int,
    val name: String,
    val clubs: List<ClubStanding>
)

@JsonClass(generateAdapter = true)
data class TournamentPlayerStat(
    val playerId: String,
    val playerName: String,
    val clubId: String,
    val clubName: String,
    val clubShortCode: String,
    val tier: Int,
    val role: T20Role,
    val stats: PlayerStats = PlayerStats()
)

@JsonClass(generateAdapter = true)
data class Fixture(
    val id: String,
    val round: Int,
    val tier: Int,
    val homeClubId: String,
    val homeClubName: String,
    val awayClubId: String,
    val awayClubName: String,
    val pitchType: PitchType,
    val isPlayoff: Boolean = false,
    val playoffLabel: String = "",
    val isCompleted: Boolean = false,
    val resultSummary: String = ""
)

@JsonClass(generateAdapter = true)
data class FinanceLedgerEntry(
    val id: String,
    val season: Int,
    val week: Int,
    val category: String,
    val description: String,
    val amount: Int
)

@JsonClass(generateAdapter = true)
data class DelegationSettings(
    val trainingDelegated: Boolean = false,
    val selectionDelegated: Boolean = false,
    val tacticsDelegated: Boolean = false,
    val transfersDelegated: Boolean = false,
    val rehabDelegated: Boolean = false
) {
    fun isDelegated(area: DelegationArea): Boolean = when (area) {
        DelegationArea.TRAINING -> trainingDelegated
        DelegationArea.SELECTION -> selectionDelegated
        DelegationArea.TACTICS -> tacticsDelegated
        DelegationArea.TRANSFERS -> transfersDelegated
        DelegationArea.REHAB -> rehabDelegated
    }

    fun toggled(area: DelegationArea): DelegationSettings = when (area) {
        DelegationArea.TRAINING -> copy(trainingDelegated = !trainingDelegated)
        DelegationArea.SELECTION -> copy(selectionDelegated = !selectionDelegated)
        DelegationArea.TACTICS -> copy(tacticsDelegated = !tacticsDelegated)
        DelegationArea.TRANSFERS -> copy(transfersDelegated = !transfersDelegated)
        DelegationArea.REHAB -> copy(rehabDelegated = !rehabDelegated)
    }
}

@JsonClass(generateAdapter = true)
data class ExecutiveBudgets(
    val ticketTier: TicketTier = TicketTier.STANDARD,
    val academyLevel: Int = 2,
    val scoutingLevel: Int = 2,
    val medicalLevel: Int = 2
) {
    val weeklyUpkeep: Int
        get() = (academyLevel * 3200) + (scoutingLevel * 2400) + (medicalLevel * 2600)
}

@JsonClass(generateAdapter = true)
data class InboxMessage(
    val id: String,
    val season: Int,
    val week: Int,
    val senderRole: StaffRole,
    val senderName: String,
    val title: String,
    val body: String,
    val relatedPlayerId: String? = null,
    val isUrgent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CareerState(
    val careerId: String,
    val clubId: String,
    val clubName: String,
    val clubShortCode: String,
    val managerName: String,
    val country: CricketNation = CricketNation.INDIA,
    val season: Int = 1,
    val week: Int = 1,
    val isPreSeason: Boolean = true,
    val totalRounds: Int = 14,
    val seasonComplete: Boolean = false,
    val divisionTier: Int = 1,
    val bankBalance: Int = 450000,
    val lastWeekNetCashflow: Int = 0,
    val boardConfidence: Int = 72,
    val managerReputation: Int = 48,
    val trophiesWon: Int = 0,
    val stadiumCapacity: Int = 18000,
    val delegation: DelegationSettings = DelegationSettings(),
    val budgets: ExecutiveBudgets = ExecutiveBudgets(),
    val trainingIntensity: TrainingIntensity = TrainingIntensity.MODERATE,
    val squad: List<Player> = emptyList(),
    val transferMarket: List<Player> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val availableStaff: List<StaffMember> = emptyList(),
    val sponsors: List<SponsorDeal> = emptyList(),
    val divisions: List<DivisionTable> = emptyList(),
    val tournamentStats: List<TournamentPlayerStat> = emptyList(),
    val fixtures: List<Fixture> = emptyList(),
    val matchHistory: List<MatchRecord> = emptyList(),
    val ledger: List<FinanceLedgerEntry> = emptyList(),
    val inbox: List<InboxMessage> = emptyList(),
    val selectedXIIds: List<String> = emptyList(),
    val impactSubId: String? = null,
    val defaultTempo: BattingTempo = BattingTempo.BALANCED,
    val defaultBowlingPlan: BowlingPlan = BowlingPlan.LINE_AND_LENGTH,
    val defaultFieldSpread: FieldSpread = FieldSpread.BALANCED,
    val preMatchTeamTalk: TeamTalkTone = TeamTalkTone.ENCOURAGING,
    val seasonSummaryBanner: String? = null,
    val seasonTicketIncome: Int = 0,
    val seasonSponsorIncome: Int = 0,
    val seasonPlayerWagesPaid: Int = 0,
    val seasonStaffWagesPaid: Int = 0,
    val seasonUpkeepPaid: Int = 0,
    val homeMatchesHosted: Int = 0,
    val clubCity: String = "",
    val seasonHistory: List<SeasonFinishRecord> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val weeklyPlayerWages: Int
        get() = squad.filter { !it.isYouthAcademy }.sumOf { it.contract.weeklyWage }

    val weeklyStaffWages: Int
        get() = staff.sumOf { it.weeklySalary }

    val weeklyTotalWages: Int
        get() = weeklyPlayerWages + weeklyStaffWages

    val weeklySponsorIncome: Int
        get() = sponsors.filter { it.isActive }.sumOf { it.totalWeeklyGuaranteed }

    val isSponsorshipWindowOpen: Boolean
        get() = isPreSeason && week == 1 && !seasonComplete

    val projectedAttendance: Int
        get() = (stadiumCapacity * budgets.ticketTier.attendanceFactor).roundToInt()

    val projectedHomeTicketRevenue: Int
        get() = projectedAttendance * budgets.ticketTier.pricePerSeat

    val projectedWeeklyGateAverage: Int
        get() = projectedHomeTicketRevenue / 2

    val projectedWeeklyTotalIncome: Int
        get() = weeklySponsorIncome + projectedWeeklyGateAverage

    val seasonTrackedIncome: Int
        get() = seasonTicketIncome + seasonSponsorIncome

    val seasonWageExpenses: Int
        get() = seasonPlayerWagesPaid + seasonStaffWagesPaid

    val nextUserFixture: Fixture?
        get() = fixtures.firstOrNull {
            !it.isCompleted && (it.homeClubId == clubId || it.awayClubId == clubId)
        }

    val currentDivision: DivisionTable?
        get() = divisions.firstOrNull { it.tier == divisionTier }

    val midSeasonTransferWeek: Int
        get() = (totalRounds / 2) + 1

    val transferWindowPhase: TransferWindowPhase
        get() = when {
            seasonComplete -> TransferWindowPhase.CLOSED
            isPreSeason && week == 1 -> TransferWindowPhase.PRE_SEASON
            week == midSeasonTransferWeek -> TransferWindowPhase.MID_SEASON
            else -> TransferWindowPhase.CLOSED
        }

    val isTransferWindowOpen: Boolean
        get() = transferWindowPhase != TransferWindowPhase.CLOSED

    val transferWindowStatus: TransferWindowStatus
        get() = when (transferWindowPhase) {
            TransferWindowPhase.PRE_SEASON -> TransferWindowStatus(
                isOpen = true,
                phase = TransferWindowPhase.PRE_SEASON,
                title = "Pre-Season Transfer Window Open",
                subtitle = "Active for 1 week (Week 1) before Season $season gets underway.",
                weeksUntilNextWindow = 0
            )
            TransferWindowPhase.MID_SEASON -> TransferWindowStatus(
                isOpen = true,
                phase = TransferWindowPhase.MID_SEASON,
                title = "Mid-Season Transfer Window Open",
                subtitle = "Active for 1 week (Week $week of $totalRounds) — closes after this round.",
                weeksUntilNextWindow = 0
            )
            TransferWindowPhase.CLOSED -> {
                val weeksUntilMid = if (!seasonComplete && week < midSeasonTransferWeek) {
                    midSeasonTransferWeek - week
                } else null
                val subtitle = when {
                    seasonComplete -> "Window closed. Re-opens for 1 week at Season ${season + 1} Pre-Season."
                    weeksUntilMid != null -> "Opens mid-season at Week $midSeasonTransferWeek (in $weeksUntilMid wk${if (weeksUntilMid == 1) "" else "s"})."
                    else -> "Mid-season window closed. Next window opens at Season ${season + 1} Pre-Season."
                }
                TransferWindowStatus(
                    isOpen = false,
                    phase = TransferWindowPhase.CLOSED,
                    title = "Transfer Window Closed",
                    subtitle = subtitle,
                    weeksUntilNextWindow = weeksUntilMid
                )
            }
        }
}

enum class TransferWindowPhase(val label: String) {
    PRE_SEASON("Pre-Season Window"),
    MID_SEASON("Mid-Season Window"),
    CLOSED("Window Closed")
}

data class TransferWindowStatus(
    val isOpen: Boolean,
    val phase: TransferWindowPhase,
    val title: String,
    val subtitle: String,
    val weeksUntilNextWindow: Int?
)

