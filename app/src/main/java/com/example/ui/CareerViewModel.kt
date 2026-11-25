package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CareerRepository
import com.example.data.local.CareerSummary
import com.example.data.local.WelcomeRewardsState
import com.example.data.local.WelcomeRewardsStore
import com.example.model.*
import com.example.simulation.*
import com.example.ui.screens.OfficeSubTab
import com.example.ui.screens.SquadSubTab
import com.example.ui.screens.TrainingMedicalSubTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

enum class MainTab(val label: String) {
    HQ("HQ"),
    SQUAD("Squad"),
    TRANSFERS("Transfers"),
    TRAINING_MEDICAL("Training & Medical"),
    OFFICE("Office"),
    MATCHDAY("MatchDay");

    companion object {
        fun bottomNavForPhase(isPreSeason: Boolean): List<MainTab> = if (isPreSeason) {
            listOf(HQ, SQUAD, TRANSFERS, OFFICE)
        } else {
            listOf(HQ, SQUAD, TRAINING_MEDICAL, OFFICE)
        }
    }
}

class CareerViewModel(
    private val repository: CareerRepository,
    private val rewardsStore: WelcomeRewardsStore? = null
) : ViewModel() {

    val savedCareers: StateFlow<List<CareerSummary>> = repository.careerSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _welcomeRewards = MutableStateFlow(rewardsStore?.getState() ?: WelcomeRewardsState())
    val welcomeRewards: StateFlow<WelcomeRewardsState> = _welcomeRewards.asStateFlow()

    private val _activeCareer = MutableStateFlow<CareerState?>(null)
    val activeCareer: StateFlow<CareerState?> = _activeCareer.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.HQ)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _officeSubTab = MutableStateFlow(OfficeSubTab.STAFF_DELEGATION)
    val officeSubTab: StateFlow<OfficeSubTab> = _officeSubTab.asStateFlow()

    private val _squadSubTab = MutableStateFlow(SquadSubTab.ROSTER)
    val squadSubTab: StateFlow<SquadSubTab> = _squadSubTab.asStateFlow()

    private val _trainingMedicalSubTab = MutableStateFlow(TrainingMedicalSubTab.TRAINING)
    val trainingMedicalSubTab: StateFlow<TrainingMedicalSubTab> = _trainingMedicalSubTab.asStateFlow()

    private val _inspectedPlayerId = MutableStateFlow<String?>(null)
    val inspectedPlayerId: StateFlow<String?> = _inspectedPlayerId.asStateFlow()

    private val _negotiation = MutableStateFlow<NegotiationSession?>(null)
    val negotiation: StateFlow<NegotiationSession?> = _negotiation.asStateFlow()

    private val _sponsorNegotiation = MutableStateFlow<SponsorNegotiationSession?>(null)
    val sponsorNegotiation: StateFlow<SponsorNegotiationSession?> = _sponsorNegotiation.asStateFlow()

    private val _liveMatch = MutableStateFlow<LiveMatchState?>(null)
    val liveMatch: StateFlow<LiveMatchState?> = _liveMatch.asStateFlow()

    private val _isAutoStreaming = MutableStateFlow(false)
    val isAutoStreaming: StateFlow<Boolean> = _isAutoStreaming.asStateFlow()

    private val _streamSpeedMs = MutableStateFlow(750L)
    val streamSpeedMs: StateFlow<Long> = _streamSpeedMs.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var activeMatchRuntime: ActiveMatchRuntime? = null
    private var liveStreamJob: Job? = null

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun postNotice(message: String) {
        _snackbarMessage.value = message
    }

    fun selectTab(tab: MainTab) {
        val career = _activeCareer.value
        if (tab == MainTab.TRANSFERS) {
            if (career != null && !career.isTransferWindowOpen) {
                postNotice(career.transferWindowStatus.subtitle)
                return
            }
        }
        if (tab == MainTab.MATCHDAY && career?.isPreSeason == true) {
            startSeason()
            return
        }
        if (tab == MainTab.SQUAD && career != null) {
            val allowed = SquadSubTab.forPhase(career.isPreSeason)
            if (_squadSubTab.value !in allowed) {
                _squadSubTab.value = allowed.first()
            }
        }
        if (tab == MainTab.OFFICE && career != null) {
            val allowed = OfficeSubTab.forPhase(career.isPreSeason)
            if (_officeSubTab.value !in allowed) {
                _officeSubTab.value = allowed.first()
            }
        }
        _selectedTab.value = tab
    }

    fun selectOfficeSubTab(subTab: OfficeSubTab) {
        _officeSubTab.value = subTab
    }

    fun openOfficeFinances() {
        _officeSubTab.value = OfficeSubTab.FINANCE_SPONSORS
        _selectedTab.value = MainTab.OFFICE
    }

    fun openStaffOffice() {
        _officeSubTab.value = OfficeSubTab.STAFF_DELEGATION
        _selectedTab.value = MainTab.OFFICE
    }

    fun openSeasonCalendar() {
        _officeSubTab.value = OfficeSubTab.SEASON_CALENDAR
        _selectedTab.value = MainTab.OFFICE
    }

    fun openLeagueStandings() {
        _officeSubTab.value = OfficeSubTab.PYRAMID
        _selectedTab.value = MainTab.OFFICE
    }

    fun selectSquadSubTab(subTab: SquadSubTab) {
        _squadSubTab.value = subTab
    }

    fun selectTrainingMedicalSubTab(subTab: TrainingMedicalSubTab) {
        _trainingMedicalSubTab.value = subTab
    }

    fun openMedicalRoom() {
        _trainingMedicalSubTab.value = TrainingMedicalSubTab.MEDICAL
        _selectedTab.value = MainTab.TRAINING_MEDICAL
    }

    fun openPlayerActivity(playerId: String) {
        _inspectedPlayerId.value = playerId
    }

    fun closePlayerActivity() {
        _inspectedPlayerId.value = null
    }

    fun refreshWelcomeRewards() {
        rewardsStore?.let { _welcomeRewards.value = it.getState() }
    }

    fun recordRewardedAdWatched() {
        val store = rewardsStore
        if (store != null) {
            val outcome = store.recordAdWatched()
            _welcomeRewards.value = outcome.updatedState
            if (outcome.granted) {
                postNotice(
                    "Earned +${formatCurrencyNotice(outcome.rewardAdded)} currency! (${outcome.updatedState.adsWatchedToday}/${outcome.updatedState.maxAdsPerDay} ads today)"
                )
            } else {
                postNotice("Daily ad limit reached (5/5). Come back tomorrow!")
            }
        } else {
            val current = _welcomeRewards.value
            if (!current.canWatchAd) {
                postNotice("Daily ad limit reached (5/5). Come back tomorrow!")
                return
            }
            val next = current.copy(
                reserveCurrency = current.reserveCurrency + current.rewardPerAd,
                adsWatchedToday = current.adsWatchedToday + 1
            )
            _welcomeRewards.value = next
            postNotice(
                "Earned +${formatCurrencyNotice(current.rewardPerAd)} currency! (${next.adsWatchedToday}/${next.maxAdsPerDay} ads today)"
            )
        }
    }

    private fun consumeBonusReserve(): Int {
        val store = rewardsStore
        return if (store != null) {
            val bonus = store.consumeReserveCurrency()
            _welcomeRewards.value = store.getState()
            bonus
        } else {
            val bonus = _welcomeRewards.value.reserveCurrency
            if (bonus > 0) {
                _welcomeRewards.value = _welcomeRewards.value.copy(reserveCurrency = 0)
            }
            bonus
        }
    }

    private fun applyWelcomeBonusIfAny(career: CareerState, bonus: Int): CareerState {
        if (bonus <= 0) return career
        return career.copy(
            bankBalance = career.bankBalance + bonus,
            ledger = (listOf(
                FinanceLedgerEntry(
                    id = "ld_ad_${UUID.randomUUID().toString().take(6)}",
                    season = career.season,
                    week = career.week,
                    category = "Sponsor Ad Reserve",
                    description = "Claimed Welcome Ad Currency",
                    amount = bonus
                )
            ) + career.ledger).take(40)
        )
    }

    fun createNewCareer(club: ClubTemplate, managerName: String) {
        viewModelScope.launch {
            val baseCareer = WorldGenerator.createNewCareer(club, managerName)
            val bonus = consumeBonusReserve()
            val newCareer = applyWelcomeBonusIfAny(baseCareer, bonus)
            repository.saveCareer(newCareer)
            _activeCareer.value = newCareer
            _selectedTab.value = MainTab.HQ
            _squadSubTab.value = SquadSubTab.ROSTER
            _officeSubTab.value = OfficeSubTab.STAFF_DELEGATION
            _inspectedPlayerId.value = null
            if (bonus > 0) {
                postNotice("Signed as Manager of ${club.name} (+${formatCurrencyNotice(bonus)} ad bonus applied)!")
            } else {
                postNotice("Signed contract as Manager of ${club.name}!")
            }
        }
    }

    fun loadCareer(careerId: String) {
        viewModelScope.launch {
            val loaded = repository.loadCareer(careerId)
            if (loaded != null) {
                val normalized = if (loaded.week > 1 && loaded.isPreSeason) {
                    loaded.copy(isPreSeason = false)
                } else {
                    loaded
                }
                val bonus = consumeBonusReserve()
                val updated = applyWelcomeBonusIfAny(normalized, bonus)
                if (bonus > 0 || normalized != loaded) {
                    repository.saveCareer(updated)
                }
                if (bonus > 0) {
                    postNotice("Resumed ${updated.clubName} (+${formatCurrencyNotice(bonus)} ad bonus deposited)!")
                }
                _activeCareer.value = updated
                _selectedTab.value = MainTab.HQ
                _squadSubTab.value = SquadSubTab.forPhase(updated.isPreSeason).first()
                _trainingMedicalSubTab.value = TrainingMedicalSubTab.TRAINING
                _officeSubTab.value = OfficeSubTab.forPhase(updated.isPreSeason).first()
                _inspectedPlayerId.value = null
            } else {
                postNotice("Unable to load saved career.")
            }
        }
    }

    fun deleteCareer(careerId: String) {
        viewModelScope.launch {
            repository.deleteCareer(careerId)
            if (_activeCareer.value?.careerId == careerId) {
                _activeCareer.value = null
            }
            postNotice("Career save deleted.")
        }
    }

    fun exitToLauncher() {
        stopAutoStream()
        _activeCareer.value = null
        _inspectedPlayerId.value = null
        _sponsorNegotiation.value = null
        _liveMatch.value = null
        activeMatchRuntime = null
    }

    private fun updateAndPersist(transform: (CareerState) -> CareerState) {
        val current = _activeCareer.value ?: return
        val updated = transform(current)
        _activeCareer.value = updated
        if (!updated.isTransferWindowOpen) {
            if (_selectedTab.value == MainTab.TRANSFERS) {
                _selectedTab.value = MainTab.HQ
            }
            if (_negotiation.value?.isRenewal == false) {
                _negotiation.value = null
            }
        }
        if (!updated.isSponsorshipWindowOpen) {
            _sponsorNegotiation.value = null
        }
        if (updated != current) {
            viewModelScope.launch {
                repository.saveCareer(updated)
            }
        }
    }

    fun toggleDelegation(area: DelegationArea) {
        updateAndPersist { state ->
            val next = state.delegation.toggled(area)
            val delegated = next.isDelegated(area)
            postNotice(
                if (delegated) "${area.label} delegated to ${area.staffOwner.label}."
                else "You took direct control of ${area.label}."
            )
            state.copy(delegation = next)
        }
    }

    fun setPlayerTraining(playerId: String, focus: TrainingFocus) {
        updateAndPersist { state ->
            state.copy(
                squad = state.squad.map {
                    if (it.id == playerId) it.copy(trainingFocus = focus) else it
                }
            )
        }
        postNotice("Training updated to ${focus.label}.")
    }

    fun setTrainingIntensity(intensity: TrainingIntensity) {
        updateAndPersist { state ->
            state.copy(trainingIntensity = intensity)
        }
        postNotice("Squad training intensity set to ${intensity.label}.")
    }

    fun setPlayerRehab(playerId: String, mode: RehabMode) {
        updateAndPersist { state ->
            state.copy(
                squad = state.squad.map {
                    if (it.id == playerId && it.injury.isInjured) {
                        it.copy(injury = it.injury.copy(rehabMode = mode))
                    } else it
                }
            )
        }
        postNotice("Rehab plan set to ${mode.label}.")
    }

    fun promoteYouthPlayer(playerId: String) {
        updateAndPersist { state ->
            val player = state.squad.firstOrNull { it.id == playerId } ?: return@updateAndPersist state
            val newWage = (player.overall * 32).coerceAtLeast(1000)
            postNotice("${player.name} promoted to the Senior T20 Squad!")
            state.copy(
                squad = state.squad.map {
                    if (it.id == playerId) {
                        it.copy(
                            isYouthAcademy = false,
                            contract = it.contract.copy(weeklyWage = newWage, weeksRemaining = 78),
                            morale = (it.morale + 10).coerceAtMost(99)
                        )
                    } else it
                }
            )
        }
    }

    fun applyStaffRecommendation(player: Player, rec: StaffRecommendation) {
        when (rec.actionType) {
            RecommendationActionType.SET_TRAINING_DRILL -> {
                rec.targetDrill?.let { setPlayerTraining(player.id, it) }
            }
            RecommendationActionType.SET_REHAB_MODE -> {
                rec.targetRehab?.let { setPlayerRehab(player.id, it) }
            }
            RecommendationActionType.PROMOTE_YOUTH -> {
                promoteYouthPlayer(player.id)
            }
            RecommendationActionType.OFFER_CONTRACT_RENEWAL -> {
                openNegotiation(player, isRenewal = true)
            }
            RecommendationActionType.INCLUDE_IN_XI -> {
                updateAndPersist { state ->
                    if (state.selectedXIIds.contains(player.id)) return@updateAndPersist state
                    val updatedXI = if (state.selectedXIIds.size >= 11) {
                        state.selectedXIIds.dropLast(1) + player.id
                    } else {
                        state.selectedXIIds + player.id
                    }
                    postNotice("${player.name} added to Matchday XI.")
                    state.copy(selectedXIIds = updatedXI)
                }
            }
            RecommendationActionType.TOGGLE_TRANSFER_LIST -> {
                toggleTransferList(player.id)
            }
        }
    }

    fun toggleTransferList(playerId: String) {
        updateAndPersist { state ->
            var listed = false
            val updated = state.squad.map {
                if (it.id == playerId) {
                    listed = !it.isOnTransferList
                    it.copy(isOnTransferList = listed)
                } else it
            }
            postNotice(if (listed) "Player placed on transfer list." else "Removed from transfer list.")
            state.copy(squad = updated)
        }
    }

    fun sellListedPlayer(playerId: String) {
        updateAndPersist { state ->
            if (!state.isTransferWindowOpen) {
                postNotice("Transfers are locked outside the 1-week Pre-Season & Mid-Season windows.")
                return@updateAndPersist state
            }
            val player = state.squad.firstOrNull { it.id == playerId } ?: return@updateAndPersist state
            if (state.squad.count { !it.isYouthAcademy } <= 12) {
                postNotice("Keep at least 12 senior players for squad depth.")
                return@updateAndPersist state
            }
            val saleFee = (player.marketValue * 0.85).roundToInt()
            postNotice("Sold ${player.name} for $$saleFee!")
            state.copy(
                bankBalance = state.bankBalance + saleFee,
                squad = state.squad.filterNot { it.id == playerId },
                selectedXIIds = state.selectedXIIds.filterNot { it == playerId },
                impactSubId = if (state.impactSubId == playerId) null else state.impactSubId,
                ledger = (listOf(
                    FinanceLedgerEntry("ld_sale_${UUID.randomUUID().toString().take(6)}", state.season, state.week, "Transfers", "Sold ${player.name}", saleFee)
                ) + state.ledger).take(40)
            )
        }
        _inspectedPlayerId.value = null
    }

    fun toggleShortlist(playerId: String) {
        updateAndPersist { state ->
            state.copy(
                transferMarket = state.transferMarket.map {
                    if (it.id == playerId) it.copy(isShortlisted = !it.isShortlisted) else it
                }
            )
        }
    }

    fun openNegotiation(player: Player, isRenewal: Boolean) {
        val career = _activeCareer.value
        if (!isRenewal && career != null && !career.isTransferWindowOpen) {
            postNotice("Transfer window is closed! Opens for 1 week at Pre-Season (W1) and Mid-Season (W${career.midSeasonTransferWeek}).")
            return
        }
        val askingFee = if (isRenewal) 0 else player.marketValue
        val askingWage = if (isRenewal) (player.contract.weeklyWage * 1.12).roundToInt() else (player.contract.weeklyWage * 1.08).roundToInt()
        _negotiation.value = NegotiationSession(
            player = player,
            isRenewal = isRenewal,
            askingFee = askingFee,
            askingWage = askingWage,
            askingYears = 2,
            offeredFee = askingFee,
            offeredWage = askingWage,
            offeredSigningBonus = if (isRenewal) 4000 else 8000,
            offeredYears = 2,
            agentPatience = 100,
            round = 1,
            agentMessage = if (isRenewal) {
                "We are open to extending at ${askingWage}/wk."
            } else {
                "Expecting ~$${askingFee} transfer fee and $${askingWage}/wk wages."
            }
        )
    }

    fun updateNegotiationOffer(fee: Int, wage: Int, bonus: Int, years: Int) {
        val current = _negotiation.value ?: return
        _negotiation.value = current.copy(
            offeredFee = fee.coerceAtLeast(0),
            offeredWage = wage.coerceAtLeast(800),
            offeredSigningBonus = bonus.coerceAtLeast(0),
            offeredYears = years.coerceIn(1, 4)
        )
    }

    fun submitNegotiationOffer() {
        val current = _negotiation.value ?: return
        val career = _activeCareer.value ?: return
        if (!current.isRenewal && !career.isTransferWindowOpen) {
            postNotice("Transfer window closed — bids cannot be completed right now.")
            _negotiation.value = null
            return
        }
        val totalUpfront = current.offeredFee + current.offeredSigningBonus
        if (totalUpfront > career.bankBalance) {
            postNotice("Insufficient club funds ($${career.bankBalance} available) for upfront fee + bonus.")
            return
        }
        val evaluated = SeasonProgressionEngine.evaluateNegotiationOffer(current, career)
        _negotiation.value = evaluated
        if (evaluated.isAccepted) {
            val signedPlayer = current.player.copy(
                contract = PlayerContract(
                    weeklyWage = current.offeredWage,
                    weeksRemaining = current.offeredYears * 52,
                    releaseClause = (current.player.marketValue * 1.5).roundToInt()
                ),
                morale = (current.player.morale + 10).coerceAtMost(99),
                isYouthAcademy = false,
                isShortlisted = false
            )
            updateAndPersist { state ->
                val newSquad = if (current.isRenewal) {
                    state.squad.map { if (it.id == signedPlayer.id) signedPlayer else it }
                } else {
                    state.squad + signedPlayer
                }
                val newMarket = if (current.isRenewal) state.transferMarket else state.transferMarket.filterNot { it.id == signedPlayer.id }
                state.copy(
                    bankBalance = state.bankBalance - totalUpfront,
                    squad = newSquad,
                    transferMarket = newMarket,
                    ledger = (listOf(
                        FinanceLedgerEntry(
                            id = "ld_tr_${UUID.randomUUID().toString().take(6)}",
                            season = state.season,
                            week = state.week,
                            category = if (current.isRenewal) "Renewals" else "Transfers",
                            description = "${if (current.isRenewal) "Renewed" else "Signed"} ${signedPlayer.name}",
                            amount = -totalUpfront
                        )
                    ) + state.ledger).take(40)
                )
            }
            postNotice("${signedPlayer.name} signed on $${current.offeredWage}/wk!")
        }
    }

    fun closeNegotiation() {
        _negotiation.value = null
    }

    fun hireStaffMember(candidate: StaffMember, focusOverride: CoachTrainingArea? = null) {
        updateAndPersist { state ->
            val signingCost = candidate.weeklySalary * 4
            if (state.bankBalance < signingCost) {
                postNotice("Need ${formatCurrencyNotice(signingCost)} signing compensation to hire ${candidate.name}.")
                return@updateAndPersist state
            }
            val resolvedFocus = if (candidate.isCoach) {
                focusOverride ?: candidate.effectiveTrainingArea ?: candidate.role.defaultTrainingArea
            } else null
            val hiredCandidate = candidate.copy(isHired = true, assignedFocus = resolvedFocus)
            val replaced = state.staff.firstOrNull { it.role == candidate.role }
            val updatedHired = state.staff.filterNot { it.role == candidate.role } + hiredCandidate
            val updatedPool = state.availableStaff.filterNot { it.id == candidate.id } +
                listOfNotNull(replaced?.copy(isHired = false))
            val focusSuffix = resolvedFocus?.let { " → ${it.label} Focus" } ?: ""
            postNotice("Hired ${candidate.name} (${candidate.rating}) as ${candidate.role.label}$focusSuffix.")
            state.copy(
                bankBalance = state.bankBalance - signingCost,
                staff = updatedHired.sortedBy { it.role.ordinal },
                availableStaff = updatedPool,
                ledger = (listOf(
                    FinanceLedgerEntry("ld_st_${UUID.randomUUID().toString().take(6)}", state.season, state.week, "Staff", "Hired ${candidate.name}", -signingCost)
                ) + state.ledger).take(40)
            )
        }
    }

    fun assignCoachTrainingFocus(staffId: String, area: CoachTrainingArea) {
        updateAndPersist { state ->
            val target = state.staff.firstOrNull { it.id == staffId } ?: return@updateAndPersist state
            if (!target.isCoach) return@updateAndPersist state
            val updatedStaff = state.staff.map {
                if (it.id == staffId) it.copy(assignedFocus = area) else it
            }
            postNotice("${target.name} assigned to ${area.label} Progression.")
            state.copy(staff = updatedStaff)
        }
    }

    private fun formatCurrencyNotice(amount: Int): String = "$$amount"

    fun signSponsorDeal(dealId: String) {
        updateAndPersist { state ->
            if (!state.isSponsorshipWindowOpen) {
                postNotice("Sponsorship portfolio is frozen after Week 1! Re-opens at Season ${state.season + 1} Pre-Season.")
                return@updateAndPersist state
            }
            val target = state.sponsors.firstOrNull { it.id == dealId } ?: return@updateAndPersist state
            if (target.isActive) return@updateAndPersist state
            val updatedSponsors = state.sponsors.map { sp ->
                when {
                    sp.id == dealId -> sp.copy(isActive = true)
                    sp.category == target.category -> sp.copy(isActive = false)
                    else -> sp
                }
            }
            val bonus = target.signingBonus
            val updatedLedger = if (bonus > 0) {
                (listOf(
                    FinanceLedgerEntry(
                        id = "ld_sp_sign_${UUID.randomUUID().toString().take(6)}",
                        season = state.season,
                        week = state.week,
                        category = "Sponsorship",
                        description = "Signing advance: ${target.brandName} (${target.category.label})",
                        amount = bonus
                    )
                ) + state.ledger).take(40)
            } else state.ledger

            val signingTag = if (bonus > 0) " + $${bonus} signing advance" else ""
            postNotice("Signed ${target.category.label} deal with ${target.brandName} ($${target.totalWeeklyGuaranteed}/wk$signingTag)!")
            state.copy(
                bankBalance = state.bankBalance + bonus,
                seasonSponsorIncome = state.seasonSponsorIncome + bonus,
                sponsors = updatedSponsors,
                ledger = updatedLedger
            )
        }
    }

    fun openSponsorNegotiation(dealId: String) {
        val career = _activeCareer.value ?: return
        if (!career.isSponsorshipWindowOpen) {
            postNotice("Sponsorship negotiations are frozen after Week 1 until Season ${career.season + 1} Pre-Season.")
            return
        }
        val target = career.sponsors.firstOrNull { it.id == dealId } ?: return
        _sponsorNegotiation.value = SponsorNegotiationEngine.startNegotiation(target, career)
    }

    fun updateSponsorNegotiationTerms(
        baseWeekly: Int,
        brandingWeekly: Int,
        winBonus: Int,
        fiftyBonus: Int,
        centuryBonus: Int,
        haulBonus: Int
    ) {
        val current = _sponsorNegotiation.value ?: return
        _sponsorNegotiation.value = current.copy(
            offeredBaseWeekly = baseWeekly.coerceAtLeast(1000),
            offeredBrandingWeekly = brandingWeekly.coerceAtLeast(0),
            offeredWinBonus = winBonus.coerceAtLeast(0),
            offeredFiftyBonus = fiftyBonus.coerceAtLeast(0),
            offeredCenturyBonus = centuryBonus.coerceAtLeast(0),
            offeredHaulBonus = haulBonus.coerceAtLeast(0)
        )
    }

    fun submitSponsorNegotiationOffer() {
        val current = _sponsorNegotiation.value ?: return
        val career = _activeCareer.value ?: return
        if (!career.isSponsorshipWindowOpen) {
            postNotice("Sponsorship window closed — negotiation aborted.")
            _sponsorNegotiation.value = null
            return
        }
        val evaluated = SponsorNegotiationEngine.evaluateCounterOffer(current)
        _sponsorNegotiation.value = evaluated
        if (evaluated.isAccepted) {
            val signedDeal = evaluated.deal
            updateAndPersist { state ->
                val wasAlreadyActive = state.sponsors.firstOrNull { it.id == signedDeal.id }?.isActive == true
                val bonus = if (wasAlreadyActive) 0 else signedDeal.signingBonus
                val updatedSponsors = state.sponsors.map { sp ->
                    when {
                        sp.id == signedDeal.id -> signedDeal.copy(isActive = true)
                        sp.category == signedDeal.category -> sp.copy(isActive = false)
                        else -> sp
                    }
                }
                val updatedLedger = if (bonus > 0) {
                    (listOf(
                        FinanceLedgerEntry(
                            id = "ld_sp_neg_${UUID.randomUUID().toString().take(6)}",
                            season = state.season,
                            week = state.week,
                            category = "Sponsorship",
                            description = "Negotiated signing advance: ${signedDeal.brandName}",
                            amount = bonus
                        )
                    ) + state.ledger).take(40)
                } else state.ledger

                state.copy(
                    bankBalance = state.bankBalance + bonus,
                    seasonSponsorIncome = state.seasonSponsorIncome + bonus,
                    sponsors = updatedSponsors,
                    ledger = updatedLedger
                )
            }
            postNotice("Negotiated & signed ${signedDeal.brandName} ($${signedDeal.totalWeeklyGuaranteed}/wk)!")
        }
    }

    fun closeSponsorNegotiation() {
        _sponsorNegotiation.value = null
    }

    fun setTicketTier(tier: TicketTier) {
        updateAndPersist { state ->
            state.copy(budgets = state.budgets.copy(ticketTier = tier))
        }
        postNotice("Ticket pricing set to ${tier.label}.")
    }

    fun adjustBudgetLevel(academy: Int? = null, scouting: Int? = null, medical: Int? = null) {
        updateAndPersist { state ->
            state.copy(
                budgets = state.budgets.copy(
                    academyLevel = (academy ?: state.budgets.academyLevel).coerceIn(1, 5),
                    scoutingLevel = (scouting ?: state.budgets.scoutingLevel).coerceIn(1, 5),
                    medicalLevel = (medical ?: state.budgets.medicalLevel).coerceIn(1, 5)
                )
            )
        }
    }

    fun togglePlayerInXI(playerId: String) {
        updateAndPersist { state ->
            val player = state.squad.firstOrNull { it.id == playerId } ?: return@updateAndPersist state
            if (!player.isAvailableForMatch) {
                postNotice("${player.name} is unavailable (${if (player.injury.isInjured) player.injury.injuryName else "Youth"}).")
                return@updateAndPersist state
            }
            val current = state.selectedXIIds.toMutableList()
            if (current.contains(playerId)) {
                current.remove(playerId)
            } else {
                if (current.size >= 11) {
                    postNotice("XI already has 11 players. Deselect someone first.")
                    return@updateAndPersist state
                }
                current.add(playerId)
            }
            val sub = if (state.impactSubId == playerId) null else state.impactSubId
            state.copy(selectedXIIds = current, impactSubId = sub)
        }
    }

    fun setImpactSub(playerId: String) {
        updateAndPersist { state ->
            if (state.selectedXIIds.contains(playerId)) {
                postNotice("Impact Sub must be outside the Starting XI.")
                return@updateAndPersist state
            }
            state.copy(impactSubId = playerId)
        }
    }

    fun autoPickXI() {
        updateAndPersist { state ->
            val pitch = state.nextUserFixture?.pitchType ?: PitchType.BALANCED_DECK
            val hcRating = state.staff.firstOrNull { it.role == StaffRole.HEAD_COACH }?.rating ?: 65
            val (xi, sub) = StaffAdvisorEngine.recommendPlayingXI(state.squad, pitch, hcRating)
            postNotice("Head Coach selected optimal XI for ${pitch.label}.")
            state.copy(selectedXIIds = xi, impactSubId = sub)
        }
    }

    fun setDefaultTactics(tempo: BattingTempo? = null, plan: BowlingPlan? = null, field: FieldSpread? = null) {
        updateAndPersist { state ->
            state.copy(
                defaultTempo = tempo ?: state.defaultTempo,
                defaultBowlingPlan = plan ?: state.defaultBowlingPlan,
                defaultFieldSpread = field ?: state.defaultFieldSpread
            )
        }
    }

    fun setPreMatchTeamTalk(tone: TeamTalkTone) {
        updateAndPersist { state ->
            state.copy(preMatchTeamTalk = tone)
        }
        postNotice("Pre-Match Team Talk set to ${tone.label}.")
    }

    fun startNextMatch(instantSim: Boolean = false) {
        stopAutoStream()
        val career = _activeCareer.value ?: return
        val fixture = career.nextUserFixture ?: run {
            postNotice("No pending fixtures left this season.")
            return
        }
        val runtime = T20MatchEngine.createMatchSession(career, fixture)
        activeMatchRuntime = runtime
        if (instantSim) {
            val nextUi = runtime.advanceUntilNextPause(instantFinishAll = true)
            _liveMatch.value = nextUi
            if (nextUi.finishedRecord != null) {
                commitFinishedMatch(fixture, nextUi.finishedRecord)
            }
        } else {
            _liveMatch.value = runtime.stepBalls(1)
            startAutoStreamLoop()
        }
    }

    fun toggleAutoStream() {
        if (_isAutoStreaming.value) {
            stopAutoStream()
        } else {
            val current = _liveMatch.value ?: return
            if (current.finishedRecord != null || current.pendingKeyMoment != null) return
            startAutoStreamLoop()
        }
    }

    fun cycleStreamSpeed() {
        _streamSpeedMs.value = if (_streamSpeedMs.value >= 700L) 320L else 750L
    }

    fun stepLiveBalls(ballCount: Int) {
        stopAutoStream()
        val runtime = activeMatchRuntime ?: return
        val nextUi = runtime.stepBalls(ballCount)
        _liveMatch.value = nextUi
        if (nextUi.finishedRecord != null) {
            commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
        }
    }

    fun advanceToNextKeyMoment() {
        stopAutoStream()
        val runtime = activeMatchRuntime ?: return
        val nextUi = runtime.advanceUntilNextPause(instantFinishAll = false)
        _liveMatch.value = nextUi
        if (nextUi.finishedRecord != null) {
            commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
        }
    }

    fun applyLiveTactics(
        tempo: BattingTempo?,
        bowlingPlan: BowlingPlan?,
        fieldSpread: FieldSpread?,
        impactSubId: String?,
        teamTalkTone: TeamTalkTone? = null
    ) {
        val runtime = activeMatchRuntime ?: return
        _liveMatch.value = runtime.applyLiveTactics(tempo, bowlingPlan, fieldSpread, impactSubId, teamTalkTone)
    }

    fun submitKeyMomentDecision(
        tempo: BattingTempo?,
        bowlingPlan: BowlingPlan?,
        fieldSpread: FieldSpread?,
        impactSubId: String?,
        instantFinishAll: Boolean = false,
        teamTalkTone: TeamTalkTone? = null
    ) {
        stopAutoStream()
        val runtime = activeMatchRuntime ?: return
        runtime.applyIntervention(tempo, bowlingPlan, fieldSpread, impactSubId, teamTalkTone)
        if (instantFinishAll) {
            val nextUi = runtime.advanceUntilNextPause(instantFinishAll = true)
            _liveMatch.value = nextUi
            if (nextUi.finishedRecord != null) {
                commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
            }
        } else {
            val nextUi = runtime.stepBalls(1)
            _liveMatch.value = nextUi
            if (nextUi.finishedRecord != null) {
                commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
            } else if (nextUi.pendingKeyMoment == null) {
                startAutoStreamLoop()
            }
        }
    }

    private fun startAutoStreamLoop() {
        liveStreamJob?.cancel()
        _isAutoStreaming.value = true
        liveStreamJob = viewModelScope.launch {
            while (_isAutoStreaming.value) {
                delay(_streamSpeedMs.value)
                val runtime = activeMatchRuntime ?: break
                val nextUi = runtime.stepBalls(1)
                _liveMatch.value = nextUi
                if (nextUi.finishedRecord != null) {
                    _isAutoStreaming.value = false
                    commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
                    break
                }
                if (nextUi.pendingKeyMoment != null) {
                    _isAutoStreaming.value = false
                    break
                }
            }
        }
    }

    private fun stopAutoStream() {
        _isAutoStreaming.value = false
        liveStreamJob?.cancel()
        liveStreamJob = null
    }

    private fun commitFinishedMatch(fixture: Fixture, record: MatchRecord) {
        updateAndPersist { state ->
            SeasonProgressionEngine.commitUserMatchAndSimulateRound(state, fixture, record)
        }
    }

    fun deliverPostMatchTeamTalk(tone: TeamTalkTone) {
        val currentLive = _liveMatch.value ?: return
        val record = currentLive.finishedRecord ?: return
        if (currentLive.postMatchTeamTalkDelivered != null) return
        activeMatchRuntime?.postMatchTeamTalkDelivered = tone
        _liveMatch.value = currentLive.copy(postMatchTeamTalkDelivered = tone)
        updateAndPersist { state ->
            SeasonProgressionEngine.applyPostMatchTeamTalk(state, record, tone)
        }
        postNotice("Post-Match Talk (${tone.label}) delivered to the squad.")
    }

    fun dismissCompletedMatch(postMatchTalk: TeamTalkTone? = null) {
        if (postMatchTalk != null && _liveMatch.value?.postMatchTeamTalkDelivered == null) {
            deliverPostMatchTeamTalk(postMatchTalk)
        }
        stopAutoStream()
        _liveMatch.value = null
        activeMatchRuntime = null
        _selectedTab.value = MainTab.HQ
    }

    fun startSeason() {
        val current = _activeCareer.value ?: return
        if (!current.isPreSeason) return
        updateAndPersist { state ->
            state.copy(isPreSeason = false)
        }
        _squadSubTab.value = SquadSubTab.FORM_STATS
        _trainingMedicalSubTab.value = TrainingMedicalSubTab.TRAINING
        _officeSubTab.value = OfficeSubTab.PYRAMID
        _selectedTab.value = MainTab.HQ
        postNotice("Season ${current.season} is underway! Sponsorships locked & MatchDay is open.")
    }

    fun saveCurrentCareer() {
        val current = _activeCareer.value ?: return
        val timestamped = current.copy(updatedAtMillis = System.currentTimeMillis())
        _activeCareer.value = timestamped
        viewModelScope.launch {
            repository.saveCareer(timestamped)
            postNotice("Game saved: ${timestamped.clubName} (S${timestamped.season} W${timestamped.week}).")
        }
    }

    fun advanceWeekOrPlay() {
        val state = _activeCareer.value ?: return
        if (state.isPreSeason) {
            startSeason()
        } else if (state.seasonComplete) {
            startNextSeason()
        } else if (state.nextUserFixture != null) {
            _selectedTab.value = MainTab.MATCHDAY
        } else {
            updateAndPersist { SeasonProgressionEngine.advanceWeeklyTick(it) }
            postNotice("Advanced to Week ${state.week + 1}.")
        }
    }

    fun startNextSeason() {
        updateAndPersist { state ->
            postNotice("Welcome to Season ${state.season + 1} Pre-Season!")
            SeasonProgressionEngine.startNextSeason(state)
        }
        _squadSubTab.value = SquadSubTab.ROSTER
        _officeSubTab.value = OfficeSubTab.STAFF_DELEGATION
        _selectedTab.value = MainTab.HQ
    }

    companion object {
        fun factory(
            repository: CareerRepository,
            rewardsStore: WelcomeRewardsStore? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CareerViewModel(repository, rewardsStore) as T
                }
            }
    }
}
