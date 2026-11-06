package com.example.tabatatimer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun TimerApp() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    var isCreating by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var runningWorkoutJson by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var showSettings by rememberSaveable {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var loadFailed by remember {
        mutableStateOf(false)
    }

    val defaultWorkoutName = stringResource(
        R.string.classic_tabata
    )

    val saveFailedMessage = stringResource(
        R.string.save_failed
    )

    val deleteFailedMessage = stringResource(
        R.string.delete_workout_failed
    )

    val duplicateFailedMessage = stringResource(
        R.string.duplicate_workout_failed
    )

    val settingsSaveFailedMessage = stringResource(
        R.string.settings_save_failed
    )

    val workoutCopyNameFormat = stringResource(
        R.string.workout_copy_name
    )

    val workoutsFlow = remember(context, defaultWorkoutName) {
        context.workoutDataStore.data
            .map { preferences ->
                val json = preferences[WorkoutsKey]

                if (json == null) {
                    listOf(
                        Workout(
                            id = "default-classic-tabata",
                            name = defaultWorkoutName,
                            prepareSeconds = 10,
                            workSeconds = 20,
                            restSeconds = 10,
                            rounds = 8,
                            cycles = 1,
                            cycleRestSeconds = 60
                        )
                    )
                } else {
                    decodeWorkouts(json)
                }
            }
            .catch { error ->
                if (error is CancellationException) {
                    throw error
                }

                loadFailed = true
            }
    }

    val settingsFlow = remember(context) {
        observeAppSettings(context)
            .catch { error ->
                if (error is CancellationException) {
                    throw error
                }

                loadFailed = true
            }
    }

    val workouts by workoutsFlow.collectAsState(
        initial = null
    )

    val appSettings by settingsFlow.collectAsState(
        initial = null
    )

    if (loadFailed) {
        Text(
            text = stringResource(R.string.app_data_load_failed),
            modifier = Modifier
                .systemBarsPadding()
                .padding(24.dp),
            color = MaterialTheme.colorScheme.error
        )

        return
    }

    val currentWorkouts = workouts
    val currentSettings = appSettings

    if (currentWorkouts == null || currentSettings == null) {
        Text(
            text = stringResource(R.string.loading_workouts),
            modifier = Modifier
                .systemBarsPadding()
                .padding(24.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        return
    }

    fun showError(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun closeEditor() {
        isCreating = false
        editingId = null
    }

    fun persistWorkouts(
        updatedWorkouts: List<Workout>,
        errorMessage: String
    ) {
        if (isSaving) {
            return
        }

        isSaving = true

        scope.launch {
            try {
                saveWorkouts(
                    context = context,
                    workouts = updatedWorkouts
                )

                closeEditor()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showError(errorMessage)
            } finally {
                isSaving = false
            }
        }
    }

    val activeWorkout = remember(runningWorkoutJson) {
        runningWorkoutJson?.let { json ->
            decodeWorkouts(json).first()
        }
    }

    if (activeWorkout != null) {
        RunWorkoutScreen(
            workout = activeWorkout,
            soundEnabled = currentSettings.soundEnabled,
            vibrationEnabled = currentSettings.vibrationEnabled,
            onExit = {
                runningWorkoutJson = null
            }
        )

        return
    }

    if (isCreating || editingId != null) {
        CreateWorkoutScreen(
            initialWorkout = currentWorkouts.firstOrNull {
                it.id == editingId
            },
            onCancel = {
                if (!isSaving) {
                    closeEditor()
                }
            },
            onStart = { workout ->
                if (!isSaving) {
                    runningWorkoutJson = encodeWorkouts(
                        listOf(workout)
                    )

                    closeEditor()
                }
            },
            onSave = { workout ->
                val idToEdit = editingId

                val updatedWorkouts = if (idToEdit == null) {
                    currentWorkouts + workout
                } else {
                    currentWorkouts.map { existing ->
                        if (existing.id == idToEdit) {
                            workout.copy(id = idToEdit)
                        } else {
                            existing
                        }
                    }
                }

                persistWorkouts(
                    updatedWorkouts = updatedWorkouts,
                    errorMessage = saveFailedMessage
                )
            },
            onDelete = {
                val idToDelete = editingId

                if (idToDelete != null) {
                    persistWorkouts(
                        updatedWorkouts =
                            currentWorkouts.filterNot {
                                it.id == idToDelete
                            },
                        errorMessage = deleteFailedMessage
                    )
                }
            },
            onDuplicate = { workout ->
                val duplicatedWorkout = workout.copy(
                    id = UUID.randomUUID().toString(),
                    name = workoutCopyNameFormat.format(
                        workout.name
                    )
                )

                persistWorkouts(
                    updatedWorkouts =
                        currentWorkouts + duplicatedWorkout,
                    errorMessage = duplicateFailedMessage
                )
            }
        )

        return
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets =
            androidx.compose.foundation.layout.WindowInsets(
                0, 0, 0, 0
            ),
        bottomBar = {
            NavigationBar(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = !showSettings,
                    onClick = {
                        showSettings = false
                    },
                    icon = {
                        Text(text = "▶")
                    },
                    label = {
                        Text(
                            text = stringResource(
                                R.string.tab_timers
                            )
                        )
                    }
                )

                NavigationBarItem(
                    selected = showSettings,
                    onClick = {
                        showSettings = true
                    },
                    icon = {
                        Text(text = "⚙")
                    },
                    label = {
                        Text(
                            text = stringResource(
                                R.string.tab_settings
                            )
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (showSettings) {
                SettingsScreen(
                    selectedLanguage = currentAppLanguage(),
                    onLanguageChange = { languageTag ->
                        changeAppLanguage(languageTag)
                    },
                    soundEnabled = currentSettings.soundEnabled,
                    vibrationEnabled =
                        currentSettings.vibrationEnabled,
                    onSoundChange = { enabled ->
                        scope.launch {
                            try {
                                saveSoundEnabled(
                                    context = context,
                                    enabled = enabled
                                )
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                showError(settingsSaveFailedMessage)
                            }
                        }
                    },
                    onVibrationChange = { enabled ->
                        scope.launch {
                            try {
                                saveVibrationEnabled(
                                    context = context,
                                    enabled = enabled
                                )
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                showError(settingsSaveFailedMessage)
                            }
                        }
                    },
                    onBack = {
                        showSettings = false
                    }
                )
            } else {
                HomeScreen(
                    workouts = currentWorkouts,
                    onCreate = {
                        editingId = null
                        isCreating = true
                    },
                    onEdit = { workoutId ->
                        isCreating = false
                        editingId = workoutId
                    },
                    onStart = { workout ->
                        runningWorkoutJson = encodeWorkouts(
                            listOf(workout)
                        )
                    }
                )
            }
        }
    }
}