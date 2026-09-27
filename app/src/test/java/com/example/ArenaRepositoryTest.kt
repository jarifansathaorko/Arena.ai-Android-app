package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ArenaDatabase
import com.example.data.model.BattleWinner
import com.example.data.repository.ArenaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaRepositoryTest {

    private lateinit var database: ArenaDatabase
    private lateinit var repository: ArenaRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ArenaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ArenaRepository(database.promptDao(), database.battleDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun defaultPromptsAreInitialized() = runBlocking {
        repository.initializeDefaultPromptsIfNeeded()
        val prompts = repository.allPrompts.first()
        assertTrue("Prompts should be pre-populated", prompts.isNotEmpty())
        assertTrue("Should contain strawberry test prompt", prompts.any { it.title.contains("Strawberry") })
    }

    @Test
    fun addCustomPromptAndToggleFavorite() = runBlocking {
        val id = repository.addPrompt("Test Title", "Coding", "Test Content")
        assertTrue(id > 0)

        var prompts = repository.allPrompts.first()
        val added = prompts.first { it.title == "Test Title" }
        assertFalse(added.isFavorite)

        repository.toggleFavorite(added.id, added.isFavorite)
        prompts = repository.allPrompts.first()
        val updated = prompts.first { it.id == added.id }
        assertTrue(updated.isFavorite)
    }

    @Test
    fun logBattleAndCheckRecord() = runBlocking {
        val id = repository.logBattle(
            modelA = "Claude 3.5 Sonnet",
            modelB = "GPT-4o",
            winner = BattleWinner.MODEL_A,
            promptTopic = "Python Async Lock",
            category = "Coding",
            notes = "Claude gave cleaner type hints"
        )
        assertTrue(id > 0)

        val battles = repository.allBattles.first()
        assertEquals(1, battles.size)
        assertEquals("Claude 3.5 Sonnet", battles[0].modelA)
        assertEquals(BattleWinner.MODEL_A, battles[0].winner)
    }
}
