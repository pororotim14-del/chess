package com.chessassistant.domain.repository

import com.chessassistant.coreengine.trackers.Opening
import kotlinx.coroutines.flow.Flow

/**
 * Supplies book names and accuracy numbers for played games.
 */
interface OpeningRepository {

    /** Best matching opening for the played [moves] (SAN). */
    fun findOpening(moves: List<String>): Opening?

    /** Accuracy of the last completed game, 0..100, or null when none yet. */
    fun lastAccuracy(): Flow<Int?>

    fun recordAccuracy(pct: Int)

    val bookNames: Flow<List<Opening>>
}