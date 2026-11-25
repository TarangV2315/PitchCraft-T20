package com.example.simulation

import com.example.model.*
import kotlin.math.roundToInt
import kotlin.random.Random

data class MatchSponsorBreakdown(
    val winBonus: Int,
    val fiftiesCount: Int,
    val centuriesCount: Int,
    val threeWicketHaulsCount: Int,
    val individualMilestoneBonus: Int
) {
    val totalBonus: Int get() = winBonus + individualMilestoneBonus
}

object SponsorNegotiationEngine {

    private val TIER_ONE_METRO_CITIES = setOf(
        "Mumbai", "Bengaluru", "Chennai", "Kolkata", "New Delhi", "Hyderabad", "Ahmedabad",
        "London", "Manchester", "Birmingham",
        "Melbourne", "Sydney", "Brisbane", "Perth",
        "Johannesburg", "Cape Town",
        "Kingston", "Port of Spain"
    )

    fun resolveCityName(career: CareerState): String {
        if (career.clubCity.isNotBlank()) return career.clubCity
        return WorldGenerator.ALL_CLUB_TEMPLATES
            .firstOrNull { it.clubId == career.clubId }
            ?.cityTag
            ?: career.clubName.substringBefore(" ")
    }

    fun evaluateClubLeverage(career: CareerState): SponsorLeverageProfile {
        val city = resolveCityName(career)
        val isMetro = TIER_ONE_METRO_CITIES.contains(city)
        val tierBonus = if (career.divisionTier == 1) 18 else 6
        val capacityScore = ((career.stadiumCapacity - 12000) / 800).coerceIn(0, 28)
        val metroBonus = if (isMetro) 24 else 11
        val nationFactor = when (career.country) {
            CricketNation.INDIA -> 22
            CricketNation.AUSTRALIA, CricketNation.ENGLAND -> 18
            CricketNation.SOUTH_AFRICA, CricketNation.CARIBBEAN -> 14
        }
        val cityDemographyScore = (metroBonus + tierBonus + capacityScore + nationFactor - 8).coerceIn(28, 99)
        val citySummary = if (isMetro) {
            "$city Metro Hub • ${(career.stadiumCapacity / 1000)}K seats • High broadcast reach"
        } else {
            "$city Regional Market • ${(career.stadiumCapacity / 1000)}K seats • Loyal local base"
        }

        // Buying pattern based on ticket pricing tier, attendance volume & active matchdays
        val crowdFillPct = (career.budgets.ticketTier.attendanceFactor * 100).roundToInt()
        val commercialDir = career.staff.firstOrNull { it.role == StaffRole.COMMERCIAL_DIRECTOR }?.rating ?: 64
        val tierBuyingScore = when (career.budgets.ticketTier) {
            TicketTier.ECONOMY -> 74
            TicketTier.STANDARD -> 78
            TicketTier.PREMIUM -> 82
        }
        val buyingPatternScore = (
            (tierBuyingScore * 0.65) +
                ((commercialDir - 50) * 0.45) +
                (if (isMetro) 14 else 6)
            ).roundToInt().coerceIn(32, 98)
        val buyingPatternSummary = when (career.budgets.ticketTier) {
            TicketTier.ECONOMY -> "$crowdFillPct% mass attendance • High retail & beverage volume"
            TicketTier.STANDARD -> "$crowdFillPct% balanced turnstile • Broad merchandise & mid-tier spend"
            TicketTier.PREMIUM -> "$crowdFillPct% VIP hospitality • High corporate & fintech spend"
        }

        // Tournament performance history (past season finishes + trophies + tier + board standing)
        val pastTopFourCount = career.seasonHistory.count { it.leagueRank <= 4 }
        val lastRank = career.seasonHistory.lastOrNull()?.leagueRank
        val historyBase = when {
            career.trophiesWon > 0 -> 72 + (career.trophiesWon * 8).coerceAtMost(22)
            pastTopFourCount > 0 -> 64 + (pastTopFourCount * 5).coerceAtMost(18)
            lastRank != null && lastRank <= 5 -> 58
            career.divisionTier == 1 -> 54
            else -> 44
        }
        val repAdj = ((career.managerReputation - 50) / 3) + ((career.boardConfidence - 65) / 4)
        val performanceHistoryScore = (historyBase + repAdj).coerceIn(28, 99)
        val performanceSummary = when {
            career.trophiesWon > 0 -> "${career.trophiesWon} Title(s) • Tier ${career.divisionTier} • Rep ${career.managerReputation}"
            career.seasonHistory.isNotEmpty() -> "Last finish #${lastRank ?: "-"} • ${pastTopFourCount} Playoff run(s)"
            else -> "Tier ${career.divisionTier} Debut • Board ${career.boardConfidence}% • Rep ${career.managerReputation}"
        }

        // Player calibre & star branding pull
        val seniorSquad = career.squad.filter { !it.isYouthAcademy }
        val top3Ovr = seniorSquad.sortedByDescending { it.overall }.take(3).map { it.overall }
        val marqueeAvg = if (top3Ovr.isNotEmpty()) top3Ovr.average() else 65.0
        val squadAvg = if (seniorSquad.isNotEmpty()) seniorSquad.map { it.overall }.average() else 62.0
        val highPotentialCount = seniorSquad.count { it.potential >= 84 }
        val playerCalibreScore = (
            ((marqueeAvg - 52) * 1.45) +
                ((squadAvg - 55) * 0.55) +
                (highPotentialCount * 2.5)
            ).roundToInt().coerceIn(30, 99)
        val starName = seniorSquad.maxByOrNull { it.overall }?.name ?: "Lead Star"
        val playerSummary = "Top-3 OVR ${marqueeAvg.roundToInt()} • $starName marquee appeal"

        val overallLeverageScore = (
            cityDemographyScore * 0.25 +
                buyingPatternScore * 0.22 +
                performanceHistoryScore * 0.25 +
                playerCalibreScore * 0.28
            ).roundToInt().coerceIn(30, 99)

        val maxLiftPct = (8 + (overallLeverageScore - 35) * 0.36).roundToInt().coerceIn(8, 32)

        return SponsorLeverageProfile(
            clubCity = city,
            cityDemographyScore = cityDemographyScore,
            cityDemographySummary = citySummary,
            buyingPatternScore = buyingPatternScore,
            buyingPatternSummary = buyingPatternSummary,
            performanceHistoryScore = performanceHistoryScore,
            performanceHistorySummary = performanceSummary,
            playerCalibreScore = playerCalibreScore,
            playerCalibreSummary = playerSummary,
            overallLeverageScore = overallLeverageScore,
            maxNegotiationLiftPct = maxLiftPct
        )
    }

    fun generateSeasonSponsorPortfolio(
        tier: Int,
        careerContext: CareerState? = null,
        country: CricketNation = careerContext?.country ?: CricketNation.INDIA,
        cityTag: String = careerContext?.let(::resolveCityName) ?: "Metro Hub",
        @Suppress("UNUSED_PARAMETER") random: Random = Random.Default
    ): List<SponsorDeal> {
        val tierMult = if (tier == 1) 1.78 else 1.18
        val nationMult = when (country) {
            CricketNation.INDIA -> 1.12
            CricketNation.AUSTRALIA, CricketNation.ENGLAND -> 1.04
            CricketNation.SOUTH_AFRICA, CricketNation.CARIBBEAN -> 0.94
        }
        val leverageMult = if (careerContext != null) {
            val lev = evaluateClubLeverage(careerContext)
            0.90 + (lev.overallLeverageScore / 280.0)
        } else {
            1.0
        }
        val starBrandingMult = if (careerContext != null) {
            val senior = careerContext.squad.filter { !it.isYouthAcademy }
            val topOvr = senior.sortedByDescending { it.overall }.take(3).map { it.overall }.average().takeIf { !it.isNaN() } ?: 72.0
            (0.85 + ((topOvr - 64.0) / 45.0)).coerceIn(0.80, 1.45)
        } else {
            if (tier == 1) 1.08 else 0.90
        }
        val m = tierMult * nationMult * leverageMult
        val isVipCrowd = careerContext?.budgets?.ticketTier == TicketTier.PREMIUM
        val isMassCrowd = careerContext?.budgets?.ticketTier == TicketTier.ECONOMY

        fun scaled(base: Int, extra: Double = 1.0): Int =
            (((base * m * extra) / 100.0).roundToInt() * 100).coerceAtLeast(200)

        val rawDeals = listOf(
            // =========================================================================
            // TITLE SPONSORS (3 contrasting options: Balanced, Safe Anchor, High Upside)
            // =========================================================================
            SponsorDeal(
                id = "sp_title_1",
                brandName = "VoltStream Energy",
                category = SponsorCategory.TITLE,
                industry = "Sports Nutrition",
                structureType = SponsorStructureType.BALANCED,
                weeklyPayout = scaled(12800),
                playerBrandingWeekly = scaled(2400, starBrandingMult),
                signingBonus = scaled(8000),
                winBonus = scaled(7600),
                perFiftyBonus = scaled(1500),
                perCenturyBonus = scaled(6500),
                perThreeWicketHaulBonus = scaled(1700),
                trophyBonus = scaled(95000),
                minRankTarget = 4,
                seasonsRemaining = 2,
                isActive = careerContext == null,
                cityFitSummary = "$cityTag Youth & Matchday Fit",
                contractSummary = "Balanced base retainer + steady star branding & match milestone payouts."
            ),
            SponsorDeal(
                id = "sp_title_2",
                brandName = "Apex Meridian Bank",
                category = SponsorCategory.TITLE,
                industry = "Private Fintech",
                structureType = SponsorStructureType.GUARANTEED_ANCHOR,
                weeklyPayout = scaled(18600, if (isVipCrowd) 1.10 else 1.0),
                playerBrandingWeekly = scaled(1400, starBrandingMult),
                signingBonus = scaled(26000),
                winBonus = scaled(3200),
                perFiftyBonus = scaled(400),
                perCenturyBonus = scaled(2000),
                perThreeWicketHaulBonus = scaled(500),
                trophyBonus = scaled(55000),
                minRankTarget = 3,
                seasonsRemaining = 2,
                isActive = false,
                cityFitSummary = if (isVipCrowd) "$cityTag VIP Hospitality Match" else "$cityTag Metro Commerce",
                contractSummary = "High guaranteed weekly floor & upfront cash; low bonus volatility."
            ),
            SponsorDeal(
                id = "sp_title_3",
                brandName = "IgniteX HyperSports",
                category = SponsorCategory.TITLE,
                industry = "Live Streaming",
                structureType = SponsorStructureType.PERFORMANCE_ESCALATOR,
                weeklyPayout = scaled(7600),
                playerBrandingWeekly = scaled(1900, starBrandingMult),
                signingBonus = 0,
                winBonus = scaled(15500),
                perFiftyBonus = scaled(3400),
                perCenturyBonus = scaled(16000),
                perThreeWicketHaulBonus = scaled(3800),
                trophyBonus = scaled(175000),
                minRankTarget = 2,
                seasonsRemaining = 1,
                isActive = false,
                cityFitSummary = "Digital Reach • Highlight Clips",
                contractSummary = "Low weekly base, huge per-win, per-century & 3W haul payout multipliers."
            ),

            // =========================================================================
            // KIT SPONSORS (Star Branding, Milestone Escalator, Safe Anchor)
            // =========================================================================
            SponsorDeal(
                id = "sp_kit_1",
                brandName = "AeroWeave Athletics",
                category = SponsorCategory.KIT,
                industry = "Performance Apparel",
                structureType = SponsorStructureType.STAR_BRANDING,
                weeklyPayout = scaled(6800),
                playerBrandingWeekly = scaled(5400, starBrandingMult * if (isMassCrowd) 1.12 else 1.0),
                signingBonus = scaled(6000),
                winBonus = scaled(3800),
                perFiftyBonus = scaled(1100),
                perCenturyBonus = scaled(4800),
                perThreeWicketHaulBonus = scaled(1200),
                trophyBonus = scaled(52000),
                minRankTarget = 5,
                seasonsRemaining = 2,
                isActive = false,
                cityFitSummary = if (isMassCrowd) "$cityTag High-Volume Jersey Sales" else "$cityTag Merch Licensing",
                contractSummary = "Star-calibre driven jersey royalties + solid weekly kit fee."
            ),
            SponsorDeal(
                id = "sp_kit_2",
                brandName = "Kinetix Pro Gear",
                category = SponsorCategory.KIT,
                industry = "Cricket Equipment",
                structureType = SponsorStructureType.PERFORMANCE_ESCALATOR,
                weeklyPayout = scaled(5200),
                playerBrandingWeekly = scaled(1600, starBrandingMult),
                signingBonus = 0,
                winBonus = scaled(5800),
                perFiftyBonus = scaled(2800),
                perCenturyBonus = scaled(13500),
                perThreeWicketHaulBonus = scaled(3200),
                trophyBonus = scaled(88000),
                minRankTarget = 3,
                seasonsRemaining = 1,
                isActive = false,
                cityFitSummary = "Player Stat Trigger Focused",
                contractSummary = "Rewards individual 50s, centuries & 3W hauls aggressively over flat retainers."
            ),
            SponsorDeal(
                id = "sp_kit_3",
                brandName = "Crestline Heritage Knit",
                category = SponsorCategory.KIT,
                industry = "Retail & Lifestyle",
                structureType = SponsorStructureType.GUARANTEED_ANCHOR,
                weeklyPayout = scaled(10900),
                playerBrandingWeekly = scaled(1200, starBrandingMult),
                signingBonus = scaled(14000),
                winBonus = scaled(1800),
                perFiftyBonus = 0,
                perCenturyBonus = scaled(1500),
                perThreeWicketHaulBonus = 0,
                trophyBonus = scaled(32000),
                minRankTarget = 6,
                seasonsRemaining = 2,
                isActive = false,
                cityFitSummary = "$cityTag Retail Storefront Network",
                contractSummary = "Predictable guaranteed weekly kit check with upfront signing advance."
            ),

            // =========================================================================
            // STADIUM SPONSORS (Balanced Hospitality, Crowd Win Bonus, Civic Naming)
            // =========================================================================
            SponsorDeal(
                id = "sp_stad_1",
                brandName = "Skyline Cloud Arena",
                category = SponsorCategory.STADIUM,
                industry = "Cloud Enterprise",
                structureType = SponsorStructureType.BALANCED,
                weeklyPayout = scaled(9600, if (isVipCrowd) 1.08 else 1.0),
                playerBrandingWeekly = scaled(1400, starBrandingMult),
                signingBonus = scaled(9000),
                winBonus = scaled(5200),
                perFiftyBonus = scaled(900),
                perCenturyBonus = scaled(4200),
                perThreeWicketHaulBonus = scaled(1100),
                trophyBonus = scaled(82000),
                minRankTarget = 4,
                seasonsRemaining = 3,
                isActive = false,
                cityFitSummary = "$cityTag Enterprise & Suites Fit",
                contractSummary = "Multi-year stadium naming stability paired with playoff & trophy kickers."
            ),
            SponsorDeal(
                id = "sp_stad_2",
                brandName = "Solara Beverages",
                category = SponsorCategory.STADIUM,
                industry = "Fan Beverages",
                structureType = SponsorStructureType.PERFORMANCE_ESCALATOR,
                weeklyPayout = scaled(5600),
                playerBrandingWeekly = scaled(2800, starBrandingMult * if (isMassCrowd) 1.15 else 1.0),
                signingBonus = scaled(4000),
                winBonus = scaled(8800),
                perFiftyBonus = scaled(1800),
                perCenturyBonus = scaled(8200),
                perThreeWicketHaulBonus = scaled(2200),
                trophyBonus = scaled(68000),
                minRankTarget = 5,
                seasonsRemaining = 2,
                isActive = false,
                cityFitSummary = if (isMassCrowd) "$cityTag Concourse Pouring Boom" else "$cityTag Matchday Concessions",
                contractSummary = "Lower stadium floor, boosted matchday win & crowd highlight bonuses."
            ),
            SponsorDeal(
                id = "sp_stad_3",
                brandName = "MetroRail Civic Group",
                category = SponsorCategory.STADIUM,
                industry = "Urban Mobility",
                structureType = SponsorStructureType.GUARANTEED_ANCHOR,
                weeklyPayout = scaled(12400),
                playerBrandingWeekly = scaled(900, starBrandingMult),
                signingBonus = scaled(16000),
                winBonus = scaled(1600),
                perFiftyBonus = 0,
                perCenturyBonus = 0,
                perThreeWicketHaulBonus = 0,
                trophyBonus = scaled(26000),
                minRankTarget = 7,
                seasonsRemaining = 2,
                isActive = false,
                cityFitSummary = "$cityTag Transit Hub Synergy",
                contractSummary = "Fixed civic naming guarantee ideal for low-risk cashflow planning."
            )
        )

        return rawDeals.map(::withProjectedTotals)
    }

    fun withProjectedTotals(deal: SponsorDeal): SponsorDeal {
        // Low campaign: 15 weeks, 2 wins, 2 fifties, 0 centuries, 2 three-wicket hauls
        val minProj = (deal.totalWeeklyGuaranteed * 15) +
            deal.signingBonus +
            (deal.winBonus * 2) +
            (deal.perFiftyBonus * 2) +
            (deal.perThreeWicketHaulBonus * 2)

        // Peak campaign: 17 weeks, 10 wins, 10 fifties, 2 centuries, 7 three-wicket hauls + trophy
        val maxProj = (deal.totalWeeklyGuaranteed * 17) +
            deal.signingBonus +
            (deal.winBonus * 10) +
            (deal.perFiftyBonus * 10) +
            (deal.perCenturyBonus * 2) +
            (deal.perThreeWicketHaulBonus * 7) +
            deal.trophyBonus

        return deal.copy(
            projectedSeasonMin = minProj,
            projectedSeasonMax = maxProj
        )
    }

    fun startNegotiation(deal: SponsorDeal, career: CareerState): SponsorNegotiationSession {
        val leverage = evaluateClubLeverage(career)
        val initialGreeting = when (deal.structureType) {
            SponsorStructureType.GUARANTEED_ANCHOR ->
                "${deal.brandName} values guaranteed stability in ${leverage.clubCity}. Max uplift headroom is ~+${leverage.maxNegotiationLiftPct}%."
            SponsorStructureType.PERFORMANCE_ESCALATOR ->
                "${deal.brandName} backs high-octane results! Push win & milestone bonuses higher—our flat base stays disciplined."
            SponsorStructureType.STAR_BRANDING ->
                "${deal.brandName} is excited by ${leverage.playerCalibreSummary}. Negotiate player branding & win kickers."
            SponsorStructureType.BALANCED ->
                "${deal.brandName} sees strong fit with your ${leverage.buyingPatternSummary.substringBefore("•").trim()}. Counter within reason."
        }
        return SponsorNegotiationSession(
            deal = deal,
            leverage = leverage,
            askBaseWeekly = deal.weeklyPayout,
            askBrandingWeekly = deal.playerBrandingWeekly,
            askWinBonus = deal.winBonus,
            askFiftyBonus = deal.perFiftyBonus,
            askCenturyBonus = deal.perCenturyBonus,
            askHaulBonus = deal.perThreeWicketHaulBonus,
            offeredBaseWeekly = deal.weeklyPayout,
            offeredBrandingWeekly = deal.playerBrandingWeekly,
            offeredWinBonus = deal.winBonus,
            offeredFiftyBonus = deal.perFiftyBonus,
            offeredCenturyBonus = deal.perCenturyBonus,
            offeredHaulBonus = deal.perThreeWicketHaulBonus,
            brandPatience = 100,
            round = 1,
            brandMessage = initialGreeting
        )
    }

    fun evaluateCounterOffer(session: SponsorNegotiationSession): SponsorNegotiationSession {
        if (session.isAccepted || session.isWalkedAway) return session
        val deal = session.deal
        val lev = session.leverage

        // Calculate season-equivalent weighted cost of original deal vs. user counter
        fun seasonWeightedValue(
            baseW: Int,
            brandW: Int,
            winB: Int,
            fiftyB: Int,
            centB: Int,
            haulB: Int
        ): Double {
            val baseWeight = when (deal.structureType) {
                SponsorStructureType.PERFORMANCE_ESCALATOR -> 19.5 // Penalizes pushing flat base on escalator brands
                SponsorStructureType.GUARANTEED_ANCHOR -> 14.5
                else -> 16.0
            }
            val brandDiscount = if (deal.structureType == SponsorStructureType.STAR_BRANDING && lev.playerCalibreScore >= 68) {
                13.5
            } else {
                16.0
            }
            val bonusWeight = when (deal.structureType) {
                SponsorStructureType.PERFORMANCE_ESCALATOR -> 0.86
                SponsorStructureType.GUARANTEED_ANCHOR -> 1.18
                else -> 1.0
            }
            return (baseW * baseWeight) +
                (brandW * brandDiscount) +
                ((winB * 6.5) + (fiftyB * 5.0) + (centB * 0.9) + (haulB * 4.0)) * bonusWeight
        }

        val initialVal = seasonWeightedValue(
            deal.weeklyPayout,
            deal.playerBrandingWeekly,
            deal.winBonus,
            deal.perFiftyBonus,
            deal.perCenturyBonus,
            deal.perThreeWicketHaulBonus
        ).coerceAtLeast(1000.0)

        val offeredVal = seasonWeightedValue(
            session.offeredBaseWeekly,
            session.offeredBrandingWeekly,
            session.offeredWinBonus,
            session.offeredFiftyBonus,
            session.offeredCenturyBonus,
            session.offeredHaulBonus
        )

        val askVal = seasonWeightedValue(
            session.askBaseWeekly,
            session.askBrandingWeekly,
            session.askWinBonus,
            session.askFiftyBonus,
            session.askCenturyBonus,
            session.askHaulBonus
        ).coerceAtLeast(initialVal)

        val maxAcceptableRatio = 1.0 + (lev.maxNegotiationLiftPct / 100.0)
        val offerVsInitialRatio = offeredVal / initialVal
        val offerVsAskRatio = offeredVal / askVal

        val liftPctAchieved = (((offeredVal / initialVal) - 1.0) * 100.0).roundToInt()

        return when {
            offerVsInitialRatio <= maxAcceptableRatio || offerVsAskRatio <= 1.02 -> {
                val finalized = withProjectedTotals(
                    deal.copy(
                        weeklyPayout = session.offeredBaseWeekly,
                        playerBrandingWeekly = session.offeredBrandingWeekly,
                        winBonus = session.offeredWinBonus,
                        perFiftyBonus = session.offeredFiftyBonus,
                        perCenturyBonus = session.offeredCenturyBonus,
                        perThreeWicketHaulBonus = session.offeredHaulBonus,
                        negotiatedPct = liftPctAchieved,
                        isActive = true
                    )
                )
                session.copy(
                    deal = finalized,
                    isAccepted = true,
                    brandMessage = "Agreed! ${deal.brandName} signed (${if (liftPctAchieved >= 0) "+$liftPctAchieved%" else "$liftPctAchieved%"} vs opening terms) anchored by your ${lev.clubCity} pull."
                )
            }

            session.brandPatience <= 26 || offerVsInitialRatio > maxAcceptableRatio * 1.36 -> {
                val nextPatience = (session.brandPatience - 38).coerceAtLeast(0)
                if (nextPatience <= 0) {
                    session.copy(
                        brandPatience = 0,
                        isWalkedAway = true,
                        brandMessage = "${deal.brandName} walked away from the table — demands exceeded ${lev.clubCity} market ceiling (+${lev.maxNegotiationLiftPct}% max)."
                    )
                } else {
                    val counterBase = blendUp(deal.weeklyPayout, session.offeredBaseWeekly, maxAcceptableRatio)
                    val counterBrand = blendUp(deal.playerBrandingWeekly, session.offeredBrandingWeekly, maxAcceptableRatio)
                    val counterWin = blendUp(deal.winBonus, session.offeredWinBonus, maxAcceptableRatio + 0.05)
                    val counterFifty = blendUp(deal.perFiftyBonus, session.offeredFiftyBonus, maxAcceptableRatio + 0.08)
                    val counterCentury = blendUp(deal.perCenturyBonus, session.offeredCenturyBonus, maxAcceptableRatio + 0.08)
                    val counterHaul = blendUp(deal.perThreeWicketHaulBonus, session.offeredHaulBonus, maxAcceptableRatio + 0.08)
                    session.copy(
                        askBaseWeekly = counterBase,
                        askBrandingWeekly = counterBrand,
                        askWinBonus = counterWin,
                        askFiftyBonus = counterFifty,
                        askCenturyBonus = counterCentury,
                        askHaulBonus = counterHaul,
                        brandPatience = nextPatience,
                        round = session.round + 1,
                        brandMessage = "Too aggressive for current Rank & City Index (${lev.overallLeverageScore}/100). Countered closer to our ceiling."
                    )
                }
            }

            else -> {
                val nextPatience = (session.brandPatience - 24).coerceAtLeast(12)
                val counterBase = ((session.offeredBaseWeekly + deal.weeklyPayout * maxAcceptableRatio) / 2.0).roundToInt() / 100 * 100
                val counterBrand = ((session.offeredBrandingWeekly + deal.playerBrandingWeekly * maxAcceptableRatio) / 2.0).roundToInt() / 100 * 100
                val counterWin = ((session.offeredWinBonus + deal.winBonus * (maxAcceptableRatio + 0.04)) / 2.0).roundToInt() / 100 * 100
                val counterFifty = ((session.offeredFiftyBonus + deal.perFiftyBonus * (maxAcceptableRatio + 0.06)) / 2.0).roundToInt() / 100 * 100
                val counterCentury = ((session.offeredCenturyBonus + deal.perCenturyBonus * (maxAcceptableRatio + 0.06)) / 2.0).roundToInt() / 100 * 100
                val counterHaul = ((session.offeredHaulBonus + deal.perThreeWicketHaulBonus * (maxAcceptableRatio + 0.06)) / 2.0).roundToInt() / 100 * 100

                session.copy(
                    askBaseWeekly = counterBase.coerceAtLeast(deal.weeklyPayout),
                    askBrandingWeekly = counterBrand.coerceAtLeast(deal.playerBrandingWeekly),
                    askWinBonus = counterWin.coerceAtLeast(deal.winBonus),
                    askFiftyBonus = counterFifty.coerceAtLeast(deal.perFiftyBonus),
                    askCenturyBonus = counterCentury.coerceAtLeast(deal.perCenturyBonus),
                    askHaulBonus = counterHaul.coerceAtLeast(deal.perThreeWicketHaulBonus),
                    brandPatience = nextPatience,
                    round = session.round + 1,
                    brandMessage = "Close! Your player calibre & buying pattern justify an uplift — meet near $$counterBase/wk base & $$counterWin/win."
                )
            }
        }
    }

    private fun blendUp(original: Int, offered: Int, maxMult: Double): Int {
        val ceiling = (original * maxMult).roundToInt()
        val mid = ((ceiling + offered.coerceAtMost((ceiling * 1.12).roundToInt())) / 2.0).roundToInt()
        return ((mid / 100) * 100).coerceAtLeast(original)
    }

    fun evaluateMatchSponsorPayout(
        sponsors: List<SponsorDeal>,
        userWon: Boolean,
        userBatters: List<BatterScoreEntry>,
        userBowlers: List<BowlerScoreEntry>
    ): MatchSponsorBreakdown {
        val active = sponsors.filter { it.isActive }
        val winBonus = if (userWon) active.sumOf { it.winBonus } else 0

        val fifties = userBatters.count { it.runs in 50..99 }
        val centuries = userBatters.count { it.runs >= 100 }
        val hauls = userBowlers.count { it.wickets >= 3 }

        val fiftyBonusTotal = fifties * active.sumOf { it.perFiftyBonus }
        val centuryBonusTotal = centuries * active.sumOf { it.perCenturyBonus }
        val haulBonusTotal = hauls * active.sumOf { it.perThreeWicketHaulBonus }

        return MatchSponsorBreakdown(
            winBonus = winBonus,
            fiftiesCount = fifties,
            centuriesCount = centuries,
            threeWicketHaulsCount = hauls,
            individualMilestoneBonus = fiftyBonusTotal + centuryBonusTotal + haulBonusTotal
        )
    }
}
