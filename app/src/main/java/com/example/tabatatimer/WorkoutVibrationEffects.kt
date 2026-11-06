package com.example.tabatatimer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun WorkoutVibrationEffects(
    workoutId: String,
    position: TimerPosition,
    hasStarted: Boolean,
    isRunning: Boolean,
    vibrationEnabled: Boolean
) {
    val context = LocalContext.current.applicationContext

    val vibrator = remember(context) {
        findWorkoutVibrator(context)
    }

    var lastPhaseKey by rememberSaveable(workoutId) {
        mutableStateOf<String?>(null)
    }

    DisposableEffect(vibrator) {
        onDispose {
            vibrator?.cancel()
        }
    }

    LaunchedEffect(
        vibrator,
        hasStarted,
        isRunning,
        vibrationEnabled,
        position.phase,
        position.round,
        position.cycle
    ) {
        if (!hasStarted) {
            lastPhaseKey = null
            vibrator?.cancel()
            return@LaunchedEffect
        }

        val phaseKey =
            "${position.phase}:${position.round}:${position.cycle}"

        if (!vibrationEnabled) {
            lastPhaseKey = phaseKey
            vibrator?.cancel()
            return@LaunchedEffect
        }

        if (!isRunning) {
            vibrator?.cancel()
            return@LaunchedEffect
        }

        val deviceVibrator = vibrator
            ?: return@LaunchedEffect

        if (!deviceVibrator.hasVibrator()) {
            return@LaunchedEffect
        }

        if (phaseKey == lastPhaseKey) {
            return@LaunchedEffect
        }

        lastPhaseKey = phaseKey

        val pattern = when (position.phase) {
            TimerPhase.PREPARE -> {
                longArrayOf(0, 150)
            }

            TimerPhase.WORK -> {
                longArrayOf(0, 200, 80, 200)
            }

            TimerPhase.REST,
            TimerPhase.CYCLE_REST -> {
                longArrayOf(0, 300)
            }

            TimerPhase.FINISHED -> {
                longArrayOf(0, 300, 100, 300, 100, 600)
            }
        }

        playWorkoutVibration(
            vibrator = deviceVibrator,
            pattern = pattern
        )
    }
}

@Suppress("DEPRECATION")
private fun findWorkoutVibrator(
    context: Context
): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(
            VibratorManager::class.java
        )

        manager?.defaultVibrator
    } else {
        context.getSystemService(
            Context.VIBRATOR_SERVICE
        ) as? Vibrator
    }
}

@Suppress("DEPRECATION")
private fun playWorkoutVibration(
    vibrator: Vibrator,
    pattern: LongArray
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                pattern,
                -1
            )
        )
    } else {
        vibrator.vibrate(
            pattern,
            -1
        )
    }
}