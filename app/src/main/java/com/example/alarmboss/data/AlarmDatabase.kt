package com.example.alarmboss.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Alarm::class, UserProfile::class, WakeEvent::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun wakeEventDao(): WakeEventDao

    companion object {
        @Volatile private var INSTANCE: AlarmDatabase? = null

        /** Adds the optional pinned date for one-time alarms; existing alarms simply get no date. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN specificDate TEXT")
            }
        }

        fun getInstance(context: Context): AlarmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "alarmboss.db"
                )
                    // A real migration, so upgrading from 2 keeps every saved alarm instead of wiping them.
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
    }
}
