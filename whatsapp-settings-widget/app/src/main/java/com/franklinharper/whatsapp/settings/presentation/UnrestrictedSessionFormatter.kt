package com.franklinharper.whatsapp.settings.presentation

import com.franklinharper.whatsapp.settings.domain.UnrestrictedSession
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UnrestrictedSessionFormatter(
    private val locale: Locale = Locale.getDefault(),
) {
    private val dateFormatter: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    private val timeFormatter: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
    private val dateTimeFormatter: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)

    fun format(session: UnrestrictedSession, nowMillis: Long = System.currentTimeMillis()): UnrestrictedSessionUiModel {
        val endMillis = session.endTimestampMillis ?: nowMillis
        val sameDay = isSameDay(session.startTimestampMillis, endMillis)
        val ongoing = session.endTimestampMillis == null
        return UnrestrictedSessionUiModel(
            id = session.id,
            header = header(session.startTimestampMillis, endMillis, ongoing, nowMillis),
            timeRange = timeRange(session.startTimestampMillis, endMillis, ongoing, sameDay),
            duration = duration(session.startTimestampMillis, endMillis, ongoing),
            ongoing = ongoing,
        )
    }

    private fun header(startMillis: Long, endMillis: Long, ongoing: Boolean, nowMillis: Long): String {
        return if (isSameDay(startMillis, endMillis)) {
            friendlyDate(startMillis, nowMillis)
        } else {
            "${friendlyDate(startMillis, nowMillis)} – ${if (ongoing) "Now" else friendlyDate(endMillis, nowMillis)}"
        }
    }

    private fun timeRange(startMillis: Long, endMillis: Long, ongoing: Boolean, sameDay: Boolean): String {
        val start = Date(startMillis)
        val end = Date(endMillis)
        return if (sameDay) {
            "${formatTime(start)} – ${if (ongoing) "Now" else formatTime(end)}"
        } else {
            "${formatDateTime(start)} – ${if (ongoing) "Now" else formatDateTime(end)}"
        }
    }

    private fun duration(startMillis: Long, endMillis: Long, ongoing: Boolean): String {
        val millis = (endMillis - startMillis).coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        var totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val days = totalMinutes / MINUTES_PER_DAY
        totalMinutes %= MINUTES_PER_DAY
        val hours = totalMinutes / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR

        val parts = mutableListOf<String>()
        if (days > 0) parts += quantity(days, "day")
        if (hours > 0) parts += quantity(hours, "hr")
        if (days == 0L && minutes > 0 || parts.isEmpty()) parts += quantity(minutes, "min")

        return "Approx. ${parts.joinToString(" ")}${if (ongoing) " so far" else ""}"
    }

    private fun quantity(value: Long, unit: String): String {
        val suffix = if (unit == "day" && value != 1L) "s" else ""
        return "$value $unit$suffix"
    }

    private fun formatTime(date: Date): String = timeFormatter.format(date).normalizeSpaces()

    private fun formatDateTime(date: Date): String = dateTimeFormatter.format(date).normalizeSpaces()

    private fun String.normalizeSpaces(): String = replace('\u202f', ' ')

    private fun friendlyDate(millis: Long, nowMillis: Long): String {
        val date = calendar(millis)
        val today = calendar(nowMillis)
        val yesterday = calendar(nowMillis).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            isSameDay(date, today) -> "Today"
            isSameDay(date, yesterday) -> "Yesterday"
            else -> dateFormatter.format(Date(millis))
        }
    }

    private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean =
        isSameDay(calendar(firstMillis), calendar(secondMillis))

    private fun isSameDay(first: Calendar, second: Calendar): Boolean =
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)

    private fun calendar(millis: Long): Calendar = Calendar.getInstance(locale).apply {
        timeInMillis = millis
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60L
        const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR
    }
}
