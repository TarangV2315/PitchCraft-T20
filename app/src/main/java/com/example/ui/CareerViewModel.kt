package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CareerRepository
import com.example.data.local.CareerSummary
import com.example.model.*
import com.example.simulation.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

enum class MainTab(val label: String) {
    HQ("HQ"),
    SQUAD("Squad"),
    MATCHDAY("Match"),
    TRANSFERS("Transfers"),
    OFFICE("Office")
}

class CareerViewModel(private val repository: CareerRepository) : ViewModel() {

    val savedCareers: StateFlow<List<CareerSummary>> = repository.careerSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCareer = MutableStateFlow<CareerState?>(null)
    val activeCareer: StateFlow<CareerState?> = _activeCareer.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.HQ)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _inspectedPlayerId = MutableStateFlow<String?>(null)
    val inspectedPlayerId: StateFlow<String?> = _inspectedPlayerId.asStateFlow()

    private val _negotiation = MutableStateFlow<NegotiationSession?>(null)
    val negotiation: StateFlow<NegotiationSession?> = _negotiation.asStateFlow()

    private val _liveMatch = MutableStateFlow<LiveMatchState?>(null)
    val liveMatch: StateFlow<LiveMatchState?> = _liveMatch.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var activeMatchRuntime: ActiveMatchRuntime? = null

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun postNotice(message: String) {
        _snackbarMessage.value = message
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openPlayerActivity(playerId: String) {
        _inspectedPlayerId.value = playerId
    }

    fun closePlayerActivity() {
        _inspectedPlayerId.value = null
    }

    fun createNewCareer(club: ClubTemplate, managerName: String) {
        viewModelScope.launch {
            val newCareer = WorldGenerator.createNewCareer(club, managerName)
            repository.saveCareer(newCareer)
            _activeCareer.value = newCareer
            _selectedTab.value = MainTab.HQ
            _inspectedPlayerId.value = null
            postNotice("Signed contract as Manager of ${club.name}!")
        }
    }

    fun loadCareer(careerId: String) {
        viewModelScope.launch {
            val loaded = repository.loadCareer(careerId)
            if (loaded != null) {
                _activeCareer.value = loaded
                _selectedTab.value = MainTab.HQ
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
        _activeCareer.value = null
        _inspectedPlayerId.value = null
        _liveMatch.value = null
        activeMatchRuntime = null
    }

    private fun updateAndPersist(transform: (CareerState) -> CareerState) {
        val current = _activeCareer.value ?: return
        val updated = transform(current)
        _activeCareer.value = updated
        viewModelScope.launch {
            repository.saveCareer(updated)
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

    fun hireStaffMember(candidate: StaffMember) {
        updateAndPersist { state ->
            val signingCost = candidate.weeklySalary * 4
            if (state.bankBalance < signingCost) {
                postNotice("Need $$signingCost signing compensation to hire ${candidate.name}.")
                return@updateAndPersist state
            }
            val replaced = state.staff.firstOrNull { it.role == candidate.role }
            val updatedHired = state.staff.filterNot { it.role == candidate.role } + candidate.copy(isHired = true)
            val updatedPool = state.availableStaff.filterNot { it.id == candidate.id } +
                listOfNotNull(replaced?.copy(isHired = false))
            postNotice("Hired ${candidate.name} (${candidate.rating}) as ${candidate.role.label}.")
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

    fun signSponsorDeal(dealId: String) {
        updateAndPersist { state ->
            val target = state.sponsors.firstOrNull { it.id == dealId } ?: return@updateAndPersist state
            val updatedSponsors = state.sponsors.map { sp ->
                when {
                    sp.id == dealId -> sp.copy(isActive = true)
                    sp.category == target.category -> sp.copy(isActive = false)
                    else -> sp
                }
            }
            postNotice("Signed ${target.category.label} deal with ${target.brandName} ($${target.weeklyPayout}/wk)!")
            state.copy(sponsors = updatedSponsors)
        }
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

    fun startNextMatch(instantSim: Boolean = false) {
        val career = _activeCareer.value ?: return
        val fixture = career.nextUserFixture ?: run {
            postNotice("No pending fixtures left this season.")
            return
        }
        val runtime = T20MatchEngine.createMatchSession(career, fixture)
        activeMatchRuntime = runtime
        val nextUi = runtime.advanceUntilNextPause(instantFinishAll = instantSim)
        _liveMatch.value = nextUi
        if (nextUi.finishedRecord != null) {
            commitFinishedMatch(fixture, nextUi.finishedRecord)
        }
    }

    fun submitKeyMomentDecision(
        tempo: BattingTempo?,
        bowlingPlan: BowlingPlan?,
        fieldSpread: FieldSpread?,
        impactSubId: String?,
        instantFinishAll: Boolean = false
    ) {
        val runtime = activeMatchRuntime ?: return
        runtime.applyIntervention(tempo, bowlingPlan, fieldSpread, impactSubId)
        val nextUi = runtime.advanceUntilNextPause(instantFinishAll = instantFinishAll)
        _liveMatch.value = nextUi
        if (nextUi.finishedRecord != null) {
            commitFinishedMatch(runtime.fixture, nextUi.finishedRecord)
        }
    }

    private fun commitFinishedMatch(fixture: Fixture, record: MatchRecord) {
        updateAndPersist { state ->
            SeasonProgressionEngine.commitUserMatchAndSimulateRound(state, fixture, record)
        }
    }

    fun dismissCompletedMatch() {
        _liveMatch.value = null
        activeMatchRuntime = null
        _selectedTab.value = MainTab.HQ
    }

    fun advanceWeekOrPlay() {
        val state = _activeCareer.value ?: return
        if (state.seasonComplete) {
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
            postNotice("Welcome to Season ${state.season + 1}!")
            SeasonProgressionEngine.startNextSeason(state)
        }
    }

    companion object {
        fun factory(repository: CareerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CareerViewModel(repository) as T
                }
            }
    }
}
