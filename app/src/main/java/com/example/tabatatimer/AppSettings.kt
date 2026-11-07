package com.example.tabatatimer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class AppSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val mainWorkoutId: String? = null
)

private val SoundEnabledKey = booleanPreferencesKey(
    "settings_sound_enabled"
)

private val VibrationEnabledKey = booleanPreferencesKey(
    "settings_vibration_enabled"
)

internal fun observeAppSettings(
    context: Context
): Flow<AppSettings> {
    return context.applicationContext.workoutDataStore.data
        .map { preferences ->
            AppSettings(
                soundEnabled =
                    preferences[SoundEnabledKey] ?: true,
                vibrationEnabled =
                    preferences[VibrationEnabledKey] ?: false,
                mainWorkoutId =
                    preferences[MainWorkoutIdKey]
            )
        }
        .distinctUntilChanged()
}

internal suspend fun saveSoundEnabled(
    context: Context,
    enabled: Boolean
) {
    context.applicationContext.workoutDataStore.edit { preferences ->
        preferences[SoundEnabledKey] = enabled
    }
}

internal suspend fun saveVibrationEnabled(
    context: Context,
    enabled: Boolean
) {
    context.applicationContext.workoutDataStore.edit { preferences ->
        preferences[VibrationEnabledKey] = enabled
    }
}

internal suspend fun saveMainWorkoutId(
    context: Context,
    workoutId: String?
) {
    context.applicationContext.workoutDataStore.edit { preferences ->
        if (workoutId == null) {
            preferences.remove(MainWorkoutIdKey)
        } else {
            preferences[MainWorkoutIdKey] = workoutId
        }
    }
}