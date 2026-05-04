package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.AlwaysAttackWhenStrongerBot
import com.franklinharper.dicewarsport.ai.CautiousBot
import com.franklinharper.dicewarsport.ai.StrategicBot
import com.franklinharper.dicewarsport.ai.TargetTheLeader

object BuiltInTournamentParticipants {
    val attackWhenStronger = TournamentParticipant(
        id = "attack-when-stronger",
        displayName = "Always Attack When Stronger Bot",
        aiFactory = { random -> AlwaysAttackWhenStrongerBot(random) },
    )

    val targetLeader = TournamentParticipant(
        id = "target-leader",
        displayName = "Target The Leader",
        aiFactory = { random -> TargetTheLeader(random) },
    )

    val cautious = TournamentParticipant(
        id = "cautious",
        displayName = "Cautious Bot",
        aiFactory = { CautiousBot() },
    )

    val strategic = TournamentParticipant(
        id = "strategic",
        displayName = "Strategic Bot",
        aiFactory = { random -> StrategicBot(random) },
    )

    val all: List<TournamentParticipant> = listOf(targetLeader, cautious, attackWhenStronger, strategic)

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
