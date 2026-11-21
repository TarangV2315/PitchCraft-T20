package com.example.model

import com.squareup.moshi.JsonClass

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
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val highScore: Int = 0,
    val ballsBowled: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val dotBalls: Int = 0,
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
    val lastMatchNote: String = "Ready for selection"
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
    val isHired: Boolean
)

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
    val isActive: Boolean
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
    val season: Int = 1,
    val week: Int = 1,
    val totalRounds: Int = 14,
    val seasonComplete: Boolean = false,
    val divisionTier: Int = 3,
    val bankBalance: Int = 450000,
    val lastWeekNetCashflow: Int = 0,
    val boardConfidence: Int = 72,
    val managerReputation: Int = 48,
    val trophiesWon: Int = 0,
    val stadiumCapacity: Int = 18000,
    val delegation: DelegationSettings = DelegationSettings(),
    val budgets: ExecutiveBudgets = ExecutiveBudgets(),
    val squad: List<Player> = emptyList(),
    val transferMarket: List<Player> = emptyList(),
    val staff: List<StaffMember> = emptyList(),
    val availableStaff: List<StaffMember> = emptyList(),
    val sponsors: List<SponsorDeal> = emptyList(),
    val divisions: List<DivisionTable> = emptyList(),
    val fixtures: List<Fixture> = emptyList(),
    val matchHistory: List<MatchRecord> = emptyList(),
    val ledger: List<FinanceLedgerEntry> = emptyList(),
    val inbox: List<InboxMessage> = emptyList(),
    val selectedXIIds: List<String> = emptyList(),
    val impactSubId: String? = null,
    val defaultTempo: BattingTempo = BattingTempo.BALANCED,
    val defaultBowlingPlan: BowlingPlan = BowlingPlan.LINE_AND_LENGTH,
    val defaultFieldSpread: FieldSpread = FieldSpread.BALANCED,
    val seasonSummaryBanner: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val weeklyPlayerWages: Int
        get() = squad.filter { !it.isYouthAcademy }.sumOf { it.contract.weeklyWage }

    val weeklyStaffWages: Int
        get() = staff.sumOf { it.weeklySalary }

    val weeklySponsorIncome: Int
        get() = sponsors.filter { it.isActive }.sumOf { it.weeklyPayout }

    val nextUserFixture: Fixture?
        get() = fixtures.firstOrNull {
            !it.isCompleted && (it.homeClubId == clubId || it.awayClubId == clubId)
        }

    val currentDivision: DivisionTable?
        get() = divisions.firstOrNull { it.tier == divisionTier }
}
