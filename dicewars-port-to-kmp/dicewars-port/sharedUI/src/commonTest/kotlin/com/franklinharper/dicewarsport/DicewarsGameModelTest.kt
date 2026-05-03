package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class DicewarsGameModelTest {

    @Test
    fun constantsMatchOriginalJs() {
        assertEquals(28, DicewarsGame.XMAX)
        assertEquals(32, DicewarsGame.YMAX)
        assertEquals(32, DicewarsGame.AREA_MAX)
        assertEquals(64, DicewarsGame.STOCK_MAX)
        assertEquals(8, DicewarsGame.MAX_DICE)
    }

    @Test
    fun defaultsMatchOriginalJs() {
        val game = DicewarsGame()

        assertEquals(7, game.pmax)
        assertEquals(0, game.user)
    }

    @Test
    fun cellNeighborCalculationMatchesJsNextCel() {
        val game = DicewarsGame()

        assertEquals(-1, game.nextCell(0, 0))
        assertEquals(1, game.nextCell(0, 1))
        assertEquals(28, game.nextCell(0, 2))
        assertEquals(-1, game.nextCell(0, 3))
        assertEquals(-1, game.nextCell(0, 4))
        assertEquals(-1, game.nextCell(0, 5))

        assertEquals(1, game.nextCell(28, 0))
        assertEquals(29, game.nextCell(28, 1))
        assertEquals(57, game.nextCell(28, 2))
        assertEquals(56, game.nextCell(28, 3))
        assertEquals(-1, game.nextCell(28, 4))
        assertEquals(0, game.nextCell(28, 5))

        assertEquals(-1, game.nextCell(55, 0))
        assertEquals(-1, game.nextCell(55, 1))
        assertEquals(-1, game.nextCell(55, 2))
        assertEquals(83, game.nextCell(55, 3))
        assertEquals(54, game.nextCell(55, 4))
        assertEquals(27, game.nextCell(55, 5))
    }

    @Test
    fun precomputedCellNeighborsUseNextCell() {
        val game = DicewarsGame()

        assertEquals((0 until 6).map { game.nextCell(29, it) }, game.cellNeighbors[29].directions.toList())
    }
}
