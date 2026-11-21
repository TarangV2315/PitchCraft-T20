package com.example.model

enum class T20Role(val label: String, val shortLabel: String) {
    OPENER("Opener", "OPN"),
    TOP_ORDER("Top Order", "TOP"),
    MIDDLE_ORDER("Middle Order", "MID"),
    FINISHER("Finisher", "FIN"),
    WICKETKEEPER("Wicketkeeper", "WK"),
    ALL_ROUNDER("All-Rounder", "AR"),
    PACE_SPEARHEAD("Pace Spearhead", "PAC"),
    DEATH_BOWLER("Death Bowler", "DTH"),
    SPINNER("Spinner", "SPN")
}

enum class BattingHand(val label: String) {
    RIGHT("RHB"),
    LEFT("LHB")
}

enum class BowlingStyle(val label: String, val isPace: Boolean, val canBowl: Boolean) {
    NONE("Part-time", false, false),
    RIGHT_ARM_FAST("Right-Arm Fast", true, true),
    LEFT_ARM_FAST("Left-Arm Fast", true, true),
    MEDIUM_PACE("Medium Pace", true, true),
    OFF_SPIN("Off Spin", false, true),
    LEG_SPIN("Leg Spin", false, true),
    LEFT_ARM_ORTHODOX("Left-Arm Orthodox", false, true)
}

enum class TrainingFocus(val label: String, val description: String) {
    BALANCED("Balanced T20", "Steady all-round maintenance with low fatigue"),
    POWER_HITTING("Power Hitting", "Boosts boundary clearing & strike rate"),
    ANCHOR_TECHNIQUE("Timing & Rotation", "Improves batting timing & strike rotation"),
    DEATH_YORKERS("Death Execution", "Sharpens yorkers, slower balls & nerve"),
    SPIN_VARIATIONS("Spin & Drift", "Builds flight, turn & middle-overs control"),
    FIELDING_AGILITY("Fielding & Reflex", "Boosts catching, ground stops & stamina"),
    FITNESS_RECOVERY("Recovery Session", "Rapidly restores match fitness & lowers fatigue")
}

enum class RehabMode(val label: String, val speedMultiplier: Float, val setbackRiskPercent: Int) {
    NONE("Fit", 1.0f, 0),
    CONSERVATIVE("Conservative Rehab", 1.0f, 2),
    BALANCED("Standard Physio", 1.35f, 8),
    ACCELERATED("Accelerated Return", 1.75f, 22)
}

enum class StaffRole(val label: String, val department: String) {
    HEAD_COACH("Head Coach", "Tactics & XI"),
    BATTING_COACH("Batting Coach", "Batting & Power"),
    BOWLING_COACH("Bowling Coach", "Pace, Spin & Death"),
    PHYSIO("Head Physio", "Fitness & Rehab"),
    CHIEF_SCOUT("Chief Scout", "Transfers & Youth"),
    COMMERCIAL_DIRECTOR("Commercial Director", "Revenue & Sponsors")
}

enum class SponsorCategory(val label: String) {
    TITLE("Principal Title"),
    KIT("Match Kit"),
    STADIUM("Stadium Naming")
}

enum class TicketTier(val label: String, val pricePerSeat: Int, val attendanceFactor: Float) {
    ECONOMY("Fan Friendly ($14)", 14, 0.94f),
    STANDARD("Matchday Standard ($24)", 24, 0.78f),
    PREMIUM("VIP Hospitality ($38)", 38, 0.58f)
}

enum class PitchType(
    val label: String,
    val summary: String,
    val runBias: Double,
    val paceBias: Double,
    val spinBias: Double
) {
    BATTING_PARADISE("Batting Paradise", "Flat deck, rapid outfield — expect 195+ totals.", 1.15, 0.90, 0.88),
    GREEN_SEAM("Green Seam", "Live grass offering early swing and bounce.", 0.92, 1.22, 0.86),
    DUSTY_TURNER("Dusty Turner", "Dry surface gripping sharply for spinners.", 0.93, 0.92, 1.24),
    BALANCED_DECK("Balanced Sporting", "True bounce rewarding smart cricket across phases.", 1.0, 1.0, 1.0)
}

enum class BattingTempo(val label: String, val runBoost: Double, val wicketRisk: Double) {
    ANCHOR("Anchor", 0.84, 0.65),
    BALANCED("Balanced", 1.00, 1.00),
    AGGRESSIVE("Aggressive", 1.18, 1.32),
    ALL_OUT("All-Out Attack", 1.36, 1.72)
}

enum class BowlingPlan(val label: String, val economyFactor: Double, val wicketFactor: Double) {
    LINE_AND_LENGTH("Hard Length", 0.96, 1.02),
    DEATH_YORKERS("Yorker Pinpoint", 0.88, 1.08),
    PACE_VARIATIONS("Cutters & Bouncers", 0.95, 1.14),
    SPIN_TRAP("Aggressive Spin Trap", 0.94, 1.16)
}

enum class FieldSpread(val label: String, val boundaryFactor: Double, val wicketFactor: Double) {
    ATTACKING("Attacking Ring", 1.12, 1.22),
    BALANCED("Balanced Spread", 1.00, 1.00),
    DEFENSIVE("Boundary Riders", 0.84, 0.82)
}

enum class DelegationArea(val label: String, val staffOwner: StaffRole) {
    TRAINING("Training Regimens", StaffRole.HEAD_COACH),
    SELECTION("Matchday XI Selection", StaffRole.HEAD_COACH),
    TACTICS("Live Match Tactics", StaffRole.HEAD_COACH),
    TRANSFERS("Contract & Transfers", StaffRole.CHIEF_SCOUT),
    REHAB("Injury Rehab Plans", StaffRole.PHYSIO)
}

enum class MatchPhase(val label: String) {
    POWERPLAY("Powerplay (1-6)"),
    MIDDLE("Middle Overs (7-15)"),
    DEATH("Death Overs (16-20)")
}
