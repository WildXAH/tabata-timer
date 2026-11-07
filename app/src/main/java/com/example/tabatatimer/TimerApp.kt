package com.example.tabatatimer

import androidx.compose.runtime.SideEffect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
internal fun TimerApp(
    onStartupReady: () -> Unit = {},
    onCloseApp: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()

    var selectedWorkoutJson by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var editorOpenedFromTimer by rememberSaveable {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var loadFailed by remember {
        mutableStateOf(false)
    }

    val saveFailedMessage =
        stringResource(R.string.save_failed)

    val deleteFailedMessage =
        stringResource(R.string.delete_workout_failed)

    val duplicateFailedMessage =
        stringResource(R.string.duplicate_workout_failed)

    val settingsSaveFailedMessage =
        stringResource(R.string.settings_save_failed)

    val workoutCopyNameFormat =
        stringResource(R.string.workout_copy_name)

    val workoutsFlow = remember(context) {
        context.workoutDataStore.data
            .map { preferences ->
                val json = preferences[WorkoutsKey]

                val savedWorkouts = if (json != null) {
                    decodeWorkouts(json)
                } else {
                    emptyList()
                }

                val setupCompleted =
                    preferences[InitialSetupCompletedKey]
                        ?: (json != null)

                StartupWorkouts(
                    workouts = savedWorkouts,
                    setupCompleted = setupCompleted,
                    mainWorkoutId = preferences[MainWorkoutIdKey]
                )
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

    val workouts by workoutsFlow.collectAsState(initial = null)
    val appSettings by settingsFlow.collectAsState(initial = null)

    val startupData = workouts

    val startupReady =
        loadFailed ||
                (startupData != null && appSettings != null)

    SideEffect {
        if (startupReady) {
            onStartupReady()
        }
    }

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

    val currentSettings = appSettings

    if (startupData == null || currentSettings == null) {
        return
    }

    val currentWorkouts = startupData.workouts

    if (!startupData.setupCompleted) {
        FirstRunSetupScreen(
            context = context,
            onCancel = onCloseApp,
            onError = { message ->
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        return
    }

    val navController = rememberNavController()

    val selectedWorkout = remember(selectedWorkoutJson) {
        selectedWorkoutJson?.let { json ->
            decodeWorkouts(json).firstOrNull()
        }
    }

    val mainWorkout = currentWorkouts.firstOrNull {
        it.id == startupData.mainWorkoutId
    } ?: currentWorkouts.firstOrNull()

    val activeWorkout =
        selectedWorkout ?: mainWorkout

    fun showError(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun openTimer(workout: Workout) {
        selectedWorkoutJson = encodeWorkouts(listOf(workout))
        editingId = null
        editorOpenedFromTimer = false

        navController.navigate(AppRoutes.TIMER) {
            popUpTo(navController.graph.id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    fun openLibraryAsRoot() {
        editingId = null
        editorOpenedFromTimer = false

        navController.navigate(AppRoutes.LIBRARY) {
            popUpTo(navController.graph.id) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    fun openEditor(
        workoutId: String?,
        fromTimer: Boolean
    ) {
        editingId = workoutId
        editorOpenedFromTimer = fromTimer

        navController.navigate(AppRoutes.EDITOR) {
            launchSingleTop = true
        }
    }

    fun cancelEditor() {
        if (isSaving) {
            return
        }

        if (editorOpenedFromTimer) {
            val savedWorkout = currentWorkouts.firstOrNull {
                it.id == editingId
            }

            if (savedWorkout != null) {
                openTimer(savedWorkout)
            } else {
                openLibraryAsRoot()
            }
        } else {
            editingId = null
            editorOpenedFromTimer = false

            if (!navController.popBackStack()) {
                openLibraryAsRoot()
            }
        }
    }

    fun persistWorkouts(
        updatedWorkouts: List<Workout>,
        errorMessage: String,
        onSuccess: () -> Unit
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

                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showError(errorMessage)
            } finally {
                isSaving = false
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (
            currentSettings.mainWorkoutId != null &&
            currentWorkouts.any { workout ->
                workout.id == currentSettings.mainWorkoutId
            }
        ) {
            AppRoutes.TIMER
        } else {
            AppRoutes.LIBRARY
        }
    ) {
        composable(AppRoutes.TIMER) {
            if (activeWorkout != null) {
                RunWorkoutScreen(
                    workout = activeWorkout,
                    soundEnabled = currentSettings.soundEnabled,
                    vibrationEnabled =
                        currentSettings.vibrationEnabled,
                    onExit = {
                        openLibraryAsRoot()
                    },
                    onEdit = {
                        openEditor(
                            workoutId = activeWorkout.id,
                            fromTimer = true
                        )
                    }
                )
            } else {
                TimerLibraryScreen(
                    workouts = currentWorkouts,
                    onCreate = {
                        openEditor(
                            workoutId = null,
                            fromTimer = false
                        )
                    },
                    onEdit = { workoutId ->
                        openEditor(
                            workoutId = workoutId,
                            fromTimer = false
                        )
                    },
                    onStart = { workout ->
                        openTimer(workout)
                    },
                    onSettings = {
                        navController.navigate(AppRoutes.SETTINGS)
                    }
                )
            }
        }

        composable(AppRoutes.LIBRARY) {
            BackHandler(enabled = activeWorkout != null) {
                activeWorkout?.let { workout ->
                    val savedWorkout = currentWorkouts.firstOrNull {
                        it.id == workout.id
                    } ?: currentWorkouts.firstOrNull()

                    if (savedWorkout != null) {
                        openTimer(savedWorkout)
                    }
                }
            }

            TimerLibraryScreen(
                workouts = currentWorkouts,
                onCreate = {
                    openEditor(
                        workoutId = null,
                        fromTimer = false
                    )
                },
                onEdit = { workoutId ->
                    openEditor(
                        workoutId = workoutId,
                        fromTimer = false
                    )
                },
                onStart = { workout ->
                    openTimer(workout)
                },
                onSettings = {
                    navController.navigate(AppRoutes.SETTINGS) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppRoutes.EDITOR) {
            CreateWorkoutScreen(
                initialWorkout = currentWorkouts.firstOrNull {
                    it.id == editingId
                },
                isMainWorkout = currentSettings.mainWorkoutId ==
                        currentWorkouts.firstOrNull {
                            it.id == editingId
                        }?.id,
                onMainWorkoutChange = { workout, isMain ->
                    scope.launch {
                        try {
                            saveMainWorkoutId(
                                context = context,
                                workoutId = if (isMain) {
                                    workout.id
                                } else {
                                    null
                                }
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            showError(settingsSaveFailedMessage)
                        }
                    }
                },
                onCancel = {
                    cancelEditor()
                },
                onStart = { workout ->
                    if (!isSaving) {
                        openTimer(workout)
                    }
                },
                onSave = { workout ->
                    val idToEdit = editingId

                    val savedWorkout = if (idToEdit != null) {
                        workout.copy(id = idToEdit)
                    } else {
                        workout
                    }

                    val updatedWorkouts = if (idToEdit == null) {
                        currentWorkouts + savedWorkout
                    } else {
                        currentWorkouts.map { existing ->
                            if (existing.id == idToEdit) {
                                savedWorkout
                            } else {
                                existing
                            }
                        }
                    }

                    persistWorkouts(
                        updatedWorkouts = updatedWorkouts,
                        errorMessage = saveFailedMessage,
                        onSuccess = {
                            openTimer(savedWorkout)
                        }
                    )
                },
                onDelete = {
                    val idToDelete = editingId

                    if (idToDelete != null) {
                        val updatedWorkouts =
                            currentWorkouts.filterNot {
                                it.id == idToDelete
                            }

                        persistWorkouts(
                            updatedWorkouts = updatedWorkouts,
                            errorMessage = deleteFailedMessage,
                            onSuccess = {
                                if (activeWorkout?.id == idToDelete) {
                                    selectedWorkoutJson = null
                                }

                                openLibraryAsRoot()
                            }
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
                        errorMessage = duplicateFailedMessage,
                        onSuccess = {
                            openLibraryAsRoot()
                        }
                    )
                }
            )
        }

        composable(AppRoutes.SETTINGS) {
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
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun TimerLibraryScreen(
    workouts: List<Workout>,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onStart: (Workout) -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Text(text = "▦")
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_timers)
                        )
                    }
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onSettings,
                    icon = {
                        Text(text = "⚙")
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_settings)
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            HomeScreen(
                workouts = workouts,
                onCreate = onCreate,
                onEdit = onEdit,
                onStart = onStart
            )
        }
    }
}