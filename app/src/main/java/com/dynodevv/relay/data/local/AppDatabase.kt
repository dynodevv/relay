package com.dynodevv.relay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dynodevv.relay.data.local.dao.AIModelDao
import com.dynodevv.relay.data.local.dao.ConversationDao
import com.dynodevv.relay.data.local.dao.MessageDao
import com.dynodevv.relay.data.local.dao.ProviderDao
import com.dynodevv.relay.data.local.entity.AIModelEntity
import com.dynodevv.relay.data.local.entity.ConversationEntity
import com.dynodevv.relay.data.local.entity.MessageEntity
import com.dynodevv.relay.data.local.entity.ProviderEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ProviderEntity::class,
        AIModelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun aiModelDao(): AIModelDao
}
