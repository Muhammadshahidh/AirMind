package com.example.airmind.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.airmind.core.database.entities.AutomationRuleEntity
import com.example.airmind.core.database.entities.GestureMappingEntity
import com.example.airmind.core.database.entities.SavedDeviceEntity
import com.example.airmind.core.database.entities.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Query("SELECT * FROM gesture_mappings ORDER BY id ASC")
    fun getAllGestures(): Flow<List<GestureMappingEntity>>

    @Query("SELECT * FROM gesture_mappings WHERE eventType = :eventType AND earbudSide = :side LIMIT 1")
    suspend fun getGesture(eventType: String, side: String): GestureMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<GestureMappingEntity>)

    @Update
    suspend fun update(mapping: GestureMappingEntity)

    @Query("DELETE FROM gesture_mappings")
    suspend fun clearAll()
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE triggerType = :triggerType AND isEnabled = 1")
    suspend fun getActiveRulesForTrigger(triggerType: String): List<AutomationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutomationRuleEntity): Long

    @Update
    suspend fun update(rule: AutomationRuleEntity)

    @Delete
    suspend fun delete(rule: AutomationRuleEntity)
}

@Dao
interface SavedDeviceDao {
    @Query("SELECT * FROM saved_devices ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<SavedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: SavedDeviceEntity)

    @Query("UPDATE saved_devices SET isConnected = :connected WHERE address = :address")
    suspend fun setConnected(address: String, connected: Boolean)

    @Query("UPDATE saved_devices SET isConnected = 0")
    suspend fun markAllDisconnected()

    @Delete
    suspend fun delete(device: SavedDeviceEntity)
}

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<VoiceNoteEntity>>

    @Insert
    suspend fun insert(note: VoiceNoteEntity): Long

    @Query("DELETE FROM voice_notes")
    suspend fun clearNotes()
}
