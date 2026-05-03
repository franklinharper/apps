package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DicewarsMapGenerationTest {

    @Test
    fun generatedMapHasValidActiveTerritories() {
        val game = DicewarsGame()
        val map = game.makeMap(SequenceRandomSource())

        val activeTerritories = map.territories.filter { it.size > 0 }
        assertTrue(activeTerritories.isNotEmpty())
        activeTerritories.forEach { territory ->
            assertTrue(territory.size > 0)
            assertTrue(territory.armyCount in 1..DicewarsGame.MAX_DICE)
            assertTrue(territory.owner in 0 until game.pmax)
        }
    }

    @Test
    fun adjacencyIsSymmetric() {
        val game = DicewarsGame()
        val map = game.makeMap(SequenceRandomSource())

        map.territories.forEachIndexed { index, territory ->
            val areaId = index + 1
            territory.adjacentTerritories.forEach { adjacentAreaId ->
                assertTrue(adjacentAreaId in 1 until DicewarsGame.AREA_MAX)
                val adjacent = map.territories[adjacentAreaId - 1]
                assertTrue(
                    areaId in adjacent.adjacentTerritories,
                    "area $areaId should be in area $adjacentAreaId adjacency list",
                )
            }
        }
    }

    @Test
    fun everyActiveCellReferencesAnActiveTerritoryUsingJsAreaIds() {
        val game = DicewarsGame()
        val map = game.makeMap(SequenceRandomSource())

        map.cells.forEach { areaId ->
            if (areaId > 0) {
                assertTrue(areaId in 1 until DicewarsGame.AREA_MAX)
                assertTrue(map.territories[areaId - 1].size > 0)
            }
        }
    }

    @Test
    fun adapterMapsGameFieldsToRendererModelConsistently() {
        val game = DicewarsGame()
        game.makeMap(SequenceRandomSource())

        val map = game.toRenderMap()

        assertEquals(DicewarsGame.XMAX, map.width)
        assertEquals(DicewarsGame.YMAX, map.height)
        assertEquals(game.cells.toList(), map.cells.toList())
        assertEquals(game.cellNeighbors[29].directions.toList(), map.cellNeighbors[29].directions.toList())

        for (areaId in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaId]
            val territory = map.territories[areaId - 1]
            assertEquals(area.owner, territory.owner)
            assertEquals(area.dice, territory.armyCount)
            assertEquals(area.centerPos, territory.centerPos)
            assertEquals(area.size, territory.size)
            assertEquals(
                area.adjacentAreas.withIndex().filter { it.index > 0 && it.value != 0 }.map { it.index },
                territory.adjacentTerritories,
            )
        }
    }
}

private class SequenceRandomSource : RandomSource {
    private var next = 0

    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        val value = next % bound
        next++
        return value
    }
}
