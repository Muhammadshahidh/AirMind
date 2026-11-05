package com.example.airmind.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_mappings")
data class GestureMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventType: String,
    val earbudSide: String, // "LEFT", "RIGHT", "BOTH"
    val actionType: String,
    val actionParam: String = "",
    val isEnabled: Boolean = true
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerType: String, // "BT_CONNECTED", "BT_DISCONNECTED", "TIME_OF_DAY", "EARBUD_TAP"
    val triggerParam: String = "",
    val actionType: String, // "LAUNCH_APP", "SPEAK_TEXT", "SET_VOLUME", "TOGGLE_FLASHLIGHT", "START_LISTENING"
    val actionParam: String = "",
    val isEnabled: Boolean = true
)

@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey
    val address: String,
    val name: String,
    val deviceType: String = "EARBUDS",
    val lastConnected: Long = System.currentTimeMillis(),
    val batteryLeft: Int = -1,
    val batteryRight: Int = -1,
    val batteryCase: Int = -1,
    val isConnected: Boolean = false
)

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)
