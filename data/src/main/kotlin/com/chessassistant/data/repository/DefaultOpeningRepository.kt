package com.chessassistant.data.repository

import com.chessassistant.coreengine.trackers.DefaultOpeningBook
import com.chessassistant.coreengine.trackers.Opening
import com.chessassistant.coreengine.trackers.OpeningBook
import com.chessassistant.domain.repository.OpeningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Stateless in-memory implementation of [OpeningRepository] backed by the
 * bundled book. Accuracy numbers would normally be persisted; this keeps the
 * module standalone and deterministic for tests.
 */
class DefaultOpeningRepository(
    private val book: OpeningBook = DefaultOpeningBook(),
) : OpeningRepository {

    private val accuracy = MutableStateFlow<Int?>(null)

    override fun findOpening(moves: List<String>): Opening? =
        book.find(moves.map { it.trim() }.filter { it.isNotEmpty() })

    override fun lastAccuracy(): Flow<Int?> = accuracy

    override fun recordAccuracy(pct: Int) {
        accuracy.value = pct.coerceIn(0, 100)
    }

    override val bookNames: Flow<List<Opening>> = MutableStateFlow(book.allOpenings())
}