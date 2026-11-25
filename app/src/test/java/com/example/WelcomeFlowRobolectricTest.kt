package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CareerSummary
import com.example.data.local.WelcomeRewardsState
import com.example.data.local.WelcomeRewardsStore
import com.example.model.CricketNation
import com.example.simulation.WorldGenerator
import com.example.ui.components.InGameTopContextBar
import com.example.ui.components.ScreenGuideDialog
import com.example.ui.components.ScreenGuideId
import com.example.ui.screens.CareerLauncherScreen
import com.example.ui.screens.CricketLoadingScreen
import com.example.ui.screens.PlayerActivityScreen
import com.example.ui.theme.PitchCraftTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WelcomeFlowRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `welcome rewards store enforces 5 ads per day cap and resets on new date`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_rewards_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        var fakeDate = "2026-11-23"
        val store = WelcomeRewardsStore(prefs = prefs, currentDateProvider = { fakeDate })

        repeat(5) { idx ->
            val outcome = store.recordAdWatched()
            assertTrue("Ad #${idx + 1} should be granted", outcome.granted)
            assertEquals(WelcomeRewardsStore.REWARD_PER_AD, outcome.rewardAdded)
            assertEquals(idx + 1, outcome.updatedState.adsWatchedToday)
        }

        val blocked = store.recordAdWatched()
        assertFalse(blocked.granted)
        assertEquals(0, blocked.rewardAdded)
        assertEquals(5, blocked.updatedState.adsWatchedToday)
        assertEquals(5 * WelcomeRewardsStore.REWARD_PER_AD, blocked.updatedState.reserveCurrency)
        assertFalse(blocked.updatedState.canWatchAd)

        fakeDate = "2026-11-24"
        val nextDayState = store.getState()
        assertEquals(0, nextDayState.adsWatchedToday)
        assertEquals(5, nextDayState.adsRemainingToday)
        assertTrue(nextDayState.canWatchAd)
        assertEquals(5 * WelcomeRewardsStore.REWARD_PER_AD, nextDayState.reserveCurrency)

        val claimed = store.consumeReserveCurrency()
        assertEquals(5 * WelcomeRewardsStore.REWARD_PER_AD, claimed)
        assertEquals(0, store.getState().reserveCurrency)
    }

    @Test
    fun `cricket loading screen displays animation and skip button triggers callback`() {
        var skipped = false
        composeRule.setContent {
            PitchCraftTheme {
                CricketLoadingScreen(onSkip = { skipped = true })
            }
        }

        composeRule.onNodeWithTag("cricket_loading_screen").assertIsDisplayed()
        composeRule.onNodeWithText("PitchCraft T20").assertIsDisplayed()
        composeRule.onNodeWithTag("loading_skip_button").performClick()
        assertTrue(skipped)
    }

    @Test
    fun `reworked landing screen shows jazzy stage, simple start new game and resume saved game options`() {
        val sampleSave = CareerSummary(
            careerId = "c_1",
            clubName = "Mumbai Strikers",
            clubShortCode = "MST",
            managerName = "Alex Mercer",
            season = 2,
            week = 4,
            divisionTier = 1,
            bankBalance = 520_000,
            trophiesWon = 1,
            updatedAtMillis = 1000L
        )
        val rewardsState = WelcomeRewardsState(
            reserveCurrency = 50_000,
            adsWatchedToday = 2,
            maxAdsPerDay = 5,
            rewardPerAd = 25_000,
            lastResetDate = "2026-11-23"
        )
        var resumedId: String? = null

        composeRule.setContent {
            PitchCraftTheme {
                CareerLauncherScreen(
                    savedCareers = listOf(sampleSave),
                    welcomeRewards = rewardsState,
                    onLoadCareer = { resumedId = it },
                    onDeleteCareer = {},
                    onCreateCareer = { _, _ -> },
                    onWatchAdReward = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(300L)

        composeRule.onNodeWithTag("screen_info_button").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_jazzy_stage").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_new_game_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_resume_button").performScrollTo().assertIsEnabled().performClick()
        assertEquals("c_1", resumedId)
    }

    @Test
    fun `landing page top-left info button opens walkthrough tutorial popup without leaving landing screen`() {
        composeRule.setContent {
            PitchCraftTheme {
                CareerLauncherScreen(
                    savedCareers = emptyList(),
                    welcomeRewards = WelcomeRewardsState(),
                    onLoadCareer = {},
                    onDeleteCareer = {},
                    onCreateCareer = { _, _ -> },
                    onWatchAdReward = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(300L)

        composeRule.onNodeWithTag("welcome_resume_button").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("screen_info_button").performClick()

        // Popup appears while user stays on landing screen
        composeRule.onNodeWithTag("screen_guide_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("GAME TUTORIAL • 1 / 7").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome & Career Launch").assertIsDisplayed()

        // Step through game screens in tutorial
        composeRule.onNodeWithTag("tutorial_next_button").performClick()
        composeRule.onNodeWithText("GAME TUTORIAL • 2 / 7").assertIsDisplayed()
        composeRule.onNodeWithText("Club HQ & Weekly Loop").assertIsDisplayed()

        composeRule.onNodeWithTag("tutorial_next_button").performClick()
        composeRule.onNodeWithText("GAME TUTORIAL • 3 / 7").assertIsDisplayed()
        composeRule.onNodeWithText("Training Intensity & Focus").assertIsDisplayed()

        composeRule.onNodeWithTag("tutorial_prev_button").performClick()
        composeRule.onNodeWithText("GAME TUTORIAL • 2 / 7").assertIsDisplayed()

        // Dismiss returns cleanly to the exact same landing screen
        composeRule.onNodeWithTag("tutorial_close_icon_button").performClick()
        composeRule.onNodeWithTag("screen_guide_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("welcome_hub_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_new_game_button").assertIsDisplayed()
    }

    @Test
    fun `new franchise creator and player telemetry pages render top-left info button and screen popup`() {
        val template = WorldGenerator.clubsFor(CricketNation.INDIA, 1).first()
        val career = WorldGenerator.createNewCareer(template, "Gaffer", Random(7))
        val player = career.squad.first()

        composeRule.setContent {
            PitchCraftTheme {
                PlayerActivityScreen(
                    player = player,
                    career = career,
                    onBack = {},
                    onApplyRecommendation = { _, _ -> },
                    onSetTraining = { _, _ -> },
                    onSetRehab = { _, _ -> },
                    onPromoteYouth = {},
                    onOpenRenewal = {},
                    onToggleTransferList = {},
                    onSellPlayer = {}
                )
            }
        }

        composeRule.onNodeWithTag("screen_info_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("screen_guide_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Player Telemetry & Actions").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_close_button").performClick()
        composeRule.onNodeWithTag("screen_guide_dialog").assertDoesNotExist()
        composeRule.onNodeWithText(player.name).assertIsDisplayed()
    }

    @Test
    fun `in-game top context bar shows info icon and opens screen-specific popup without leaving screen`() {
        composeRule.setContent {
            var showPopup by remember { mutableStateOf(false) }
            PitchCraftTheme {
                androidx.compose.foundation.layout.Column {
                    InGameTopContextBar(
                        guideId = ScreenGuideId.OFFICE_FINANCES,
                        clubShortCode = "MUM",
                        divisionTier = 1,
                        season = 1,
                        week = 1,
                        bankBalance = 450_000,
                        onInfoClick = { showPopup = true }
                    )
                    if (showPopup) {
                        ScreenGuideDialog(
                            initialScreenId = ScreenGuideId.OFFICE_FINANCES,
                            onDismiss = { showPopup = false }
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("screen_info_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("screen_guide_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Finances & Sponsorships").assertIsDisplayed()

        // Can toggle into Full Tour inside popup and back without navigating screens
        composeRule.onNodeWithTag("toggle_tutorial_tour_button").performClick()
        composeRule.onNodeWithText("GAME TUTORIAL • 1 / 7").assertIsDisplayed()
        composeRule.onNodeWithTag("tutorial_close_icon_button").performClick()
        composeRule.onNodeWithTag("screen_guide_dialog").assertDoesNotExist()
    }

    @Test
    fun `squad portrait resolver maps all generated portrait drawables and handles legacy saves`() {
        val template = WorldGenerator.clubsFor(CricketNation.AUSTRALIA, 1).first()
        val career = WorldGenerator.createNewCareer(template, "Skipper", Random(33))
        val mappedDrawables = career.squad.map { com.example.ui.components.playerPortraitDrawable(it) }.toSet()

        assertEquals(8, com.example.ui.components.PLAYER_PORTRAIT_DRAWABLES.size)
        assertEquals(8, mappedDrawables.size)

        val legacyPlayer = career.squad.first().copy(portraitVariant = -1)
        val fallbackDrawable = com.example.ui.components.playerPortraitDrawable(legacyPlayer)
        assertTrue(com.example.ui.components.PLAYER_PORTRAIT_DRAWABLES.contains(fallbackDrawable))
    }
}

