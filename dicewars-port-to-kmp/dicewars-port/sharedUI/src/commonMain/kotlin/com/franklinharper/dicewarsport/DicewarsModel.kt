package com.franklinharper.dicewarsport

interface RandomSource {
    fun nextInt(bound: Int): Int
}

data class AreaData(
    var size: Int = 0,
    var centerPos: Int = 0,
    var owner: Int = 0,
    var dice: Int = 0,
    var left: Int = 0,
    var right: Int = 0,
    var top: Int = 0,
    var bottom: Int = 0,
    var centerX: Int = 0,
    var centerY: Int = 0,
    var minDistance: Int = 0,
    val lineCells: IntArray = IntArray(100),
    val lineDirections: IntArray = IntArray(100),
    val adjacentAreas: IntArray = IntArray(DicewarsGame.AREA_MAX),
)

data class PlayerData(
    var areaCount: Int = 0,
    var maxConnectedAreaCount: Int = 0,
    var diceCount: Int = 0,
    var diceRank: Int = 0,
    var stock: Int = 0,
)

data class CellNeighbors(
    val directions: IntArray = IntArray(6),
)

data class HistoryData(
    val from: Int = 0,
    val to: Int = 0,
    val result: Int = 0,
)

class DicewarsGame {
    companion object {
        const val XMAX: Int = 28
        const val YMAX: Int = 32
        const val AREA_MAX: Int = 32
        const val STOCK_MAX: Int = 64
        const val MAX_DICE: Int = 8
    }

    val cellMax: Int = XMAX * YMAX
    val cells: IntArray = IntArray(cellMax)
    val cellNeighbors: List<CellNeighbors> = List(cellMax) { cellIndex ->
        CellNeighbors(IntArray(6) { direction -> nextCell(cellIndex, direction) })
    }
    val areas: MutableList<AreaData> = MutableList(AREA_MAX) { AreaData() }
    val num: IntArray = IntArray(cellMax) { it }
    val adjacentCells: IntArray = IntArray(cellMax)
    val nextFlags: IntArray = IntArray(cellMax)
    val areaList: IntArray = IntArray(AREA_MAX)
    val check: IntArray = IntArray(AREA_MAX)
    val connectedAreaCounts: IntArray = IntArray(AREA_MAX)

    var pmax: Int = 7
    var user: Int = 0
    var averageDicePlacement: Int = 3
    val turnOrder: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
    var turnIndex: Int = 0
    var areaFrom: Int = 0
    var areaTo: Int = 0
    var defeat: Int = 0

    val players: MutableList<PlayerData> = MutableList(8) { PlayerData() }
    val listFrom: IntArray = IntArray(AREA_MAX * AREA_MAX)
    val listTo: IntArray = IntArray(AREA_MAX * AREA_MAX)
    val history: MutableList<HistoryData> = mutableListOf()
    val initialOwners: IntArray = IntArray(AREA_MAX)
    val initialDice: IntArray = IntArray(AREA_MAX)

    fun nextCell(position: Int, direction: Int): Int {
        val originX = position % XMAX
        val originY = position / XMAX
        val rowParity = originY % 2
        var deltaX = 0
        var deltaY = 0

        when (direction) {
            0 -> {
                deltaX = rowParity
                deltaY = -1
            }
            1 -> deltaX = 1
            2 -> {
                deltaX = rowParity
                deltaY = 1
            }
            3 -> {
                deltaX = rowParity - 1
                deltaY = 1
            }
            4 -> deltaX = -1
            5 -> {
                deltaX = rowParity - 1
                deltaY = -1
            }
            else -> return -1
        }

        val x = originX + deltaX
        val y = originY + deltaY
        if (x < 0 || y < 0 || x >= XMAX || y >= YMAX) return -1
        return y * XMAX + x
    }

    fun currentPlayer(): Int = turnOrder[turnIndex]
}
