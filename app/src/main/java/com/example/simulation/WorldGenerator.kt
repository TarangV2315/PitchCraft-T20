package com.example.simulation

import com.example.model.*
import java.util.UUID
import kotlin.random.Random

data class ClubTemplate(
    val clubId: String,
    val name: String,
    val shortCode: String,
    val country: CricketNation,
    val tier: Int,
    val baseStrength: Int,
    val stadiumCapacity: Int,
    val startingBudget: Int,
    val cityTag: String
)

object WorldGenerator {
    val ALL_CLUB_TEMPLATES: List<ClubTemplate> = listOf(
        // ============================================================
        // INDIA — 2-Tier Domestic T20 Pyramid (Major Cities vs Smaller Cities)
        // ============================================================
        // India Tier 1: Major Cities Premier League
        ClubTemplate("in_t1_1", "Mumbai Mariners", "MM", CricketNation.INDIA, 1, 83, 38000, 950000, "Mumbai"),
        ClubTemplate("in_t1_2", "Bengaluru Blazers", "BB", CricketNation.INDIA, 1, 81, 35000, 890000, "Bengaluru"),
        ClubTemplate("in_t1_3", "Chennai Spartans", "CS", CricketNation.INDIA, 1, 80, 34000, 870000, "Chennai"),
        ClubTemplate("in_t1_4", "Kolkata Knights United", "KK", CricketNation.INDIA, 1, 79, 36000, 850000, "Kolkata"),
        ClubTemplate("in_t1_5", "Delhi Emperors", "DE", CricketNation.INDIA, 1, 78, 33000, 840000, "New Delhi"),
        ClubTemplate("in_t1_6", "Hyderabad Sunrises", "HS", CricketNation.INDIA, 1, 77, 32000, 810000, "Hyderabad"),
        ClubTemplate("in_t1_7", "Ahmedabad Titans", "AT", CricketNation.INDIA, 1, 77, 42000, 830000, "Ahmedabad"),
        ClubTemplate("in_t1_8", "Pune Challengers", "PC", CricketNation.INDIA, 1, 75, 28000, 760000, "Pune"),
        // India Tier 2: Smaller Cities T20 Championship
        ClubTemplate("in_t2_1", "Jaipur Panthers", "JP", CricketNation.INDIA, 2, 72, 24000, 610000, "Jaipur"),
        ClubTemplate("in_t2_2", "Lucknow Nawabs", "LN", CricketNation.INDIA, 2, 71, 25000, 590000, "Lucknow"),
        ClubTemplate("in_t2_3", "Kochi Tuskers XI", "KT", CricketNation.INDIA, 2, 70, 22000, 560000, "Kochi"),
        ClubTemplate("in_t2_4", "Indore Warriors", "IW", CricketNation.INDIA, 2, 69, 20000, 540000, "Indore"),
        ClubTemplate("in_t2_5", "Rajkot Royals", "RR", CricketNation.INDIA, 2, 68, 19000, 520000, "Rajkot"),
        ClubTemplate("in_t2_6", "Ranchi Strikers", "RS", CricketNation.INDIA, 2, 67, 21000, 510000, "Ranchi"),
        ClubTemplate("in_t2_7", "Vizag Mariners", "VM", CricketNation.INDIA, 2, 66, 18000, 490000, "Visakhapatnam"),
        ClubTemplate("in_t2_8", "Guwahati Rhinos", "GR", CricketNation.INDIA, 2, 65, 19000, 480000, "Guwahati"),

        // ============================================================
        // ENGLAND — 2-Tier Domestic T20 Pyramid (Major Cities vs Smaller Cities)
        // ============================================================
        // England Tier 1: Major Cities T20 Blast
        ClubTemplate("en_t1_1", "London Royals", "LR", CricketNation.ENGLAND, 1, 81, 30000, 880000, "London"),
        ClubTemplate("en_t1_2", "Manchester Originals", "MO", CricketNation.ENGLAND, 1, 79, 26000, 820000, "Manchester"),
        ClubTemplate("en_t1_3", "Birmingham Bears T20", "BM", CricketNation.ENGLAND, 1, 78, 25000, 800000, "Birmingham"),
        ClubTemplate("en_t1_4", "Leeds Yorkshire Hawks", "LY", CricketNation.ENGLAND, 1, 77, 22000, 770000, "Leeds"),
        ClubTemplate("en_t1_5", "Nottingham Outlaws", "NO", CricketNation.ENGLAND, 1, 76, 20000, 740000, "Nottingham"),
        ClubTemplate("en_t1_6", "Liverpool Mersey Sixers", "LM", CricketNation.ENGLAND, 1, 74, 19000, 710000, "Liverpool"),
        ClubTemplate("en_t1_7", "Bristol Severnside", "BS", CricketNation.ENGLAND, 1, 73, 18000, 690000, "Bristol"),
        ClubTemplate("en_t1_8", "Sheffield Steelbacks", "SS", CricketNation.ENGLAND, 1, 72, 17000, 670000, "Sheffield"),
        // England Tier 2: Smaller Cities & Regional T20 Division
        ClubTemplate("en_t2_1", "Cardiff Dragons", "CD", CricketNation.ENGLAND, 2, 69, 16000, 540000, "Cardiff"),
        ClubTemplate("en_t2_2", "Southampton Coastal", "SC", CricketNation.ENGLAND, 2, 68, 17000, 530000, "Southampton"),
        ClubTemplate("en_t2_3", "Durham Riversiders", "DR", CricketNation.ENGLAND, 2, 67, 15000, 510000, "Durham"),
        ClubTemplate("en_t2_4", "Leicester Foxes T20", "LF", CricketNation.ENGLAND, 2, 66, 14000, 490000, "Leicester"),
        ClubTemplate("en_t2_5", "Worcester Rapids", "WR", CricketNation.ENGLAND, 2, 65, 13000, 470000, "Worcester"),
        ClubTemplate("en_t2_6", "Derby Falcons", "DF", CricketNation.ENGLAND, 2, 64, 13000, 460000, "Derby"),
        ClubTemplate("en_t2_7", "Northampton Mariners", "NM", CricketNation.ENGLAND, 2, 63, 12000, 450000, "Northampton"),
        ClubTemplate("en_t2_8", "Taunton Cidermen", "TC", CricketNation.ENGLAND, 2, 62, 12000, 440000, "Taunton"),

        // ============================================================
        // AUSTRALIA — Single National T20 League (1 Tier)
        // ============================================================
        ClubTemplate("au_t1_1", "Melbourne Strikeforce", "MS", CricketNation.AUSTRALIA, 1, 82, 36000, 900000, "Melbourne"),
        ClubTemplate("au_t1_2", "Sydney Harbour Six", "SH", CricketNation.AUSTRALIA, 1, 80, 32000, 860000, "Sydney"),
        ClubTemplate("au_t1_3", "Perth Scorching Heat", "PS", CricketNation.AUSTRALIA, 1, 79, 30000, 830000, "Perth"),
        ClubTemplate("au_t1_4", "Brisbane Heatwave", "BH", CricketNation.AUSTRALIA, 1, 78, 28000, 800000, "Brisbane"),
        ClubTemplate("au_t1_5", "Adelaide Meteors", "AM", CricketNation.AUSTRALIA, 1, 76, 25000, 760000, "Adelaide"),
        ClubTemplate("au_t1_6", "Hobart Hurricanes XI", "HH", CricketNation.AUSTRALIA, 1, 74, 18000, 710000, "Hobart"),
        ClubTemplate("au_t1_7", "Canberra Capitals T20", "CC", CricketNation.AUSTRALIA, 1, 72, 16000, 680000, "Canberra"),
        ClubTemplate("au_t1_8", "Gold Coast Breakers", "GC", CricketNation.AUSTRALIA, 1, 71, 17000, 660000, "Gold Coast"),

        // ============================================================
        // SOUTH AFRICA — Single National T20 League (1 Tier)
        // ============================================================
        ClubTemplate("za_t1_1", "Cape Town Titans", "CT", CricketNation.SOUTH_AFRICA, 1, 80, 28000, 820000, "Cape Town"),
        ClubTemplate("za_t1_2", "Johannesburg Rapids", "JR", CricketNation.SOUTH_AFRICA, 1, 79, 30000, 810000, "Johannesburg"),
        ClubTemplate("za_t1_3", "Durban Super Dolphins", "DS", CricketNation.SOUTH_AFRICA, 1, 77, 25000, 770000, "Durban"),
        ClubTemplate("za_t1_4", "Pretoria Capitals XI", "PR", CricketNation.SOUTH_AFRICA, 1, 76, 24000, 750000, "Pretoria"),
        ClubTemplate("za_t1_5", "Gqeberha Bay Warriors", "GW", CricketNation.SOUTH_AFRICA, 1, 74, 20000, 710000, "Gqeberha"),
        ClubTemplate("za_t1_6", "Paarl Royals XI", "PL", CricketNation.SOUTH_AFRICA, 1, 73, 18000, 690000, "Paarl"),
        ClubTemplate("za_t1_7", "Bloemfontein Knights", "BK", CricketNation.SOUTH_AFRICA, 1, 71, 17000, 660000, "Bloemfontein"),
        ClubTemplate("za_t1_8", "Potchefstroom Lions", "PT", CricketNation.SOUTH_AFRICA, 1, 70, 15000, 640000, "Potchefstroom"),

        // ============================================================
        // CARIBBEAN — Single Regional T20 League (1 Tier)
        // ============================================================
        ClubTemplate("cb_t1_1", "Kingston Hurricanes", "KH", CricketNation.CARIBBEAN, 1, 78, 22000, 760000, "Kingston"),
        ClubTemplate("cb_t1_2", "Trinidad Cavaliers", "TV", CricketNation.CARIBBEAN, 1, 77, 24000, 750000, "Port of Spain"),
        ClubTemplate("cb_t1_3", "Barbados Trident Kings", "BT", CricketNation.CARIBBEAN, 1, 76, 21000, 730000, "Bridgetown"),
        ClubTemplate("cb_t1_4", "Guyana Amazon Warriors", "GA", CricketNation.CARIBBEAN, 1, 75, 20000, 710000, "Providence"),
        ClubTemplate("cb_t1_5", "St Lucia Kings XI", "SL", CricketNation.CARIBBEAN, 1, 73, 17000, 680000, "Gros Islet"),
        ClubTemplate("cb_t1_6", "Antigua Barbuda Falcons", "AB", CricketNation.CARIBBEAN, 1, 72, 16000, 660000, "St John's"),
        ClubTemplate("cb_t1_7", "St Kitts Patriots", "SK", CricketNation.CARIBBEAN, 1, 70, 15000, 640000, "Basseterre"),
        ClubTemplate("cb_t1_8", "Grenada Spice Strikers", "GS", CricketNation.CARIBBEAN, 1, 69, 14000, 620000, "St George's")
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

    fun clubsFor(country: CricketNation, tier: Int): List<ClubTemplate> {
        val targetTier = if (country.supportsPromotionRelegation) tier.coerceIn(1, 2) else 1
        return ALL_CLUB_TEMPLATES.filter { it.country == country && it.tier == targetTier }
    }

    fun createNewCareer(
        template: ClubTemplate,
        managerName: String,
        random: Random = Random(System.currentTimeMillis())
    ): CareerState {
        val careerId = "career_${UUID.randomUUID().toString().take(8)}"
        val country = template.country
        val squad = generateClubSquad(template.tier, template.baseStrength, random, country)
        val market = generateTransferMarket(template.tier, count = 26, random = random, nation = country)
        val staff = generateInitialStaff(template.tier, random)
        val availableStaff = generateStaffCandidates(template.tier, count = 12, random = random)
        val sponsors = generateSponsorDeals(template.tier, random, country, template.cityTag)

        val tierRange = if (country.supportsPromotionRelegation) listOf(1, 2) else listOf(1)
        val divisions = tierRange.map { tier ->
            val name = divisionNameFor(country, tier)
            val clubs = ALL_CLUB_TEMPLATES
                .filter { it.country == country && it.tier == tier }
                .map { club ->
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
        val tournamentRosters = generateTournamentRosters(divisions, template.clubId, country, random)
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
                body = "Welcome to ${template.name}, $managerName. Our ${divisionNameFor(country, template.tier)} opener is queued against domestic rivals. Adjust training, tactics, and Playing XI yourself or delegate to your staff.",
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
            country = country,
            season = 1,
            week = 1,
            totalRounds = 14,
            divisionTier = template.tier,
            bankBalance = template.startingBudget,
            boardConfidence = 74,
            managerReputation = when (template.tier) {
                1 -> 62
                else -> 48
            },
            stadiumCapacity = template.stadiumCapacity,
            squad = squad,
            transferMarket = market,
            staff = staff,
            availableStaff = availableStaff,
            sponsors = sponsors,
            divisions = divisions,
            tournamentStats = tournamentRosters,
            fixtures = fixtures,
            inbox = starterInbox,
            selectedXIIds = initialXI.first,
            impactSubId = initialXI.second,
            clubCity = template.cityTag
        )
    }

    fun generateTournamentRosters(
        divisions: List<DivisionTable>,
        userClubId: String,
        @Suppress("UNUSED_PARAMETER") nation: CricketNation,
        random: Random
    ): List<TournamentPlayerStat> {
        val starRoles = listOf(
            T20Role.OPENER,
            T20Role.TOP_ORDER,
            T20Role.FINISHER,
            T20Role.PACE_SPEARHEAD,
            T20Role.DEATH_BOWLER,
            T20Role.SPINNER
        )
        val result = mutableListOf<TournamentPlayerStat>()
        for (div in divisions) {
            for (club in div.clubs) {
                if (club.clubId == userClubId) continue
                val usedNames = mutableSetOf<String>()
                starRoles.forEachIndexed { idx, role ->
                    var name: String
                    var attempts = 0
                    do {
                        name = "${FIRST_NAMES.random(random)} ${LAST_NAMES.random(random)}"
                        attempts++
                    } while (!usedNames.add(name) && attempts < 15)
                    result.add(
                        TournamentPlayerStat(
                            playerId = "tp_${club.clubId}_$idx",
                            playerName = name,
                            clubId = club.clubId,
                            clubName = club.name,
                            clubShortCode = club.shortCode,
                            tier = div.tier,
                            role = role,
                            stats = PlayerStats()
                        )
                    )
                }
            }
        }
        return result
    }

    fun divisionNameFor(country: CricketNation, tier: Int): String = when (country) {
        CricketNation.INDIA -> if (tier == 1) "India Tier 1 • Major Cities Premier" else "India Tier 2 • Minor Cities League"
        CricketNation.ENGLAND -> if (tier == 1) "England Tier 1 • Major Cities Blast" else "England Tier 2 • Minor Cities League"
        CricketNation.AUSTRALIA -> "Australia T20 Premier League"
        CricketNation.SOUTH_AFRICA -> "South Africa T20 League"
        CricketNation.CARIBBEAN -> "Caribbean T20 Premier League"
    }

    fun divisionTierShortLabel(country: CricketNation, tier: Int): String = when {
        country.supportsPromotionRelegation && tier == 1 -> "Major Cities"
        country.supportsPromotionRelegation && tier == 2 -> "Minor Cities"
        else -> "Single League"
    }

    fun divisionNameForTier(tier: Int): String = when (tier) {
        1 -> "Major Cities Premier T20"
        else -> "Minor Cities Championship"
    }

    private fun generateClubSquad(
        tier: Int,
        strength: Int,
        random: Random,
        nation: CricketNation
    ): List<Player> {
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
        val portraitPool = List(3) { (0 until PORTRAIT_VARIANT_COUNT).shuffled(random) }.flatten()
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
                random = random,
                nation = nation,
                portraitVariant = portraitPool[index]
            )
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
        val academy = academyRoles.mapIndexed { index, role ->
            createPlayer(
                role = role,
                targetOvr = (strength - 14 + random.nextInt(-3, 6)).coerceIn(46, 70),
                age = random.nextInt(16, 19),
                isYouth = true,
                tier = tier,
                usedNames = usedNames,
                random = random,
                nation = nation,
                portraitVariant = portraitPool[roleBlueprint.size + index]
            )
        }
        return senior + academy
    }

    fun generateTransferMarket(
        tier: Int,
        count: Int,
        random: Random,
        nation: CricketNation? = null
    ): List<Player> {
        val roles = T20Role.entries
        val usedNames = mutableSetOf<String>()
        val base = when (tier) {
            1 -> 74
            else -> 66
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
                random = random,
                nation = nation
            )
        }
    }

    const val PORTRAIT_VARIANT_COUNT = 8

    fun createPlayer(
        role: T20Role,
        targetOvr: Int,
        age: Int,
        isYouth: Boolean,
        tier: Int,
        usedNames: MutableSet<String> = mutableSetOf(),
        random: Random = Random.Default,
        nation: CricketNation? = null,
        portraitVariant: Int = random.nextInt(PORTRAIT_VARIANT_COUNT)
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

        val resolvedNationality = if (nation != null && (isYouth || random.nextFloat() < 0.74f)) {
            nation.shortCode
        } else {
            NATIONALITIES.random(random)
        }
        val isOverseas = nation != null && resolvedNationality != nation.shortCode

        return Player(
            id = "p_${UUID.randomUUID().toString().take(8)}",
            name = fullName,
            nationality = resolvedNationality,
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
            isForeign = isOverseas,
            portraitVariant = portraitVariant.coerceIn(0, PORTRAIT_VARIANT_COUNT - 1)
        )
    }

    private fun generateInitialStaff(tier: Int, random: Random): List<StaffMember> {
        val baseRating = when (tier) {
            1 -> 74
            else -> 65
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
                isHired = true,
                assignedFocus = role.defaultTrainingArea
            )
        }
    }

    fun generateStaffCandidates(tier: Int, count: Int, random: Random): List<StaffMember> {
        val specialtiesByRole = mapOf(
            StaffRole.HEAD_COACH to listOf("Aggressive Chase Planner", "Powerplay Blueprint", "Tactical Matchups"),
            StaffRole.BATTING_COACH to listOf("Six-Hitting Biomechanics", "Boundary Geometry", "Spin-Hitting Footwork"),
            StaffRole.BOWLING_COACH to listOf("Reverse-Swing & Cutters", "Death Yorker Specialist", "Spin Choke Tactician"),
            StaffRole.PHYSIO to listOf("Accelerated Injury Rehab", "Workload Telemetry", "Sprint & Agility Conditioning"),
            StaffRole.CHIEF_SCOUT to listOf("Global Franchise Scouting", "Emerging U-19 Network", "Data-Driven Recruitment"),
            StaffRole.COMMERCIAL_DIRECTOR to listOf("Broadcast Deal Broker", "Matchday Hospitality", "Global Brand Partnerships")
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
                specialty = specialtiesByRole.getValue(role).random(random),
                weeklySalary = rating * 52 + random.nextInt(300, 900),
                isHired = false,
                assignedFocus = role.defaultTrainingArea
            )
        }
    }

    fun generateSponsorDeals(
        tier: Int,
        random: Random,
        nation: CricketNation = CricketNation.INDIA,
        cityTag: String = "Mumbai"
    ): List<SponsorDeal> {
        return SponsorNegotiationEngine.generateSeasonSponsorPortfolio(
            tier = tier,
            careerContext = null,
            country = nation,
            cityTag = cityTag,
            random = random
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
