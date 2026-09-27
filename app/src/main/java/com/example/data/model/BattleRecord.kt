package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battle_records")
data class BattleRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelA: String,
    val modelB: String,
    val winner: BattleWinner, // MODEL_A, MODEL_B, TIE, BOTH_BAD
    val promptTopic: String = "",
    val category: String = "General",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class BattleWinner {
    MODEL_A,
    MODEL_B,
    TIE,
    BOTH_BAD
}
