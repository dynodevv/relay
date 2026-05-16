package com.dynodevv.relay.data.repository

import android.content.Context
import android.net.Uri
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
import com.dynodevv.relay.domain.model.Message
import com.dynodevv.relay.domain.model.MessageRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RelayBackup(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val providers: List<ProviderEntity> = emptyList(),
    val aiModels: List<AIModelEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val conversations: List<ConversationEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val conversationTags: List<ConversationTagCrossRef> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val capabilityCache: List<CapabilityCacheEntity> = emptyList()
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerDao: ProviderDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val templateDao: TemplateDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    suspend fun exportAllData(uri: Uri): Boolean {
        return try {
            val backup = RelayBackup(
                providers = providerDao.getAllOnce(),
                conversations = conversationDao.getAllOnce(),
                messages = messageDao.getAllMessages(),
                folders = folderDao.getAllOnce(),
                tags = tagDao.getAllOnce(),
                conversationTags = tagDao.getAllCrossRefs(),
                templates = templateDao.getAllOnce(),
                aiModels = emptyList(), // We skip AI models as they are fetched dynamically
                capabilityCache = emptyList()
            )
            val data = json.encodeToString(backup)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(data.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importAllData(uri: Uri): Boolean {
        return try {
            val data = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().decodeToString()
            } ?: return false

            val backup = json.decodeFromString<RelayBackup>(data)

            // Clear existing data (keep providers if desired, but let's do full replace)
            conversationDao.getAllOnce().forEach { conversationDao.deleteById(it.id) }
            folderDao.getAllOnce().forEach { folderDao.deleteById(it.id) }
            tagDao.getAllOnce().forEach { tagDao.deleteById(it.id) }
            templateDao.getAllOnce().forEach { templateDao.deleteById(it.id) }
            providerDao.getAllOnce().forEach { providerDao.deleteById(it.id) }

            // Insert in order: providers, folders, tags, conversations, messages, cross refs, templates
            backup.providers.forEach { providerDao.insert(it) }
            backup.folders.forEach { folderDao.insert(it) }
            backup.tags.forEach { tagDao.insert(it) }
            backup.conversations.forEach { conversationDao.insert(it) }
            backup.messages.forEach { messageDao.insert(it) }
            backup.conversationTags.forEach { tagDao.addTagToConversation(it) }
            backup.templates.forEach { templateDao.insert(it) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportConversationToMarkdown(conversationId: Long): String {
        val conversation = conversationDao.getById(conversationId) ?: return ""
        val messages = messageDao.getByConversationOnce(conversationId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.appendLine("# ${conversation.title}")
        sb.appendLine()
        sb.appendLine("**Created:** ${dateFormat.format(Date(conversation.createdAt))}")
        sb.appendLine("**Updated:** ${dateFormat.format(Date(conversation.updatedAt))}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        messages.forEach { msg ->
            val role = when (msg.role) {
                "user" -> "**User**"
                "assistant" -> "**Assistant**"
                else -> "**${msg.role}**"
            }
            sb.appendLine("$role (${dateFormat.format(Date(msg.createdAt))})")
            sb.appendLine()
            sb.appendLine(msg.content)
            sb.appendLine()
            sb.appendLine("---")
            sb.appendLine()
        }

        return sb.toString()
    }

    suspend fun exportConversationToJson(conversationId: Long): String {
        val conversation = conversationDao.getById(conversationId) ?: return ""
        val messages = messageDao.getByConversationOnce(conversationId)
        val tags = tagDao.getTagsForConversationOnce(conversationId)

        val export = ConversationExportJson(
            title = conversation.title,
            modelId = conversation.modelId,
            systemPrompt = conversation.systemPrompt,
            createdAt = conversation.createdAt,
            updatedAt = conversation.updatedAt,
            tags = tags.map { it.name },
            messages = messages.map { MessageExportJson(it.role, it.content, it.createdAt) }
        )

        return json.encodeToString(export)
    }
}

@Serializable
private data class ConversationExportJson(
    val title: String,
    val modelId: String,
    val systemPrompt: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String>,
    val messages: List<MessageExportJson>
)

@Serializable
private data class MessageExportJson(
    val role: String,
    val content: String,
    val createdAt: Long
)
