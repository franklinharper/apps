package com.franklinharper.dicewarsport.ai

import com.franklinharper.dicewarsport.DicewarsGame
import com.franklinharper.dicewarsport.RandomSource

class AlwaysAttackWhenStrongerBot(private val random: RandomSource) : AiStrategy {
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
