package com.example
 
import android.app.Application
import com.example.data.local.ArenaDatabase
import com.example.data.repository.ArenaRepository
 
class ArenaApplication : Application() {
 
    val database: ArenaDatabase by lazy {
        ArenaDatabase.getInstance(this)
    }
 
    val repository: ArenaRepository by lazy {
        ArenaRepository(database.promptDao(), database.battleDao())
    }
 
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
 
    companion object {
        lateinit var instance: ArenaApplication
            private set
    }
}
