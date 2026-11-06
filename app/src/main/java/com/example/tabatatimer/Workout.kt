package com.example.tabatatimer

import java.util.UUID

data class Workout(
    val name: String,
    val prepareSeconds: Int,
    val workSeconds: Int,
    val restSeconds: Int,
    val rounds: Int,
    val cycles: Int,
    val cycleRestSeconds: Int,
    val id: String = UUID.randomUUID().toString()
)