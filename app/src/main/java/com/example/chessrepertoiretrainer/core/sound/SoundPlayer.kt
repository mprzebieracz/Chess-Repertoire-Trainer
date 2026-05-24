package com.example.chessrepertoiretrainer.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.chessrepertoiretrainer.R
import com.example.chessrepertoiretrainer.feature.settings.data.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SoundPlayer(
    context: Context,
    userSettingsRepository: UserSettingsRepository,
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<MoveSoundType, Int> = mapOf(
        MoveSoundType.MOVE to soundPool.load(context, R.raw.move, 1),
        MoveSoundType.CAPTURE to soundPool.load(context, R.raw.capture, 1),
        MoveSoundType.CHECK to soundPool.load(context, R.raw.check, 1),
        MoveSoundType.MATE to soundPool.load(context, R.raw.mate, 1),
    )

    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
    }

    @Volatile
    private var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            userSettingsRepository.settingsFlow
                .map { it.soundsEnabled }
                .collect { enabled = it }
        }
    }

    fun play(type: MoveSoundType) {
        if (!enabled) return
        val id = soundIds[type] ?: return
        if (id !in loaded) return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }
}
