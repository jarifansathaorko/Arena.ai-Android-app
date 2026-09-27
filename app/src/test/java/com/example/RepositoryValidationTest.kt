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

/**
 * End-to-end validation of repository input contracts against a real
 * (in-memory) Room database: blank/duplicate inputs are rejected and
 * stored values are trimmed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryValidationTest {

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
    fun addPrompt_rejectsBlankTitleOrContent() = runBlocking {
        try {
            repository.addPrompt("  ", "Coding", "content")
            fail("Expected IllegalArgumentException for blank title")
        } catch (_: IllegalArgumentException) { }

        try {
            repository.addPrompt("Title", "Coding", "   ")
            fail("Expected IllegalArgumentException for blank content")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun addPrompt_trimsAndDefaultsBlankCategory() = runBlocking {
        repository.addPrompt("  Padded Title  ", "   ", "  padded content  ")
        val stored = repository.allPrompts.first().first { it.title == "Padded Title" }
        assertEquals("Padded Title", stored.title)
        assertEquals("padded content", stored.content)
        assertEquals("General", stored.category)
    }

    @Test
    fun logBattle_rejectsBlankOrDuplicateModels() = runBlocking {
        try {
            repository.logBattle(" ", "GPT", BattleWinner.MODEL_A, "", "Coding", "")
            fail("Expected IllegalArgumentException for blank model")
        } catch (_: IllegalArgumentException) { }

        try {
            repository.logBattle("Claude", " claude ", BattleWinner.TIE, "", "Coding", "")
            fail("Expected IllegalArgumentException for identical models")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun logBattle_trimsFields() = runBlocking {
        repository.logBattle("  Claude  ", "GPT-4o ", BattleWinner.MODEL_A, " topic ", "Coding", " note ")
        val stored = repository.allBattles.first().single()
        assertEquals("Claude", stored.modelA)
        assertEquals("GPT-4o", stored.modelB)
        assertEquals("topic", stored.promptTopic)
        assertEquals("note", stored.notes)
    }

    @Test
    fun deleteAndClear_roundTrip() = runBlocking {
        repository.logBattle("A", "B", BattleWinner.TIE, "", "General", "")
        repository.logBattle("C", "D", BattleWinner.MODEL_B, "", "General", "")
        assertEquals(2, repository.allBattles.first().size)

        val first = repository.allBattles.first().first()
        repository.deleteBattle(first)
        assertEquals(1, repository.allBattles.first().size)

        repository.clearBattles()
        assertTrue(repository.allBattles.first().isEmpty())
    }
}
