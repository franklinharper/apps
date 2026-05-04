package com.franklinharper.dicewarsport.tournament

import com.franklinharper.dicewarsport.ai.DefaultAi
import com.franklinharper.dicewarsport.ai.DefensiveAi
import com.franklinharper.dicewarsport.ai.ExampleAi

object BuiltInTournamentParticipants {
    val example = TournamentParticipant(
        id = "example",
        displayName = "Example AI",
        aiFactory = { random -> ExampleAi(random) },
    )

    val default = TournamentParticipant(
        id = "default",
        displayName = "Default AI",
        aiFactory = { random -> DefaultAi(random) },
    )

    val defensive = TournamentParticipant(
        id = "defensive",
        displayName = "Defensive AI",
        aiFactory = { DefensiveAi() },
    )

    val all: List<TournamentParticipant> = listOf(default, defensive, example)

    val byId: Map<String, TournamentParticipant> = all.associateBy { it.id }
}
