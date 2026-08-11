package com.chessassistant.featureassistant.a11y

import com.chessassistant.corechess.model.Color
import com.chessassistant.corechess.model.Piece
import com.chessassistant.corechess.model.PieceType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Reads a chess position directly from the pixels of a screenshot of any
 * chess app (Chess.com, Lichess, …), so the assistant works over apps that do
 * not expose any readable move text.
 *
 * Pipeline:
 *  1. [detectGrid] locates the 8x8 board as the region whose square
 *     luminances alternate in a strongly bimodal light/dark pattern.
 *  2. Squares are classified as empty or holding a white/black piece by
 *     comparing the square-centre colour with the square's own corner colour.
 *  3. When the standard starting position is recognised (by colour alone),
 *     the app's piece art is harvested into a [BoardTemplate]; afterwards
 *     every screenshot is labelled by normalized cross-correlation against
 *     those templates so piece *types* are known and a FEN can be built.
 *
 * All functions are pure (they operate on an ARGB IntArray), so the heavy
 * part can run on a background thread and be unit-tested on synthetic images.
 */
class ScreenBoardReader {

    data class Grid(val left: Int, val top: Int, val cell: Int) {
        val right: Int get() = left + cell * 8
        val bottom: Int get() = top + cell * 8
    }

    /** 64 squares, indexed [rank*8 + file]; rank 0 == rank 8 (top of board). */
    class BoardDetection(val layout: Array<Piece?>, val grid: Grid)

    private val templates = HashMap<Int, BoardTemplate>()
    private var lastLayout: Array<Piece?>? = null
    private var lastSide: Color = Color.WHITE

    // ------------------------------------------------------------ detection

    fun detect(pixels: IntArray, width: Int, height: Int): BoardDetection? {
        val grid = detectGrid(pixels, width, height) ?: return null
        val layout = classifySquares(pixels, width, grid)
        orientToStandard(layout)

        if (matchesStart(layout)) {
            fillStart(layout)
            val template = templates.getOrPut(grid.cell) { BoardTemplate() }
            seedTemplate(pixels, width, grid, template, layout)
            return BoardDetection(layout, grid)
        }

        val template = templates[grid.cell] ?: return null
        if (!template.configured) return null
        return applyTemplates(pixels, width, grid, layout, template)?.let {
            BoardDetection(it, grid)
        }
    }

    /** Advances the tracked side-to-move based on the layout diff. */
    fun updateSide(layout: Array<Piece?>): Color {
        if (matchesStart(layout)) {
            lastSide = Color.WHITE
        } else {
            lastLayout?.let { prev ->
                inferMover(prev, layout)?.let { lastSide = it.opposite }
            }
        }
        lastLayout = layout
        return lastSide
    }

    // ---------------------------------------------------------------- grid

    /** Finds the 8x8 board on a downscaled luminance copy of the screen. */
    fun detectGrid(pixels: IntArray, width: Int, height: Int): Grid? {
        val scale = 8
        val sw = width / scale
        val sh = height / scale
        if (sw < 8 * 2 || sh < 8 * 2) return null

        val lum = IntArray(sw * sh)
        for (y in 0 until sh) {
            val rowBase = y * scale
            for (x in 0 until sw) {
                var sum = 0L
                for (dy in 0 until scale) {
                    val src = (rowBase + dy) * width + x * scale
                    for (dx in 0 until scale) sum += luminance(pixels[src + dx])
                }
                lum[y * sw + x] = (sum / (scale * scale)).toInt()
            }
        }

        val minCell = maxOf(2, minOf(sw, sh) / 14)
        val maxCell = maxOf(minCell, minOf(sw, sh) / 4)
        val perCell = ArrayList<Pair<Int, Pair<Double, Grid>>>()
        for (cell in minCell..maxCell) {
            val step = maxOf(1, cell / 4)
            val side = cell * 8
            var cellBest = -1.0
            var cellBestGrid: Grid? = null
            var ty = 0
            while (ty + side <= sh) {
                var tx = 0
                while (tx + side <= sw) {
                    val score = gridScore(lum, sw, tx, ty, cell)
                    if (score > cellBest) {
                        cellBest = score
                        cellBestGrid = Grid(tx, ty, cell)
                    }
                    tx += step
                }
                ty += step
            }
            if (cellBestGrid != null) perCell.add(cell to (cellBest to cellBestGrid))
        }
        val maxScore = perCell.maxOfOrNull { it.second.first } ?: return null
        val scaled = perCell
            .filter { it.second.first >= 0.85 * maxScore }
            .maxByOrNull { it.first }
            ?.second?.second ?: return null

        val refined = refineGrid(lum, sw, sh, scaled)
        return Grid(refined.left * scale, refined.top * scale, refined.cell * scale)
    }

    /**
     * Snaps a coarse detection onto the real square boundaries: for the right
     * alignment the lines between squares sit exactly on light/dark edges, so
     * the luminance contrast across each internal boundary is maximal.
     */
    private fun refineGrid(lum: IntArray, sw: Int, sh: Int, coarse: Grid): Grid {
        var best = coarse
        var bestScore = -1.0
        for (dc in -1..1) {
            val cell = coarse.cell + dc
            if (cell < 2 || cell * 8 > minOf(sw, sh)) continue
            val lo = cell
            for (dy in -lo..lo) {
                for (dx in -lo..lo) {
                    val tx = coarse.left + dx
                    val ty = coarse.top + dy
                    if (tx < 1 || ty < 1 || tx + cell * 8 >= sw - 1 || ty + cell * 8 >= sh - 1) continue
                    val s = boundaryScore(lum, sw, tx, ty, cell)
                    if (s > bestScore) {
                        bestScore = s
                        best = Grid(tx, ty, cell)
                    }
                }
            }
        }
        return best
    }

    /** Total luminance contrast across the 14 internal square boundaries. */
    private fun boundaryScore(lum: IntArray, sw: Int, tx: Int, ty: Int, cell: Int): Double {
        var sum = 0.0
        val side = cell * 8
        for (k in 1..7) {
            val bx = tx + k * cell
            for (y in 0 until side) {
                val row = (ty + y) * sw + bx
                sum += abs(lum[row] - lum[row - 1])
            }
            val by = ty + k * cell
            for (x in 0 until side) {
                val col = (by) * sw + tx + x
                sum += abs(lum[col] - lum[col - sw])
            }
        }
        return sum
    }

    /**
     * Scores a candidate 8x8 window: high when neighbouring square
     * luminances alternate a lot AND the 64 luminances are strongly bimodal
     * (i.e. real board squares, not a photo or a solid UI region).
     */
    private fun gridScore(lum: IntArray, sw: Int, tx: Int, ty: Int, cell: Int): Double {
        val avgs = IntArray(64)
        for (r in 0..7) {
            for (c in 0..7) {
                avgs[r * 8 + c] = lum[(ty + r * cell + cell / 2) * sw + (tx + c * cell + cell / 2)]
            }
        }
        var hAlt = 0.0
        var vAlt = 0.0
        for (r in 0..7) for (c in 0..6) hAlt += abs(avgs[r * 8 + c] - avgs[r * 8 + c + 1])
        for (c in 0..7) for (r in 0..6) vAlt += abs(avgs[r * 8 + c] - avgs[(r + 1) * 8 + c])
        val avgAlt = (hAlt + vAlt) / 112.0

        val sorted = avgs.copyOf().apply { sort() }
        var lowSum = 0L
        var highSum = 0L
        for (i in 0 until 32) {
            lowSum += sorted[i]
            highSum += sorted[63 - i]
        }
        val lowMean = lowSum / 32.0
        val highMean = highSum / 32.0
        val gap = highMean - lowMean
        if (gap < 10.0) return 0.0
        var lowVar = 0.0
        var highVar = 0.0
        for (i in 0 until 32) {
            lowVar += (sorted[i] - lowMean) * (sorted[i] - lowMean)
            highVar += (sorted[63 - i] - highMean) * (sorted[63 - i] - highMean)
        }
        val spread = sqrt(lowVar / 32.0) + sqrt(highVar / 32.0)
        val bimodal = gap / (gap + spread + 1.0)
        return avgAlt * bimodal
    }

    // ------------------------------------------------------------ squares

    private fun classifySquares(pixels: IntArray, width: Int, grid: Grid): Array<Piece?> {
        val layout = arrayOfNulls<Piece>(64)
        for (r in 0..7) {
            for (c in 0..7) {
                val base = cornerColor(pixels, width, grid, r, c)
                val centre = centreColor(pixels, width, grid, r, c)
                if (colourDistance(centre, base) >= OCCUPIED_MANHATTAN) {
                    val color = if (luminance(centre) > luminance(base)) Color.WHITE else Color.BLACK
                    layout[r * 8 + c] = Piece(PieceType.PAWN, color)
                }
            }
        }
        return layout
    }

    private fun cornerColor(pixels: IntArray, width: Int, grid: Grid, r: Int, c: Int): Int {
        val cell = grid.cell
        val k = maxOf(2, cell / 8)
        val x0 = grid.left + c * cell
        val y0 = grid.top + r * cell
        var rAcc = 0L
        var gAcc = 0L
        var bAcc = 0L
        val patches = arrayOf(0 to 0, cell - k to 0, 0 to cell - k, cell - k to cell - k)
        for ((dx, dy) in patches) {
            for (y in 0 until k) {
                for (x in 0 until k) {
                    val col = pixels[(y0 + dy + y) * width + (x0 + dx + x)]
                    rAcc += (col shr 16) and 0xFF
                    gAcc += (col shr 8) and 0xFF
                    bAcc += col and 0xFF
                }
            }
        }
        val n = patches.size * k * k
        return (0xFF shl 24) or ((rAcc / n).toInt() shl 16) or ((gAcc / n).toInt() shl 8) or (bAcc / n).toInt()
    }

    private fun centreColor(pixels: IntArray, width: Int, grid: Grid, r: Int, c: Int): Int {
        val cell = grid.cell
        val s = (cell * 0.42f).toInt().coerceAtLeast(1)
        val off = (cell - s) / 2
        val x0 = grid.left + c * cell + off
        val y0 = grid.top + r * cell + off
        var rAcc = 0L
        var gAcc = 0L
        var bAcc = 0L
        for (y in 0 until s) {
            for (x in 0 until s) {
                val col = pixels[(y0 + y) * width + (x0 + x)]
                rAcc += (col shr 16) and 0xFF
                gAcc += (col shr 8) and 0xFF
                bAcc += col and 0xFF
            }
        }
        val n = s * s
        return (0xFF shl 24) or ((rAcc / n).toInt() shl 16) or ((gAcc / n).toInt() shl 8) or (bAcc / n).toInt()
    }

    // ---------------------------------------------------------- templates

    private fun seedTemplate(
        pixels: IntArray,
        width: Int,
        grid: Grid,
        template: BoardTemplate,
        startLayout: Array<Piece?>,
    ) {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = startLayout[r * 8 + c] ?: continue
                template.register(piece.fenChar, cropTile(pixels, width, grid, r, c))
            }
        }
    }

    private fun applyTemplates(
        pixels: IntArray,
        width: Int,
        grid: Grid,
        colourLayout: Array<Piece?>,
        template: BoardTemplate,
    ): Array<Piece?>? {
        val out = arrayOfNulls<Piece>(64)
        for (r in 0..7) {
            for (c in 0..7) {
                val hint = colourLayout[r * 8 + c] ?: continue
                val tile = cropTile(pixels, width, grid, r, c)
                val fenChar = template.match(hint.color, tile) ?: return null
                out[r * 8 + c] = Piece.fromFen(fenChar)
            }
        }
        return out
    }

    private fun cropTile(pixels: IntArray, width: Int, grid: Grid, r: Int, c: Int): IntArray {
        val cell = grid.cell
        val s = maxOf(5, (cell * 0.62f).toInt())
        val off = (cell - s) / 2
        val x0 = grid.left + c * cell + off
        val y0 = grid.top + r * cell + off
        val tile = IntArray(s * s)
        var i = 0
        for (y in 0 until s) {
            var src = (y0 + y) * width + x0
            for (x in 0 until s) tile[i++] = luminance(pixels[src++])
        }
        return tile
    }

    /** Stores one normalised piece tile per piece, keyed by FEN character. */
    class BoardTemplate {
        private val tiles = HashMap<Char, IntArray>()
        private var tileSize = 0

        val configured: Boolean get() = tiles.size >= 8
        val size: Int get() = tiles.size

        fun register(fenChar: Char, tile: IntArray) {
            if (tiles.isEmpty()) tileSize = tile.size
            tiles[fenChar] = tile
        }

        fun match(pieceColor: Color, tile: IntArray): Char? {
            if (tile.size != tileSize) return null
            var best = MIN_PIECE_CORRELATION
            var bestChar: Char? = null
            for ((fenChar, t) in tiles) {
                val isWhite = fenChar.isUpperCase()
                if (isWhite != (pieceColor == Color.WHITE)) continue
                val corr = ncc(t, tile)
                if (corr > best) {
                    best = corr
                    bestChar = fenChar
                }
            }
            return bestChar
        }

        private fun ncc(a: IntArray, b: IntArray): Double {
            val n = a.size
            var sumA = 0L
            var sumB = 0L
            for (i in 0 until n) {
                sumA += a[i]
                sumB += b[i]
            }
            val meanA = sumA / n.toDouble()
            val meanB = sumB / n.toDouble()
            var num = 0.0
            var devA = 0.0
            var devB = 0.0
            for (i in 0 until n) {
                val da = a[i] - meanA
                val db = b[i] - meanB
                num += da * db
                devA += da * da
                devB += db * db
            }
            if (devA == 0.0 || devB == 0.0) return 0.0
            return num / sqrt(devA * devB)
        }
    }

    // ------------------------------------------------------------- helpers

    private fun orientToStandard(layout: Array<Piece?>) {
        var whiteTop = 0
        var blackTop = 0
        for (c in 0..7) {
            when (layout[c]?.color) {
                Color.WHITE -> whiteTop++
                Color.BLACK -> blackTop++
                null -> {}
            }
        }
        if (whiteTop > blackTop) {
            for (r in 0..3) {
                for (c in 0..7) {
                    val tmp = layout[r * 8 + c]
                    layout[r * 8 + c] = layout[(7 - r) * 8 + c]
                    layout[(7 - r) * 8 + c] = tmp
                }
            }
        }
    }

    private fun matchesStart(layout: Array<Piece?>): Boolean {
        val backRank: (Int, Color) -> Boolean = { rank, color ->
            (0..7).all { layout[rank * 8 + it]?.color == color }
        }
        val middle = (2..5).all { r -> (0..7).all { layout[r * 8 + it] == null } }
        return backRank(0, Color.BLACK) && backRank(1, Color.BLACK) &&
            backRank(6, Color.WHITE) && backRank(7, Color.WHITE) && middle
    }

    private fun fillStart(layout: Array<Piece?>) {
        val start = START_PIECES
        for (i in 0 until 64) layout[i] = start[i]
    }

    private fun inferMover(prev: Array<Piece?>, cur: Array<Piece?>): Color? {
        var added: Color? = null
        var removed: Color? = null
        for (i in 0 until 64) {
            val a = cur[i]
            val b = prev[i]
            if (a != b) {
                if (a != null) added = a.color
                if (b != null) removed = b.color
            }
        }
        return added ?: removed?.opposite
    }

    private fun luminance(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun colourDistance(a: Int, b: Int): Int {
        val dr = abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF))
        val dg = abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF))
        val db = abs((a and 0xFF) - (b and 0xFF))
        return dr + dg + db
    }

    // -------------------------------------------------------------- static

    companion object {
        private const val OCCUPIED_MANHATTAN = 96
        private const val MIN_PIECE_CORRELATION = 0.55

        private val START_PIECES: Array<Piece?> = run {
            val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"
            val ranks = fen.split('/')
            val layout = arrayOfNulls<Piece>(64)
            for (r in 0..7) {
                var file = 0
                for (ch in ranks[r]) {
                    if (ch.isDigit()) {
                        file += ch - '0'
                    } else {
                        layout[r * 8 + file] = Piece.fromFen(ch)
                        file++
                    }
                }
            }
            layout
        }

        /** Renders a FEN-compatible layout to FEN. Castling/ep are unknown. */
        fun fen(layout: Array<Piece?>, side: Color): String {
            val sb = StringBuilder()
            for (r in 0..7) {
                var empty = 0
                for (c in 0..7) {
                    val p = layout[r * 8 + c]
                    if (p == null) {
                        empty++
                    } else {
                        if (empty > 0) {
                            sb.append(empty)
                            empty = 0
                        }
                        sb.append(p.fenChar)
                    }
                }
                if (empty > 0) sb.append(empty)
                if (r < 7) sb.append('/')
            }
            sb.append(' ').append(side.fenSymbol)
            sb.append(" - 0 1")
            return sb.toString()
        }
    }
}
