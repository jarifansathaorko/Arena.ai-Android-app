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

    @Test
    fun updatePrompt_modifiesExistingRecord() = runBlocking {
        val id = repository.addPrompt("Original Title", "Writing", "Original Content")
        repository.updatePrompt(id, "Updated Title", "Coding", "Updated Content")

        val prompts = repository.allPrompts.first()
        val updated = prompts.first { it.id == id }
        assertEquals("Updated Title", updated.title)
        assertEquals("Coding", updated.category)
        assertEquals("Updated Content", updated.content)
    }

    @Test
    fun duplicatePrompt_createsCopy() = runBlocking {
        val id = repository.addPrompt("Unique Prompt", "Logic", "Calculate 2+2")
        val original = repository.allPrompts.first().first { it.id == id }

        val copyId = repository.duplicatePrompt(original)
        assertTrue(copyId > 0)

        val prompts = repository.allPrompts.first()
        assertEquals(2, prompts.size)
        assertTrue(prompts.any { it.title == "Unique Prompt (Copy)" && it.category == "Logic" })
    }

    @Test
    fun filterBattles_filtersByCategoryAndWinner() = runBlocking {
        repository.logBattle("GPT-4", "Claude-3", BattleWinner.MODEL_A, "Python", "Coding", "Fast")
        repository.logBattle("Gemini", "Llama", BattleWinner.MODEL_B, "Poem", "Creative", "Good rhythm")

        val all = repository.allBattles.first()
        val codingBattles = ArenaRepository.filterBattles(all, category = "Coding")
        assertEquals(1, codingBattles.size)
        assertEquals("GPT-4", codingBattles[0].modelA)

        val winnerBBattles = ArenaRepository.filterBattles(all, winnerFilter = BattleWinner.MODEL_B)
        assertEquals(1, winnerBBattles.size)
        assertEquals("Gemini", winnerBBattles[0].modelA)

        val noMatch = ArenaRepository.filterBattles(all, category = "Math")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun deletePrompt_removesRecord() = runBlocking {
        val id = repository.addPrompt("To Delete", "General", "Remove me")
        var prompts = repository.allPrompts.first()
        assertEquals(1, prompts.size)

        repository.deletePrompt(prompts.first())
        prompts = repository.allPrompts.first()
        assertTrue(prompts.isEmpty())
    }
}
