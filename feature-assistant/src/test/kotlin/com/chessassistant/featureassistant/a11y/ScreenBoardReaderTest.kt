package com.chessassistant.featureassistant.a11y

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.PieceType
import com.chessassistant.corechess.model.Position
import com.chessassistant.corechess.model.Move
import com.chessassistant.corechess.notation.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenBoardReaderTest {

    private val light = 0xFFE8D0B0.toInt()
    private val dark = 0xFFB08860.toInt()
    private val bg = 0xFF9A9A9A.toInt()
    private val whitePiece = 0xFFFFFFFF.toInt()
    private val blackPiece = 0xFF181818.toInt()
    private val mark = 0xFF505050.toInt()

    @Test
    fun `detects grid and start position`() {
        val layout = toTopLayout(Position.STARTING.board)
        val (px, w, h) = render(layout, left = 100, top = 120, cell = 40)

        val reader = ScreenBoardReader()
        val grid = reader.detectGrid(px, w, h)
        assertNotNull(grid)
        assertEquals(100.0, grid!!.left.toDouble(), 10.0)
        assertEquals(120.0, grid.top.toDouble(), 10.0)
        assertEquals(40.0, grid.cell.toDouble(), 8.0)

        val det = reader.detect(px, w, h)
        assertNotNull(det)
        assertEquals(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
            ScreenBoardReader.fen(det!!.layout, Color.WHITE).substringBefore(' '),
        )
    }

    @Test
    fun `reads a moved position through template matching`() {
        val reader = ScreenBoardReader()
        val start = toTopLayout(Position.STARTING.board)
        val (px0, w0, h0) = render(start, left = 100, top = 120, cell = 40)
        assertNotNull(reader.detect(px0, w0, h0))

        val afterE4 = FenParser.parse(START_FEN)!!.apply(Move.fromUci("e2e4")!!)
        val expect = toTopLayout(afterE4.board)
        val (px1, w1, h1) = render(expect, left = 100, top = 120, cell = 40)
        val det = reader.detect(px1, w1, h1)

        assertNotNull(det)
        assertArrayEquals(expect, det!!.layout)
    }

    @Test
    fun `handles a flipped board layout`() {
        val layout = toTopLayout(Position.STARTING.board)
        val (px, w, h) = render(layout, left = 100, top = 120, cell = 40, flipped = true)
        val det = ScreenBoardReader().detect(px, w, h)
        assertNotNull(det)
        assertEquals(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
            ScreenBoardReader.fen(det!!.layout, Color.WHITE).substringBefore(' '),
        )
    }

    @Test
    fun `renders fen for a layout`() {
        val layout = toTopLayout(Position.STARTING.board)
        assertEquals("$START_BOARD w - 0 1", ScreenBoardReader.fen(layout, Color.WHITE))
    }

    @Test
    fun `tracks side to move across moves`() {
        val reader = ScreenBoardReader()
        assertEquals(Color.WHITE, reader.updateSide(toTopLayout(Position.STARTING.board)))
        val afterE4 = FenParser.parse(START_FEN)!!.apply(Move.fromUci("e2e4")!!)
        assertEquals(Color.BLACK, reader.updateSide(toTopLayout(afterE4.board)))
        val afterE5 = afterE4.apply(Move.fromUci("e7e5")!!)
        assertEquals(Color.WHITE, reader.updateSide(toTopLayout(afterE5.board)))
    }

    @Test
    fun `returns null on a plain screen`() {
        val w = 400
        val h = 600
        val px = IntArray(w * h) { bg }
        assertNull(ScreenBoardReader().detect(px, w, h))
    }

    // -------------------------------------------------------------- helpers

    private fun toTopLayout(board: Array<Piece?>): Array<Piece?> {
        val out = arrayOfNulls<Piece>(64)
        for (fenRank in 0..7) {
            for (file in 0..7) {
                out[fenRank * 8 + file] = board[file + (7 - fenRank) * 8]
            }
        }
        return out
    }

    private fun render(
        layout: Array<Piece?>,
        left: Int,
        top: Int,
        cell: Int,
        flipped: Boolean = false,
    ): Triple<IntArray, Int, Int> {
        val w = 500
        val h = 600
        val px = IntArray(w * h) { bg }
        for (row in 0..7) {
            for (col in 0..7) {
                val (rank, file) = if (flipped) (7 - row) to col else row to col
                val base = if ((rank + file) % 2 == 0) light else dark
                fillRect(px, w, left + col * cell, top + row * cell, cell, cell, base)
                val piece = layout[rank * 8 + file]
                if (piece != null) {
                    drawPiece(px, w, left + col * cell, top + row * cell, cell, piece)
                }
            }
        }
        return Triple(px, w, h)
    }

    private fun drawPiece(px: IntArray, w: Int, squareLeft: Int, squareTop: Int, cell: Int, piece: Piece) {
        val cx = squareLeft + cell / 2
        val cy = squareTop + cell / 2
        val radius = (cell * 0.30f).toInt()
        fillCircle(px, w, cx, cy, radius, if (piece.color == Color.WHITE) whitePiece else blackPiece)

        val m = maxOf(2, radius / 3)
        val d = maxOf(1, radius / 2)
        when (piece.type) {
            PieceType.PAWN -> {}
            PieceType.KNIGHT -> fillRect(px, w, cx + d, cy - m / 2, m, m, mark)
            PieceType.BISHOP -> fillRect(px, w, cx - m / 2, cy - d - m, m, m, mark)
            PieceType.ROOK -> fillRect(px, w, cx - m / 2, cy + d, m, m, mark)
            PieceType.QUEEN -> fillRect(px, w, cx - d - m, cy - m / 2, m, m, mark)
            PieceType.KING -> fillRect(px, w, cx - m / 2, cy - m / 2, m, m, mark)
        }
    }

    private fun fillCircle(px: IntArray, w: Int, cx: Int, cy: Int, radius: Int, color: Int) {
        val r2 = radius * radius
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                if (x * x + y * y <= r2) {
                    setPx(px, w, cx + x, cy + y, color)
                }
            }
        }
    }

    private fun fillRect(px: IntArray, w: Int, x: Int, y: Int, rw: Int, rh: Int, color: Int) {
        for (dy in 0 until rh) {
            for (dx in 0 until rw) {
                setPx(px, w, x + dx, y + dy, color)
            }
        }
    }

    private fun setPx(px: IntArray, w: Int, x: Int, y: Int, color: Int) {
        if (x in 0 until w && y in 0 until (px.size / w)) {
            px[y * w + x] = color
        }
    }

    private fun assertArrayEquals(expected: Array<Piece?>, actual: Array<Piece?>) {
        assertEquals(64, actual.size)
        for (i in 0 until 64) {
            assertEquals("square $i", expected[i], actual[i])
        }
    }

    companion object {
        private const val START_BOARD = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
        private const val START_FEN = "$START_BOARD w KQkq - 0 1"
    }
}
