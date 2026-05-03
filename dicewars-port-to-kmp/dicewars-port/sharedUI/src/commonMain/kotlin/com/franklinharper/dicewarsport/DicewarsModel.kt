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

data class Territory(
    val id: Int,
    val owner: Int,
    val armyCount: Int,
    val centerPos: Int,
    val size: Int,
    val adjacentTerritories: List<Int>,
)

data class GameMap(
    val gridWidth: Int,
    val gridHeight: Int,
    val maxTerritories: Int,
    val cells: IntArray,
    val territories: List<Territory>,
    val cellNeighbors: List<CellNeighbors>,
) {
    val width: Int = gridWidth
    val height: Int = gridHeight
}

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

    fun makeMap(random: RandomSource): GameMap {
        // Correct Fisher-Yates shuffle
        for (i in 0 until cellMax) num[i] = i
        for (i in cellMax - 1 downTo 0) {
            val r = random.nextInt(i + 1)
            val tmp = num[i]
            num[i] = num[r]
            num[r] = tmp
        }
        // shuffleOrder[i] = position of value i in the shuffled array
        val shuffleOrder = IntArray(cellMax) { i -> num.indexOf(i) }

        for (i in 0 until cellMax) {
            cells[i] = 0
            adjacentCells[i] = 0
        }
        resetAreas()

        var areaNumber = 1
        adjacentCells[random.nextInt(cellMax)] = 1

        while (areaNumber < AREA_MAX) {
            var seedCell = -1
            var lowestShuffle = Int.MAX_VALUE
            for (i in 0 until cellMax) {
                if (cells[i] > 0) continue
                if (adjacentCells[i] == 0) continue
                if (shuffleOrder[i] >= lowestShuffle) continue
                lowestShuffle = shuffleOrder[i]
                seedCell = i
            }
            if (seedCell == -1) break

            growTerritory(seedCell, areaNumber, shuffleOrder)
            areaNumber++
        }

        removeSingleCellSeas()
        resetAreas()
        calculateAreaSizes()
        removeSmallAreas()
        calculateCentersAndAdjacency()
        assignOwners(random)
        createAreaLines()
        placeDice(random)
        ensureSymmetricAdjacency()
        for (player in 0 until pmax) setAreaTc(player)

        return toRenderMap()
    }

    private fun growTerritory(seedCell: Int, areaNumber: Int, shuffleOrder: IntArray) {
        val nextCells = BooleanArray(cellMax) { false }
        var currentCell = seedCell
        var size = 0

        // Phase 1: Grow to target size following a path from the current cell
        while (size < 8) {
            cells[currentCell] = areaNumber
            size++

            val neighbors = cellNeighbors[currentCell].directions
            for (neighbor in neighbors) {
                if (neighbor >= 0) nextCells[neighbor] = true
            }

            var nextCell = -1
            var lowestShuffle = Int.MAX_VALUE
            for (neighbor in neighbors) {
                if (neighbor >= 0 && cells[neighbor] == 0 && shuffleOrder[neighbor] < lowestShuffle) {
                    nextCell = neighbor
                    lowestShuffle = shuffleOrder[neighbor]
                }
            }

            if (nextCell == -1) break
            currentCell = nextCell
        }

        // Phase 2: Add all remaining adjacent unassigned cells
        for (i in 0 until cellMax) {
            if (!nextCells[i]) continue
            if (cells[i] > 0) continue
            cells[i] = areaNumber

            val neighbors = cellNeighbors[i].directions
            for (neighbor in neighbors) {
                if (neighbor >= 0) adjacentCells[neighbor] = 1
            }
        }
    }

    private fun removeSingleCellSeas() {
        for (i in 0 until cellMax) {
            if (cells[i] > 0) continue
            var hasSeaNeighbor = false
            var adjacentArea = 0
            for (direction in 0 until 6) {
                val position = cellNeighbors[i].directions[direction]
                if (position < 0) continue
                if (cells[position] == 0) {
                    hasSeaNeighbor = true
                } else {
                    adjacentArea = cells[position]
                }
            }
            if (!hasSeaNeighbor) cells[i] = adjacentArea
        }
    }

    private fun resetAreas() {
        for (i in 0 until AREA_MAX) {
            areas[i] = AreaData()
        }
    }

    private fun calculateAreaSizes() {
        for (i in 0 until cellMax) {
            val areaNumber = cells[i]
            if (areaNumber > 0) areas[areaNumber].size++
        }
    }

    private fun removeSmallAreas() {
        for (i in 1 until AREA_MAX) {
            if (areas[i].size <= 5) areas[i].size = 0
        }
        for (i in 0 until cellMax) {
            val areaNumber = cells[i]
            if (areas[areaNumber].size == 0) cells[i] = 0
        }
    }

    private fun calculateCentersAndAdjacency() {
        for (i in 1 until AREA_MAX) {
            areas[i].left = XMAX
            areas[i].right = -1
            areas[i].top = YMAX
            areas[i].bottom = -1
            areas[i].minDistance = 9999
            areas[i].adjacentAreas.fill(0)
        }

        var cellIndex = 0
        for (y in 0 until YMAX) {
            for (x in 0 until XMAX) {
                val areaNumber = cells[cellIndex]
                if (areaNumber > 0) {
                    val area = areas[areaNumber]
                    if (x < area.left) area.left = x
                    if (x > area.right) area.right = x
                    if (y < area.top) area.top = y
                    if (y > area.bottom) area.bottom = y
                }
                cellIndex++
            }
        }

        for (i in 1 until AREA_MAX) {
            areas[i].centerX = (areas[i].left + areas[i].right) / 2
            areas[i].centerY = (areas[i].top + areas[i].bottom) / 2
        }

        cellIndex = 0
        for (y in 0 until YMAX) {
            for (x in 0 until XMAX) {
                val areaNumber = cells[cellIndex]
                if (areaNumber > 0) {
                    val area = areas[areaNumber]
                    var distance = kotlin.math.abs(area.centerX - x) + kotlin.math.abs(area.centerY - y)
                    var boundary = false
                    for (direction in 0 until 6) {
                        val position = cellNeighbors[cellIndex].directions[direction]
                        if (position > 0) {
                            val adjacentArea = cells[position]
                            if (adjacentArea != areaNumber) {
                                boundary = true
                                if (adjacentArea > 0) area.adjacentAreas[adjacentArea] = 1
                            }
                        }
                    }
                    if (boundary) distance += 4
                    if (distance < area.minDistance) {
                        area.minDistance = distance
                        area.centerPos = cellIndex
                    }
                }
                cellIndex++
            }
        }
    }

    private fun assignOwners(random: RandomSource) {
        for (i in 0 until AREA_MAX) areas[i].owner = -1
        var owner = 0
        while (true) {
            var count = 0
            for (i in 1 until AREA_MAX) {
                if (areas[i].size == 0) continue
                if (areas[i].owner >= 0) continue
                areaList[count] = i
                count++
            }
            if (count == 0) break
            val areaNumber = areaList[random.nextInt(count)]
            areas[areaNumber].owner = owner
            owner++
            if (owner >= pmax) owner = 0
        }
    }

    private fun createAreaLines() {
        check.fill(0)
        for (i in 0 until cellMax) {
            val area = cells[i]
            if (area == 0) continue
            if (check[area] > 0) continue
            for (direction in 0 until 6) {
                if (check[area] > 0) break
                val neighbor = cellNeighbors[i].directions[direction]
                if (neighbor >= 0 && cells[neighbor] != area) {
                    setAreaLine(i, direction)
                    check[area] = 1
                }
            }
        }
    }

    private fun setAreaLine(oldCell: Int, oldDirection: Int) {
        var cell = oldCell
        var direction = oldDirection
        val area = cells[cell]
        var count = 0
        areas[area].lineCells[count] = cell
        areas[area].lineDirections[count] = direction
        count++
        for (i in 0 until 100) {
            direction++
            if (direction >= 6) direction = 0
            val neighbor = cellNeighbors[cell].directions[direction]
            if (neighbor >= 0 && cells[neighbor] == area) {
                cell = neighbor
                direction -= 2
                if (direction < 0) direction += 6
            }
            if (count < 100) {
                areas[area].lineCells[count] = cell
                areas[area].lineDirections[count] = direction
                count++
            }
            if (cell == oldCell && direction == oldDirection) break
        }
    }

    private fun placeDice(random: RandomSource) {
        var activeAreaCount = 0
        for (i in 1 until AREA_MAX) {
            if (areas[i].size > 0) {
                activeAreaCount++
                areas[i].dice = 1
            }
        }

        val diceToPlace = activeAreaCount * (averageDicePlacement - 1)
        for (i in 0 until diceToPlace) {
            val player = i % pmax
            var count = 0
            for (areaNumber in 1 until AREA_MAX) {
                val area = areas[areaNumber]
                if (area.size == 0) continue
                if (area.owner != player) continue
                if (area.dice >= MAX_DICE) continue
                areaList[count] = areaNumber
                count++
            }
            if (count == 0) continue
            val areaNumber = areaList[random.nextInt(count)]
            areas[areaNumber].dice++
        }
    }

    private fun ensureSymmetricAdjacency() {
        for (areaNumber in 1 until AREA_MAX) {
            if (areas[areaNumber].size == 0) continue
            for (adjacentArea in 1 until AREA_MAX) {
                if (areas[areaNumber].adjacentAreas[adjacentArea] == 0) continue
                if (areas[adjacentArea].size == 0) continue
                areas[adjacentArea].adjacentAreas[areaNumber] = 1
            }
        }
    }
}

fun DicewarsGame.toRenderMap(): GameMap = GameMap(
    gridWidth = DicewarsGame.XMAX,
    gridHeight = DicewarsGame.YMAX,
    maxTerritories = DicewarsGame.AREA_MAX,
    cells = cells.copyOf(),
    territories = (1 until DicewarsGame.AREA_MAX).map { areaNumber ->
        val area = areas[areaNumber]
        Territory(
            id = areaNumber,
            owner = area.owner,
            armyCount = area.dice,
            centerPos = area.centerPos,
            size = area.size,
            adjacentTerritories = area.adjacentAreas.withIndex()
                .filter { it.index > 0 && it.value != 0 && areas[it.index].size > 0 }
                .map { it.index },
        )
    },
    cellNeighbors = cellNeighbors.map { CellNeighbors(it.directions.copyOf()) },
)

