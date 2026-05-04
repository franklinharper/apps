package com.franklinharper.dicewarsport

data class Move(val from: Int, val to: Int)

interface AiStrategy {
    fun chooseMove(game: DicewarsGame): Move?
}

class ExampleAi(private val random: RandomSource) : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        val moves = mutableListOf<Move>()

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0) continue
            if (attacker.owner != player) continue
            if (attacker.dice <= 1) continue

            for (to in 1 until DicewarsGame.AREA_MAX) {
                val defender = game.areas[to]
                if (defender.size == 0) continue
                if (defender.owner == player) continue
                if (attacker.adjacentAreas[to] == 0) continue
                if (defender.dice >= attacker.dice) continue
                moves.add(Move(from, to))
            }
        }

        return moves.randomOrNull(random)
    }
}

class DefaultAi(private val random: RandomSource) : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? {
        val areaCounts = MutableList(8) { 0 }
        val diceCounts = MutableList(8) { 0 }
        for (areaNumber in 1 until DicewarsGame.AREA_MAX) {
            val area = game.areas[areaNumber]
            if (area.size == 0) continue
            val owner = area.owner
            if (owner !in 0 until 8) continue
            areaCounts[owner]++
            diceCounts[owner] += area.dice
        }

        val rankedPlayers = (0 until 8).sortedByDescending { diceCounts[it] }
        val diceRanks = IntArray(8)
        rankedPlayers.forEachIndexed { rank, player -> diceRanks[player] = rank }

        val totalDice = diceCounts.sum()
        var topPlayer = -1
        for (player in 0 until 8) {
            if (diceCounts[player] > totalDice * 2 / 5) topPlayer = player
        }

        val currentPlayer = game.currentPlayer()
        val moves = mutableListOf<Move>()

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0) continue
            if (attacker.owner != currentPlayer) continue
            if (attacker.dice <= 1) continue

            for (to in 1 until DicewarsGame.AREA_MAX) {
                val defender = game.areas[to]
                if (defender.size == 0) continue
                if (defender.owner == currentPlayer) continue
                if (attacker.adjacentAreas[to] == 0) continue
                if (topPlayer >= 0 && attacker.owner != topPlayer && defender.owner != topPlayer) continue
                if (defender.dice > attacker.dice) continue
                if (defender.dice == attacker.dice) {
                    val enemy = defender.owner
                    var shouldAttack = false
                    if (diceRanks[currentPlayer] == 0) shouldAttack = true
                    if (diceRanks[enemy] == 0) shouldAttack = true
                    if (random.nextInt(10) > 1) shouldAttack = true
                    if (!shouldAttack) continue
                }
                moves.add(Move(from, to))
            }
        }

        return moves.randomOrNull(random)
    }
}

class DefensiveAi : AiStrategy {
    override fun chooseMove(game: DicewarsGame): Move? {
        val currentPlayer = game.currentPlayer()
        val areaInfo = List(DicewarsGame.AREA_MAX) { areaId -> analyzeArea(game, areaId) }
        var bestMove: Move? = null

        for (defenderId in 1 until DicewarsGame.AREA_MAX) {
            val defender = game.areas[defenderId]
            if (defender.size == 0) continue
            if (defender.owner == currentPlayer) continue

            for (attackerId in 1 until DicewarsGame.AREA_MAX) {
                val attacker = game.areas[attackerId]
                if (attacker.size == 0) continue
                if (attacker.owner != currentPlayer) continue
                if (attacker.adjacentAreas[defenderId] == 0 && defender.adjacentAreas[attackerId] == 0) continue
                if (!game.isLegalAttack(attackerId, defenderId, currentPlayer)) continue

                if (defender.dice >= attacker.dice && attacker.dice != DicewarsGame.MAX_DICE) continue
                if (areaInfo[defenderId].highestFriendlyNeighborDice > attacker.dice) continue
                if (game.players[currentPlayer].maxConnectedAreaCount > 4 &&
                    areaInfo[attackerId].secondHighestUnfriendlyNeighborDice > 2 &&
                    game.players[currentPlayer].stock == 0
                ) continue

                val previous = bestMove
                if (previous == null) {
                    bestMove = Move(attackerId, defenderId)
                } else {
                    val previousInfo = areaInfo[previous.from]
                    val challengerInfo = areaInfo[attackerId]
                    if (previousInfo.unfriendlyNeighbors == 1) {
                        if (challengerInfo.unfriendlyNeighbors == 1) {
                            if (attacker.dice < game.areas[previous.from].dice) continue
                            if (attacker.dice == game.areas[previous.from].dice &&
                                challengerInfo.numNeighbors < previousInfo.numNeighbors
                            ) continue
                        } else {
                            continue
                        }
                    }
                    bestMove = Move(attackerId, defenderId)
                }
            }
        }

        return bestMove
    }

    private fun analyzeArea(game: DicewarsGame, areaId: Int): AreaInfo {
        var friendlyNeighbors = 0
        var unfriendlyNeighbors = 0
        var highestFriendlyNeighborDice = 0
        var highestUnfriendlyNeighborDice = 0
        var secondHighestUnfriendlyNeighborDice = 0
        val area = game.areas[areaId]

        for (neighborId in 1 until DicewarsGame.AREA_MAX) {
            if (neighborId == areaId) continue
            if (area.adjacentAreas[neighborId] == 0) continue
            val neighbor = game.areas[neighborId]
            if (neighbor.size == 0) continue
            val dice = neighbor.dice
            if (area.owner == neighbor.owner) {
                friendlyNeighbors++
                if (highestFriendlyNeighborDice < dice) highestFriendlyNeighborDice = dice
            } else {
                unfriendlyNeighbors++
                if (highestUnfriendlyNeighborDice < dice) {
                    secondHighestUnfriendlyNeighborDice = highestUnfriendlyNeighborDice
                    highestUnfriendlyNeighborDice = dice
                } else if (secondHighestUnfriendlyNeighborDice < dice) {
                    secondHighestUnfriendlyNeighborDice = dice
                }
            }
        }

        return AreaInfo(
            friendlyNeighbors = friendlyNeighbors,
            unfriendlyNeighbors = unfriendlyNeighbors,
            highestFriendlyNeighborDice = highestFriendlyNeighborDice,
            highestUnfriendlyNeighborDice = highestUnfriendlyNeighborDice,
            secondHighestUnfriendlyNeighborDice = secondHighestUnfriendlyNeighborDice,
            numNeighbors = friendlyNeighbors + unfriendlyNeighbors,
        )
    }
}

private data class AreaInfo(
    val friendlyNeighbors: Int,
    val unfriendlyNeighbors: Int,
    val highestFriendlyNeighborDice: Int,
    val highestUnfriendlyNeighborDice: Int,
    val secondHighestUnfriendlyNeighborDice: Int,
    val numNeighbors: Int,
)

private fun List<Move>.randomOrNull(random: RandomSource): Move? =
    if (isEmpty()) null else this[random.nextInt(size)]
