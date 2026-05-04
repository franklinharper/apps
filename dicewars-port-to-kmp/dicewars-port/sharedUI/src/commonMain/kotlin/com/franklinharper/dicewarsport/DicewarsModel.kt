package com.franklinharper.dicewarsport

interface RandomSource {
    fun nextInt(bound: Int): Int
}

data class AreaData(
    val size: Int = 0,
    val centerPos: Int = 0,
    val owner: Int = 0,
    val dice: Int = 0,
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
    val centerX: Int = 0,
    val centerY: Int = 0,
    val minDistance: Int = 0,
    val lineCells: List<Int> = emptyList(),
    val lineDirections: List<Int> = emptyList(),
    val adjacentAreas: List<Int> = List(DicewarsGame.AREA_MAX) { 0 },
)

data class PlayerData(
    val areaCount: Int = 0,
    val maxConnectedAreaCount: Int = 0,
    val diceCount: Int = 0,
    val diceRank: Int = 0,
    val stock: Int = 0,
)

data class CellNeighbors(
    val directions: List<Int>,
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
    val cells: List<Int>,
    val territories: List<Territory>,
    val cellNeighbors: List<CellNeighbors>,
) {
    val width: Int = gridWidth
    val height: Int = gridHeight
}

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

        fun generate(pmax: Int, random: RandomSource, user: Int = 0): DicewarsGame {
            val cellMax = XMAX * YMAX
            val cells = MutableList(cellMax) { 0 }
            val cellNeighbors = List(cellMax) { cellIndex ->
                CellNeighbors(List(6) { direction -> nextCell(cellIndex, direction) })
            }
            val areas = MutableList(AREA_MAX) { AreaData() }
            val num = MutableList(cellMax) { it }
            val adjacentCells = MutableList(cellMax) { 0 }
            val areaList = MutableList(AREA_MAX) { 0 }
            val players = MutableList(8) { PlayerData() }
            val turnOrder = MutableList(8) { it }
            val connectedAreaCounts = MutableList(AREA_MAX) { 0 }

            // Fisher-Yates shuffle
            for (i in cellMax - 1 downTo 0) {
                val r = random.nextInt(i + 1)
                val tmp = num[i]; num[i] = num[r]; num[r] = tmp
            }
            val shuffleOrder = List(cellMax) { i -> num.indexOf(i) }

            // Reset
            for (i in 0 until cellMax) {
                cells[i] = 0
                adjacentCells[i] = 0
            }
            for (i in 0 until AREA_MAX) areas[i] = AreaData()

            // Grow territories
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

                // growTerritory
                val nextCells = BooleanArray(cellMax) { false }
                var currentCell = seedCell
                var size = 0
                while (size < 8) {
                    cells[currentCell] = areaNumber
                    size++
                    val neighbors = cellNeighbors[currentCell].directions
                    for (neighbor in neighbors) {
                        if (neighbor >= 0) nextCells[neighbor] = true
                    }
                    var nextCell = -1
                    var lowest = Int.MAX_VALUE
                    for (neighbor in neighbors) {
                        if (neighbor >= 0 && cells[neighbor] == 0 && shuffleOrder[neighbor] < lowest) {
                            nextCell = neighbor
                            lowest = shuffleOrder[neighbor]
                        }
                    }
                    if (nextCell == -1) break
                    currentCell = nextCell
                }
                for (i in 0 until cellMax) {
                    if (!nextCells[i]) continue
                    if (cells[i] > 0) continue
                    cells[i] = areaNumber
                    val neighbors = cellNeighbors[i].directions
                    for (neighbor in neighbors) {
                        if (neighbor >= 0) adjacentCells[neighbor] = 1
                    }
                }
                areaNumber++
            }

            // Remove single-cell seas
            for (i in 0 until cellMax) {
                if (cells[i] > 0) continue
                var hasSeaNeighbor = false
                var adjacentArea = 0
                for (direction in 0 until 6) {
                    val position = cellNeighbors[i].directions[direction]
                    if (position < 0) continue
                    if (cells[position] == 0) hasSeaNeighbor = true
                    else adjacentArea = cells[position]
                }
                if (!hasSeaNeighbor) cells[i] = adjacentArea
            }

            // Reset areas
            for (i in 0 until AREA_MAX) areas[i] = AreaData()

            // Calculate area sizes
            for (i in 0 until cellMax) {
                val an = cells[i]
                if (an > 0) areas[an] = areas[an].copy(size = areas[an].size + 1)
            }

            // Remove small areas
            for (i in 1 until AREA_MAX) {
                if (areas[i].size in 1..5) areas[i] = areas[i].copy(size = 0)
            }
            for (i in 0 until cellMax) {
                val an = cells[i]
                if (areas[an].size == 0) cells[i] = 0
            }

            // Calculate bounding boxes
            for (i in 1 until AREA_MAX) {
                areas[i] = areas[i].copy(
                    left = XMAX, right = -1, top = YMAX, bottom = -1,
                    minDistance = 9999,
                    adjacentAreas = List(AREA_MAX) { 0 },
                )
            }
            var cellIndex = 0
            for (y in 0 until YMAX) {
                for (x in 0 until XMAX) {
                    val an = cells[cellIndex]
                    if (an > 0) {
                        val area = areas[an]
                        areas[an] = area.copy(
                            left = minOf(area.left, x),
                            right = maxOf(area.right, x),
                            top = minOf(area.top, y),
                            bottom = maxOf(area.bottom, y),
                        )
                    }
                    cellIndex++
                }
            }

            // Calculate centers
            for (i in 1 until AREA_MAX) {
                areas[i] = areas[i].copy(
                    centerX = (areas[i].left + areas[i].right) / 2,
                    centerY = (areas[i].top + areas[i].bottom) / 2,
                )
            }

            // Calculate adjacency and center positions
            cellIndex = 0
            for (y in 0 until YMAX) {
                for (x in 0 until XMAX) {
                    val an = cells[cellIndex]
                    if (an > 0) {
                        val area = areas[an]
                        var distance = kotlin.math.abs(area.centerX - x) + kotlin.math.abs(area.centerY - y)
                        var boundary = false
                        val mutableAdj = area.adjacentAreas.toMutableList()
                        for (direction in 0 until 6) {
                            val position = cellNeighbors[cellIndex].directions[direction]
                            if (position > 0) {
                                val adjacentArea = cells[position]
                                if (adjacentArea != an) {
                                    boundary = true
                                    if (adjacentArea > 0) mutableAdj[adjacentArea] = 1
                                }
                            }
                        }
                        if (boundary) distance += 4
                        if (distance < area.minDistance) {
                            areas[an] = area.copy(
                                minDistance = distance,
                                centerPos = cellIndex,
                                adjacentAreas = mutableAdj.toList(),
                            )
                        } else {
                            areas[an] = area.copy(adjacentAreas = mutableAdj.toList())
                        }
                    }
                    cellIndex++
                }
            }

            // Assign owners
            for (i in 0 until AREA_MAX) areas[i] = areas[i].copy(owner = -1)
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
                val chosenArea = areaList[random.nextInt(count)]
                areas[chosenArea] = areas[chosenArea].copy(owner = owner)
                owner++
                if (owner >= pmax) owner = 0
            }

            // Create area lines
            val lineCheck = MutableList(AREA_MAX) { 0 }
            for (i in 0 until cellMax) {
                val area = cells[i]
                if (area == 0) continue
                if (lineCheck[area] > 0) continue
                for (direction in 0 until 6) {
                    if (lineCheck[area] > 0) break
                    val neighbor = cellNeighbors[i].directions[direction]
                    if (neighbor >= 0 && cells[neighbor] != area) {
                        val lineCells = mutableListOf<Int>()
                        val lineDirections = mutableListOf<Int>()
                        var cell = i
                        var dir = direction
                        lineCells.add(cell)
                        lineDirections.add(dir)
                        for (iter in 0 until 100) {
                            dir++
                            if (dir >= 6) dir = 0
                            val n = cellNeighbors[cell].directions[dir]
                            if (n >= 0 && cells[n] == area) {
                                cell = n
                                dir -= 2
                                if (dir < 0) dir += 6
                            }
                            lineCells.add(cell)
                            lineDirections.add(dir)
                            if (cell == i && dir == direction) break
                        }
                        areas[area] = areas[area].copy(
                            lineCells = lineCells.toList(),
                            lineDirections = lineDirections.toList(),
                        )
                        lineCheck[area] = 1
                    }
                }
            }

            // Place dice
            val activeAreaIds = mutableListOf<Int>()
            val playerAreaCounts = MutableList(pmax) { 0 }
            for (an in 1 until AREA_MAX) {
                val area = areas[an]
                if (area.size == 0) continue
                activeAreaIds.add(an)
                areas[an] = area.copy(dice = 1)
                if (area.owner in 0 until pmax) playerAreaCounts[area.owner]++
            }

            val desiredTotalArmies = activeAreaIds.size * AVERAGE_DICE_PLACEMENT
            val targetArmiesPerPlayer = maxOf(
                playerAreaCounts.maxOrNull() ?: 0,
                (desiredTotalArmies + pmax - 1) / pmax,
            )

            fun playerDiceCount(player: Int): Int {
                var count = 0
                for (an in 1 until AREA_MAX) {
                    val area = areas[an]
                    if (area.size == 0 || area.owner != player) continue
                    count += area.dice
                }
                return count
            }

            for (player in 0 until pmax) {
                while (playerDiceCount(player) < targetArmiesPerPlayer) {
                    val playerAreas = activeAreaIds.filter { an ->
                        areas[an].owner == player && areas[an].dice < MAX_DICE
                    }
                    if (playerAreas.isEmpty()) break
                    val chosen = playerAreas[random.nextInt(playerAreas.size)]
                    areas[chosen] = areas[chosen].copy(dice = areas[chosen].dice + 1)
                }
            }

            // Shuffle turn order
            for (i in turnOrder.indices) turnOrder[i] = i
            for (i in pmax - 1 downTo 0) {
                val r = random.nextInt(i + 1)
                val tmp = turnOrder[i]; turnOrder[i] = turnOrder[r]; turnOrder[r] = tmp
            }

            // Ensure symmetric adjacency
            for (an in 1 until AREA_MAX) {
                if (areas[an].size == 0) continue
                for (adj in 1 until AREA_MAX) {
                    if (areas[an].adjacentAreas[adj] == 0) continue
                    if (areas[adj].size == 0) continue
                    val otherAdj = areas[adj].adjacentAreas.toMutableList()
                    otherAdj[an] = 1
                    areas[adj] = areas[adj].copy(adjacentAreas = otherAdj.toList())
                }
            }

            // setAreaTc for all players
            fun localSetAreaTc(player: Int) {
                val localCheck = MutableList(AREA_MAX) { it }
                while (true) {
                    var changed = false
                    run loop@{
                        for (i in 1 until AREA_MAX) {
                            if (areas[i].size == 0 || areas[i].owner != player) continue
                            for (j in 1 until AREA_MAX) {
                                if (areas[j].size == 0 || areas[j].owner != player) continue
                                if (areas[i].adjacentAreas[j] == 0) continue
                                if (localCheck[j] == localCheck[i]) continue
                                if (localCheck[i] > localCheck[j]) localCheck[i] = localCheck[j]
                                else localCheck[j] = localCheck[i]
                                changed = true
                                return@loop
                            }
                        }
                    }
                    if (!changed) break
                }
                for (i in 0 until AREA_MAX) connectedAreaCounts[i] = 0
                var areaCount = 0
                var diceCount = 0
                for (i in 1 until AREA_MAX) {
                    if (areas[i].size == 0 || areas[i].owner != player) continue
                    connectedAreaCounts[localCheck[i]]++
                    areaCount++
                    diceCount += areas[i].dice
                }
                var maxConnected = 0
                for (i in 0 until AREA_MAX) {
                    if (connectedAreaCounts[i] > maxConnected) maxConnected = connectedAreaCounts[i]
                }
                players[player] = players[player].copy(
                    areaCount = areaCount,
                    diceCount = diceCount,
                    maxConnectedAreaCount = maxConnected,
                )
            }

            for (player in 0 until pmax) localSetAreaTc(player)

            return DicewarsGame(
                pmax = pmax,
                user = user,
                cells = cells.toList(),
                cellNeighbors = cellNeighbors,
                areas = areas.toList(),
                players = players.toList(),
                turnOrder = turnOrder.toList(),
                turnIndex = 0,
            )
        }
    }

    fun currentPlayer(): Int = turnOrder[turnIndex]
}

fun DicewarsGame.toRenderMap(): GameMap = GameMap(
    gridWidth = DicewarsGame.XMAX,
    gridHeight = DicewarsGame.YMAX,
    maxTerritories = DicewarsGame.AREA_MAX,
    cells = cells,
    territories = (1 until DicewarsGame.AREA_MAX).map { areaNumber ->
        val area = areas[areaNumber]
        Territory(
            id = areaNumber,
            owner = area.owner,
            armyCount = area.dice,
            centerPos = area.centerPos,
            size = area.size,
            adjacentTerritories = area.adjacentAreas.mapIndexedNotNull { index, value ->
                if (index > 0 && value != 0 && areas[index].size > 0) index else null
            },
        )
    },
    cellNeighbors = cellNeighbors,
)
