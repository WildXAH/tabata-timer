package com.example.tabatatimer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val DefaultPhaseColors = mapOf(
    TimerPhase.PREPARE to Color(0xFF2F7FE0),
    TimerPhase.WORK to Color(0xFF2FBF5A),
    TimerPhase.REST to Color(0xFFF59A32),
    TimerPhase.CYCLE_REST to Color(0xFF9B4FD6)
)

internal data class PhaseColorSettings(
    val colors: Map<TimerPhase, Color>,
    val onSelect: (TimerPhase, Color) -> Unit
)

internal val LocalPhaseColors = compositionLocalOf {
    PhaseColorSettings(
        colors = DefaultPhaseColors,
        onSelect = { _, _ -> }
    )
}

private fun phaseColorKey(
    phase: TimerPhase
) = intPreferencesKey("phase_color_${phase.name}")

@Composable
internal fun PhaseColorsProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    var loadFailed by remember {
        mutableStateOf(false)
    }

    val saveError = stringResource(
        R.string.color_save_failed
    )

    val unavailable = stringResource(
        R.string.colors_unavailable
    )

    val colorsFlow = remember(context) {
        context.workoutDataStore.data
            .map { preferences ->
                DefaultPhaseColors.mapValues { (phase, fallback) ->
                    val stored = preferences[phaseColorKey(phase)]

                    if (stored == null) {
                        fallback
                    } else {
                        Color(stored)
                    }
                }
            }
            .catch { error ->
                if (error is CancellationException) {
                    throw error
                }

                loadFailed = true
                emit(DefaultPhaseColors)
            }
    }

    val storedColors by colorsFlow.collectAsState(
        initial = null
    )

    val settings = PhaseColorSettings(
        colors = storedColors ?: DefaultPhaseColors,
        onSelect = { phase, color ->
            if (storedColors == null || loadFailed) {
                Toast.makeText(
                    context,
                    unavailable,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                scope.launch {
                    try {
                        context.workoutDataStore.edit { preferences ->
                            preferences[phaseColorKey(phase)] =
                                color.toArgb()
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            saveError,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    )

    CompositionLocalProvider(
        LocalPhaseColors provides settings,
        content = content
    )
}

@Composable
internal fun timerPhaseColor(
    phase: TimerPhase
): Color {
    return if (phase == TimerPhase.FINISHED) {
        MaterialTheme.colorScheme.background
    } else {
        LocalPhaseColors.current.colors.getValue(phase)
    }
}

@Composable
internal fun timerPhaseLabel(
    phase: TimerPhase
): String {
    return stringResource(
        when (phase) {
            TimerPhase.PREPARE -> R.string.prepare
            TimerPhase.WORK -> R.string.work
            TimerPhase.REST -> R.string.rest
            TimerPhase.CYCLE_REST -> R.string.cycle_rest
            TimerPhase.FINISHED -> R.string.workout_finished
        }
    )
}

@Composable
internal fun PhaseColorPickerDialog(
    label: String,
    currentColor: Color,
    onSelect: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = listOf(
        Color(0xFF2F7FE0),
        Color(0xFF22C3E6),
        Color(0xFF2FBF5A),
        Color(0xFFF5D02F),
        Color(0xFFF59A32),
        Color(0xFFEF4F4F),
        Color(0xFFEC5FA8),
        Color(0xFF9B4FD6)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.phase_color_title,
                    label
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                palette.chunked(4).forEachIndexed { rowIndex, colors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceEvenly
                    ) {
                        colors.forEachIndexed { columnIndex, color ->
                            val isSelected = color == currentColor

                            val description = stringResource(
                                R.string.color_option,
                                rowIndex * 4 + columnIndex + 1
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme
                                                .onSurface
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onSelect(color)
                                    }
                                    .semantics {
                                        contentDescription = description
                                        selected = isSelected
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = Color(0xFF101318),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.close)
                )
            }
        }
    )
}