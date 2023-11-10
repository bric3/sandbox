/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
@file:Suppress("unused")

package sandbox

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.concurrent.TimeUnit
import kotlin.math.log2
import kotlin.math.pow

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
