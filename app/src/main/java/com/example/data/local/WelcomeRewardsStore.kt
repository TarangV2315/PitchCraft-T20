package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WelcomeRewardsState(
    val reserveCurrency: Int = 0,
    val adsWatchedToday: Int = 0,
    val maxAdsPerDay: Int = WelcomeRewardsStore.MAX_ADS_PER_DAY,
    val rewardPerAd: Int = WelcomeRewardsStore.REWARD_PER_AD,
    val lastResetDate: String = ""
) {
    val adsRemainingToday: Int
        get() = (maxAdsPerDay - adsWatchedToday).coerceAtLeast(0)

    val canWatchAd: Boolean
        get() = adsWatchedToday < maxAdsPerDay
}

data class AdWatchOutcome(
    val granted: Boolean,
    val rewardAdded: Int,
    val updatedState: WelcomeRewardsState
)

class WelcomeRewardsStore(
    private val prefs: SharedPreferences,
    private val currentDateProvider: () -> String = { defaultTodayKey() }
) {
    constructor(context: Context) : this(
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    fun getState(): WelcomeRewardsState {
        val today = currentDateProvider()
        val storedDate = prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
        var watchedToday = prefs.getInt(KEY_ADS_WATCHED_TODAY, 0)
        if (storedDate != today) {
            watchedToday = 0
            prefs.edit()
                .putString(KEY_LAST_RESET_DATE, today)
                .putInt(KEY_ADS_WATCHED_TODAY, 0)
                .apply()
        }
        val reserve = prefs.getInt(KEY_RESERVE_CURRENCY, 0).coerceAtLeast(0)
        return WelcomeRewardsState(
            reserveCurrency = reserve,
            adsWatchedToday = watchedToday.coerceIn(0, MAX_ADS_PER_DAY),
            maxAdsPerDay = MAX_ADS_PER_DAY,
            rewardPerAd = REWARD_PER_AD,
            lastResetDate = today
        )
    }

    fun recordAdWatched(): AdWatchOutcome {
        val current = getState()
        if (!current.canWatchAd) {
            return AdWatchOutcome(
                granted = false,
                rewardAdded = 0,
                updatedState = current
            )
        }
        val nextWatched = current.adsWatchedToday + 1
        val nextReserve = current.reserveCurrency + REWARD_PER_AD
        val today = currentDateProvider()
        prefs.edit()
            .putInt(KEY_ADS_WATCHED_TODAY, nextWatched)
            .putInt(KEY_RESERVE_CURRENCY, nextReserve)
            .putString(KEY_LAST_RESET_DATE, today)
            .apply()

        return AdWatchOutcome(
            granted = true,
            rewardAdded = REWARD_PER_AD,
            updatedState = current.copy(
                reserveCurrency = nextReserve,
                adsWatchedToday = nextWatched,
                lastResetDate = today
            )
        )
    }

    fun consumeReserveCurrency(): Int {
        val current = getState()
        if (current.reserveCurrency <= 0) return 0
        val amount = current.reserveCurrency
        prefs.edit()
            .putInt(KEY_RESERVE_CURRENCY, 0)
            .apply()
        return amount
    }

    companion object {
        const val PREFS_NAME = "pitchcraft_welcome_rewards"
        const val MAX_ADS_PER_DAY = 5
        const val REWARD_PER_AD = 25_000
        private const val KEY_RESERVE_CURRENCY = "reserve_currency"
        private const val KEY_ADS_WATCHED_TODAY = "ads_watched_today"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"

        fun defaultTodayKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}
