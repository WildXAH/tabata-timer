package com.example.tabatatimer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex

@Composable
internal fun RunWorkoutScreen(
    workout: Workout,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onExit: () -> Unit,
    onEdit: () -> Unit
) {
    var elapsedMillis by rememberSaveable {
        mutableLongStateOf(0L)
    }

    var hasStarted by rememberSaveable {
        mutableStateOf(false)
    }

    var isRunning by rememberSaveable {
        mutableStateOf(false)
    }

    var showResetDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var wasRunningBeforeReset by rememberSaveable {
        mutableStateOf(false)
    }

    var showExitDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var wasRunningBeforeExit by rememberSaveable {
        mutableStateOf(false)
    }

    val totalMillis = workoutDurationSeconds(workout) * 1000

    val position = timerPosition(
        workout = workout,
        elapsedMillis = elapsedMillis
    )

    val nextPosition = upcomingTimerPositions(
        workout = workout,
        elapsedMillis = elapsedMillis
    ).firstOrNull()

    val isFinished = position.phase == TimerPhase.FINISHED
    val phaseColor = timerPhaseColor(position.phase)

    val ink = if (isFinished) {
        MaterialTheme.colorScheme.onBackground
    } else {
        Color(0xFF101318)
    }

    val secondsLeftTotal = (
            (totalMillis - elapsedMillis).coerceAtLeast(0L) + 999
            ) / 1000

    val roundsLeft = when (position.phase) {
        TimerPhase.FINISHED -> 0
        TimerPhase.CYCLE_REST -> workout.rounds
        TimerPhase.REST -> workout.rounds - position.round
        else -> workout.rounds - position.round + 1
    }

    val cyclesLeft = when (position.phase) {
        TimerPhase.FINISHED -> 0
        TimerPhase.CYCLE_REST -> workout.cycles - position.cycle
        else -> workout.cycles - position.cycle + 1
    }

    val countdownText = stringResource(
        R.string.duration_format,
        position.secondsLeft / 60,
        position.secondsLeft % 60
    )

    val totalTimeText = stringResource(
        R.string.total_duration_format,
        secondsLeftTotal / 3600,
        (secondsLeftTotal % 3600) / 60,
        secondsLeftTotal % 60
    )

    val controlLabel = stringResource(
        when {
            isFinished -> R.string.exit_timer
            !hasStarted -> R.string.start
            isRunning -> R.string.pause
            else -> R.string.resume
        }
    )

    val exitDescription = stringResource(R.string.tab_timers)
    val resetDescription = stringResource(R.string.reset_workout)
    val editDescription = stringResource(R.string.timer_edit_current)

    WorkoutSoundEffects(
        workoutId = workout.id,
        position = position,
        hasStarted = hasStarted,
        isRunning = isRunning,
        soundEnabled = soundEnabled,
        workSeconds = workout.workSeconds,
        halfwaySignalEnabled = workout.halfwaySignalEnabled
    )

    WorkoutVibrationEffects(
        workoutId = workout.id,
        position = position,
        hasStarted = hasStarted,
        isRunning = isRunning,
        vibrationEnabled = vibrationEnabled
    )

    val view = LocalView.current

    DisposableEffect(view, isFinished) {
        val previousKeepScreenOn = view.keepScreenOn

        view.keepScreenOn = !isFinished

        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    LaunchedEffect(isRunning, isFinished) {
        if (isRunning && !isFinished) {
            val startElapsed = elapsedMillis
            val startedAt = SystemClock.elapsedRealtime()

            while (true) {
                elapsedMillis = (
                        startElapsed +
                                SystemClock.elapsedRealtime() - startedAt
                        ).coerceAtMost(totalMillis)

                if (elapsedMillis >= totalMillis) {
                    break
                }

                delay(100L)
            }
        }
    }

    var exitToEditor by rememberSaveable {
        mutableStateOf(false)
    }

    val leaveTimer: () -> Unit = {
        if (exitToEditor) {
            onEdit()
        } else {
            onExit()
        }
    }

    val cancelReset: () -> Unit = {
        showResetDialog = false
        isRunning = wasRunningBeforeReset
    }

    val cancelExit: () -> Unit = {
        showExitDialog = false
        exitToEditor = false
        isRunning = wasRunningBeforeExit
    }

    val requestExit: () -> Unit = {
        if (!hasStarted || isFinished) {
            leaveTimer()
        } else {
            wasRunningBeforeExit = isRunning
            isRunning = false
            showExitDialog = true
        }
    }

    BackHandler {
        when {
            showExitDialog -> cancelExit()
            showResetDialog -> cancelReset()
            else -> requestExit()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF101318)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFF101318))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ink = Color(0xFFF3F5F7)
                FilledIconButton(
                    onClick = requestExit,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = exitDescription
                        },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ink.copy(alpha = 0.10f),
                        contentColor = ink
                    )
                ) {
                    Text(
                        text = "▦",
                        fontSize = 28.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = workout.name,
                        color = ink.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = totalTimeText,
                        color = ink,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFeatureSettings = "tnum"
                        )
                    )
                }

                FilledIconButton(
                    onClick = {
                        exitToEditor = true
                        requestExit()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = editDescription
                        },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF3F5F7).copy(alpha = 0.10f),
                        contentColor = Color(0xFFF3F5F7)
                    )
                ) {
                    Text(
                        text = "⚙",
                        fontSize = 24.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .zIndex(1f)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(phaseColor)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val fontScale = LocalDensity.current.fontScale
                    val digitCount = countdownText.length.coerceAtLeast(5)

                    val widthLimit =
                        maxWidth.value / (digitCount * 0.62f)

                    val heightLimit = maxHeight.value * 0.42f

                    val countdownSize = (
                            minOf(widthLimit, heightLimit, 160f) / fontScale
                            ).coerceAtLeast(1f).sp

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = timerPhaseLabel(position.phase).uppercase(),
                            color = ink,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                        )

                        Text(
                            text = countdownText,
                            modifier = Modifier.fillMaxWidth(),
                            color = ink,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = countdownSize,
                                lineHeight = countdownSize,
                                letterSpacing = (-2).sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )

                    }
                }

                FilledIconButton(
                    enabled = hasStarted,
                    onClick = {
                        wasRunningBeforeReset =
                            isRunning && !isFinished

                        isRunning = false
                        showResetDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-72).dp, y = (-12).dp)
                        .size(52.dp)
                        .border(
                            width = 2.dp,
                            color = phaseColor,
                            shape = CircleShape
                        )
                        .semantics {
                            contentDescription = resetDescription
                        },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF101318),
                        contentColor = Color(0xFFF3F5F7),
                        disabledContainerColor = Color(0xFF101318),
                        disabledContentColor =
                            Color(0xFFF3F5F7).copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "↺",
                        fontSize = 28.sp
                    )
                }
            }

            if (!isFinished && nextPosition != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .background(
                            timerPhaseColor(nextPosition.phase)
                        )
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timerPhaseLabel(nextPosition.phase).uppercase(),
                        color = Color(0xFF101318),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = stringResource(
                            R.string.duration_format,
                            nextPosition.secondsLeft / 60,
                            nextPosition.secondsLeft % 60
                        ),
                        color = Color(0xFF101318),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101318))
                    .padding(
                        horizontal = 16.dp,
                        vertical = 20.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ink = Color(0xFFF3F5F7)
                TimerCounter(
                    value = roundsLeft,
                    label = stringResource(R.string.rounds_left),
                    color = ink,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isFinished) {
                                onExit()
                            } else {
                                hasStarted = true
                                isRunning = !isRunning
                            }
                        },
                        modifier = Modifier
                            .size(88.dp)
                            .semantics {
                                contentDescription = controlLabel
                            },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = when {
                                isFinished -> "✓"
                                isRunning -> "Ⅱ"
                                else -> "▶"
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = controlLabel,
                        color = ink,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }

                TimerCounter(
                    value = cyclesLeft,
                    label = stringResource(R.string.cycles_left),
                    color = ink,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showExitDialog) {
        TimerConfirmationDialog(
            title = stringResource(R.string.end_workout_title),
            message = stringResource(R.string.end_workout_message),
            confirmLabel = stringResource(R.string.end_workout),
            cancelLabel = stringResource(R.string.continue_workout),
            onDismiss = cancelExit,
            onConfirm = {
                isRunning = false
                showExitDialog = false
                leaveTimer()
            }
        )
    }

    if (showResetDialog) {
        TimerConfirmationDialog(
            title = stringResource(R.string.reset_workout_title),
            message = stringResource(R.string.reset_workout_message),
            confirmLabel = stringResource(R.string.reset),
            cancelLabel = stringResource(R.string.cancel),
            onDismiss = cancelReset,
            onConfirm = {
                isRunning = false
                elapsedMillis = 0L
                hasStarted = false
                wasRunningBeforeReset = false
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun TimerCounter(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value.toString(),
            color = color,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = label,
            color = color.copy(alpha = 0.70f),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(text = cancelLabel)
            }
        }
    )
}