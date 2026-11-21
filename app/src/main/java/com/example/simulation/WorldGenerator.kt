package com.example.simulation

import com.example.model.*
import java.util.UUID
import kotlin.random.Random

data class ClubTemplate(
    val clubId: String,
    val name: String,
    val shortCode: String,
    val tier: Int,
    val baseStrength: Int,
    val stadiumCapacity: Int,
    val startingBudget: Int,
    val cityTag: String
)

object WorldGenerator {
    val ALL_CLUB_TEMPLATES: List<ClubTemplate> = listOf(
        // Tier 1: Global Premier T20
        ClubTemplate("c_t1_1", "Mumbai Mariners", "MM", 1, 83, 38000, 950000, "Mumbai"),
        ClubTemplate("c_t1_2", "Melbourne Strikeforce", "MS", 1, 82, 36000, 900000, "Melbourne"),
        ClubTemplate("c_t1_3", "London Royals", "LR", 1, 81, 32000, 880000, "London"),
        ClubTemplate("c_t1_4", "Bengaluru Blazers", "BB", 1, 80, 34000, 860000, "Bengaluru"),
        ClubTemplate("c_t1_5", "Cape Town Titans", "CT", 1, 79, 28000, 810000, "Cape Town"),
        ClubTemplate("c_t1_6", "Dubai Falcons", "DF", 1, 78, 30000, 840000, "Dubai"),
        ClubTemplate("c_t1_7", "Lahore Lions", "LL", 1, 77, 29000, 780000, "Lahore"),
        ClubTemplate("c_t1_8", "Auckland Aces XI", "AA", 1, 76, 25000, 750000, "Auckland"),
        // Tier 2: Continental Championship
        ClubTemplate("c_t2_1", "Chennai Spartans", "CS", 2, 74, 24000, 620000, "Chennai"),
        ClubTemplate("c_t2_2", "Birmingham Bears T20", "BM", 2, 73, 22000, 590000, "Birmingham"),
        ClubTemplate("c_t2_3", "Sydney Harbour Six", "SH", 2, 72, 23000, 580000, "Sydney"),
        ClubTemplate("c_t2_4", "Kingston Hurricanes", "KH", 2, 71, 20000, 540000, "Kingston"),
        ClubTemplate("c_t2_5", "Johannesburg Rapids", "JR", 2, 70, 21000, 530000, "Johannesburg"),
        ClubTemplate("c_t2_6", "Colombo Kings", "CK", 2, 70, 22000, 520000, "Colombo"),
        ClubTemplate("c_t2_7", "Kolkata Knights United", "KK", 2, 69, 25000, 560000, "Kolkata"),
        ClubTemplate("c_t2_8", "Toronto Timberwolves", "TT", 2, 68, 18000, 500000, "Toronto"),
        // Tier 3: Challenger T20 League
        ClubTemplate("c_t3_1", "Jaipur Panthers", "JP", 3, 66, 18000, 440000, "Jaipur"),
        ClubTemplate("c_t3_2", "Cardiff Dragons", "CD", 3, 65, 16000, 420000, "Cardiff"),
        ClubTemplate("c_t3_3", "Adelaide Meteors", "AM", 3, 65, 17500, 430000, "Adelaide"),
        ClubTemplate("c_t3_4", "Trinidad Cavaliers", "TC", 3, 64, 15000, 400000, "Port of Spain"),
        ClubTemplate("c_t3_5", "Dhaka Tigers", "DT", 3, 63, 19000, 410000, "Dhaka"),
        ClubTemplate("c_t3_6", "Nairobi Crown", "NC", 3, 62, 14000, 380000, "Nairobi"),
        ClubTemplate("c_t3_7", "Amsterdam Windmills", "AW", 3, 61, 13000, 370000, "Amsterdam"),
        ClubTemplate("c_t3_8", "Dallas Mavericks XI", "DM", 3, 61, 16500, 420000, "Dallas")
    )

    private val FIRST_NAMES = listOf(
        "Aarav", "Rohan", "Liam", "Marcus", "Devon", "Kagiso", "Shaheen", "Zayd",
        "Ishaan", "Callum", "Toby", "Haris", "Nikhil", "Mateo", "Finn", "Tariq",
        "Vihaan", "Josh", "Samir", "Darius", "Kieran", "Arjun", "Rashid", "Ollie",
        "Kabir", "Ethan", "Yash", "Suryansh", "Andre", "Trent", "Shubham", "Mitchell",
        "Varun", "Jofra", "Noor", "Heinrich", "Travis", "Marco", "Rinku", "Will"
    )

    private val LAST_NAMES = listOf(
        "Sharma", "Patel", "Verma", "Stoinis", "Conway", "Rabada", "Afridi", "Khan",
        "Gill", "Brook", "Curran", "Rauf", "Nair", "Banton", "Allen", "Hussain",
        "Reddy", "Inglis", "Malik", "Pooran", "Salt", "Desai", "Zaman", "Pope",
        "Bhatia", "Marsh", "Jaiswal", "Rana", "Russell", "Boult", "Samson", "Santner",
        "Chakravarthy", "Archer", "Ahmad", "Klaasen", "Head", "Jansen", "Singh", "Jacks"
    )

    private val NATIONALITIES = listOf(
        "IND", "AUS", "ENG", "RSA", "NZL", "PAK", "WIN", "SRI", "BAN", "AFG", "NED", "USA"
    )

    fun createNewCareer(
        template: ClubTemplate,
        managerName: String,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        val careerId = "career_${UUID.randomUUID().toString().take(8)}"
        val squad = generateClubSquad(template.tier, template.baseStrength, random)
        val market = generateTransferMarket(template.tier, count = 26, random = random)
        val staff = generateInitialStaff(template.tier, random)
        val availableStaff = generateStaffCandidates(template.tier, count = 12, random = random)
        val sponsors = generateSponsorDeals(template.tier, random)

        val divisions = listOf(1, 2, 3).map { tier ->
            val name = divisionNameForTier(tier)
            val clubs = ALL_CLUB_TEMPLATES.filter { it.tier == tier }.map { club ->
                ClubStanding(
                    clubId = club.clubId,
                    name = club.name,
                    shortCode = club.shortCode,
                    tier = tier,
                    strength = club.baseStrength,
                    isUserClub = club.clubId == template.clubId
                )
            }
            DivisionTable(tier = tier, name = name, clubs = clubs)
        }

        val fixtures = generateDivisionFixtures(divisions, random)
        val initialXI = StaffAdvisorEngine.recommendPlayingXI(
            squad = squad,
            pitchType = fixtures.firstOrNull {
                it.homeClubId == template.clubId || it.awayClubId == template.clubId
            }?.pitchType ?: PitchType.BALANCED_DECK,
            headCoachRating = staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
        )

        val starterInbox = listOf(
            InboxMessage(
                id = "msg_welcome",
                season = 1,
                week = 1,
                senderRole = StaffRole.HEAD_COACH,
                senderName = staff.first { it.role == StaffRole.HEAD_COACH }.name,
                title = "Pre-Season Briefing & XI Draft",
                body = "Welcome to ${template.name}, $managerName. Our ${divisionNameForTier(template.tier)} opener is queued. You can adjust training, tactics, and Playing XI yourself or delegate any area to us.",
                isUrgent = false
            ),
            InboxMessage(
                id = "msg_sponsor",
                season = 1,
                week = 1,
                senderRole = StaffRole.COMMERCIAL_DIRECTOR,
                senderName = staff.first { it.role == StaffRole.COMMERCIAL_DIRECTOR }.name,
                title = "Sponsorship Deals on the Table",
                body = "Two commercial slots (Kit & Stadium) remain open for Season 1. Visit Club Office → Sponsors to lock in weekly cashflow.",
                isUrgent = true
            )
        )

        return CareerState(
            careerId = careerId,
            clubId = template.clubId,
            clubName = template.name,
            clubShortCode = template.shortCode,
            managerName = managerName.ifBlank { "Gaffer" },
            season = 1,
            week = 1,
            totalRounds = 14,
            divisionTier = template.tier,
            bankBalance = template.startingBudget,
            boardConfidence = 74,
            managerReputation = when (template.tier) {
                1 -> 65
                2 -> 52
                else -> 40
            },
            stadiumCapacity = template.stadiumCapacity,
            squad = squad,
            transferMarket = market,
            staff = staff,
            availableStaff = availableStaff,
            sponsors = sponsors,
            divisions = divisions,
            fixtures = fixtures,
            inbox = starterInbox,
            selectedXIIds = initialXI.first,
            impactSubId = initialXI.second
        )
    }

    fun divisionNameForTier(tier: Int): String = when (tier) {
        1 -> "Tier 1 • Global Premier T20"
        2 -> "Tier 2 • Continental Championship"
        else -> "Tier 3 • Challenger T20 League"
    }

    private fun generateClubSquad(tier: Int, strength: Int, random: Random): List<Player> {
        val roleBlueprint = listOf(
            T20Role.OPENER, T20Role.OPENER,
            T20Role.TOP_ORDER, T20Role.TOP_ORDER,
            T20Role.MIDDLE_ORDER, T20Role.FINISHER,
            T20Role.WICKETKEEPER, T20Role.WICKETKEEPER,
            T20Role.ALL_ROUNDER, T20Role.ALL_ROUNDER, T20Role.ALL_ROUNDER,
            T20Role.PACE_SPEARHEAD, T20Role.PACE_SPEARHEAD,
            T20Role.DEATH_BOWLER,
            T20Role.SPINNER, T20Role.SPINNER,
            T20Role.FINISHER
        )
        val usedNames = mutableSetOf<String>()
        val senior = roleBlueprint.mapIndexed { index, role ->
            val variance = random.nextInt(-6, 7)
            val targetOvr = (strength + variance).coerceIn(52, 88)
            val age = when (index) {
                0, 8, 11 -> random.nextInt(24, 30)
                16 -> random.nextInt(19, 23)
                else -> random.nextInt(20, 34)
            }
            val player = createPlayer(
                role = role,
                targetOvr = targetOvr,
                age = age,
                isYouth = false,
                tier = tier,
                usedNames = usedNames,
                random = random
            )
            // Add realistic opening storylines: one slight niggle, one expiring contract
            when (index) {
                5 -> player.copy(
                    contract = player.contract.copy(weeksRemaining = 10)
                )
                12 -> player.copy(
                    fitness = 76,
                    injury = InjuryState(
                        isInjured = true,
                        injuryName = "Side Strain",
                        weeksRemaining = 2,
                        rehabMode = RehabMode.BALANCED
                    )
                )
                else -> player
            }
        }
        val academyRoles = listOf(
            T20Role.OPENER, T20Role.ALL_ROUNDER, T20Role.PACE_SPEARHEAD, T20Role.SPINNER
        )
        val academy = academyRoles.map { role ->
            createPlayer(
                role = role,
                targetOvr = (strength - 14 + random.nextInt(-3, 6)).coerceIn(46, 70),
                age = random.nextInt(16, 19),
                isYouth = true,
                tier = tier,
                usedNames = usedNames,
                random = random
            )
        }
        return senior + academy
    }

    fun generateTransferMarket(tier: Int, count: Int, random: Random): List<Player> {
        val roles = T20Role.entries
        val usedNames = mutableSetOf<String>()
        val base = when (tier) {
            1 -> 74
            2 -> 68
            else -> 62
        }
        return (0 until count).map { idx ->
            val role = roles[idx % roles.size]
            val targetOvr = (base + random.nextInt(-8, 14)).coerceIn(54, 90)
            val age = random.nextInt(19, 34)
            createPlayer(
                role = role,
                targetOvr = targetOvr,
                age = age,
                isYouth = false,
                tier = tier,
                usedNames = usedNames,
                random = random
            )
        }
    }

    fun createPlayer(
        role: T20Role,
        targetOvr: Int,
        age: Int,
        isYouth: Boolean,
        tier: Int,
        usedNames: MutableSet<String> = mutableSetOf(),
        random: Random = Random.Default
    ): Player {
        var fullName: String
        var attempts = 0
        do {
            fullName = "${FIRST_NAMES.random(random)} ${LAST_NAMES.random(random)}"
            attempts++
        } while (!usedNames.add(fullName) && attempts < 20)

        val batHand = if (random.nextFloat() < 0.34f) BattingHand.LEFT else BattingHand.RIGHT
        val bowlingStyle = when (role) {
            T20Role.PACE_SPEARHEAD, T20Role.DEATH_BOWLER -> listOf(
                BowlingStyle.RIGHT_ARM_FAST,
                BowlingStyle.LEFT_ARM_FAST,
                BowlingStyle.MEDIUM_PACE
            ).random(random)
            T20Role.SPINNER -> listOf(
                BowlingStyle.LEG_SPIN,
                BowlingStyle.OFF_SPIN,
                BowlingStyle.LEFT_ARM_ORTHODOX
            ).random(random)
            T20Role.ALL_ROUNDER -> listOf(
                BowlingStyle.MEDIUM_PACE,
                BowlingStyle.RIGHT_ARM_FAST,
                BowlingStyle.OFF_SPIN,
                BowlingStyle.LEFT_ARM_ORTHODOX
            ).random(random)
            else -> BowlingStyle.NONE
        }

        val attributes = when (role) {
            T20Role.OPENER, T20Role.TOP_ORDER -> PlayerAttributes(
                battingTiming = (targetOvr + random.nextInt(2, 9)).coerceIn(40, 96),
                powerHitting = (targetOvr + random.nextInt(-2, 8)).coerceIn(40, 95),
                paceBowling = random.nextInt(15, 35),
                spinVariation = random.nextInt(15, 35),
                deathExecution = (targetOvr - 8 + random.nextInt(-4, 6)).coerceIn(30, 88),
                fielding = (targetOvr + random.nextInt(-5, 8)).coerceIn(45, 92)
            )
            T20Role.MIDDLE_ORDER, T20Role.WICKETKEEPER -> PlayerAttributes(
                battingTiming = (targetOvr + random.nextInt(0, 7)).coerceIn(40, 95),
                powerHitting = (targetOvr + random.nextInt(0, 8)).coerceIn(40, 96),
                paceBowling = random.nextInt(12, 28),
                spinVariation = random.nextInt(12, 28),
                deathExecution = (targetOvr + random.nextInt(-2, 6)).coerceIn(38, 92),
                fielding = (targetOvr + random.nextInt(2, 12)).coerceIn(50, 96)
            )
            T20Role.FINISHER -> PlayerAttributes(
                battingTiming = (targetOvr - 2 + random.nextInt(-3, 6)).coerceIn(40, 92),
                powerHitting = (targetOvr + random.nextInt(5, 13)).coerceIn(50, 98),
                paceBowling = random.nextInt(20, 42),
                spinVariation = random.nextInt(15, 35),
                deathExecution = (targetOvr + random.nextInt(4, 10)).coerceIn(45, 96),
                fielding = (targetOvr + random.nextInt(-3, 7)).coerceIn(45, 92)
            )
            T20Role.ALL_ROUNDER -> PlayerAttributes(
                battingTiming = (targetOvr - 2 + random.nextInt(-4, 6)).coerceIn(42, 90),
                powerHitting = (targetOvr + random.nextInt(-2, 8)).coerceIn(42, 92),
                paceBowling = if (bowlingStyle.isPace) (targetOvr + random.nextInt(-3, 7)).coerceIn(45, 92) else random.nextInt(20, 38),
                spinVariation = if (!bowlingStyle.isPace) (targetOvr + random.nextInt(-3, 7)).coerceIn(45, 92) else random.nextInt(20, 38),
                deathExecution = (targetOvr + random.nextInt(-2, 6)).coerceIn(42, 90),
                fielding = (targetOvr + random.nextInt(-2, 7)).coerceIn(48, 92)
            )
            T20Role.PACE_SPEARHEAD -> PlayerAttributes(
                battingTiming = random.nextInt(22, 48),
                powerHitting = random.nextInt(25, 55),
                paceBowling = (targetOvr + random.nextInt(3, 10)).coerceIn(48, 97),
                spinVariation = random.nextInt(15, 35),
                deathExecution = (targetOvr + random.nextInt(-2, 7)).coerceIn(45, 94),
                fielding = (targetOvr + random.nextInt(-6, 5)).coerceIn(40, 88)
            )
            T20Role.DEATH_BOWLER -> PlayerAttributes(
                battingTiming = random.nextInt(20, 45),
                powerHitting = random.nextInt(24, 52),
                paceBowling = (targetOvr + random.nextInt(0, 8)).coerceIn(48, 95),
                spinVariation = random.nextInt(20, 40),
                deathExecution = (targetOvr + random.nextInt(6, 13)).coerceIn(54, 98),
                fielding = (targetOvr + random.nextInt(-5, 5)).coerceIn(42, 88)
            )
            T20Role.SPINNER -> PlayerAttributes(
                battingTiming = random.nextInt(25, 52),
                powerHitting = random.nextInt(22, 50),
                paceBowling = random.nextInt(15, 32),
                spinVariation = (targetOvr + random.nextInt(4, 11)).coerceIn(50, 97),
                deathExecution = (targetOvr + random.nextInt(0, 7)).coerceIn(46, 93),
                fielding = (targetOvr + random.nextInt(-4, 6)).coerceIn(42, 90)
            )
        }

        val potentialBoost = when {
            age <= 19 -> random.nextInt(12, 24)
            age <= 23 -> random.nextInt(6, 15)
            age <= 27 -> random.nextInt(2, 7)
            else -> 0
        }
        val potential = (targetOvr + potentialBoost).coerceIn(targetOvr, 95)
        val marketVal = ((targetOvr * targetOvr) * (if (age <= 24) 16 else 12) + (potential - targetOvr) * 1400)
            .coerceIn(18000, 320000)
        val weeklyWage = if (isYouth) 450 else ((targetOvr * targetOvr) / 2 + random.nextInt(400, 1400))
            .coerceIn(1100, 14000)

        return Player(
            id = "p_${UUID.randomUUID().toString().take(8)}",
            name = fullName,
            nationality = NATIONALITIES.random(random),
            age = age,
            role = role,
            battingHand = batHand,
            bowlingStyle = bowlingStyle,
            attributes = attributes,
            overall = targetOvr,
            potential = potential,
            form = random.nextInt(58, 84),
            fitness = random.nextInt(86, 99),
            morale = random.nextInt(65, 88),
            contract = PlayerContract(
                weeklyWage = weeklyWage,
                weeksRemaining = if (isYouth) 104 else random.nextInt(24, 96),
                releaseClause = (marketVal * 1.45).toInt()
            ),
            marketValue = marketVal,
            isYouthAcademy = isYouth,
            isForeign = random.nextFloat() < 0.35f
        )
    }

    private fun generateInitialStaff(tier: Int, random: Random): List<StaffMember> {
        val baseRating = when (tier) {
            1 -> 74
            2 -> 67
            else -> 61
        }
        val specialties = mapOf(
            StaffRole.HEAD_COACH to "Powerplay Blueprint",
            StaffRole.BATTING_COACH to "Boundary Geometry",
            StaffRole.BOWLING_COACH to "Death Yorker Specialist",
            StaffRole.PHYSIO to "Rapid Soft-Tissue Care",
            StaffRole.CHIEF_SCOUT to "Emerging U-19 Network",
            StaffRole.COMMERCIAL_DIRECTOR to "Matchday Hospitality"
        )
        return StaffRole.entries.map { role ->
            val rating = (baseRating + random.nextInt(-4, 6)).coerceIn(52, 85)
            StaffMember(
                id = "st_${role.name.lowercase()}_${UUID.randomUUID().toString().take(5)}",
                name = "${FIRST_NAMES.random(random)} ${LAST_NAMES.random(random)}",
                role = role,
                rating = rating,
                specialty = specialties.getValue(role),
                weeklySalary = rating * 45 + random.nextInt(200, 600),
                isHired = true
            )
        }
    }

    fun generateStaffCandidates(tier: Int, count: Int, random: Random): List<StaffMember> {
        val specialties = listOf(
            "Aggressive Chase Planner", "Six-Hitting Biomechanics", "Reverse-Swing & Cutters",
            "Accelerated Injury Rehab", "Global Franchise Scouting", "Broadcast Deal Broker",
            "Spin Choke Tactician", "Workload Telemetry"
        )
        return (0 until count).map { i ->
            val role = StaffRole.entries[i % StaffRole.entries.size]
            val minRating = if (tier == 1) 70 else 60
            val rating = random.nextInt(minRating, 93)
            StaffMember(
                id = "cand_${UUID.randomUUID().toString().take(7)}",
                name = "${FIRST_NAMES.random(random)} ${LAST_NAMES.random(random)}",
                role = role,
                rating = rating,
                specialty = specialties.random(random),
                weeklySalary = rating * 52 + random.nextInt(300, 900),
                isHired = false
            )
        }
    }

    fun generateSponsorDeals(tier: Int, random: Random): List<SponsorDeal> {
        val tierMult = when (tier) {
            1 -> 1.9
            2 -> 1.35
            else -> 1.0
        }
        return listOf(
            SponsorDeal(
                id = "sp_title_1",
                brandName = "VoltStream Energy",
                category = SponsorCategory.TITLE,
                weeklyPayout = (14500 * tierMult).toInt(),
                winBonus = (8000 * tierMult).toInt(),
                trophyBonus = (120000 * tierMult).toInt(),
                minRankTarget = 4,
                seasonsRemaining = 2,
                isActive = true
            ),
            SponsorDeal(
                id = "sp_title_2",
                brandName = "Apex Meridian Bank",
                category = SponsorCategory.TITLE,
                weeklyPayout = (18000 * tierMult).toInt(),
                winBonus = (11000 * tierMult).toInt(),
                trophyBonus = (165000 * tierMult).toInt(),
                minRankTarget = 2,
                seasonsRemaining = 2,
                isActive = false
            ),
            SponsorDeal(
                id = "sp_kit_1",
                brandName = "AeroWeave Athletics",
                category = SponsorCategory.KIT,
                weeklyPayout = (9500 * tierMult).toInt(),
                winBonus = (4500 * tierMult).toInt(),
                trophyBonus = (60000 * tierMult).toInt(),
                minRankTarget = 6,
                seasonsRemaining = 2,
                isActive = false
            ),
            SponsorDeal(
                id = "sp_kit_2",
                brandName = "Kinetix Pro Gear",
                category = SponsorCategory.KIT,
                weeklyPayout = (12200 * tierMult).toInt(),
                winBonus = (6000 * tierMult).toInt(),
                trophyBonus = (85000 * tierMult).toInt(),
                minRankTarget = 3,
                seasonsRemaining = 1,
                isActive = false
            ),
            SponsorDeal(
                id = "sp_stad_1",
                brandName = "Skyline Cloud Arena",
                category = SponsorCategory.STADIUM,
                weeklyPayout = (11000 * tierMult).toInt(),
                winBonus = (5000 * tierMult).toInt(),
                trophyBonus = (75000 * tierMult).toInt(),
                minRankTarget = 5,
                seasonsRemaining = 3,
                isActive = false
            ),
            SponsorDeal(
                id = "sp_stad_2",
                brandName = "Solara Beverages",
                category = SponsorCategory.STADIUM,
                weeklyPayout = (8500 * tierMult).toInt(),
                winBonus = (3800 * tierMult).toInt(),
                trophyBonus = (48000 * tierMult).toInt(),
                minRankTarget = 7,
                seasonsRemaining = 2,
                isActive = false
            )
        )
    }

    fun generateDivisionFixtures(divisions: List<DivisionTable>, random: Random): List<Fixture> {
        val allFixtures = mutableListOf<Fixture>()
        for (division in divisions) {
            val clubs = division.clubs
            val n = clubs.size
            if (n < 2) continue
            val rotating = clubs.drop(1).toMutableList()
            val fixed = clubs.first()

            // 14 rounds (home & away double round-robin for 8 clubs)
            for (leg in 0 until 2) {
                for (r in 0 until (n - 1)) {
                    val roundNumber = leg * (n - 1) + r + 1
                    val order = listOf(fixed) + rotating
                    for (pairIdx in 0 until n / 2) {
                        val c1 = order[pairIdx]
                        val c2 = order[n - 1 - pairIdx]
                        val home = if ((leg + pairIdx) % 2 == 0) c1 else c2
                        val away = if (home.clubId == c1.clubId) c2 else c1
                        allFixtures.add(
                            Fixture(
                                id = "fx_t${division.tier}_r${roundNumber}_${home.shortCode}_${away.shortCode}",
                                round = roundNumber,
                                tier = division.tier,
                                homeClubId = home.clubId,
                                homeClubName = home.name,
                                awayClubId = away.clubId,
                                awayClubName = away.name,
                                pitchType = PitchType.entries.random(random)
                            )
                        )
                    }
                    // Rotate
                    val last = rotating.removeAt(rotating.lastIndex)
                    rotating.add(0, last)
                }
            }
        }
        return allFixtures
    }
}
