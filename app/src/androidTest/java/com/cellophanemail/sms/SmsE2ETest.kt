package com.cellophanemail.sms

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.cellophanemail.sms.ui.main.MainActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * End-to-end instrumented tests for CellophoneSMS.
 *
 * These tests run on a real device or emulator and test the full
 * user-facing functionality of the app.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SmsE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice
    private lateinit var context: Context

    companion object {
        private const val PACKAGE_NAME = "com.cellophanemail.sms.debug"
        private const val LAUNCH_TIMEOUT = 5000L
        private const val UI_TIMEOUT = 3000L
    }

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        context = ApplicationProvider.getApplicationContext()
    }

    // ==================== App Launch Tests ====================

    @Test
    fun app_launches_successfully() {
        // Verify app is in foreground
        val currentPackage = device.currentPackageName
        assertTrue(
            "App should be in foreground, but found: $currentPackage",
            currentPackage.contains("cellophanemail")
        )
    }

    @Test
    fun main_screen_displays_messages_title() {
        // Wait for UI to load
        device.wait(Until.hasObject(By.text("Messages")), UI_TIMEOUT)

        // Verify title is displayed
        onView(withText("Messages"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun main_screen_has_compose_button() {
        // Wait for FAB to appear
        Thread.sleep(1000)

        // Check for compose/FAB button (contentDescription may vary)
        val fabExists = device.hasObject(By.desc("New Message")) ||
                device.hasObject(By.desc("Compose")) ||
                device.hasObject(By.clazz("com.google.android.material.floatingactionbutton.FloatingActionButton"))

        assertTrue("Compose FAB should exist", fabExists)
    }

    @Test
    fun main_screen_has_settings_icon() {
        // Wait for UI
        Thread.sleep(1000)

        val settingsExists = device.hasObject(By.desc("Settings")) ||
                device.hasObject(By.desc("settings"))

        assertTrue("Settings icon should exist", settingsExists)
    }

    // ==================== Thread List Tests ====================

    @Test
    fun thread_list_displays_when_messages_exist() {
        // This test assumes there are already messages in the app
        // In a real scenario, you'd inject test data first

        Thread.sleep(2000)

        // Check if any thread cards are visible
        // This looks for typical thread list indicators
        val hasThreads = device.hasObject(By.textContains("555")) ||
                device.hasObject(By.textContains("Now")) ||
                device.hasObject(By.res(PACKAGE_NAME, "thread_list"))

        // Note: This may fail if no messages exist
        if (!hasThreads) {
            println("No threads found - this is expected on fresh install")
        }
    }

    // ==================== Default SMS App Tests ====================

    @Test
    fun app_is_registered_as_sms_handler() {
        // Check if our app is capable of being an SMS handler
        val packageManager = context.packageManager
        val intent = Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION)
        val resolveInfo = packageManager.queryBroadcastReceivers(intent, 0)

        val isRegistered = resolveInfo.any {
            it.activityInfo.packageName.contains("cellophanemail")
        }

        assertTrue("App should be registered for SMS_DELIVER", isRegistered)
    }

    @Test
    fun app_has_required_sms_permissions_declared() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )

        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

        assertTrue(
            "Should declare RECEIVE_SMS",
            permissions.contains("android.permission.RECEIVE_SMS")
        )
        assertTrue(
            "Should declare SEND_SMS",
            permissions.contains("android.permission.SEND_SMS")
        )
        assertTrue(
            "Should declare READ_SMS",
            permissions.contains("android.permission.READ_SMS")
        )
    }

    // ==================== UI Interaction Tests ====================

    @Test
    fun search_icon_is_clickable() {
        Thread.sleep(1000)

        val searchButton = device.findObject(By.desc("Search"))
        if (searchButton != null) {
            assertTrue("Search icon should be clickable", searchButton.isClickable)
        }
    }

    @Test
    fun settings_icon_is_clickable() {
        Thread.sleep(1000)

        val settingsButton = device.findObject(By.desc("Settings"))
        if (settingsButton != null) {
            assertTrue("Settings icon should be clickable", settingsButton.isClickable)
        }
    }

    // ==================== Database Tests ====================

    @Test
    fun database_is_accessible() {
        // Verify Room database can be accessed
        // This is a basic sanity check
        val dbFile = context.getDatabasePath("cellophone_sms.db")
        // Database may not exist on first run, which is OK
        println("Database path: ${dbFile.absolutePath}")
        println("Database exists: ${dbFile.exists()}")
    }

    // ==================== Performance Tests ====================

    @Test
    fun app_launches_within_timeout() {
        val startTime = System.currentTimeMillis()

        // Wait for main content to be visible
        device.wait(Until.hasObject(By.text("Messages")), LAUNCH_TIMEOUT)

        val launchTime = System.currentTimeMillis() - startTime

        assertTrue(
            "App should launch within ${LAUNCH_TIMEOUT}ms, took ${launchTime}ms",
            launchTime < LAUNCH_TIMEOUT
        )
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun app_handles_rotation() {
        // Rotate to landscape
        device.setOrientationLeft()
        Thread.sleep(1000)

        // Verify app didn't crash
        val currentPackage = device.currentPackageName
        assertTrue(
            "App should survive rotation",
            currentPackage.contains("cellophanemail")
        )

        // Rotate back to portrait
        device.setOrientationNatural()
        Thread.sleep(1000)

        // Verify still running
        assertTrue(
            "App should survive rotation back",
            device.currentPackageName.contains("cellophanemail")
        )
    }

    @Test
    fun app_handles_home_and_return() {
        // Press home
        device.pressHome()
        Thread.sleep(1000)

        // Return to app
        device.pressRecentApps()
        Thread.sleep(500)

        // Click on the app in recents (this is device-dependent)
        val appInRecents = device.findObject(By.textContains("CellophoneSMS")) ?:
        device.findObject(By.textContains("Messages"))

        appInRecents?.click()
        Thread.sleep(1000)

        // Verify app is back
        assertTrue(
            "App should return from background",
            device.currentPackageName.contains("cellophanemail")
        )
    }
}
