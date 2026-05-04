package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.BattleRoll
import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource
import com.franklinharper.dicewarsport.isLegalAttack
import com.franklinharper.dicewarsport.resolveBattle

/**
 * A bot using 3-ply expectiminimax search within each turn.
 *
 * For each candidate attack, the bot:
 * 1. Looks up exact win probability from a precomputed table
 * 2. Simulates the win and loss outcomes via resolveBattle
 * 3. Recursively evaluates the best continuation from each resulting position
 * 4. Computes expected value across the win/loss branches
 *
 * At each ply, "end turn" is always an option (evaluated as the current
 * position's static value). The bot attacks only when the expected value
 * of attacking exceeds the value of ending the turn.
 *
 * Candidate attacks are filtered using CautiousBot's proven safety rules
 * to keep the search tree small and avoid obviously wasteful attacks.
 */
class StrategicBot(private val random: RandomSource) : AiStrategy {

    override fun chooseMove(game: DicewarsGame): Move? {
        val player = game.currentPlayer()
        val candidates = filteredMoves(game, player)
        if (candidates.isEmpty()) return null

        val currentEval = evaluate(game, player)
        var bestMove: Move? = null
        var bestEv = currentEval // must exceed "end turn" value

        for (move in candidates) {
            val attackerDice = game.areas[move.from].dice
            val defenderDice = game.areas[move.to].dice
            val winProb = winProbability(attackerDice, defenderDice)

            if (winProb < 0.15) continue

            val gameAfterWin = game.resolveBattle(move.from, move.to, WIN_ROLL)
            val gameAfterLoss = game.resolveBattle(move.from, move.to, LOSS_ROLL)

            val ev = winProb * search(gameAfterWin, player, SEARCH_DEPTH - 1) +
                     (1.0 - winProb) * search(gameAfterLoss, player, SEARCH_DEPTH - 1)

            if (ev > bestEv) {
                bestEv = ev
                bestMove = move
            }
        }

        return bestMove
    }

    /**
     * Recursive expectiminimax: find the best expected value achievable
     * from this position with the given remaining search depth.
     */
    private fun search(game: DicewarsGame, player: Int, depth: Int): Double {
        // Base case: evaluate position statically
        if (depth <= 0) return evaluate(game, player)

        // If player was eliminated by a previous attack in this sequence
        if (game.players[player].maxConnectedAreaCount == 0) return EVAL_ELIMINATED

        val candidates = filteredMoves(game, player)
        if (candidates.isEmpty()) return evaluate(game, player) // no attacks = end turn

        val currentEval = evaluate(game, player)
        var bestEv = currentEval // "end turn" is always an option

        for (move in candidates) {
            val winProb = winProbability(game.areas[move.from].dice, game.areas[move.to].dice)
            if (winProb < 0.15) continue

            val gameAfterWin = game.resolveBattle(move.from, move.to, WIN_ROLL)
            val gameAfterLoss = game.resolveBattle(move.from, move.to, LOSS_ROLL)

            val ev = winProb * search(gameAfterWin, player, depth - 1) +
                     (1.0 - winProb) * search(gameAfterLoss, player, depth - 1)

            if (ev > bestEv) bestEv = ev
        }

        return bestEv
    }

    // --- Candidate filtering (CautiousBot's proven rules) ---

    private fun filteredMoves(game: DicewarsGame, player: Int): List<Move> {
        val stock = game.players[player].stock
        val established = game.players[player].maxConnectedAreaCount > 4
        val moves = mutableListOf<Move>()

        for (from in 1 until DicewarsGame.AREA_MAX) {
            val attacker = game.areas[from]
            if (attacker.size == 0 || attacker.owner != player || attacker.dice <= 1) continue

            val fromVuln = secondHighestUnfrienemyDice(game, from, player)

            for (to in 1 until DicewarsGame.AREA_MAX) {
                if (!game.isLegalAttack(from, to, player)) continue
                val defender = game.areas[to]

                // CautiousBot filter: must have more dice (max-dice exception)
                if (defender.dice >= attacker.dice && attacker.dice != DicewarsGame.MAX_DICE) continue

                // CautiousBot filter: let a stronger friendly neighbor attack instead
                if (highestFriendlyNeighborDice(game, to, player) > attacker.dice) continue

                // CautiousBot filter: don't attack from vulnerable positions when established
                if (established && fromVuln > 2 && stock == 0) continue

                moves.add(Move(from, to))
            }
        }
        return moves
    }

    private fun highestFriendlyNeighborDice(game: DicewarsGame, areaId: Int, player: Int): Int {
        val area = game.areas[areaId]
        var best = 0
        for (n in 1 until DicewarsGame.AREA_MAX) {
            if (area.adjacentAreas[n] == 0) continue
            val na = game.areas[n]
            if (na.size > 0 && na.owner == player && na.dice > best) best = na.dice
        }
        return best
    }

    private fun secondHighestUnfrienemyDice(game: DicewarsGame, areaId: Int, player: Int): Int {
        val area = game.areas[areaId]
        var hi = 0; var hi2 = 0
        for (n in 1 until DicewarsGame.AREA_MAX) {
            if (area.adjacentAreas[n] == 0) continue
            val na = game.areas[n]
            if (na.size == 0 || na.owner == player) continue
            if (na.dice > hi) { hi2 = hi; hi = na.dice }
            else if (na.dice > hi2) hi2 = na.dice
        }
        return hi2
    }

    // --- Position evaluation ---

    /**
     * Evaluate position as 2×my_strength − sum(all_strengths).
     * This MaxN-style heuristic values both growing your own position
     * and weakening opponents equally.
     */
    private fun evaluate(game: DicewarsGame, player: Int): Double {
        val pd = game.players[player]
        if (pd.maxConnectedAreaCount == 0) return EVAL_ELIMINATED

        val myStrength = playerStrength(game, player)
        var totalStrength = myStrength
        for (p in 0 until game.pmax) {
            if (p == player || game.players[p].maxConnectedAreaCount == 0) continue
            totalStrength += playerStrength(game, p)
        }

        return 2.0 * myStrength - totalStrength
    }

    private fun playerStrength(game: DicewarsGame, player: Int): Double {
        val pd = game.players[player]
        var s = pd.maxConnectedAreaCount.toDouble() * W_SUPPLY
        s += pd.diceCount.toDouble() * W_DICE
        s += pd.areaCount.toDouble() * W_TERRITORY
        s += pd.stock.toDouble() * W_STOCK
        if (pd.areaCount > 0) {
            s += (pd.diceCount.toDouble() / pd.areaCount) * W_CONCENTRATION
        }
        return s
    }

    // --- Exact win probability ---

    companion object {
        private const val SEARCH_DEPTH = 3
        private const val TIEBREAK_MARGIN = 5.0
        private const val EVAL_ELIMINATED = -100_000.0

        private const val W_SUPPLY = 100.0
        private const val W_DICE = 10.0
        private const val W_TERRITORY = 5.0
        private const val W_STOCK = 8.0
        private const val W_CONCENTRATION = 12.0

        private val WIN_ROLL = BattleRoll(emptyList(), emptyList(), 0, 0, true)
        private val LOSS_ROLL = BattleRoll(emptyList(), emptyList(), 0, 0, false)

        private val WIN_PROBABILITY: Array<DoubleArray> = computeWinProbabilities()

        private fun computeWinProbabilities(): Array<DoubleArray> {
            val table = Array(9) { DoubleArray(9) { 0.0 } }
            fun sumDist(n: Int): Map<Int, Double> {
                var dist = mapOf(0 to 1.0)
                repeat(n) {
                    val next = mutableMapOf<Int, Double>()
                    for ((s, p) in dist) for (f in 1..6) next[s + f] = (next[s + f] ?: 0.0) + p / 6.0
                    dist = next
                }
                return dist
            }
            for (a in 1..8) {
                val ad = sumDist(a)
                for (d in 1..8) {
                    val dd = sumDist(d)
                    var wp = 0.0
                    for ((aS, aP) in ad) for ((dS, dP) in dd) if (aS > dS) wp += aP * dP
                    table[a][d] = wp
                }
            }
            return table
        }

        fun winProbability(attackerDice: Int, defenderDice: Int): Double {
            if (attackerDice !in 1..8 || defenderDice !in 1..8) return 0.0
            return WIN_PROBABILITY[attackerDice][defenderDice]
        }
    }
}
