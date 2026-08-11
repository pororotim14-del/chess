package com.chessassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chessassistant.corechess.rules.GameStatus
import com.chessassistant.domain.model.GameOutcome
import com.chessassistant.domain.model.GameSummary
import com.chessassistant.domain.model.StoredGame
import com.chessassistant.domain.model.StoredMove
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val initialFen: String,
    val whiteName: String,
    val blackName: String,
    val movesJson: String,
    val result: String,
    val outcome: String,
    val createdAtEpochMs: Long,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromStorage(game: StoredGame): GameEntity =
            GameEntity(
                id = game.id.value,
                initialFen = game.initialFen,
                whiteName = game.whiteName,
                blackName = game.blackName,
                movesJson = json.encodeToString(game.moves),
                result = game.result,
                outcome = game.toSummary().outcome.name,
                createdAtEpochMs = game.createdAtEpochMs,
            )
    }

    fun toStorage(): StoredGame =
        StoredGame(
            id = com.chessassistant.domain.model.GameId(id),
            initialFen = initialFen,
            moves = runCatching {
                json.decodeFromString<List<StoredMove>>(movesJson)
            }.getOrDefault(emptyList()),
            whiteName = whiteName,
            blackName = blackName,
            result = result,
            createdAtEpochMs = createdAtEpochMs,
        )

    fun toSummary(): GameSummary = toStorage().toSummary()
}

@Entity(tableName = "game_moves")
data class GameMoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val ply: Int,
    val uci: String,
    val san: String,
    val fenAfter: String,
)

@Entity(tableName = "analysis_cache")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fen: String,
    val depth: Int,
    val evaluationCp: Int,
    val pvFenListJson: String,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "openings")
data class OpeningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eco: String,
    val name: String,
    val movesJson: String,
    val isFavorite: Boolean = false,
)