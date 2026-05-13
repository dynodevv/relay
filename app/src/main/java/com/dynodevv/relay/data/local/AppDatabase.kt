package com.dynodevv.relay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dynodevv.relay.data.local.dao.AIModelDao
import com.dynodevv.relay.data.local.dao.CapabilityCacheDao
import com.dynodevv.relay.data.local.dao.ConversationDao
import com.dynodevv.relay.data.local.dao.MessageDao
import com.dynodevv.relay.data.local.dao.ProviderDao
import com.dynodevv.relay.data.local.entity.AIModelEntity
import com.dynodevv.relay.data.local.entity.CapabilityCacheEntity
import com.dynodevv.relay.data.local.entity.ConversationEntity
import com.dynodevv.relay.data.local.entity.MessageEntity
import com.dynodevv.relay.data.local.entity.ProviderEntity

val MIGRATION_3_6 = object : Migration(3, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ai_models ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN systemPrompt TEXT")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN temperature REAL")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN maxTokens INTEGER")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN topP REAL")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN topK INTEGER")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN presencePenalty REAL")
        db.execSQL("ALTER TABLE ai_models ADD COLUMN frequencyPenalty REAL")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ai_models ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS capability_cache (
                modelId TEXT PRIMARY KEY NOT NULL,
                supportsVision INTEGER NOT NULL DEFAULT 0,
                supportsTools INTEGER NOT NULL DEFAULT 0,
                supportsReasoning INTEGER NOT NULL DEFAULT 0,
                cachedAt INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )
    }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ProviderEntity::class,
        AIModelEntity::class,
        CapabilityCacheEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun aiModelDao(): AIModelDao
    abstract fun capabilityCacheDao(): CapabilityCacheDao
}
