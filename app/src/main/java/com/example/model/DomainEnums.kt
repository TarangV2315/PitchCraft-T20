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

enum class TrainingFocus(
    val label: String,
    val description: String,
    val drillLoadFactor: Double
) {
    BALANCED("Balanced T20", "Steady all-round maintenance with low fatigue", 0.95),
    POWER_HITTING("Power Hitting", "Boosts boundary clearing & strike rate", 1.30),
    ANCHOR_TECHNIQUE("Timing & Rotation", "Improves batting timing & strike rotation", 0.90),
    DEATH_YORKERS("Death Execution", "Sharpens yorkers, slower balls & nerve", 1.35),
    SPIN_VARIATIONS("Spin & Drift", "Builds flight, turn & middle-overs control", 1.05),
    FIELDING_AGILITY("Fielding & Reflex", "Boosts catching, ground stops & stamina", 1.20),
    FITNESS_RECOVERY("Recovery Session", "Rapidly restores match fitness & lowers fatigue", 0.45)
}

enum class TrainingIntensity(
    val label: String,
    val workloadFactor: Double,
    val growthMultiplier: Float,
    val fitnessDelta: Int,
    val summary: String
) {
    LIGHT("Light", 0.65, 0.78f, 6, "Low physical strain • boosts weekly fitness & minimizes injury risk"),
    MODERATE("Moderate", 1.00, 1.00f, 0, "Balanced T20 workload • steady progression with standard risk"),
    HIGH("High", 1.45, 1.25f, -4, "Heavy drills • faster attribute growth, elevated soft-tissue strain"),
    INTENSE("Intense", 1.95, 1.48f, -8, "Peak overload • max skill progression with high injury probability")
}

enum class RehabMode(val label: String, val speedMultiplier: Float, val setbackRiskPercent: Int) {
    NONE("Fit", 1.0f, 0),
    CONSERVATIVE("Conservative Rehab", 1.0f, 2),
    BALANCED("Standard Physio", 1.35f, 8),
    ACCELERATED("Accelerated Return", 1.75f, 22)
}

enum class CoachTrainingArea(val label: String, val shortLabel: String, val boostSummary: String) {
    BATTING("Batting", "Bat", "Boosts timing, power hitting & anchor technique progression"),
    BOWLING("Bowling", "Bowl", "Boosts pace, spin variation & death yorker progression"),
    FITNESS("Fitness", "Fit", "Boosts weekly stamina recovery, fielding & injury prevention")
}

enum class StaffRole(val label: String, val department: String, val defaultTrainingArea: CoachTrainingArea?) {
    HEAD_COACH("Head Coach", "Tactics & XI", CoachTrainingArea.BATTING),
    BATTING_COACH("Batting Coach", "Batting & Power", CoachTrainingArea.BATTING),
    BOWLING_COACH("Bowling Coach", "Pace, Spin & Death", CoachTrainingArea.BOWLING),
    PHYSIO("Head Physio", "Fitness & Rehab", CoachTrainingArea.FITNESS),
    CHIEF_SCOUT("Chief Scout", "Transfers & Youth", null),
    COMMERCIAL_DIRECTOR("Commercial Director", "Revenue & Sponsors", null)
}

enum class SponsorCategory(val label: String) {
    TITLE("Principal Title"),
    KIT("Match Kit"),
    STADIUM("Stadium Naming")
}

enum class SponsorStructureType(val label: String, val riskTag: String) {
    GUARANTEED_ANCHOR("Guaranteed Anchor", "Low Variance • High Base"),
    BALANCED("Balanced Franchise", "Steady Mix • Base + Bonuses"),
    PERFORMANCE_ESCALATOR("Performance Escalator", "High Upside • Win & Milestone Heavy"),
    STAR_BRANDING("Player Branding", "Merchandise & Star Image Driven")
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

enum class CricketNation(
    val label: String,
    val shortCode: String,
    val tierCount: Int,
    val leagueBrand: String,
    val summary: String
) {
    INDIA(
        label = "India",
        shortCode = "IND",
        tierCount = 2,
        leagueBrand = "India Domestic T20",
        summary = "2-tier domestic pyramid (Major Metro Cities & Smaller Cities) with 1-up / 1-down promotion & relegation"
    ),
    ENGLAND(
        label = "England",
        shortCode = "ENG",
        tierCount = 2,
        leagueBrand = "England Domestic T20",
        summary = "2-tier domestic pyramid (Major Cities Blast & Regional Smaller Cities) with 1-up / 1-down promotion & relegation"
    ),
    AUSTRALIA(
        label = "Australia",
        shortCode = "AUS",
        tierCount = 1,
        leagueBrand = "Australia T20 Premier",
        summary = "Single-tier 8-team national league competing for Top-4 Playoffs & Championship Trophy"
    ),
    SOUTH_AFRICA(
        label = "South Africa",
        shortCode = "RSA",
        tierCount = 1,
        leagueBrand = "South Africa T20 League",
        summary = "Single-tier 8-team national league competing for Top-4 Playoffs & Championship Trophy"
    ),
    CARIBBEAN(
        label = "Caribbean",
        shortCode = "WIN",
        tierCount = 1,
        leagueBrand = "Caribbean T20 Premier",
        summary = "Single-tier 8-team regional franchise league competing for Top-4 Playoffs & Championship Trophy"
    );

    val supportsPromotionRelegation: Boolean
        get() = tierCount > 1
}

enum class SquadViewMode(val label: String) {
    TILE("Tiles"),
    TABLE("Table")
}

enum class TeamTalkStage(val label: String) {
    PRE_MATCH("Pre-Match Dressing Room"),
    INNINGS_BREAK("Innings Break Huddle"),
    POST_MATCH("Post-Match Debrief")
}

enum class TeamTalkTone(
    val label: String,
    val shortLabel: String,
    val runRateDelta: Double,
    val wicketRiskDelta: Double,
    val bowlingEconDelta: Double,
    val bowlingWktDelta: Double,
    val composureFitnessBonus: Int,
    val moraleDelta: Int,
    val formDelta: Int,
    val summary: String
) {
    AGGRESSIVE(
        label = "Aggressive",
        shortLabel = "Fired Up",
        runRateDelta = 0.08,
        wicketRiskDelta = 0.04,
        bowlingEconDelta = 0.02,
        bowlingWktDelta = 0.06,
        composureFitnessBonus = 1,
        moraleDelta = 3,
        formDelta = 2,
        summary = "+8% scoring punch & +6% bowling strike intent, +4% batting risk"
    ),
    CALM(
        label = "Calm",
        shortLabel = "Composed",
        runRateDelta = 0.01,
        wicketRiskDelta = -0.06,
        bowlingEconDelta = -0.05,
        bowlingWktDelta = 0.01,
        composureFitnessBonus = 4,
        moraleDelta = 2,
        formDelta = 1,
        summary = "-6% batting collapse risk, -5% runs conceded, +4% composure"
    ),
    ENCOURAGING(
        label = "Encouraging",
        shortLabel = "United",
        runRateDelta = 0.04,
        wicketRiskDelta = -0.02,
        bowlingEconDelta = -0.03,
        bowlingWktDelta = 0.03,
        composureFitnessBonus = 6,
        moraleDelta = 5,
        formDelta = 2,
        summary = "+4% scoring flow, +6% match fitness poise, highest morale lift"
    ),
    ASSERTIVE(
        label = "Assertive",
        shortLabel = "Demanding",
        runRateDelta = 0.05,
        wicketRiskDelta = -0.03,
        bowlingEconDelta = -0.04,
        bowlingWktDelta = 0.05,
        composureFitnessBonus = 2,
        moraleDelta = 1,
        formDelta = 3,
        summary = "+5% execution sharpness & bowling threat, boosts player form"
    );

    fun speechLine(stage: TeamTalkStage, wonMatch: Boolean = true): String = when (stage) {
        TeamTalkStage.PRE_MATCH -> when (this) {
            AGGRESSIVE -> "\"Take the pitch on the front foot from ball one—attack the powerplay and make a statement!\""
            CALM -> "\"Breathe, stick to our match plans, and trust your preparation—let the pressure sit on them.\""
            ENCOURAGING -> "\"You've trained hard for this stage. Back each other out there and play with freedom!\""
            ASSERTIVE -> "\"Standards matter today. Execute every role cleanly and leave nothing to chance.\""
        }
        TeamTalkStage.INNINGS_BREAK -> when (this) {
            AGGRESSIVE -> "\"This second half is ours for the taking—go hard, hunt wickets or boundaries early!\""
            CALM -> "\"Stay clinical through the middle overs. Dot balls and smart singles will swing this our way.\""
            ENCOURAGING -> "\"Great effort in the first half—keep the energy high in the field and back your skills!\""
            ASSERTIVE -> "\"Twenty overs left to finish the job. Tighten our lines and close out key moments.\""
        }
        TeamTalkStage.POST_MATCH -> when (this) {
            AGGRESSIVE -> if (wonMatch) {
                "\"Clinical dominance! Keep that ruthless edge rolling straight into next week.\""
            } else {
                "\"Not good enough in key overs—use that frustration to come back sharper and meaner.\""
            }
            CALM -> if (wonMatch) {
                "\"Composed win under pressure. Bank the points and recover smartly for the next round.\""
            } else {
                "\"Stay level-headed. We review the turning points calmly and fix them before next game.\""
            }
            ENCOURAGING -> if (wonMatch) {
                "\"Proud of every player in this room—that team spirit won us the match!\""
            } else {
                "\"Heads up—plenty of fight out there tonight. Stick together and the results will turn.\""
            }
            ASSERTIVE -> if (wonMatch) {
                "\"Good result, now raise the bar even higher—never settle when the trophy is the goal.\""
            } else {
                "\"I expect a stronger response at training this week. Selection spots are earned every day.\""
            }
        }
    }
}

