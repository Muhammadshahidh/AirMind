package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.airmind.core.audio.RemoteListeningEngine
import com.example.airmind.core.database.AirMindDatabase
import com.example.airmind.core.database.entities.AutomationRuleEntity
import com.example.airmind.core.database.entities.GestureMappingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AirMindDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AirMindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context verifies AIRMIND AI app name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("AIRMIND AI", appName)
    }

    @Test
    fun `database gesture mapping insert and retrieve`() = runBlocking {
        val mapping = GestureMappingEntity(
            eventType = "DOUBLE_TAP",
            earbudSide = "BOTH",
            actionType = "NEXT_TRACK"
        )
        database.gestureDao().insertAll(listOf(mapping))

        val retrieved = database.gestureDao().getGesture("DOUBLE_TAP", "BOTH")
        assertNotNull(retrieved)
        assertEquals("NEXT_TRACK", retrieved?.actionType)
    }

    @Test
    fun `database automation rules trigger filtering`() = runBlocking {
        val rule = AutomationRuleEntity(
            title = "Welcome Chime",
            triggerType = "BT_CONNECTED",
            actionType = "SPEAK_TEXT",
            isEnabled = true
        )
        database.automationDao().insert(rule)

        val activeRules = database.automationDao().getActiveRulesForTrigger("BT_CONNECTED")
        assertEquals(1, activeRules.size)
        assertEquals("Welcome Chime", activeRules.first().title)
    }

    @Test
    fun `remote listening engine configuration math`() {
        val engine = RemoteListeningEngine(context)
        engine.updateSettings(volume = 0.8f, balance = 0.5f, noiseReduction = true, voiceEnhance = true)
        assertEquals(1.2f, engine.volumeMultiplier, 0.01f)
        assertEquals(0.5f, engine.leftBalance, 0.01f)
        assertEquals(1.0f, engine.rightBalance, 0.01f)
        assertTrue(engine.isNoiseReductionEnabled)
    }

    @Test
    fun `bluetooth scanner permissions and initialization`() {
        val scanner = com.example.airmind.core.bluetooth.BluetoothScanner(context)
        val permissions = scanner.getRequiredPermissions()
        assertTrue(permissions.isNotEmpty())
        assertNotNull(scanner.discoveredDevices.value)
    }

    @Test
    fun `noise cancellation controller cycles and sets modes`() {
        val listeningEngine = com.example.airmind.core.audio.RemoteListeningEngine(context)
        val mobileControl = com.example.airmind.core.controls.MobileControlManager(context)
        val prefs = com.example.airmind.core.datastore.UserPreferencesRepository(context)
        val controller = com.example.airmind.core.audio.NoiseCancellationController(
            context = context,
            listeningEngine = listeningEngine,
            mobileControlManager = mobileControl,
            preferencesRepository = prefs
        )
        assertEquals(com.example.airmind.core.audio.NoiseControlMode.OFF, controller.currentMode.value)

        controller.setMode(com.example.airmind.core.audio.NoiseControlMode.ANC)
        assertEquals(com.example.airmind.core.audio.NoiseControlMode.ANC, controller.currentMode.value)

        controller.setAncLevel(0.9f)
        assertEquals(0.9f, controller.ancLevel.value, 0.01f)

        controller.toggleTransparency()
        assertEquals(com.example.airmind.core.audio.NoiseControlMode.TRANSPARENCY, controller.currentMode.value)

        controller.turnOff()
        assertEquals(com.example.airmind.core.audio.NoiseControlMode.OFF, controller.currentMode.value)
    }

    @Test
    fun `orientation sensor manager computes angles`() {
        val sensorManager = com.example.airmind.core.controls.OrientationSensorManager(context)
        assertNotNull(sensorManager.orientationAngles.value)
        assertEquals(0f, sensorManager.orientationAngles.value.pitch, 0.01f)
    }

    @Test
    fun `bluetooth device manager safe initialization and refresh`() {
        val manager = com.example.airmind.core.bluetooth.BluetoothDeviceManager(context)
        assertNotNull(manager.connectedState.value)
        assertNotNull(manager.discoveredDevices.value)
        // refreshConnectedDevice should complete safely without throwing SecurityException
        manager.refreshConnectedDevice()
        assertNotNull(manager.connectedState.value)
    }
}
