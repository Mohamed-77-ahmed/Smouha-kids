package com.example.tracker.ui

import android.content.Context
import android.view.View
import androidx.core.text.BidiFormatter
import com.example.tracker.R
import com.example.tracker.domain.AgeGroup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Locale-aware formatting. Digits are always 0-9 (DecimalStyle.STANDARD), in Arabic too. */
object Fmt {

    fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

    /** Wraps a user-typed name so "Augmentin 1g" keeps its order inside an Arabic sentence. */
    fun bidi(context: Context, text: String): String =
        BidiFormatter.getInstance(isRtl(context)).unicodeWrap(text)

    fun time(context: Context, minuteOfDay: Int): String =
        LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale(context))
                .withDecimalStyle(DecimalStyle.STANDARD)
        )

    fun date(context: Context, epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale(context))
                .withDecimalStyle(DecimalStyle.STANDARD)
        )

    fun dayShort(context: Context, isoDay: Int): String =
        DayOfWeek.of(isoDay).getDisplayName(TextStyle.SHORT, locale(context))

    fun dayFull(context: Context, isoDay: Int): String =
        DayOfWeek.of(isoDay).getDisplayName(TextStyle.FULL, locale(context))

    /** ISO day numbers in the locale's week order (Saturday first for Arabic/Egypt). */
    fun weekOrder(context: Context): List<Int> {
        val first = WeekFields.of(locale(context)).firstDayOfWeek
        return (0L..6L).map { first.plus(it).value }
    }

    /** A bare number on screen (points, streak, level, age), digits 0-9 in every language. */
    fun number(value: Int): String = String.format(Locale.ROOT, "%d", value)

    fun points(context: Context, points: Int): String =
        context.resources.getQuantityString(R.plurals.points_count, points, points)

    fun days(context: Context, days: Int): String =
        context.resources.getQuantityString(R.plurals.days_count, days, days)

    fun ageGroup(context: Context, group: AgeGroup): String = context.getString(
        when (group) {
            AgeGroup.CHILD -> R.string.age_group_child
            AgeGroup.TEEN -> R.string.age_group_teen
            AgeGroup.ADULT -> R.string.age_group_adult
        }
    )
}
