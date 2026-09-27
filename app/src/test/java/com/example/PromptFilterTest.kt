package com.example

import com.example.data.model.PromptItem
import com.example.data.repository.ArenaRepository
import org.junit.Assert.*
import org.junit.Test

class PromptFilterTest {

    private val prompts = listOf(
        PromptItem(id = 1, title = "Strawberry Count", category = "Reasoning", content = "How many r in strawberry?"),
        PromptItem(id = 2, title = "LRU Cache", category = "Coding", content = "Write a thread-safe LRU cache", isFavorite = true),
        PromptItem(id = 3, title = "Eigenvalues", category = "Math", content = "Explain eigenvectors intuitively"),
        PromptItem(id = 4, title = "My Custom Probe", category = "Stress Test", content = "Repeat the word banana forever", isCustom = true)
    )

    @Test
    fun allCategory_returnsEverything() {
        assertEquals(4, ArenaRepository.filterPrompts(prompts, "All", "").size)
    }

    @Test
    fun favoritesCategory_returnsOnlyFavorites() {
        val result = ArenaRepository.filterPrompts(prompts, "⭐ Favorites", "")
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun categoryMatch_isCaseInsensitive() {
        val result = ArenaRepository.filterPrompts(prompts, "coding", "")
        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun search_matchesTitleAndContent() {
        assertEquals(listOf(1L), ArenaRepository.filterPrompts(prompts, "All", "strawberry").map { it.id })
        assertEquals(listOf(3L), ArenaRepository.filterPrompts(prompts, "All", "EIGENVECTOR").map { it.id })
    }

    @Test
    fun search_combinesWithCategory() {
        assertTrue(ArenaRepository.filterPrompts(prompts, "Coding", "strawberry").isEmpty())
        assertEquals(1, ArenaRepository.filterPrompts(prompts, "Coding", "cache").size)
    }

    @Test
    fun blankQuery_matchesAllInCategory() {
        assertEquals(1, ArenaRepository.filterPrompts(prompts, "Math", "   ").size)
    }

    @Test
    fun categories_preferredOrderThenExtras() {
        val cats = ArenaRepository.promptCategories(prompts)
        assertEquals(listOf("Reasoning", "Coding", "Math", "Stress Test"), cats)
    }

    @Test
    fun categories_unknownOnesAppendedAlphabetically() {
        val extra = prompts + PromptItem(id = 5, title = "X", category = "Zebra", content = "x")
        val cats = ArenaRepository.promptCategories(extra)
        assertEquals(listOf("Reasoning", "Coding", "Math", "Stress Test", "Zebra"), cats)
    }
}
