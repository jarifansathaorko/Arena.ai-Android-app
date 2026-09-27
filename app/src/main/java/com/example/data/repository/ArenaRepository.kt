package com.example.data.repository

import com.example.data.local.BattleDao
import com.example.data.local.PromptDao
import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.data.model.PromptItem
import kotlinx.coroutines.flow.Flow

data class ModelStats(
    val modelName: String,
    val wins: Int,
    val battles: Int,
    val winRate: Float
)

class ArenaRepository(
    private val promptDao: PromptDao,
    private val battleDao: BattleDao
) {
    val allPrompts: Flow<List<PromptItem>> = promptDao.getAllPrompts()
    val allBattles: Flow<List<BattleRecord>> = battleDao.getAllBattles()

    suspend fun initializeDefaultPromptsIfNeeded() {
        if (promptDao.getCount() == 0) {
            promptDao.insertAll(DEFAULT_PROMPTS)
        }
    }

    suspend fun addPrompt(title: String, category: String, content: String): Long {
        require(title.isNotBlank()) { "Prompt title must not be blank" }
        require(content.isNotBlank()) { "Prompt content must not be blank" }
        return promptDao.insertPrompt(
            PromptItem(
                title = title.trim(),
                category = category.trim().ifBlank { "General" },
                content = content.trim(),
                isCustom = true
            )
        )
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        promptDao.updateFavorite(id, !currentFavorite)
    }

    suspend fun updatePrompt(id: Long, title: String, category: String, content: String) {
        require(title.isNotBlank()) { "Prompt title must not be blank" }
        require(content.isNotBlank()) { "Prompt content must not be blank" }
        val existing = promptDao.getPromptById(id) ?: return
        promptDao.updatePrompt(
            existing.copy(
                title = title.trim(),
                category = category.trim().ifBlank { "General" },
                content = content.trim()
            )
        )
    }

    suspend fun duplicatePrompt(prompt: PromptItem): Long {
        return promptDao.insertPrompt(
            PromptItem(
                title = "${prompt.title} (Copy)",
                category = prompt.category,
                content = prompt.content,
                isCustom = true,
                isFavorite = false
            )
        )
    }

    suspend fun deletePrompt(prompt: PromptItem) {
        promptDao.deletePrompt(prompt)
    }

    suspend fun logBattle(
        modelA: String,
        modelB: String,
        winner: BattleWinner,
        promptTopic: String,
        category: String,
        notes: String
    ): Long {
        require(modelA.isNotBlank()) { "Model A must not be blank" }
        require(modelB.isNotBlank()) { "Model B must not be blank" }
        require(!modelA.trim().equals(modelB.trim(), ignoreCase = true)) {
            "Model A and Model B must be different"
        }
        return battleDao.insertBattle(
            BattleRecord(
                modelA = modelA.trim(),
                modelB = modelB.trim(),
                winner = winner,
                promptTopic = promptTopic.trim(),
                category = category.trim().ifBlank { "General" },
                notes = notes.trim()
            )
        )
    }

    suspend fun deleteBattle(battle: BattleRecord) {
        battleDao.deleteBattle(battle)
    }

    suspend fun clearBattles() {
        battleDao.clearAllBattles()
    }

    companion object {
        fun filterBattles(
            battles: List<BattleRecord>,
            query: String = "",
            category: String = "All",
            winnerFilter: BattleWinner? = null
        ): List<BattleRecord> {
            val q = query.trim()
            return battles.filter { battle ->
                val matchesCategory = category == "All" || battle.category.equals(category, ignoreCase = true)
                val matchesWinner = winnerFilter == null || battle.winner == winnerFilter
                val matchesQuery = q.isEmpty() ||
                        battle.modelA.contains(q, ignoreCase = true) ||
                        battle.modelB.contains(q, ignoreCase = true) ||
                        battle.promptTopic.contains(q, ignoreCase = true) ||
                        battle.notes.contains(q, ignoreCase = true)
                matchesCategory && matchesWinner && matchesQuery
            }
        }

        fun battleCategories(battles: List<BattleRecord>): List<String> {
            val preferred = listOf("Reasoning", "Coding", "Math", "Creative", "Factuality", "General")
            val present = battles.map { it.category }.distinct()
            return preferred.filter { it in present } + (present - preferred.toSet()).sorted()
        }
        /**
         * Pure, testable aggregation of per-model win stats.
         * Ties / "both bad" count as participations but not wins for either side.
         * Empty or blank model names are skipped.
         */
        fun computeModelStats(battles: List<BattleRecord>): List<ModelStats> {
            val wins = mutableMapOf<String, Int>()
            val totals = mutableMapOf<String, Int>()
            for (b in battles) {
                val a = b.modelA.trim()
                val other = b.modelB.trim()
                if (a.isEmpty() || other.isEmpty()) continue
                totals[a] = (totals[a] ?: 0) + 1
                totals[other] = (totals[other] ?: 0) + 1
                when (b.winner) {
                    BattleWinner.MODEL_A -> wins[a] = (wins[a] ?: 0) + 1
                    BattleWinner.MODEL_B -> wins[other] = (wins[other] ?: 0) + 1
                    else -> Unit
                }
            }
            return totals.map { (name, total) ->
                val winCount = wins[name] ?: 0
                ModelStats(
                    modelName = name,
                    wins = winCount,
                    battles = total,
                    winRate = if (total > 0) winCount.toFloat() / total.toFloat() * 100f else 0f
                )
            }.sortedByDescending { it.winRate }
        }

        /**
         * Pure, testable prompt filtering used by the library sheet.
         */
        fun filterPrompts(
            prompts: List<PromptItem>,
            category: String,
            query: String
        ): List<PromptItem> {
            val q = query.trim()
            return prompts.filter { prompt ->
                val matchesCategory = when (category) {
                    "All" -> true
                    "⭐ Favorites" -> prompt.isFavorite
                    else -> prompt.category.equals(category, ignoreCase = true)
                }
                val matchesSearch = q.isEmpty() ||
                        prompt.title.contains(q, ignoreCase = true) ||
                        prompt.content.contains(q, ignoreCase = true)
                matchesCategory && matchesSearch
            }
        }

        /** Distinct categories present in [prompts], preserving a stable order. */
        fun promptCategories(prompts: List<PromptItem>): List<String> {
            val preferred = listOf("Reasoning", "Coding", "Math", "Creative", "Factuality", "Stress Test", "General")
            val present = prompts.map { it.category }.distinct()
            return preferred.filter { it in present } + (present - preferred.toSet()).sorted()
        }
        val DEFAULT_PROMPTS = listOf(
            // Reasoning
            PromptItem(
                title = "Strawberry 'r' Count",
                category = "Reasoning",
                content = "How many 'r's are in the word 'strawberry'? Please explain your step-by-step reasoning."
            ),
            PromptItem(
                title = "Apples Word Problem",
                category = "Reasoning",
                content = "If I have 5 apples and you take away 3 apples, how many apples do you have?"
            ),
            PromptItem(
                title = "River Crossing Riddle",
                category = "Reasoning",
                content = "A farmer needs to cross a river with a wolf, a goat, and a cabbage. The boat can only carry the farmer and one item. If left alone, the wolf eats the goat, or the goat eats the cabbage. How can the farmer get everything across safely?"
            ),
            PromptItem(
                title = "Monty Hall Simulation",
                category = "Reasoning",
                content = "Explain the Monty Hall problem. Why is switching doors mathematically advantageous? Provide intuitive logic as well as Bayesian probability."
            ),

            // Coding
            PromptItem(
                title = "Thread-Safe LRU Cache",
                category = "Coding",
                content = "Write a complete, thread-safe LRU Cache in Python with O(1) get and put operations, including unit tests and edge case handling."
            ),
            PromptItem(
                title = "React Custom Debounce Hook",
                category = "Coding",
                content = "Implement a robust custom React Hook `useDebounceValue<T>(value: T, delay: number): T` in TypeScript. Handle unmounting and rapid re-renders cleanly."
            ),
            PromptItem(
                title = "Rust Concurrency & Channels",
                category = "Coding",
                content = "Show how to implement a worker thread pool in Rust using `std::sync::mpsc` channels and `Arc<Mutex<...>>` without third-party crates."
            ),
            PromptItem(
                title = "Regex for Strict Email & URL",
                category = "Coding",
                content = "Provide an RFC-5322 compliant regex for email validation and another regex for URLs, explaining what each capture group does and common vulnerabilities (ReDoS)."
            ),

            // Math & Hard Logic
            PromptItem(
                title = "Coin Toss Probability",
                category = "Math",
                content = "What is the expected number of coin flips to get two consecutive Heads (HH) versus Head then Tail (HT)? Provide full mathematical proof."
            ),
            PromptItem(
                title = "Matrix Eigenvalue Intuition",
                category = "Math",
                content = "Explain eigenvalues and eigenvectors intuitively to someone who knows basic 2D geometry, using visual transformations and real-world examples."
            ),

            // Creative Writing
            PromptItem(
                title = "Cyberpunk Detective Monologue",
                category = "Creative",
                content = "Write a gritty, atmospheric noir detective monologue set in Neo-Tokyo in the year 2089 during a torrential acid-rain storm. Keep the tone cynical yet poetic."
            ),
            PromptItem(
                title = "Shakespeare on Modern Wi-Fi",
                category = "Creative",
                content = "Write a dramatic Shakespearean soliloquy in iambic pentameter about a student whose internet connection disconnects 5 minutes before midnight assignment deadline."
            ),

            // Factuality & Nuance
            PromptItem(
                title = "Quantum Computing Limits",
                category = "Factuality",
                content = "Explain what quantum computers can and cannot do. Specifically address common myths (e.g., will they replace all CPUs? will they solve NP-complete problems instantly?)."
            ),
            PromptItem(
                title = "Semiconductor Lithography (EUV)",
                category = "Factuality",
                content = "How does Extreme Ultraviolet (EUV) lithography work at ASML? Explain why 13.5nm wavelength was chosen and the engineering challenge of tin droplets and CO2 lasers."
            )
        )
    }
}
