package com.franklinharper.dicewarsport.tournamentcli

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CliOptionsTest {
    @Test
    fun parsesRequiredAndOptionalArguments() {
        val options = CliOptions.parse(
            arrayOf(
                "--bots", "default,defensive,example",
                "--rounds", "25",
                "--seed", "42",
                "--format", "csv",
                "--out", "reports/out.csv",
                "--max-actions", "1234",
            ),
        )

        assertEquals(listOf("default", "defensive", "example"), options.botIds)
        assertEquals(25, options.rounds)
        assertEquals(42, options.seed)
        assertEquals("csv", options.format)
        assertEquals("reports/out.csv", options.outPath)
        assertEquals(1234, options.maxActions)
    }

    @Test
    fun supportsEqualsStyleArguments() {
        val options = CliOptions.parse(arrayOf("--bots=default,defensive", "--rounds=3"))

        assertEquals(listOf("default", "defensive"), options.botIds)
        assertEquals(3, options.rounds)
        assertEquals("text", options.format)
    }

    @Test
    fun invalidArgumentsReturnUsefulErrors() {
        assertFailsWith<IllegalArgumentException> {
            CliOptions.parse(arrayOf("--bots", "default", "--rounds", "0"))
        }
    }

    @Test
    fun cliPrintsTextReportToStdout() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "--bots", "default,defensive",
                "--rounds", "1",
                "--seed", "1",
                "--max-actions", "1",
                "--format", "text",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Dicewars Bot Tournament")
        assertContains(stdout.toString(), "Rounds requested: 1")
    }

    @Test
    fun parsesActionLogFlags() {
        val failedOnly = CliOptions.parse(arrayOf("--bots", "default,defensive", "--rounds", "1", "--log-failed-rounds"))
        val allRounds = CliOptions.parse(arrayOf("--bots", "default,defensive", "--rounds", "1", "--log-all-rounds"))

        assertTrue(failedOnly.logFailedRounds)
        assertTrue(allRounds.logAllRounds)
    }

    @Test
    fun replayRoundSubcommandPrintsStepByStepOutput() {
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "replay-round",
                "--round-seed", "123",
                "--seats", "default,defensive",
                "--max-actions", "2",
                "--steps", "2",
            ),
            stdout = { stdout.append(it) },
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(0, exitCode, stderr.toString())
        assertContains(stdout.toString(), "Round replay")
        assertContains(stdout.toString(), "Round seed: 123")
        assertContains(stdout.toString(), "Seats: default,defensive")
        assertContains(stdout.toString(), "Step 1:")
    }

    @Test
    fun cliReportsGeneratedSeedWhenSeedIsOmitted() {
        val stdout = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf(
                "--bots", "default,defensive",
                "--rounds", "1",
                "--max-actions", "1",
                "--format", "text",
            ),
            stdout = { stdout.append(it) },
            stderr = {},
        )

        assertEquals(0, exitCode)
        assertTrue(Regex("Seed: -?\\d+").containsMatchIn(stdout.toString()))
        assertTrue(!stdout.toString().contains("Seed: random"))
    }

    @Test
    fun unknownBotReturnsErrorExitCode() {
        val stderr = StringBuilder()

        val exitCode = runTournamentCli(
            args = arrayOf("--bots", "default,nope", "--rounds", "1"),
            stdout = {},
            stderr = { stderr.appendLine(it) },
        )

        assertEquals(2, exitCode)
        assertTrue(stderr.toString().contains("Unknown bot 'nope'"))
    }
}
