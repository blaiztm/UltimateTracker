package com.example.ultimatetracker.data.local.account

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["status"])],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: String,
    val isGuest: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "user_profiles",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class UserProfileEntity(
    @PrimaryKey val userId: Long,
    val displayName: String,
    val locale: String,
    val createdAt: Long,
    val updatedAt: Long,
    val avatarUri: String? = null,
)

@Entity(
    tableName = "accounts",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["provider", "providerAccountId"], unique = true),
        Index(value = ["emailNormalized"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val provider: String,
    val providerAccountId: String,
    val emailNormalized: String?,
    val passwordHash: String?,
    val passwordSalt: String?,
    val passwordIterations: Int?,
    val emailVerifiedAt: Long? = null,
    val failedLoginAttempts: Int = 0,
    val lockedUntil: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["tokenHash"], unique = true),
        Index(value = ["expiresAt"]),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val tokenHash: String,
    val deviceName: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val expiresAt: Long,
    val revokedAt: Long? = null,
)

@Entity(
    tableName = "user_lists",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["ownerUserId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index(value = ["ownerUserId"]),
        Index(value = ["ownerUserId", "position"]),
    ],
)
data class UserListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val title: String,
    val position: Int,
    val rowVersion: Long = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "audit_events",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index(value = ["userId", "createdAt"])],
)
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long?,
    val action: String,
    val subjectType: String,
    val subjectId: Long?,
    val createdAt: Long,
)

object AccountConstants {
    const val ACTIVE = "ACTIVE"
    const val DELETED = "DELETED"
    const val LOCAL_PROVIDER = "LOCAL"
    const val GUEST_PROVIDER = "GUEST"
    const val GUEST_USER_ID = 1L
    const val GUEST_LIST_ID = 1L
}
