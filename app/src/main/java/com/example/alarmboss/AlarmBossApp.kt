package com.example.alarmboss

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.alarmboss.data.AlarmDatabase
import com.example.alarmboss.data.AlarmRepository
import com.example.alarmboss.data.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AlarmBossApp : Application() {

    lateinit var repository: AlarmRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    /** Outlives any single Activity, for writes that must finish after the screen closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AlarmDatabase.getInstance(this)
        repository = AlarmRepository(db.alarmDao())
        profileRepository = ProfileRepository(db.userProfileDao(), db.wakeEventDao())
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            setBypassDnd(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(alarmChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "alarm_channel"
    }
}
