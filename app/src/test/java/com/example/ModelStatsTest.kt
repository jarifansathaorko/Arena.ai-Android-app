package com.example

import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.data.repository.ArenaRepository
import com.example.data.repository.ModelStats
import org.junit.Assert.*
import org.junit.Test

class ModelStatsTest {

    private fun battle(
        a: String,
        b: String,
        winner: BattleWinner,
        timestamp: Long = 0L
    ) = BattleRecord(
        id = 0, modelA = a, modelB = b, winner = winner,
        promptTopic = "", category = "General", notes = "", timestamp = timestamp
    )

    @Test
    fun emptyBattles_yieldEmptyStats() {
        assertTrue(ArenaRepository.computeModelStats(emptyList()).isEmpty())
    }

    @Test
    fun singleWin_countsForWinnerOnly() {
        val stats = ArenaRepository.computeModelStats(
            listOf(battle("Claude", "GPT", BattleWinner.MODEL_A))
        )
        val byName = stats.associateBy { it.modelName }
        assertEquals(2, stats.size)
        assertEquals(ModelStats("Claude", wins = 1, battles = 1, winRate = 100f), byName["Claude"])
        assertEquals(ModelStats("GPT", wins = 0, battles = 1, winRate = 0f), byName["GPT"])
    }

    @Test
    fun tieAndBothBad_countAsParticipationWithoutWins() {
        val stats = ArenaRepository.computeModelStats(
            listOf(
                battle("A", "B", BattleWinner.TIE),
                battle("A", "B", BattleWinner.BOTH_BAD)
            )
        )
        val byName = stats.associateBy { it.modelName }
        assertEquals(0, byName["A"]!!.wins)
        assertEquals(2, byName["A"]!!.battles)
        assertEquals(0f, byName["A"]!!.winRate)
        assertEquals(0, byName["B"]!!.wins)
        assertEquals(2, byName["B"]!!.battles)
    }

    @Test
    fun stats_sortedByWinRateDescending() {
        val stats = ArenaRepository.computeModelStats(
            listOf(
                battle("Loser", "Mid", BattleWinner.MODEL_B), // Mid beats Loser
                battle("Winner", "Loser", BattleWinner.MODEL_A), // Winner beats Loser
                battle("Winner", "Mid", BattleWinner.MODEL_A) // Winner beats Mid
            )
        )
        assertEquals(listOf("Winner", "Mid", "Loser"), stats.map { it.modelName })
        assertEquals(2, stats[0].wins)
        assertEquals(100f, stats[0].winRate)
    }

    @Test
    fun blankModelNames_areSkipped() {
        val stats = ArenaRepository.computeModelStats(
            listOf(battle("  ", "GPT", BattleWinner.MODEL_B))
        )
        assertTrue(stats.isEmpty())
    }

    @Test
    fun namesAreTrimmedBeforeAggregation() {
        val stats = ArenaRepository.computeModelStats(
            listOf(
                battle("Claude ", "GPT", BattleWinner.MODEL_A),
                battle(" Claude", "GPT", BattleWinner.MODEL_B)
            )
        )
        val claude = stats.first { it.modelName == "Claude" }
        assertEquals(1, claude.wins)
        assertEquals(2, claude.battles)
        assertEquals(50f, claude.winRate)
    }
}
