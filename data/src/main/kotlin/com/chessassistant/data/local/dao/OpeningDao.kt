package com.chessassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chessassistant.data.local.OpeningEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningDao {

    @Query("SELECT * FROM openings ORDER BY eco ASC")
    fun observeAll(): Flow<List<OpeningEntity>>

    @Query("SELECT * FROM openings WHERE eco = :eco LIMIT 1")
    suspend fun byEco(eco: String): OpeningEntity?

    @Insert
    suspend fun insertAll(openings: List<OpeningEntity>)

    @Insert
    suspend fun insert(opening: OpeningEntity): Long

    @Update
    suspend fun update(opening: OpeningEntity)

    @Query("SELECT * FROM openings WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<OpeningEntity>>
}