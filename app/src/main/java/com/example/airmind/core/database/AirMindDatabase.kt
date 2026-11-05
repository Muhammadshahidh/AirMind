package com.example.airmind.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.airmind.core.database.dao.AutomationDao
import com.example.airmind.core.database.dao.GestureDao
import com.example.airmind.core.database.dao.SavedDeviceDao
import com.example.airmind.core.database.dao.VoiceNoteDao
import com.example.airmind.core.database.entities.AutomationRuleEntity
import com.example.airmind.core.database.entities.GestureMappingEntity
import com.example.airmind.core.database.entities.SavedDeviceEntity
import com.example.airmind.core.database.entities.VoiceNoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GestureMappingEntity::class,
        AutomationRuleEntity::class,
        SavedDeviceEntity::class,
        VoiceNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AirMindDatabase : RoomDatabase() {
    abstract fun gestureDao(): GestureDao
    abstract fun automationDao(): AutomationDao
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun voiceNoteDao(): VoiceNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AirMindDatabase? = null

        fun getInstance(context: Context): AirMindDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AirMindDatabase::class.java,
                    "airmind_database.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default gesture mappings
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultGestures = listOf(
                                GestureMappingEntity(eventType = "SINGLE_TAP", earbudSide = "BOTH", actionType = "PLAY_PAUSE"),
                                GestureMappingEntity(eventType = "DOUBLE_TAP", earbudSide = "BOTH", actionType = "NEXT_TRACK"),
                                GestureMappingEntity(eventType = "TRIPLE_TAP", earbudSide = "BOTH", actionType = "PREV_TRACK"),
                                GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = "RIGHT", actionType = "VOLUME_UP"),
                                GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = "LEFT", actionType = "VOLUME_DOWN"),
                                GestureMappingEntity(eventType = "HEADSET_HOOK", earbudSide = "BOTH", actionType = "VOICE_ASSISTANT")
                            )
                            val defaultAutomations = listOf(
                                AutomationRuleEntity(
                                    title = "Welcome Chime on Connect",
                                    triggerType = "BT_CONNECTED",
                                    actionType = "SPEAK_TEXT",
                                    actionParam = "AIRMIND connected. All systems active.",
                                    isEnabled = true
                                ),
                                AutomationRuleEntity(
                                    title = "Auto-Boost Listening Volume",
                                    triggerType = "BT_CONNECTED",
                                    actionType = "SET_VOLUME",
                                    actionParam = "80",
                                    isEnabled = true
                                )
                            )
                            getInstance(context).gestureDao().insertAll(defaultGestures)
                            defaultAutomations.forEach {
                                getInstance(context).automationDao().insert(it)
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
