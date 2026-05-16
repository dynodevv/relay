package com.dynodevv.relay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "capability_cache")
data class CapabilityCacheEntity(
    @PrimaryKey
    val modelId: String,
    val supportsVision: Boolean,
    val supportsTools: Boolean,
    val supportsReasoning: Boolean,
    val cachedAt: Long
)
