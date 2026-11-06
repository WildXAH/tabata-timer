package com.example.tabatatimer

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun WorkoutSoundEffects(
    workoutId: String,
    position: TimerPosition,
    hasStarted: Boolean,
    isRunning: Boolean,
    soundEnabled: Boolean
) {
    var toneGenerator by remember {
        mutableStateOf<ToneGenerator?>(null)
    }

    var lastPhaseKey by rememberSaveable(workoutId) {
        mutableStateOf<String?>(null)
    }

    var lastCountdownKey by rememberSaveable(workoutId) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(Unit) {
        val generator = try {
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                70
            )
        } catch (_: RuntimeException) {
            null
        }

        toneGenerator = generator

        onDispose {
            generator?.release()
        }
    }

    LaunchedEffect(
        toneGenerator,
        hasStarted,
        isRunning,
        soundEnabled,
        position.phase,
        position.round,
        position.cycle,
        position.secondsLeft
    ) {
        if (!hasStarted) {
            lastPhaseKey = null
            lastCountdownKey = null
            return@LaunchedEffect
        }

        if (!isRunning || !soundEnabled) {
            toneGenerator?.stopTone()
            return@LaunchedEffect
        }

        val generator = toneGenerator
            ?: return@LaunchedEffect

        val phaseKey =
            "${position.phase}:${position.round}:${position.cycle}"

        if (phaseKey != lastPhaseKey) {
            lastPhaseKey = phaseKey

            when (position.phase) {
                TimerPhase.PREPARE -> {
                    generator.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        150
                    )
                }

                TimerPhase.WORK -> {
                    generator.startTone(
                        ToneGenerator.TONE_PROP_ACK,
                        250
                    )
                }

                TimerPhase.REST,
                TimerPhase.CYCLE_REST -> {
                    generator.startTone(
                        ToneGenerator.TONE_PROP_BEEP2,
                        250
                    )
                }

                TimerPhase.FINISHED -> {
                    generator.startTone(
                        ToneGenerator.TONE_PROP_ACK,
                        700
                    )
                }
            }
        } else if (
            position.phase != TimerPhase.FINISHED &&
            position.secondsLeft in 1L..3L
        ) {
            val countdownKey =
                "$phaseKey:${position.secondsLeft}"

            if (countdownKey != lastCountdownKey) {
                lastCountdownKey = countdownKey

                generator.startTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    100
                )
            }
        }

        if (
            position.phase != TimerPhase.FINISHED &&
            position.secondsLeft in 1L..3L
        ) {
            lastCountdownKey =
                "$phaseKey:${position.secondsLeft}"
        }
    }
}