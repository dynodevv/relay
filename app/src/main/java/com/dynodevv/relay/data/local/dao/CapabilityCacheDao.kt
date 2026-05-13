package com.dynodevv.relay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dynodevv.relay.data.local.entity.CapabilityCacheEntity

@Dao
interface CapabilityCacheDao {
    @Query("SELECT * FROM capability_cache WHERE modelId = :modelId LIMIT 1")
    suspend fun getById(modelId: String): CapabilityCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CapabilityCacheEntity>)

    @Query("DELETE FROM capability_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM capability_cache")
    suspend fun getCount(): Int

    @Query("SELECT * FROM capability_cache")
    suspend fun getAll(): List<CapabilityCacheEntity>
}
