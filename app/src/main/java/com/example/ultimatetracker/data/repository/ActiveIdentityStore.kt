package com.example.ultimatetracker.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveIdentity(
    val userId: Long,
    val listId: Long,
    val sessionId: Long,
    val isGuest: Boolean,
)

class ActiveIdentityStore {
    private val _identity = MutableStateFlow<ActiveIdentity?>(null)
    val identity: StateFlow<ActiveIdentity?> = _identity.asStateFlow()

    fun set(value: ActiveIdentity?) {
        _identity.value = value
    }
}
