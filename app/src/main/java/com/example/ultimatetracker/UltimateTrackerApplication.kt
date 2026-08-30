package com.example.ultimatetracker

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.repository.MediaRepository
import com.example.ultimatetracker.data.remote.TmdbClient

class UltimateTrackerApplication : Application() {
    val tmdbClient by lazy { TmdbClient(BuildConfig.TMDB_READ_ACCESS_TOKEN) }
    val repository: MediaRepository by lazy {
        val database = Room.databaseBuilder(this, AppDatabase::class.java, "ultimate-tracker.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .build()
        MediaRepository(database.mediaDao(), database.mediaTypeDao())
    }

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_media_types` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_media_types_name` ON `custom_media_types` (`name`)")
                db.execSQL("UPDATE `media_items` SET `type` = 'Фильм' WHERE `type` = 'MOVIE'")
                db.execSQL("UPDATE `media_items` SET `type` = 'Сериал' WHERE `type` = 'SERIES'")
                db.execSQL("UPDATE `media_items` SET `type` = 'Аниме' WHERE `type` = 'ANIME'")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE `media_items` SET `type` = 'MOVIE' WHERE `type` = 'Фильм'")
                db.execSQL("UPDATE `media_items` SET `type` = 'SERIES' WHERE `type` = 'Сериал'")
                db.execSQL("UPDATE `media_items` SET `type` = 'ANIME' WHERE `type` = 'Аниме'")
            }
        }
    }
}
