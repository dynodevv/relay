package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.TemplateDao
import com.dynodevv.relay.data.local.entity.TemplateEntity
import com.dynodevv.relay.domain.model.Template
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao
) {
    fun getTemplates(): Flow<List<Template>> =
        templateDao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun createTemplate(name: String, content: String): Long {
        return templateDao.insert(TemplateEntity(name = name, content = content))
    }

    suspend fun updateTemplate(id: Long, name: String, content: String) {
        templateDao.getById(id)?.let { existing ->
            templateDao.update(existing.copy(name = name, content = content))
        }
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.deleteById(id)
    }

    private fun TemplateEntity.toDomain() = Template(
        id = id,
        name = name,
        content = content,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}
