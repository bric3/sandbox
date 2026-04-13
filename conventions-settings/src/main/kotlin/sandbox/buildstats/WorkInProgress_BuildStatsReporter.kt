/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.buildstats

import com.jakewharton.picnic.BorderStyle.Hidden
import com.jakewharton.picnic.TextAlignment.BottomCenter
import com.jakewharton.picnic.TextAlignment.BottomLeft
import com.jakewharton.picnic.TextAlignment.MiddleRight
import com.jakewharton.picnic.table
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class WorkInProgress_BuildStatsReporter : BuildService<WorkInProgress_BuildStatsReporter.Parameters> {
  interface Parameters : BuildServiceParameters {
    val enabled: Property<Boolean>
  }

  lateinit var settings: Settings

  fun emitReport(collected: BuildStatsCollector) {
    if (settings.extensions.findByType(BuildStatsExtension::class.java)?.enabled?.orNull == true) {

      return
    }

    picnicTable()

    val filterValues = collected.projectStats.filterValues { !it.hadTasksToExecute }
    filterValues.ifEmpty { return }
  }

  private fun picnicTable() {
    table {
      style {
        borderStyle = Hidden
      }
      cellStyle {
        alignment = MiddleRight
        paddingLeft = 1
        paddingRight = 1
        borderLeft = true
        borderRight = true
      }
      header {
        cellStyle {
          border = true
          alignment = BottomLeft
        }
        row {
          cell("APK") {
            rowSpan = 2
          }
          cell("compressed") {
            alignment = BottomCenter
            columnSpan = 3
          }
          cell("uncompressed") {
            alignment = BottomCenter
            columnSpan = 3
          }
        }
        row("old", "new", "diff", "old", "new", "diff")
      }
      body {
        row("dex", "664.8 KiB", "664.8 Kib", "-25 B", "1.5 MiB", "1.5 MiB", "-112 B")
        // "arsc", "manifest", etc…
      }
      footer {
        cellStyle {
          border = true
        }
        row("total", "1.3 MiB", "1.3 MiB", "-39 B", "2.2 MiB", "2.2 MiB", "-112 B")
      }
    }
  }
}