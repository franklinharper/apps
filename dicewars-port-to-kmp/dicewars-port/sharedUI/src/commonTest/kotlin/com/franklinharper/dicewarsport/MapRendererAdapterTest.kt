package com.franklinharper.dicewarsport

import com.franklinharper.dicewarsport.presentation.components.computeTerritoryLabelPositionsForTest
import com.franklinharper.dicewarsport.presentation.components.findDicewarsTerritoryAtPositionForTest
import com.franklinharper.dicewarsport.presentation.components.visibleDiceCountLabelsForTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MapRendererAdapterTest {

    @Test
    fun generatedMapExposesRendererRequiredFields() {
        val map = DicewarsGame().makeMap(RendererTestRandomSource())

        assertEquals(DicewarsGame.XMAX, map.gridWidth)
        assertEquals(DicewarsGame.YMAX, map.gridHeight)
        assertEquals(DicewarsGame.AREA_MAX, map.maxTerritories)
        assertEquals(DicewarsGame.XMAX * DicewarsGame.YMAX, map.cells.size)
        assertEquals(DicewarsGame.XMAX * DicewarsGame.YMAX, map.cellNeighbors.size)
        assertEquals(DicewarsGame.AREA_MAX - 1, map.territories.size)
        assertNotNull(map.territories.first().id)
    }

    @Test
    fun labelPositionHelpersDoNotCrashForEmptyTerritories() {
        val map = DicewarsGame().makeMap(RendererTestRandomSource())

        val positions = computeTerritoryLabelPositionsForTest(map, cellWidth = 27f, cellHeight = 18f)

        assertEquals(map.territories.size, positions.size)
    }

    @Test
    fun clickMappingReturnsExpectedJsAreaIdThroughAdapter() {
        val map = DicewarsGame().makeMap(RendererTestRandomSource())
        val activeCellIndex = map.cells.indexOfFirst { it > 0 }
        val expectedAreaId = map.cells[activeCellIndex]
        val (cellX, cellY) = HexGrid.getCellPosition(activeCellIndex, 27f, 18f)
        val (centerX, centerY) = HexGeometry.getHexCenter(cellX, cellY, 27f, 18f)

        val clickedAreaId = findDicewarsTerritoryAtPositionForTest(centerX, centerY, 27f, 18f, map)

        assertEquals(expectedAreaId, clickedAreaId)
    }

    @Test
    fun diceCountLabelsAreVisibleForActiveTerritories() {
        val map = DicewarsGame().makeMap(RendererTestRandomSource())

        val labels = visibleDiceCountLabelsForTest(map)

        assertTrue(labels.isNotEmpty())
        labels.forEach { label -> assertTrue(label.text.toInt() in 1..DicewarsGame.MAX_DICE) }
    }
}

private class RendererTestRandomSource : RandomSource {
    private var next = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = (next * 17 + 3) % bound
        next++
        return value
    }
}
