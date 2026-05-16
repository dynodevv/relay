package com.dynodevv.relay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dynodevv.relay.data.local.dao.AIModelDao
import com.dynodevv.relay.data.local.dao.CapabilityCacheDao
import com.dynodevv.relay.data.local.dao.ConversationDao
import com.dynodevv.relay.data.local.dao.FolderDao
import com.dynodevv.relay.data.local.dao.MessageDao
import com.dynodevv.relay.data.local.dao.ProviderDao
import com.dynodevv.relay.data.local.dao.TagDao
import com.dynodevv.relay.data.local.dao.TemplateDao
import com.dynodevv.relay.data.local.entity.AIModelEntity
import com.dynodevv.relay.data.local.entity.CapabilityCacheEntity
import com.dynodevv.relay.data.local.entity.ConversationEntity
import com.dynodevv.relay.data.local.entity.ConversationTagCrossRef
import com.dynodevv.relay.data.local.entity.FolderEntity
import com.dynodevv.relay.data.local.entity.MessageEntity
import com.dynodevv.relay.data.local.entity.ProviderEntity
import com.dynodevv.relay.data.local.entity.TagEntity
import com.dynodevv.relay.data.local.entity.TemplateEntity

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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add archive and folder fields to conversations
        db.execSQL("ALTER TABLE conversations ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN folderId INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_folderId ON conversations(folderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_isArchived ON conversations(isArchived)")

        // Create folders table
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )

        // Create tags table
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL DEFAULT '#FF6B6B',
                createdAt INTEGER NOT NULL DEFAULT 0
            )""".trimIndent()
        )

        // Create conversation_tags junction table
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS conversation_tags (
                conversationId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(conversationId, tagId),
                FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
            )""".trimIndent()
        )

        // Create templates table
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                content TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
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
        CapabilityCacheEntity::class,
        FolderEntity::class,
        TagEntity::class,
        ConversationTagCrossRef::class,
        TemplateEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun aiModelDao(): AIModelDao
    abstract fun capabilityCacheDao(): CapabilityCacheDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun templateDao(): TemplateDao
}
