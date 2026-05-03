package com.dynodevv.relay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val apiBaseUrl: String,
    val apiPath: String = "/chat/completions",
    val apiKey: String? = null,
    val isBuiltin: Boolean = false,
    val iconName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
