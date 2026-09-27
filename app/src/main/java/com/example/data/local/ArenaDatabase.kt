package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.data.model.PromptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptDao {
    @Query("SELECT * FROM benchmark_prompts ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllPrompts(): Flow<List<PromptItem>>

    @Query("SELECT * FROM benchmark_prompts WHERE category = :category ORDER BY isFavorite DESC, createdAt DESC")
    fun getPromptsByCategory(category: String): Flow<List<PromptItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(prompts: List<PromptItem>)

    @Update
    suspend fun updatePrompt(prompt: PromptItem)

    @Delete
    suspend fun deletePrompt(prompt: PromptItem)

    @Query("UPDATE benchmark_prompts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM benchmark_prompts WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isFavorite DESC, createdAt DESC")
    fun searchPrompts(query: String): Flow<List<PromptItem>>

    @Query("SELECT * FROM benchmark_prompts WHERE id = :id LIMIT 1")
    suspend fun getPromptById(id: Long): PromptItem?

    @Query("SELECT COUNT(*) FROM benchmark_prompts")
    suspend fun getCount(): Int
}

@Dao
interface BattleDao {
    @Query("SELECT * FROM battle_records ORDER BY timestamp DESC")
    fun getAllBattles(): Flow<List<BattleRecord>>

    @Query("SELECT * FROM battle_records WHERE modelA LIKE '%' || :query || '%' OR modelB LIKE '%' || :query || '%' OR promptTopic LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchBattles(query: String): Flow<List<BattleRecord>>

    @Query("SELECT * FROM battle_records WHERE id = :id LIMIT 1")
    suspend fun getBattleById(id: Long): BattleRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattle(battle: BattleRecord): Long

    @Delete
    suspend fun deleteBattle(battle: BattleRecord)

    @Query("DELETE FROM battle_records")
    suspend fun clearAllBattles()
}

class Converters {
    @TypeConverter
    fun fromWinner(winner: BattleWinner): String = winner.name

    @TypeConverter
    fun toWinner(value: String): BattleWinner = try {
        BattleWinner.valueOf(value)
    } catch (e: Exception) {
        BattleWinner.TIE
    }
}

@Database(entities = [PromptItem::class, BattleRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ArenaDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
    abstract fun battleDao(): BattleDao

    companion object {
        @Volatile
        private var INSTANCE: ArenaDatabase? = null

        fun getInstance(context: Context): ArenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArenaDatabase::class.java,
                    "arena_companion.db"
                ).fallbackToDestructiveMigration(dropAllTables = false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
