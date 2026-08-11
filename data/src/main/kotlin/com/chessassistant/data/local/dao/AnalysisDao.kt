package com.chessassistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.chessassistant.data.local.AnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analysis_cache ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<AnalysisEntity>>

    @Insert
    suspend fun insert(entity: AnalysisEntity): Long

    @Query("SELECT * FROM analysis_cache WHERE fen = :fen LIMIT 1")
    suspend fun byFen(fen: String): AnalysisEntity?

    @Query("DELETE FROM analysis_cache WHERE createdAtEpochMs < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}