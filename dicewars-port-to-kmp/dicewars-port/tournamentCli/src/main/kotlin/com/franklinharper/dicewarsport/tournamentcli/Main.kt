package com.franklinharper.dicewarsport.tournamentcli

import com.franklinharper.dicewarsport.tournament.BuiltInTournamentParticipants
import com.franklinharper.dicewarsport.tournament.CsvTournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.BotRoundStepper
import com.franklinharper.dicewarsport.tournament.PlainTextTournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.RoundActionLogEntry
import com.franklinharper.dicewarsport.tournament.RoundActionType
import com.franklinharper.dicewarsport.tournament.RoundReplaySpec
import com.franklinharper.dicewarsport.tournament.TournamentConfig
import com.franklinharper.dicewarsport.tournament.TournamentReportFormatter
import com.franklinharper.dicewarsport.tournament.TournamentRunner
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = runTournamentCli(
        args = args,
        stdout = { print(it) },
        stderr = { System.err.println(it) },
    )
    if (exitCode != 0) exitProcess(exitCode)
}

fun runTournamentCli(
    args: Array<String>,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    if (args.firstOrNull() == "replay-round") {
        return runReplayRoundCli(args.drop(1).toTypedArray(), stdout, stderr)
    }
    val runArgs = if (args.firstOrNull() == "run-tournament") args.drop(1).toTypedArray() else args
    val options = try {
        CliOptions.parse(runArgs)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}\n\n${usage()}")
        return 2
    }

    if (options.help) {
        stdout(usage() + "\n")
        return 0
    }

    val participants = try {
        options.botIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val formatter = try {
        formatterFor(options.format)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val result = TournamentRunner().run(
        TournamentConfig(
            participants = participants,
            rounds = options.rounds,
            seed = options.seed,
            maxActionsPerRound = options.maxActions,
            logFailedRounds = options.logFailedRounds,
            logAllRounds = options.logAllRounds,
        ),
    )
    val report = formatter.format(result)

    if (options.outPath == null) {
        stdout(report)
    } else {
        File(options.outPath).also { file ->
            file.parentFile?.mkdirs()
            file.writeText(report)
        }
    }

    return 0
}

data class CliOptions(
    val botIds: List<String>,
    val rounds: Int,
    val seed: Int?,
    val format: String,
    val outPath: String?,
    val maxActions: Int,
    val logFailedRounds: Boolean = false,
    val logAllRounds: Boolean = false,
    val help: Boolean = false,
) {
    companion object {
        fun parse(args: Array<String>): CliOptions {
            if (args.any { it == "--help" || it == "-h" }) {
                return CliOptions(
                    botIds = emptyList(),
                    rounds = 1,
                    seed = null,
                    format = "text",
                    outPath = null,
                    maxActions = 100_000,
                    help = true,
                )
            }

            val values = parseKeyValues(args)
            val botIds = values["bots"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing required --bots option")
            require(botIds.size >= 2) { "At least two bots are required" }

            val rounds = values["rounds"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing or invalid required --rounds option")
            require(rounds > 0) { "--rounds must be greater than zero" }

            val maxActions = values["max-actions"]?.toIntOrNull() ?: 100_000
            require(maxActions > 0) { "--max-actions must be greater than zero" }

            val seed = values["seed"]?.toIntOrNull()
                ?: values["seed"]?.let { throw IllegalArgumentException("--seed must be an integer") }

            return CliOptions(
                botIds = botIds,
                rounds = rounds,
                seed = seed,
                format = values["format"] ?: "text",
                outPath = values["out"],
                maxActions = maxActions,
                logFailedRounds = values["log-failed-rounds"] == "true",
                logAllRounds = values["log-all-rounds"] == "true",
            )
        }

        private fun parseKeyValues(args: Array<String>): Map<String, String> {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                require(arg.startsWith("--")) { "Unexpected argument '$arg'" }
                val body = arg.removePrefix("--")
                if (body in setOf("log-failed-rounds", "log-all-rounds")) {
                    values[body] = "true"
                    index++
                } else if ('=' in body) {
                    val key = body.substringBefore('=')
                    val value = body.substringAfter('=')
                    require(key.isNotBlank()) { "Invalid option '$arg'" }
                    values[key] = value
                    index++
                } else {
                    require(index + 1 < args.size) { "Missing value for option '$arg'" }
                    val value = args[index + 1]
                    require(!value.startsWith("--")) { "Missing value for option '$arg'" }
                    values[body] = value
                    index += 2
                }
            }
            return values
        }
    }
}

private fun runReplayRoundCli(
    args: Array<String>,
    stdout: (String) -> Unit,
    stderr: (String) -> Unit,
): Int {
    val options = try {
        ReplayOptions.parse(args)
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}\n\n${replayUsage()}")
        return 2
    }

    val participants = try {
        options.seatIds.map { botId ->
            BuiltInTournamentParticipants.byId[botId]
                ?: throw IllegalArgumentException("Unknown bot '$botId'. Available bots: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}")
        }
    } catch (error: IllegalArgumentException) {
        stderr("Error: ${error.message}")
        return 2
    }

    val stepper = BotRoundStepper()
    var state = stepper.initialState(
        RoundReplaySpec(
            roundSeed = options.roundSeed,
            participants = participants,
            maxActionsPerRound = options.maxActions,
        ),
    )

    val output = buildString {
        appendLine("Round replay")
        appendLine("Round seed: ${options.roundSeed}")
        appendLine("Seats: ${options.seatIds.joinToString(",")}")
        appendLine("Max actions: ${options.maxActions}")
        appendLine()

        while (!state.completed && !state.failed && shouldContinueReplay(state.actionsTaken, options)) {
            val step = stepper.step(state)
            state = step.state
            appendLine(formatReplayStep(step.actionLogEntry))
        }

        if (state.completed) appendLine("Completed: winner=${state.winnerParticipantId}")
        if (state.failed) appendLine("Failed: ${state.failureReason}")
    }
    stdout(output)
    return 0
}

data class ReplayOptions(
    val roundSeed: Int,
    val seatIds: List<String>,
    val maxActions: Int,
    val steps: Int,
    val untilFailed: Boolean,
    val untilComplete: Boolean,
) {
    companion object {
        fun parse(args: Array<String>): ReplayOptions {
            val values = parseReplayKeyValues(args)
            val roundSeed = values["round-seed"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Missing or invalid required --round-seed option")
            val seatIds = values["seats"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing required --seats option")
            require(seatIds.size >= 2) { "At least two seats are required" }
            val maxActions = values["max-actions"]?.toIntOrNull() ?: 100_000
            require(maxActions > 0) { "--max-actions must be greater than zero" }
            val steps = values["steps"]?.toIntOrNull() ?: 20
            require(steps > 0) { "--steps must be greater than zero" }
            return ReplayOptions(
                roundSeed = roundSeed,
                seatIds = seatIds,
                maxActions = maxActions,
                steps = steps,
                untilFailed = values["until-failed"] == "true",
                untilComplete = values["until-complete"] == "true",
            )
        }

        private fun parseReplayKeyValues(args: Array<String>): Map<String, String> {
            val values = mutableMapOf<String, String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                require(arg.startsWith("--")) { "Unexpected argument '$arg'" }
                val body = arg.removePrefix("--")
                if (body in setOf("until-failed", "until-complete")) {
                    values[body] = "true"
                    index++
                } else if ('=' in body) {
                    values[body.substringBefore('=')] = body.substringAfter('=')
                    index++
                } else {
                    require(index + 1 < args.size) { "Missing value for option '$arg'" }
                    val value = args[index + 1]
                    require(!value.startsWith("--")) { "Missing value for option '$arg'" }
                    values[body] = value
                    index += 2
                }
            }
            return values
        }
    }
}

private fun shouldContinueReplay(actionsTaken: Int, options: ReplayOptions): Boolean =
    options.untilFailed || options.untilComplete || actionsTaken < options.steps

private fun formatReplayStep(entry: RoundActionLogEntry): String {
    val detail = when (entry.actionType) {
        RoundActionType.Attack -> "${entry.participantId} attacks ${entry.from} -> ${entry.to}, ${if (entry.battleRoll?.success == true) "success" else "fail"}"
        RoundActionType.IllegalMove -> "${entry.participantId} illegal move ${entry.from} -> ${entry.to}; ends turn"
        RoundActionType.EndTurn -> "${entry.participantId} ends turn, supplied: ${entry.suppliedAreas.joinToString(",").ifBlank { "none" }}"
        RoundActionType.RoundFailed -> "round failed"
        RoundActionType.RoundWon -> "round won by ${entry.participantId}"
    }
    val eliminated = entry.eliminatedParticipantIds.joinToString(",").ifBlank { "none" }
    return "Step ${entry.actionNumber}: $detail, eliminated: $eliminated"
}

private fun formatterFor(format: String): TournamentReportFormatter = when (format.lowercase()) {
    "text", "plain" -> PlainTextTournamentReportFormatter
    "csv" -> CsvTournamentReportFormatter
    else -> throw IllegalArgumentException("Unknown format '$format'. Supported formats: text,csv")
}

private fun usage(): String = """
Dicewars bot tournament

Usage:
  run-tournament --bots default,defensive,example --rounds 100 [options]

Options:
  --bots <ids>          Comma-separated bot IDs. Available: ${BuiltInTournamentParticipants.byId.keys.sorted().joinToString(",")}
  --rounds <count>      Number of rounds to attempt.
  --seed <int>          Optional seed for reproducible tournaments.
  --format <text|csv>   Output format. Default: text.
  --out <path>          Optional output path. Defaults to stdout.
  --max-actions <int>   Max actions per round. Default: 100000.
  --log-failed-rounds   Capture action logs for failed rounds by rerunning failed rounds with the same round seed.
  --log-all-rounds      Capture action logs for every round.
  --help, -h            Show this help.

Replay:
  run-tournament replay-round --round-seed 123 --seats default,defensive --max-actions 100000 --steps 50
""".trimIndent()

private fun replayUsage(): String = """
Dicewars round replay

Usage:
  replay-round --round-seed <int> --seats default,defensive,example [options]

Options:
  --round-seed <int>    Required round seed from a tournament report.
  --seats <ids>         Required comma-separated bot IDs in seated order.
  --max-actions <int>   Max actions for the round. Default: 100000.
  --steps <int>         Number of steps to print. Default: 20.
  --until-failed        Continue until the round fails or otherwise terminates.
  --until-complete      Continue until the round completes or otherwise terminates.
""".trimIndent()
