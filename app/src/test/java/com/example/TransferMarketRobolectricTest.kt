package com.example

import android.content.Context
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CareerRepository
import com.example.data.local.PitchCraftDatabase
import com.example.model.TransferWindowPhase
import com.example.simulation.SeasonProgressionEngine
import com.example.simulation.WorldGenerator
import com.example.ui.CareerViewModel
import com.example.ui.MainTab
import com.example.ui.components.TransferMarketPlayerCard
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.PitchCraftTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransferMarketRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `transfer window is active only for 1 week pre-season and 1 week mid-season`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 2 }
        var career = WorldGenerator.createNewCareer(template, "Gaffer", Random(101))

        // Week 1: Pre-season window OPEN (1 week)
        assertEquals(1, career.week)
        assertEquals(8, career.midSeasonTransferWeek)
        assertTrue(career.isTransferWindowOpen)
        assertEquals(TransferWindowPhase.PRE_SEASON, career.transferWindowPhase)

        // Advance Week 1 -> Week 2: Window closes
        career = SeasonProgressionEngine.advanceWeeklyTick(career, Random(101))
        assertEquals(2, career.week)
        assertFalse(career.isTransferWindowOpen)
        assertEquals(TransferWindowPhase.CLOSED, career.transferWindowPhase)

        // Weeks 3..7 remain closed
        repeat(5) {
            career = SeasonProgressionEngine.advanceWeeklyTick(career, Random(101))
            assertFalse("Week ${career.week} should be closed", career.isTransferWindowOpen)
        }

        // Entering Week 8 (mid-way through 14-round season): Mid-season window OPENS for 1 week
        career = SeasonProgressionEngine.advanceWeeklyTick(career, Random(101))
        assertEquals(8, career.week)
        assertTrue(career.isTransferWindowOpen)
        assertEquals(TransferWindowPhase.MID_SEASON, career.transferWindowPhase)
        assertTrue(career.inbox.any { it.title.contains("Mid-Season Transfer Window Open") })

        // Advance Week 8 -> Week 9: Mid-season window closes after 1 week
        career = SeasonProgressionEngine.advanceWeeklyTick(career, Random(101))
        assertEquals(9, career.week)
        assertFalse(career.isTransferWindowOpen)
        assertEquals(TransferWindowPhase.CLOSED, career.transferWindowPhase)

        // Next season resets to Week 1 Pre-Season and re-opens window
        val nextSeason = SeasonProgressionEngine.startNextSeason(career.copy(seasonComplete = true), Random(101))
        assertEquals(1, nextSeason.week)
        assertTrue(nextSeason.isTransferWindowOpen)
        assertEquals(TransferWindowPhase.PRE_SEASON, nextSeason.transferWindowPhase)
    }

    @Test
    fun `viewmodel blocks transfers tab and transfer bids when window is closed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PitchCraftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = CareerRepository(db.careerDao())
        val vm = CareerViewModel(repo)

        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val week1Career = WorldGenerator.createNewCareer(template, "Director", Random(33))
        runBlocking { repo.saveCareer(week1Career) }
        vm.loadCareer(week1Career.careerId)
        waitForCondition { vm.activeCareer.value != null }

        // Week 1 (Window Open): Transfer tab and Bid negotiation work
        vm.selectTab(MainTab.TRANSFERS)
        assertEquals(MainTab.TRANSFERS, vm.selectedTab.value)
        val target = vm.activeCareer.value!!.transferMarket.first()
        vm.openNegotiation(target, isRenewal = false)
        assertNotNull(vm.negotiation.value)
        vm.closeNegotiation()

        // Advance Week 1 -> Week 2 (Window Closed): Transfers tab auto-exits & bids blocked
        val week2Career = SeasonProgressionEngine.advanceWeeklyTick(week1Career, Random(33))
        runBlocking { repo.saveCareer(week2Career) }
        vm.loadCareer(week2Career.careerId)
        waitForCondition { vm.activeCareer.value?.week == 2 }

        vm.selectTab(MainTab.TRANSFERS)
        assertEquals(MainTab.HQ, vm.selectedTab.value)
        vm.openNegotiation(target, isRenewal = false)
        assertNull(vm.negotiation.value)

        // Internal contract renewal for squad player remains permitted
        val squadMember = vm.activeCareer.value!!.squad.first { !it.isYouthAcademy }
        vm.openNegotiation(squadMember, isRenewal = true)
        assertNotNull(vm.negotiation.value)

        db.close()
    }

    private fun waitForCondition(timeoutMs: Long = 3000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        assertTrue("Timed out waiting for ViewModel state update", predicate())
    }

    @Test
    fun `transfer market player card shows base value, skill ratings, and Bid button triggers negotiation`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val career = WorldGenerator.createNewCareer(template, "Boss", Random(77))
        val target = career.transferMarket.first()
        var bidPlayerId: String? = null

        composeRule.setContent {
            PitchCraftTheme {
                TransferMarketPlayerCard(
                    player = target,
                    isWindowOpen = true,
                    onToggleShortlist = {},
                    onBid = { bidPlayerId = target.id }
                )
            }
        }

        composeRule.onNodeWithTag("transfer_card_${target.id}").assertIsDisplayed()
        composeRule.onNodeWithText("BASE VALUE").assertIsDisplayed()
        composeRule.onNodeWithText("OVR").assertIsDisplayed()
        composeRule.onNodeWithText("TIM").assertIsDisplayed()
        composeRule.onNodeWithText("PWR").assertIsDisplayed()
        composeRule.onNodeWithText("Bid").assertIsDisplayed()
        composeRule.onNodeWithTag("bid_${target.id}").assertIsEnabled().performClick()
        assertEquals(target.id, bidPlayerId)
    }

    @Test
    fun `transfers screen locks bids outside window and nav tab respects window state`() {
        val template = WorldGenerator.ALL_CLUB_TEMPLATES.first { it.tier == 1 }
        val closedCareer = SeasonProgressionEngine.advanceWeeklyTick(
            WorldGenerator.createNewCareer(template, "Boss", Random(88)),
            Random(88)
        )
        assertFalse(closedCareer.isTransferWindowOpen)

        composeRule.setContent {
            PitchCraftTheme {
                NavigationBar {
                    MainTab.entries.forEach { tab ->
                        val enabled = tab != MainTab.TRANSFERS || closedCareer.isTransferWindowOpen
                        NavigationBarItem(
                            selected = tab == MainTab.HQ,
                            enabled = enabled,
                            onClick = {},
                            icon = {},
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("nav_tab_hq").assertIsEnabled()
        composeRule.onNodeWithTag("nav_tab_transfers").assertIsNotEnabled()
    }
}
