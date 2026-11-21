package com.example.model

enum class RecommendationActionType {
    SET_TRAINING_DRILL,
    SET_REHAB_MODE,
    OFFER_CONTRACT_RENEWAL,
    PROMOTE_YOUTH,
    INCLUDE_IN_XI,
    TOGGLE_TRANSFER_LIST
}

data class StaffRecommendation(
    val id: String,
    val staffRole: StaffRole,
    val staffName: String,
    val staffRating: Int,
    val title: String,
    val rationale: String,
    val actionType: RecommendationActionType,
    val actionLabel: String,
    val targetDrill: TrainingFocus? = null,
    val targetRehab: RehabMode? = null,
    val suggestedWage: Int? = null,
    val priorityHigh: Boolean = false
)

data class NegotiationSession(
    val player: Player,
    val isRenewal: Boolean,
    val askingFee: Int,
    val askingWage: Int,
    val askingYears: Int,
    val offeredFee: Int,
    val offeredWage: Int,
    val offeredSigningBonus: Int,
    val offeredYears: Int,
    val agentPatience: Int, // 0..100
    val round: Int,
    val agentMessage: String,
    val isAccepted: Boolean = false,
    val isCollapsed: Boolean = false
)
