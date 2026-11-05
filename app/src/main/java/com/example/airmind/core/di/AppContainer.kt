package com.example.airmind.core.di

import android.content.Context
import com.example.airmind.core.assistant.AiAssistantEngine
import com.example.airmind.core.assistant.SpeechManager
import com.example.airmind.core.audio.RemoteListeningEngine
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.bluetooth.EarbudActionHandler
import com.example.airmind.core.controls.MobileControlManager
import com.example.airmind.core.database.AirMindDatabase
import com.example.airmind.core.datastore.UserPreferencesRepository

interface AppContainer {
    val context: Context
    val database: AirMindDatabase
    val userPreferencesRepository: UserPreferencesRepository
    val bluetoothDeviceManager: BluetoothDeviceManager
    val bluetoothScanner: com.example.airmind.core.bluetooth.BluetoothScanner
    val mobileControlManager: MobileControlManager
    val remoteListeningEngine: RemoteListeningEngine
    val speechManager: SpeechManager
    val aiAssistantEngine: AiAssistantEngine
    val earbudActionHandler: EarbudActionHandler
    val noiseCancellationController: com.example.airmind.core.audio.NoiseCancellationController
    val orientationSensorManager: com.example.airmind.core.controls.OrientationSensorManager
}

class DefaultAppContainer(override val context: Context) : AppContainer {
    override val database: AirMindDatabase by lazy {
        AirMindDatabase.getInstance(context)
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val bluetoothDeviceManager: BluetoothDeviceManager by lazy {
        BluetoothDeviceManager(context)
    }

    override val bluetoothScanner: com.example.airmind.core.bluetooth.BluetoothScanner by lazy {
        com.example.airmind.core.bluetooth.BluetoothScanner(context)
    }

    override val mobileControlManager: MobileControlManager by lazy {
        MobileControlManager(context)
    }

    override val remoteListeningEngine: RemoteListeningEngine by lazy {
        RemoteListeningEngine(context)
    }

    override val speechManager: SpeechManager by lazy {
        SpeechManager(context)
    }

    override val aiAssistantEngine: AiAssistantEngine by lazy {
        AiAssistantEngine(
            context = context,
            speechManager = speechManager,
            mobileControlManager = mobileControlManager,
            bluetoothDeviceManager = bluetoothDeviceManager,
            database = database
        )
    }

    override val earbudActionHandler: EarbudActionHandler by lazy {
        EarbudActionHandler(
            context = context,
            mobileControlManager = mobileControlManager,
            database = database,
            noiseCancellationController = noiseCancellationController
        )
    }

    override val noiseCancellationController: com.example.airmind.core.audio.NoiseCancellationController by lazy {
        com.example.airmind.core.audio.NoiseCancellationController(
            context = context,
            listeningEngine = remoteListeningEngine,
            mobileControlManager = mobileControlManager,
            preferencesRepository = userPreferencesRepository
        )
    }

    override val orientationSensorManager: com.example.airmind.core.controls.OrientationSensorManager by lazy {
        com.example.airmind.core.controls.OrientationSensorManager(context)
    }
}
