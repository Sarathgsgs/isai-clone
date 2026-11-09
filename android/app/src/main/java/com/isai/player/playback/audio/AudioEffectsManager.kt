package com.isai.player.playback.audio

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import androidx.media3.exoplayer.ExoPlayer
import com.isai.player.data.equalizer.ClippingMath
import com.isai.player.data.equalizer.EffectCapabilities
import com.isai.player.data.equalizer.EqualizerState
import com.isai.player.data.repository.EqualizerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class AudioEffectsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerRepository: EqualizerRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var currentSessionId: Int = 0
        private set
    private var exoPlayer: ExoPlayer? = null

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _capabilities = MutableStateFlow(EffectCapabilities())
    val capabilities: StateFlow<EffectCapabilities> = _capabilities.asStateFlow()

    init {
        scope.launch {
            equalizerRepository.equalizerState.collectLatest { state ->
                applyEffects(state)
            }
        }
    }

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        val sessionId = player.audioSessionId
        if (sessionId != 0 && sessionId != currentSessionId) {
            onAudioSessionIdChanged(sessionId)
        }
    }

    fun onAudioSessionIdChanged(newSessionId: Int) {
        if (newSessionId == 0 || newSessionId == currentSessionId) return

        releaseEffects()
        currentSessionId = newSessionId
        broadcastSessionOpen(newSessionId)
        initEffects(newSessionId)

        scope.launch {
            equalizerRepository.equalizerState.collectLatest { state ->
                applyEffects(state)
            }
        }
    }

    private fun initEffects(sessionId: Int) {
        // 1. Equalizer initialization & probe
        try {
            val eq = Equalizer(0, sessionId).apply { enabled = false }
            equalizer = eq
            val numBands = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange
            val minLevel = levelRange[0].toInt()
            val maxLevel = levelRange[1].toInt()
            val centerFreqs = (0 until numBands).map { band ->
                eq.getCenterFreq(band.toShort()) / 1000 // Convert mHz to Hz
            }

            // 2. BassBoost initialization & probe
            var bbSupported = false
            try {
                val bb = BassBoost(0, sessionId).apply { enabled = false }
                bassBoost = bb
                bbSupported = bb.strengthSupported
            } catch (e: Exception) {
                bassBoost = null
            }

            // 3. Virtualizer initialization & probe
            var virtSupported = false
            try {
                val virt = Virtualizer(0, sessionId).apply { enabled = false }
                virtualizer = virt
                virtSupported = virt.strengthSupported
            } catch (e: Exception) {
                virtualizer = null
            }

            // 4. LoudnessEnhancer initialization & probe
            var leSupported = false
            try {
                val le = LoudnessEnhancer(sessionId).apply { enabled = false }
                loudnessEnhancer = le
                leSupported = true
            } catch (e: Exception) {
                loudnessEnhancer = null
            }

            _capabilities.value = EffectCapabilities(
                isEqualizerSupported = true,
                bandCount = numBands,
                minLevelMilliBels = minLevel,
                maxLevelMilliBels = maxLevel,
                centerFreqsHz = centerFreqs,
                isBassBoostSupported = bbSupported,
                isVirtualizerSupported = virtSupported,
                isLoudnessEnhancerSupported = leSupported
            )
        } catch (e: Exception) {
            equalizer = null
            _capabilities.value = EffectCapabilities(isEqualizerSupported = false)
        }
    }

    private fun applyEffects(state: EqualizerState) {
        val eq = equalizer
        val bb = bassBoost
        val virt = virtualizer
        val le = loudnessEnhancer
        val player = exoPlayer

        if (!state.enabled || eq == null) {
            runCatching { eq?.enabled = false }
            runCatching { bb?.enabled = false }
            runCatching { virt?.enabled = false }
            runCatching { le?.enabled = false }
            player?.volume = 1.0f
            return
        }

        // Apply Equalizer bands
        runCatching {
            eq.enabled = true
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                if (i in state.bandGains.indices) {
                    val gain = state.bandGains[i]
                    val range = eq.bandLevelRange
                    val clamped = gain.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                    eq.setBandLevel(i.toShort(), clamped)
                }
            }
        }

        // Apply BassBoost
        runCatching {
            if (bb != null && bb.strengthSupported) {
                bb.enabled = state.bassBoostStrength > 0
                bb.setStrength(state.bassBoostStrength.toShort())
            }
        }

        // Apply Virtualizer
        runCatching {
            if (virt != null && virt.strengthSupported) {
                virt.enabled = state.virtualizerStrength > 0
                virt.setStrength(state.virtualizerStrength.toShort())
            }
        }

        // Clipping analysis & Preamp compensation
        val analysis = ClippingMath.analyze(
            preampDb = state.preampDb,
            bandGainsMilliBels = state.bandGains,
            preventClipping = state.preventClipping
        )

        runCatching {
            if (analysis.effectivePreampDb > 0.1f && le != null) {
                le.enabled = true
                val gainMilliBels = (analysis.effectivePreampDb * 100f).roundToInt()
                le.setTargetGain(gainMilliBels)
                player?.volume = 1.0f
            } else {
                le?.enabled = false
                // Digital gain attenuation via player volume
                player?.volume = analysis.linearScaleFactor.coerceIn(0.01f, 1.0f)
            }
        }
    }

    private fun broadcastSessionOpen(sessionId: Int) {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        context.sendBroadcast(intent)
    }

    private fun broadcastSessionClose(sessionId: Int) {
        if (sessionId == 0) return
        val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        }
        context.sendBroadcast(intent)
    }

    fun releaseEffects() {
        broadcastSessionClose(currentSessionId)
        currentSessionId = 0

        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
    }
}
