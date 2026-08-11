package com.chessassistant.data.local

import androidx.room.TypeConverter
import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.rules.GameStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single set of Room converters shared by all tables.
 */
class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun gameStatusToString(value: GameStatus): String = value.name

    @TypeConverter
    fun stringToGameStatus(value: String): GameStatus =
        GameStatus.entries.firstOrNull { it.name == value } ?: GameStatus.NORMAL

    @TypeConverter
    fun colorToString(value: Color): String = value.name

    @TypeConverter
    fun stringToColor(value: String): Color = Color.valueOf(value)

    @TypeConverter
    fun moveListToJson(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun jsonToMoveList(value: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}