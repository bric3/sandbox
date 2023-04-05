@file:Suppress("unused")

package sandbox

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.concurrent.TimeUnit
import kotlin.math.log2
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun Long.formatMs(): String {
    fun normalize(number: Long): String = String.format("%02d", number)

    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    return when (val hours = TimeUnit.MILLISECONDS.toHours(this)) {
        0L -> "${normalize(minutes)}:${normalize(seconds)}"
        else -> "${normalize(hours)}:${normalize(minutes)}:${normalize(seconds)}"
    }
}

fun Int.formatBytes(): String = toLong().formatBytes()

fun Long.formatBytes(): String {
    return log2(if (this != 0L) toDouble() else 1.0).toInt().div(10).let {
        val precision = when (it) {
            0 -> 0;
            1 -> 1;
            else -> 2
        }
        val prefix = arrayOf("", "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi", "Yi")
        String.format(
            "%.${precision}f ${prefix[it]}B",
            toDouble() / 2.0.pow(it * 10.0)
        )
    }
}

fun Long.groupDigits(): String = decimalFormat.format(this)

private val decimalFormat = DecimalFormat().apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply { groupingSeparator = ',' }
    groupingSize = 3
}
