package com.fushengce.app

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import androidx.annotation.RawRes

enum class UiSoundEffect(@RawRes val resourceId: Int) {
    PageFlip(R.raw.page_flip),
    AlbumOpen(R.raw.album_open),
    BrushWrite(R.raw.brush_write),
    SealStamp(R.raw.seal_stamp),
    RealmSwitch(R.raw.realm_switch),
    ArmorShift(R.raw.armor_shift),
    ListenStart(R.raw.listen_start),
    Found(R.raw.found),
    Complete(R.raw.complete),
    Issue(R.raw.issue),
    Unavailable(R.raw.unavailable),
    TaskEnd(R.raw.task_end),
}

class UiSoundPlayer(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loadedSamples = mutableSetOf<Int>()
    private val sampleIds: Map<UiSoundEffect, Int>
    private var lastPlayedAtMillis: Long? = null

    var enabled: Boolean = preferences.getBoolean(ENABLED_KEY, true)
        private set

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSamples += sampleId
        }
        sampleIds = UiSoundEffect.entries.associateWith { effect ->
            soundPool.load(appContext, effect.resourceId, 1)
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        preferences.edit().putBoolean(ENABLED_KEY, value).apply()
    }

    fun play(effect: UiSoundEffect) {
        val now = SystemClock.elapsedRealtime()
        val elapsed = lastPlayedAtMillis?.let { previous -> now - previous }
        val canPlay = UiSoundPolicy.canPlay(
            enabled = enabled,
            ringerIsNormal = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            dndAllowsSound = notificationManager.currentInterruptionFilter ==
                NotificationManager.INTERRUPTION_FILTER_ALL,
            elapsedSincePreviousMillis = elapsed,
        )
        val sampleId = sampleIds[effect]
        if (!canPlay || sampleId == null || sampleId !in loadedSamples) return

        soundPool.play(sampleId, VOLUME, VOLUME, 1, 0, 1f)
        lastPlayedAtMillis = now
    }

    override fun close() {
        soundPool.release()
    }

    private companion object {
        const val PREFERENCES_NAME = "fushengce-ui-sound"
        const val ENABLED_KEY = "enabled"
        const val VOLUME = 0.22f
    }
}

internal object UiSoundPolicy {
    private const val MIN_INTERVAL_MILLIS = 180L

    fun canPlay(
        enabled: Boolean,
        ringerIsNormal: Boolean,
        dndAllowsSound: Boolean,
        elapsedSincePreviousMillis: Long?,
    ): Boolean = enabled &&
        ringerIsNormal &&
        dndAllowsSound &&
        (elapsedSincePreviousMillis == null || elapsedSincePreviousMillis >= MIN_INTERVAL_MILLIS)
}
