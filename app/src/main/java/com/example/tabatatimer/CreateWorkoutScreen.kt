package com.example.tabatatimer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource

@Composable
internal fun CreateWorkoutScreen(
    initialWorkout: Workout? = null,
    isFirstSetup: Boolean = false,
    isSubmitting: Boolean = false,
    isMainWorkout: Boolean = false,
    onSaveWithMain: ((Workout, Boolean) -> Unit)? = null,
    onCancel: () -> Unit,
    onSave: (Workout) -> Unit,
    onStart: (Workout) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (Workout) -> Unit
) {
    var showMenu by rememberSaveable {
        mutableStateOf(false)
    }

    var showDeleteDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDiscardDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var workoutName by rememberSaveable(initialWorkout) {
        mutableStateOf(initialWorkout?.name ?: "")
    }

    var isMainWorkoutSelected by rememberSaveable(initialWorkout?.id) {
        mutableStateOf(isMainWorkout)
    }

    var halfwaySignalEnabled by rememberSaveable(initialWorkout?.id) {
        mutableStateOf(initialWorkout?.halfwaySignalEnabled ?: false)
    }

    var prepareSeconds by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.prepareSeconds ?: 10)
    }

    var workSeconds by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.workSeconds ?: 20)
    }

    var restSeconds by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.restSeconds ?: 10)
    }

    var rounds by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.rounds ?: 8)
    }

    var cycles by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.cycles ?: 1)
    }

    var cycleRestSeconds by rememberSaveable(initialWorkout) {
        mutableIntStateOf(initialWorkout?.cycleRestSeconds ?: 60)
    }

    val workoutId = rememberSaveable(initialWorkout?.id) {
        initialWorkout?.id ?: UUID.randomUUID().toString()
    }

    val hasChanges =
        halfwaySignalEnabled !=
                (initialWorkout?.halfwaySignalEnabled ?: false) ||
        isMainWorkoutSelected != isMainWorkout ||
        workoutName != (initialWorkout?.name ?: "") ||
                prepareSeconds != (initialWorkout?.prepareSeconds ?: 10) ||
                workSeconds != (initialWorkout?.workSeconds ?: 20) ||
                restSeconds != (initialWorkout?.restSeconds ?: 10) ||
                rounds != (initialWorkout?.rounds ?: 8) ||
                cycles != (initialWorkout?.cycles ?: 1) ||
                cycleRestSeconds !=
                (initialWorkout?.cycleRestSeconds ?: 60)

    val canSubmit =
        workoutName.isNotBlank() && !isSubmitting

    val draft = Workout(
        id = workoutId,
        name = workoutName.trim(),
        prepareSeconds = prepareSeconds,
        workSeconds = workSeconds,
        restSeconds = restSeconds,
        rounds = rounds,
        cycles = cycles,
        cycleRestSeconds = cycleRestSeconds,
        halfwaySignalEnabled = halfwaySignalEnabled
    )

    val totalSeconds = workoutDurationSeconds(draft)

    val backDescription = stringResource(R.string.editor_back)
    val moreDescription = stringResource(R.string.editor_more)

    val requestBack: () -> Unit = {
        if (!isSubmitting) {
            if (hasChanges) {
                showDiscardDialog = true
            } else {
                onCancel()
            }
        }
    }

    BackHandler {
        when {
            showMenu -> showMenu = false
            showDiscardDialog -> showDiscardDialog = false
            showDeleteDialog -> showDeleteDialog = false
            else -> requestBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = requestBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = backDescription
                    }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = stringResource(
                    when {
                        isFirstSetup -> R.string.first_timer_title
                        initialWorkout == null -> R.string.new_workout
                        else -> R.string.edit_workout
                    }
                ),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall
            )

            if (initialWorkout != null && !isFirstSetup) {
                Box {
                    IconButton(
                        onClick = {
                            showMenu = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = moreDescription
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                        },
                        modifier = Modifier.width(232.dp),
                        shape = RoundedCornerShape(20.dp),
                        containerColor =
                            MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        shadowElevation = 12.dp
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        R.string.duplicate_workout
                                    ),
                                    style =
                                        MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_duplicate),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (canSubmit) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.38f
                                        )
                                    }
                                )
                            },
                            enabled = canSubmit,
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            ),
                            onClick = {
                                showMenu = false
                                onDuplicate(draft)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            ),
                            color = MaterialTheme.colorScheme
                                .onSurface.copy(alpha = 0.08f)
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(
                                        R.string.delete_workout
                                    ),
                                    color =
                                        MaterialTheme.colorScheme.error,
                                    style =
                                        MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            ),
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = workoutName,
            onValueChange = {
                workoutName = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            label = {
                Text(
                    text = stringResource(R.string.workout_name)
                )
            },
            textStyle = MaterialTheme.typography.titleLarge,
            singleLine = true
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isMainWorkoutSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Text(
                    text = stringResource(R.string.main_timer),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = isMainWorkoutSelected,
                    enabled = !isSubmitting,
                    onCheckedChange = { checked ->
                        isMainWorkoutSelected = checked
                    }
                )
            }
        }

        TimeSettingRow(
            label = stringResource(R.string.prepare),
            seconds = prepareSeconds,
            phase = TimerPhase.PREPARE,
            minimum = 0,
            onValueChange = {
                prepareSeconds = it
            }
        )

        TimeSettingRow(
            label = stringResource(R.string.work),
            seconds = workSeconds,
            phase = TimerPhase.WORK,
            minimum = 1,
            onValueChange = {
                workSeconds = it
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.halfway_work_signal),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )

                Switch(
                    checked = halfwaySignalEnabled,
                    enabled = !isSubmitting,
                    onCheckedChange = { enabled ->
                        halfwaySignalEnabled = enabled
                    }
                )
            }
        }

        TimeSettingRow(
            label = stringResource(R.string.rest),
            seconds = restSeconds,
            phase = TimerPhase.REST,
            minimum = 0,
            onValueChange = {
                restSeconds = it
            }
        )

        CountSettingRow(
            label = stringResource(R.string.rounds),
            count = rounds,
            maximum = 30,
            presets = listOf(4, 6, 8, 10, 12, 16),
            onValueChange = {
                rounds = it
            }
        )

        CountSettingRow(
            label = stringResource(R.string.cycles),
            count = cycles,
            maximum = Int.MAX_VALUE,
            presets = listOf(1, 2, 3, 4, 5, 6),
            onValueChange = {
                cycles = it
            }
        )

        if (cycles > 1) {
            TimeSettingRow(
                label = stringResource(R.string.cycle_rest),
                seconds = cycleRestSeconds,
                phase = TimerPhase.CYCLE_REST,
                minimum = 0,
                onValueChange = {
                    cycleRestSeconds = it
                },
                presets = listOf(60, 120, 180)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.total_duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(
                    R.string.total_duration_format,
                    totalSeconds / 3600,
                    (totalSeconds % 3600) / 60,
                    totalSeconds % 60
                ),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displaySmall
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!isFirstSetup) {
                TextButton(
                    enabled = !isSubmitting,
                    onClick = requestBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Button(
                enabled = canSubmit,
                onClick = {
                    val saveWithMain = onSaveWithMain

                    if (saveWithMain != null) {
                        saveWithMain(draft, isMainWorkoutSelected)
                    } else {
                        onSave(draft)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme.colorScheme.primary,
                    contentColor =
                        MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(
                        when {
                            isFirstSetup -> R.string.save_and_continue
                            initialWorkout == null -> R.string.create_workout_action
                            else -> R.string.save
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showDeleteDialog && initialWorkout != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(
                    text = stringResource(
                        R.string.delete_workout_title
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_workout_message,
                        initialWorkout.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.delete_workout
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cancel)
                    )
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
            },
            title = {
                Text(
                    text = stringResource(
                        R.string.discard_changes_title
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.discard_changes_message
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(
                            R.string.discard_changes
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(
                            R.string.keep_editing
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun TimeSettingRow(
    label: String,
    seconds: Int,
    phase: TimerPhase,
    minimum: Int,
    onValueChange: (Int) -> Unit,
    presets: List<Int> = listOf(5, 10, 15, 20, 25, 30)
) {
    var showDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showColorDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val colorSettings = LocalPhaseColors.current
    val phaseColor = timerPhaseColor(phase)

    val colorDescription = stringResource(
        R.string.change_phase_color,
        label
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showColorDialog = true }
                    .semantics {
                        contentDescription = colorDescription
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 80.dp)
                        .background(phaseColor)
                )

                Text(
                    text = label,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 12.dp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.14f
                        ),
                        shape = CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .clickable { showDialog = true }
                    .padding(
                        horizontal = 20.dp,
                        vertical = 24.dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        R.string.duration_format,
                        seconds / 60,
                        seconds % 60
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        fontFeatureSettings = "tnum"
                    )
                )
            }
        }
    }

    if (showDialog) {
        DurationInputDialog(
            label = label,
            initialSeconds = seconds,
            minimum = minimum,
            presets = presets,
            onDismiss = { showDialog = false },
            onConfirm = { newSeconds ->
                onValueChange(newSeconds)
                showDialog = false
            }
        )
    }

    if (showColorDialog) {
        PhaseColorPickerDialog(
            label = label,
            currentColor = phaseColor,
            onSelect = { color ->
                colorSettings.onSelect(phase, color)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
}

@Composable
private fun DurationInputDialog(
    label: String,
    initialSeconds: Int,
    minimum: Int,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutesText by rememberSaveable {
        mutableStateOf((initialSeconds / 60).toString())
    }

    var secondsText by rememberSaveable {
        mutableStateOf((initialSeconds % 60).toString())
    }

    val minutes = minutesText.ifEmpty { "0" }.toIntOrNull()
    val seconds = secondsText.ifEmpty { "0" }.toIntOrNull()

    val totalSeconds = if (minutes != null && seconds != null) {
        minutes * 60 + seconds
    } else {
        null
    }

    val isValid =
        minutes != null &&
                seconds != null &&
                seconds in 0..59 &&
                totalSeconds != null &&
                totalSeconds in minimum..3600

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = stringResource(
                    R.string.duration_title,
                    label
                )
            )
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { input ->
                            if (
                                input.length <= 2 &&
                                input.all { it in '0'..'9' }
                            ) {
                                minutesText = input
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    minutesText = ""
                                }
                            },
                        label = {
                            Text(stringResource(R.string.minutes))
                        },
                        placeholder = {
                            Text("0")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { input ->
                            if (
                                input.length <= 2 &&
                                input.all { it in '0'..'9' }
                            ) {
                                secondsText = input
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    secondsText = ""
                                }
                            },
                        label = {
                            Text(stringResource(R.string.seconds))
                        },
                        placeholder = {
                            Text("0")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }

                Text(
                    text = stringResource(R.string.quick_presets),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.chunked(3).forEach { presetRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetRow.forEach { preset ->
                                FilterChip(
                                    selected = totalSeconds == preset,
                                    onClick = {
                                        minutesText = (preset / 60).toString()
                                        secondsText = (preset % 60).toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    label = {
                                        Text(
                                            text = if (preset >= 60 && preset % 60 == 0) {
                                                stringResource(
                                                    R.string.preset_minutes,
                                                    preset / 60
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.preset_seconds,
                                                    preset
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (!isValid) {
                    Text(
                        text = stringResource(
                            R.string.duration_error,
                            minimum
                        ),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },

        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(
                            alpha = 0.15f
                        ),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    enabled = isValid,
                    onClick = {
                        if (isValid) {
                            onConfirm(totalSeconds)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.done),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}

@Composable
private fun CountSettingRow(
    label: String,
    count: Int,
    maximum: Int,
    presets: List<Int>,
    onValueChange: (Int) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(
                    start = 24.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 20.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = count.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    fontFeatureSettings = "tnum"
                )
            )
        }
    }

    if (showDialog) {
        CountInputDialog(
            label = label,
            initialCount = count,
            maximum = maximum,
            presets = presets,
            onDismiss = { showDialog = false },
            onConfirm = { newCount ->
                onValueChange(newCount)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CountInputDialog(
    label: String,
    initialCount: Int,
    maximum: Int,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var countText by rememberSaveable {
        mutableStateOf(initialCount.toString())
    }

    val count = countText.toIntOrNull()
    val isValid = count != null && count in 1..maximum

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(label)
        },

        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { input ->
                        if (
                            input.isEmpty() ||
                            (
                                    input.all { it in '0'..'9' } &&
                                            input.toIntOrNull() != null
                                    )
                        ) {
                            countText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.count))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    isError = !isValid
                )

                if (!isValid) {
                    Text(
                        text = if (maximum == Int.MAX_VALUE) {
                            stringResource(R.string.positive_count_error)
                        } else {
                            stringResource(
                                R.string.count_error,
                                maximum
                            )
                        },
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = stringResource(R.string.quick_presets),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.chunked(3).forEach { presetRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            presetRow.forEach { preset ->
                                FilterChip(
                                    selected = count == preset,
                                    onClick = {
                                        countText = preset.toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = CircleShape,
                                    label = {
                                        Text(
                                            text = preset.toString(),
                                            modifier =
                                                Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.error.copy(
                                alpha = 0.15f
                            ),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    enabled = isValid,
                    onClick = {
                        if (isValid) {
                            onConfirm(count)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    )
}

@Composable
private fun EditorActionIcon(
    isDelete: Boolean,
    color: Color
) {
    Canvas(
        modifier = Modifier.size(24.dp)
    ) {
        val unit = size.width / 24f
        val stroke = Stroke(width = 1.8f * unit)

        if (isDelete) {
            drawLine(
                color = color,
                start = Offset(4f * unit, 6f * unit),
                end = Offset(20f * unit, 6f * unit),
                strokeWidth = stroke.width
            )

            drawRoundRect(
                color = color,
                topLeft = Offset(9f * unit, 3f * unit),
                size = Size(6f * unit, 3f * unit),
                cornerRadius = CornerRadius(unit, unit),
                style = stroke
            )

            drawRoundRect(
                color = color,
                topLeft = Offset(6f * unit, 6f * unit),
                size = Size(12f * unit, 15f * unit),
                cornerRadius = CornerRadius(
                    2f * unit,
                    2f * unit
                ),
                style = stroke
            )

            drawLine(
                color = color,
                start = Offset(10f * unit, 10f * unit),
                end = Offset(10f * unit, 17f * unit),
                strokeWidth = stroke.width
            )

            drawLine(
                color = color,
                start = Offset(14f * unit, 10f * unit),
                end = Offset(14f * unit, 17f * unit),
                strokeWidth = stroke.width
            )
        } else {
            drawRoundRect(
                color = color,
                topLeft = Offset(8f * unit, 8f * unit),
                size = Size(12f * unit, 13f * unit),
                cornerRadius = CornerRadius(
                    2f * unit,
                    2f * unit
                ),
                style = stroke
            )

            drawLine(
                color = color,
                start = Offset(5f * unit, 16f * unit),
                end = Offset(4f * unit, 16f * unit),
                strokeWidth = stroke.width
            )

            drawLine(
                color = color,
                start = Offset(4f * unit, 16f * unit),
                end = Offset(4f * unit, 3f * unit),
                strokeWidth = stroke.width
            )

            drawLine(
                color = color,
                start = Offset(4f * unit, 3f * unit),
                end = Offset(16f * unit, 3f * unit),
                strokeWidth = stroke.width
            )

            drawLine(
                color = color,
                start = Offset(16f * unit, 3f * unit),
                end = Offset(16f * unit, 5f * unit),
                strokeWidth = stroke.width
            )
        }
    }
}