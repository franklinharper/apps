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

fun DicewarsGame.rollBattle(
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

fun DicewarsGame.resolveBattle(from: Int, to: Int, roll: BattleRoll): BattleRoll {
    val attackerOwner = areas[from].owner
    val defenderOwner = areas[to].owner

    if (roll.success) {
        areas[to].dice = areas[from].dice - 1
        areas[from].dice = 1
        areas[to].owner = attackerOwner
        setAreaTc(attackerOwner)
        setAreaTc(defenderOwner)
    } else {
        areas[from].dice = 1
    }

    setHistory(from, to, if (roll.success) 1 else 0)
    return roll
}

fun DicewarsGame.startSupply(player: Int = currentPlayer()): Int {
    setAreaTc(player)
    players[player].stock = (players[player].stock + players[player].maxConnectedAreaCount)
        .coerceAtMost(DicewarsGame.STOCK_MAX)
    return players[player].stock
}

fun DicewarsGame.supplyOneDie(player: Int = currentPlayer(), random: RandomSource): Int? {
    val candidates = mutableListOf<Int>()
    for (areaNumber in 1 until DicewarsGame.AREA_MAX) {
        val area = areas[areaNumber]
        if (area.size == 0) continue
        if (area.owner != player) continue
        if (area.dice >= DicewarsGame.MAX_DICE) continue
        candidates.add(areaNumber)
    }

    if (candidates.isEmpty() || players[player].stock <= 0) return null

    val areaNumber = candidates[random.nextInt(candidates.size)]
    players[player].stock--
    areas[areaNumber].dice++
    setHistory(areaNumber, 0, 0)
    return areaNumber
}

fun DicewarsGame.nextPlayer(): Int {
    for (i in 0 until pmax) {
        turnIndex++
        if (turnIndex >= pmax) turnIndex = 0
        val player = turnOrder[turnIndex]
        if (players[player].maxConnectedAreaCount > 0) break
    }
    return currentPlayer()
}

fun DicewarsGame.setAreaTc(player: Int) {
    players[player].maxConnectedAreaCount = 0

    for (i in 0 until DicewarsGame.AREA_MAX) check[i] = i

    while (true) {
        var changed = false
        loop@ for (i in 1 until DicewarsGame.AREA_MAX) {
            if (areas[i].size == 0) continue
            if (areas[i].owner != player) continue
            for (j in 1 until DicewarsGame.AREA_MAX) {
                if (areas[j].size == 0) continue
                if (areas[j].owner != player) continue
                if (areas[i].adjacentAreas[j] == 0) continue
                if (check[j] == check[i]) continue
                if (check[i] > check[j]) {
                    check[i] = check[j]
                } else {
                    check[j] = check[i]
                }
                changed = true
                break@loop
            }
        }
        if (!changed) break
    }

    for (i in 0 until DicewarsGame.AREA_MAX) connectedAreaCounts[i] = 0
    var areaCount = 0
    var diceCount = 0
    for (i in 1 until DicewarsGame.AREA_MAX) {
        if (areas[i].size == 0) continue
        if (areas[i].owner != player) continue
        connectedAreaCounts[check[i]]++
        areaCount++
        diceCount += areas[i].dice
    }

    var maxConnected = 0
    for (i in 0 until DicewarsGame.AREA_MAX) {
        if (connectedAreaCounts[i] > maxConnected) maxConnected = connectedAreaCounts[i]
    }

    players[player].areaCount = areaCount
    players[player].diceCount = diceCount
    players[player].maxConnectedAreaCount = maxConnected
}

fun DicewarsGame.setHistory(from: Int, to: Int, result: Int) {
    history.add(HistoryData(from = from, to = to, result = result))
}
