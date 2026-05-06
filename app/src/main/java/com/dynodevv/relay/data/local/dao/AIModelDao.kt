package com.dynodevv.relay.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dynodevv.relay.data.local.entity.AIModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIModelDao {
    @Query("SELECT * FROM ai_models WHERE providerId = :providerId ORDER BY isFavorite DESC, displayName ASC")
    fun getByProvider(providerId: Long): Flow<List<AIModelEntity>>

    @Query("SELECT * FROM ai_models WHERE id = :id AND providerId = :providerId")
    suspend fun getById(id: String, providerId: Long): AIModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: AIModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<AIModelEntity>)

    @Update
    suspend fun update(model: AIModelEntity)

    @Delete
    suspend fun delete(model: AIModelEntity)

    @Query("DELETE FROM ai_models WHERE id = :id AND providerId = :providerId")
    suspend fun deleteById(id: String, providerId: Long)

    @Query("UPDATE ai_models SET isFavorite = :isFavorite WHERE id = :id AND providerId = :providerId")
    suspend fun setFavorite(id: String, providerId: Long, isFavorite: Boolean)
}
