package com.example.airmind.core.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.airmind.AirMindApplication

/**
 * Foreground Service maintaining the MediaSession and background physical media controls.
 * Ensures Bluetooth earbuds touch actions (Single tap, Double tap, Triple tap, Long press)
 * are received and handled even when the screen is locked, display is off, or app is in background.
 */
class AirMindEarbudService : Service() {

    companion object {
        const val CHANNEL_ID = "airmind_earbud_service_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.airmind.ACTION_START_EARBUD_SERVICE"
        const val ACTION_STOP = "com.example.airmind.ACTION_STOP_EARBUD_SERVICE"
        const val ACTION_TRIGGER_AI = "com.example.airmind.ACTION_TRIGGER_AI"
        const val ACTION_TOGGLE_LISTEN = "com.example.airmind.ACTION_TOGGLE_LISTEN"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, AirMindEarbudService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, AirMindEarbudService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_AI -> {
                val app = application as? AirMindApplication
                app?.appContainer?.earbudActionHandler?.executeMappedAction("DOUBLE_TAP", "BOTH", "ACTIVATE_AI")
            }
            ACTION_TOGGLE_LISTEN -> {
                val app = application as? AirMindApplication
                app?.appContainer?.earbudActionHandler?.executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        updatePlaybackState()
        return START_STICKY
    }

    private fun initializeMediaSession() {
        try {
            mediaSession = MediaSession(this, "AirMindEarbudMediaSession").apply {
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )

                setCallback(object : MediaSession.Callback() {
                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                        }

                        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                            val app = application as? AirMindApplication
                            app?.appContainer?.earbudActionHandler?.handleMediaKeyEvent(keyEvent)
                            return true
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }

                    override fun onPlay() {
                        val app = application as? AirMindApplication
                        app?.appContainer?.earbudActionHandler?.executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
                    }

                    override fun onPause() {
                        val app = application as? AirMindApplication
                        app?.appContainer?.earbudActionHandler?.executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
                    }

                    override fun onSkipToNext() {
                        val app = application as? AirMindApplication
                        app?.appContainer?.earbudActionHandler?.executeMappedAction("DOUBLE_TAP", "BOTH", "ACTIVATE_AI")
                    }

                    override fun onSkipToPrevious() {
                        val app = application as? AirMindApplication
                        app?.appContainer?.earbudActionHandler?.executeMappedAction("TRIPLE_TAP", "BOTH", "REPEAT_RESPONSE")
                    }
                })

                isActive = true
            }
        } catch (_: Exception) {}
    }

    private fun updatePlaybackState() {
        try {
            val state = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_FAST_FORWARD or
                    PlaybackState.ACTION_REWIND
                )
                .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
                .build()

            mediaSession?.setPlaybackState(state)
        } catch (_: Exception) {}
    }

    private fun buildNotification(): Notification {
        val app = application as? AirMindApplication
        val devState = app?.appContainer?.bluetoothDeviceManager?.connectedState?.value
        val deviceName = if (devState?.isConnected == true) devState.deviceName else "Earbuds Controller Active"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val listenIntent = Intent(this, AirMindEarbudService::class.java).apply {
            action = ACTION_TOGGLE_LISTEN
        }
        val listenPendingIntent = PendingIntent.getService(
            this,
            1,
            listenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val aiIntent = Intent(this, AirMindEarbudService::class.java).apply {
            action = ACTION_TRIGGER_AI
        }
        val aiPendingIntent = PendingIntent.getService(
            this,
            2,
            aiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AIRMIND AI — Hands-Free Mode")
            .setContentText("Connected: $deviceName | Touch Controls Active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, "Listen", listenPendingIntent)
            .addAction(android.R.drawable.ic_dialog_info, "Activate AI", aiPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AIRMIND Earbuds Controller",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Bluetooth earbuds physical touch controls responsive in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
