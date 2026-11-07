package com.example.tabatatimer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal val InitialSetupCompletedKey =
    booleanPreferencesKey("initial_setup_completed")

internal val MainWorkoutIdKey =
    stringPreferencesKey("main_workout_id")

internal data class StartupWorkouts(
    val workouts: List<Workout>,
    val setupCompleted: Boolean,
    val mainWorkoutId: String?
)

internal suspend fun saveFirstWorkout(
    context: Context,
    workout: Workout
) {
    context.workoutDataStore.edit { preferences ->
        preferences[WorkoutsKey] = encodeWorkouts(listOf(workout))
        preferences[MainWorkoutIdKey] = workout.id
        preferences[InitialSetupCompletedKey] = true
    }
}

@Composable
internal fun FirstRunSetupScreen(
    context: Context,
    onCancel: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var isSaving by remember {
        mutableStateOf(false)
    }

    val defaultName = stringResource(R.string.morning_tabata)
    val saveFailedMessage = stringResource(R.string.save_failed)

    val initialName = rememberSaveable {
        defaultName
    }

    val initialWorkout = remember(initialName) {
        Workout(
            id = "first-morning-tabata",
            name = initialName,
            prepareSeconds = 10,
            workSeconds = 30,
            restSeconds = 5,
            rounds = 8,
            cycles = 1,
            cycleRestSeconds = 60
        )
    }

    CreateWorkoutScreen(
        initialWorkout = initialWorkout,
        isFirstSetup = true,
        isSubmitting = isSaving,
        onCancel = {
            if (!isSaving) {
                onCancel()
            }
        },
        onSave = { workout ->
            if (!isSaving) {
                isSaving = true

                scope.launch {
                    try {
                        saveFirstWorkout(
                            context = context,
                            workout = workout
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        onError(saveFailedMessage)
                    } finally {
                        isSaving = false
                    }
                }
            }
        },
        onStart = {},
        onDelete = {},
        onDuplicate = {}
    )
}