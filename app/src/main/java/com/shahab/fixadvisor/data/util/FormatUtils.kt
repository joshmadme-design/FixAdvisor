package com.shahab.fixadvisor.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small display helpers shared by the screens. */

/** Formats a Ringgit amount, e.g. 150.0 -> "RM150", 149.5 -> "RM149.50". */
fun formatPriceRm(amount: Double?): String {
    if (amount == null) return "—"
    return if (amount % 1.0 == 0.0) "RM${amount.toInt()}" else "RM${"%.2f".format(amount)}"
}

/** Formats an epoch-millis timestamp as "20 Oct 2026". */
fun formatDate(millis: Long): String {
    if (millis <= 0) return ""
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
}

/** One-decimal rating text, e.g. 4.5 -> "4.5". */
fun formatRating(rating: Double): String = "%.1f".format(rating)

/** Makes sure a link opens in a browser (adds https:// when missing). */
fun normalizeUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}
