package com.chessassistant.coreengine.analysis

import com.chessassistant.coreengine.trackers.DefaultOpeningBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningBookTest {

    private val book = DefaultOpeningBook()

    @Test
    fun `matches long line to the most specific opening`() {
        val opening = book.find(listOf("e4", "e5", "Nf3", "Nc6", "Bb5"))
        assertEquals("Ruy Lopez", opening?.name)
        assertEquals("C60", opening?.eco)
    }

    @Test
    fun `returns null when out of book`() {
        assertNull(book.find(listOf("a3")))
        assertNull(book.find(emptyList()))
    }

    @Test
    fun `longest prefix wins`() {
        // e4 e5 matches both "King's Pawn Game" and the deeper lines; the
        // most specific one should be returned.
        val opening = book.find(listOf("e4", "e5", "Nf3", "Nc6"))
        assertEquals("Italian Game", opening?.name)
    }
}