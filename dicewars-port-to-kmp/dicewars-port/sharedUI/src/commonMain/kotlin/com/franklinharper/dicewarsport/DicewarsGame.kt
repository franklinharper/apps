package com.franklinharper.dicewarsport

data class DicewarsGame(
    val pmax: Int = 7,
    val user: Int = 0,
    val cells: List<Int> = List(XMAX * YMAX) { 0 },
    val cellNeighbors: List<CellNeighbors> = List(XMAX * YMAX) { cellIndex ->
        CellNeighbors(List(6) { direction -> nextCell(cellIndex, direction) })
    },
    val areas: List<AreaData> = List(AREA_MAX) { AreaData() },
    val players: List<PlayerData> = List(8) { PlayerData() },
    val turnOrder: List<Int> = List(8) { it },
    val turnIndex: Int = 0,
    val history: List<HistoryData> = emptyList(),
) {
    companion object {
        const val XMAX: Int = 28
        const val YMAX: Int = 32
        const val AREA_MAX: Int = 32
        const val STOCK_MAX: Int = 64
        const val MAX_DICE: Int = 8
        const val AVERAGE_DICE_PLACEMENT: Int = 3

        fun nextCell(position: Int, direction: Int): Int {
            val originX = position % XMAX
            val originY = position / XMAX
            val rowParity = originY % 2
            val deltaX: Int
            val deltaY: Int
            when (direction) {
                0 -> { deltaX = rowParity; deltaY = -1 }
                1 -> { deltaX = 1; deltaY = 0 }
                2 -> { deltaX = rowParity; deltaY = 1 }
                3 -> { deltaX = rowParity - 1; deltaY = 1 }
                4 -> { deltaX = -1; deltaY = 0 }
                5 -> { deltaX = rowParity - 1; deltaY = -1 }
                else -> return -1
            }
            val x = originX + deltaX
            val y = originY + deltaY
            if (x < 0 || y < 0 || x >= XMAX || y >= YMAX) return -1
            return y * XMAX + x
        }

        fun generate(pmax: Int, random: RandomSource, user: Int = 0): DicewarsGame =
            DicewarsMapGenerator.generate(pmax = pmax, random = random, user = user)
    }

    fun currentPlayer(): Int = turnOrder[turnIndex]
}
