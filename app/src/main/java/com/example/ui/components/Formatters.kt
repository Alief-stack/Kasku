package com.example.ui.components

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val indonesianLocale = Locale.forLanguageTag("id-ID")

fun formatRupiah(amount: Long): String {
    val nf = NumberFormat.getNumberInstance(indonesianLocale)
    val absPrefix = if (amount < 0) "-Rp " else "Rp "
    return absPrefix + nf.format(kotlin.math.abs(amount))
}

fun formatCompactRupiah(amount: Long): String {
    val abs = kotlin.math.abs(amount)
    val prefix = if (amount < 0) "-Rp " else "Rp "
    return when {
        abs >= 1_000_000_000L -> String.format(indonesianLocale, "%s%.1f M", prefix, abs / 1_000_000_000.0)
        abs >= 1_000_000L -> String.format(indonesianLocale, "%s%.1f jt", prefix, abs / 1_000_000.0)
        abs >= 1_000L -> String.format(indonesianLocale, "%s%.0f rb", prefix, abs / 1_000.0)
        else -> formatRupiah(amount)
    }
}

fun currentMonthKey(): String {
    val cal = Calendar.getInstance()
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun epochMsToMonthKey(epochMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return currentMonthKey()
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, deltaMonths)
    }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun formatMonthLabel(monthKey: String): String {
    val parts = monthKey.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
    val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
    return sdf.format(cal.time).replaceFirstChar { it.titlecase(indonesianLocale) }
}

fun formatShortDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
    return sdf.format(Date(epochMs))
}

fun formatDayMonth(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM", indonesianLocale)
    return sdf.format(Date(epochMs))
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF0F5243)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val parsed = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or parsed)
            8 -> Color(parsed)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun categoryColor(category: String): Color {
    return when (category) {
        "Makanan" -> Color(0xFFD97706)
        "Transportasi" -> Color(0xFF0284C7)
        "Tagihan" -> Color(0xFFDC2626)
        "Belanja" -> Color(0xFF7C3AED)
        "Kesehatan" -> Color(0xFF0D9488)
        "Hiburan" -> Color(0xFFDB2777)
        "Pendidikan" -> Color(0xFF2563EB)
        "Gaji" -> Color(0xFF15803D)
        "Bonus" -> Color(0xFF059669)
        "Usaha" -> Color(0xFF0F766E)
        "Investasi" -> Color(0xFF4F46E5)
        else -> Color(0xFF525E58)
    }
}
