package com.dynodevv.relay.data.repository

import com.dynodevv.relay.data.local.dao.FolderDao
import com.dynodevv.relay.data.local.entity.FolderEntity
import com.dynodevv.relay.domain.model.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getFolders(): Flow<List<Folder>> =
        folderDao.getAll().map { list ->
            list.map { it.toDomain() }
        }

    suspend fun createFolder(name: String): Long {
        return folderDao.insert(FolderEntity(name = name))
    }

    suspend fun updateFolder(id: Long, name: String) {
        folderDao.getById(id)?.let { existing ->
            folderDao.update(existing.copy(name = name))
        }
    }

    suspend fun deleteFolder(id: Long) {
        folderDao.deleteById(id)
    }

    suspend fun updateSortOrder(id: Long, sortOrder: Int) {
        folderDao.updateSortOrder(id, sortOrder)
    }

    private fun FolderEntity.toDomain() = Folder(
        id = id,
        name = name,
        sortOrder = sortOrder,
        createdAt = createdAt
    )
}
