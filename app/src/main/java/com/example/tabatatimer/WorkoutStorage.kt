package com.example.tabatatimer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal val Context.workoutDataStore by preferencesDataStore(
    name = "workouts"
)

internal val WorkoutsKey = stringPreferencesKey("workout_list")

internal fun encodeWorkouts(
    workouts: List<Workout>
): String {
    val array = JSONArray()

    workouts.forEach { workout ->
        array.put(
            JSONObject().apply {
                put("id", workout.id)
                put("name", workout.name)
                put("prepareSeconds", workout.prepareSeconds)
                put("workSeconds", workout.workSeconds)
                put("restSeconds", workout.restSeconds)
                put("rounds", workout.rounds)
                put("cycles", workout.cycles)
                put("cycleRestSeconds", workout.cycleRestSeconds)
            }
        )
    }

    return array.toString()
}

internal fun decodeWorkouts(
    json: String
): List<Workout> {
    val array = JSONArray(json)

    return List(array.length()) { index ->
        val item = array.getJSONObject(index)

        val storedId = item.optString("id", "")

        val workoutId = storedId.ifBlank {
            UUID.nameUUIDFromBytes(
                "legacy-workout-$index".toByteArray(Charsets.UTF_8)
            ).toString()
        }

        Workout(
            id = workoutId,
            name = item.getString("name"),
            prepareSeconds = item.getInt("prepareSeconds"),
            workSeconds = item.getInt("workSeconds"),
            restSeconds = item.getInt("restSeconds"),
            rounds = item.getInt("rounds"),
            cycles = item.getInt("cycles"),
            cycleRestSeconds = item.getInt("cycleRestSeconds")
        )
    }
}

internal suspend fun saveWorkouts(
    context: Context,
    workouts: List<Workout>
) {
    val json = encodeWorkouts(workouts)

    context.workoutDataStore.edit { preferences ->
        preferences[WorkoutsKey] = json
    }
}