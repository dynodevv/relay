package com.dynodevv.relay.domain.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#FF6B6B",
    val createdAt: Long = System.currentTimeMillis()
)
