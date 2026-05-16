package com.dynodevv.relay.domain.model

data class Template(
    val id: Long = 0,
    val name: String,
    val content: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
