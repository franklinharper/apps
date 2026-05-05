package com.franklinharper.dicewarsport

data class BattleRoll(
    val attackerDice: List<Int>,
    val defenderDice: List<Int>,
    val attackerTotal: Int,
    val defenderTotal: Int,
    val success: Boolean,
)

fun DicewarsGame.isLegalAttack(from: Int, to: Int, player: Int = currentPlayer()): Boolean {
    if (from !in 1 until DicewarsGame.AREA_MAX) return false
    if (to !in 1 until DicewarsGame.AREA_MAX) return false

    val source = areas[from]
    val target = areas[to]
    if (source.size == 0 || target.size == 0) return false
    if (source.owner != player) return false
    if (source.dice <= 1) return false
    if (target.owner == player) return false
    if (source.adjacentAreas[to] == 0 && target.adjacentAreas[from] == 0) return false

    return true
}

fun rollBattle(
    attackerDiceCount: Int,
    defenderDiceCount: Int,
    random: RandomSource,
): BattleRoll {
    val attackerDice = List(attackerDiceCount) { random.nextInt(6) + 1 }
    val defenderDice = List(defenderDiceCount) { random.nextInt(6) + 1 }
    val attackerTotal = attackerDice.sum()
    val defenderTotal = defenderDice.sum()
    return BattleRoll(
        attackerDice = attackerDice,
        defenderDice = defenderDice,
        attackerTotal = attackerTotal,
        defenderTotal = defenderTotal,
        success = attackerTotal > defenderTotal,
    )
}

fun DicewarsGame.resolveBattle(from: Int, to: Int, roll: BattleRoll): DicewarsGame {
    val attackerOwner = areas[from].owner
    val defenderOwner = areas[to].owner
    val newAreas = areas.toMutableList()

    if (roll.success) {
        newAreas[to] = newAreas[to].copy(dice = newAreas[from].dice - 1, owner = attackerOwner)
        newAreas[from] = newAreas[from].copy(dice = 1)
    } else {
        newAreas[from] = newAreas[from].copy(dice = 1)
    }

    val newHistory = history + HistoryData(from = from, to = to, result = if (roll.success) 1 else 0)
    var game = copy(areas = newAreas.toList(), history = newHistory)
    game = game.setAreaTc(attackerOwner)
    if (roll.success) game = game.setAreaTc(defenderOwner)
    return game
}

fun DicewarsGame.startSupply(player: Int = currentPlayer()): DicewarsGame {
    var game = setAreaTc(player)
    val playerData = game.players[player]
    val newStock = (playerData.stock + playerData.maxConnectedAreaCount)
        .coerceAtMost(DicewarsGame.STOCK_MAX)
    val newPlayers = game.players.toMutableList()
    newPlayers[player] = playerData.copy(stock = newStock)
    return game.copy(players = newPlayers.toList())
}

fun DicewarsGame.supplyOneDie(player: Int, random: RandomSource): Pair<DicewarsGame, Int?> {
    val candidates = mutableListOf<Int>()
    for (areaNumber in 1 until DicewarsGame.AREA_MAX) {
        val area = areas[areaNumber]
        if (area.size == 0) continue
        if (area.owner != player) continue
        if (area.dice >= DicewarsGame.MAX_DICE) continue
        candidates.add(areaNumber)
    }

    if (candidates.isEmpty() || players[player].stock <= 0) return this to null

    val areaNumber = candidates[random.nextInt(candidates.size)]
    val newPlayers = players.toMutableList()
    newPlayers[player] = newPlayers[player].copy(stock = newPlayers[player].stock - 1)
    val newAreas = areas.toMutableList()
    newAreas[areaNumber] = newAreas[areaNumber].copy(dice = newAreas[areaNumber].dice + 1)
    return copy(
        areas = newAreas.toList(),
        players = newPlayers.toList(),
        history = history + HistoryData(from = areaNumber, to = 0, result = 0),
    ) to areaNumber
}

fun DicewarsGame.nextPlayer(): DicewarsGame {
    var newTurnIndex = turnIndex
    for (i in 0 until pmax) {
        newTurnIndex++
        if (newTurnIndex >= pmax) newTurnIndex = 0
        val player = turnOrder[newTurnIndex]
        if (players[player].maxConnectedAreaCount > 0) break
    }
    return copy(turnIndex = newTurnIndex)
}

fun DicewarsGame.setAreaTc(player: Int): DicewarsGame {
    val check = MutableList(DicewarsGame.AREA_MAX) { it }
    while (true) {
        var changed = false
        run loop@{
            for (i in 1 until DicewarsGame.AREA_MAX) {
                if (areas[i].size == 0 || areas[i].owner != player) continue
                for (j in 1 until DicewarsGame.AREA_MAX) {
                    if (areas[j].size == 0 || areas[j].owner != player) continue
                    if (areas[i].adjacentAreas[j] == 0) continue
                    if (check[j] == check[i]) continue
                    if (check[i] > check[j]) check[i] = check[j]
                    else check[j] = check[i]
                    changed = true
                    return@loop
                }
            }
        }
        if (!changed) break
    }

    val connectedAreaCounts = MutableList(DicewarsGame.AREA_MAX) { 0 }
    var areaCount = 0
    var diceCount = 0
    for (i in 1 until DicewarsGame.AREA_MAX) {
        if (areas[i].size == 0 || areas[i].owner != player) continue
        connectedAreaCounts[check[i]]++
        areaCount++
        diceCount += areas[i].dice
    }

    var maxConnected = 0
    for (count in connectedAreaCounts) {
        if (count > maxConnected) maxConnected = count
    }

    val newPlayers = players.toMutableList()
    newPlayers[player] = newPlayers[player].copy(
        areaCount = areaCount,
        diceCount = diceCount,
        maxConnectedAreaCount = maxConnected,
    )
    return copy(players = newPlayers.toList())
}
