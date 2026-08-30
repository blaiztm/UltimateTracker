package com.example.ultimatetracker

import android.app.Application
import android.os.Build
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.repository.MediaRepository
import com.example.ultimatetracker.data.repository.AccountRepository
import com.example.ultimatetracker.data.repository.ActiveIdentityStore
import com.example.ultimatetracker.data.repository.BackupRepository
import com.example.ultimatetracker.data.remote.TmdbClient

class UltimateTrackerApplication : Application() {
    private val preferences by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val identityStore by lazy { ActiveIdentityStore() }
    val tmdbClient by lazy {
        val token = if (preferences.contains("tmdb_token")) preferences.getString("tmdb_token", "").orEmpty() else BuildConfig.TMDB_READ_ACCESS_TOKEN
        TmdbClient(token) { preferences.edit().putString("tmdb_token", it).apply() }
    }
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "ultimate-tracker.db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .build()
    }
    val accountRepository by lazy {
        AccountRepository(database, preferences, identityStore, "${Build.MANUFACTURER} ${Build.MODEL}")
    }
    val repository: MediaRepository by lazy { MediaRepository(database.mediaDao(), database.mediaTypeDao(), identityStore) }
    val backupRepository by lazy { BackupRepository(this, database, identityStore) }

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
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `media_items` ADD COLUMN `review` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `media_items` ADD COLUMN `rating` INTEGER")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `media_items` ADD COLUMN `watchedEpisodes` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `status` TEXT NOT NULL, `isGuest` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_status` ON `users` (`status`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`userId` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `locale` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`userId`), FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `provider` TEXT NOT NULL, `providerAccountId` TEXT NOT NULL, `emailNormalized` TEXT, `passwordHash` TEXT, `passwordSalt` TEXT, `passwordIterations` INTEGER, `emailVerifiedAt` INTEGER, `failedLoginAttempts` INTEGER NOT NULL, `lockedUntil` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_userId` ON `accounts` (`userId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_provider_providerAccountId` ON `accounts` (`provider`, `providerAccountId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_emailNormalized` ON `accounts` (`emailNormalized`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `tokenHash` TEXT NOT NULL, `deviceName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastUsedAt` INTEGER NOT NULL, `expiresAt` INTEGER NOT NULL, `revokedAt` INTEGER, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_userId` ON `sessions` (`userId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sessions_tokenHash` ON `sessions` (`tokenHash`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_expiresAt` ON `sessions` (`expiresAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_lists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ownerUserId` INTEGER NOT NULL, `title` TEXT NOT NULL, `position` INTEGER NOT NULL, `rowVersion` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `archivedAt` INTEGER, `deletedAt` INTEGER, FOREIGN KEY(`ownerUserId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_lists_ownerUserId` ON `user_lists` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_lists_ownerUserId_position` ON `user_lists` (`ownerUserId`, `position`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `audit_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER, `action` TEXT NOT NULL, `subjectType` TEXT NOT NULL, `subjectId` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_events_userId_createdAt` ON `audit_events` (`userId`, `createdAt`)")
                db.execSQL("INSERT INTO `users` (`id`, `status`, `isGuest`, `createdAt`, `updatedAt`, `deletedAt`) VALUES (1, 'ACTIVE', 1, 0, 0, NULL)")
                db.execSQL("INSERT INTO `user_profiles` (`userId`, `displayName`, `locale`, `createdAt`, `updatedAt`) VALUES (1, 'Guest', '', 0, 0)")
                db.execSQL("INSERT INTO `user_lists` (`id`, `ownerUserId`, `title`, `position`, `rowVersion`, `createdAt`, `updatedAt`, `archivedAt`, `deletedAt`) VALUES (1, 1, 'My list', 0, 1, 0, 0, NULL, NULL)")
                db.execSQL("CREATE TABLE `media_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `listId` INTEGER NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `length` INTEGER NOT NULL, `genres` TEXT NOT NULL, `keywords` TEXT NOT NULL, `category` TEXT NOT NULL, `coverUri` TEXT, `review` TEXT NOT NULL, `rating` INTEGER, `watchedEpisodes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `rowVersion` INTEGER NOT NULL, `deletedAt` INTEGER, FOREIGN KEY(`listId`) REFERENCES `user_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO `media_items_new` (`id`, `listId`, `title`, `type`, `length`, `genres`, `keywords`, `category`, `coverUri`, `review`, `rating`, `watchedEpisodes`, `createdAt`, `updatedAt`, `rowVersion`, `deletedAt`) SELECT `id`, 1, `title`, `type`, `length`, `genres`, `keywords`, `category`, `coverUri`, `review`, `rating`, `watchedEpisodes`, `createdAt`, `updatedAt`, 1, NULL FROM `media_items`")
                db.execSQL("DROP TABLE `media_items`")
                db.execSQL("ALTER TABLE `media_items_new` RENAME TO `media_items`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_items_listId` ON `media_items` (`listId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_media_items_listId_updatedAt` ON `media_items` (`listId`, `updatedAt`)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profiles` ADD COLUMN `avatarUri` TEXT")
            }
        }
    }
}
