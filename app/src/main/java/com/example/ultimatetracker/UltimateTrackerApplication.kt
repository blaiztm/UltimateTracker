package com.example.ultimatetracker

import android.app.Application
import androidx.room.Room
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.repository.MediaRepository

class UltimateTrackerApplication : Application() {
    val repository: MediaRepository by lazy {
        val database = Room.databaseBuilder(this, AppDatabase::class.java, "ultimate-tracker.db").build()
        MediaRepository(database.mediaDao())
    }
}
