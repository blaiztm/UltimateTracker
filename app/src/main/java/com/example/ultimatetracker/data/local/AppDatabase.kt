package com.example.ultimatetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ultimatetracker.data.local.account.AccountDao
import com.example.ultimatetracker.data.local.account.AccountEntity
import com.example.ultimatetracker.data.local.account.AuditEventEntity
import com.example.ultimatetracker.data.local.account.SessionEntity
import com.example.ultimatetracker.data.local.account.UserEntity
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.data.local.account.UserProfileEntity

@Database(
    entities = [
        MediaEntity::class,
        MediaTypeEntity::class,
        UserEntity::class,
        UserProfileEntity::class,
        AccountEntity::class,
        SessionEntity::class,
        UserListEntity::class,
        AuditEventEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun mediaTypeDao(): MediaTypeDao
    abstract fun accountDao(): AccountDao
}
