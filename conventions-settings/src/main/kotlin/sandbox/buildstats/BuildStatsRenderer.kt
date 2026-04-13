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

import com.jakewharton.picnic.TextBorder
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

object BuildStatsRenderer {
  fun render(snapshot: BuildStatsSnapshot): List<String> {
    val blocks = mutableListOf<String>()
    blocks += snapshot.notices
    val allTasks = snapshot.taskStats
    val totalDurationMs = snapshot.totalDurationMs ?: 0L
    val projectTaskStats = allTasks
      .groupBy { it.projectPath }
      .map { (projectPath, tasks) ->
        ProjectTaskStat(
          path = projectPath,
          durationMs = tasks.sumOf { it.durationMs },
          taskCount = tasks.size,
          executedCount = tasks.count { it.outcome == TaskOutcome.EXECUTED },
          cacheHitCount = tasks.count { it.outcome == TaskOutcome.FROM_CACHE },
          failedCount = tasks.count { it.outcome == TaskOutcome.FAILED },
        )
      }
      .sortedByDescending { it.durationMs }
      .take(10)
    val slowTasks = allTasks
      .filter { it.durationMs >= snapshot.minTaskDurationMs }
      .sortedByDescending { it.durationMs }
      .take(15)
    val outcomeCounts = TaskOutcome.entries.associateWith { outcome ->
      allTasks.count { it.outcome == outcome }
    }
    val incompatibleSuffix = if (snapshot.configurationCacheIncompatible) {
      " (Configuration Cache Incompatible)"
    } else {
      ""
    }

    snapshot.sections.forEach { section ->
      when (section) {
        BuildStatsSection.BUILD_STATS -> {
          if (allTasks.isNotEmpty()) {
            blocks += "Build Stats\n" + table {
              header { row("Metric", "Value") }
              row("Observed tasks", allTasks.size)
              row("Build span", totalDurationMs.milliseconds)
              row("Executed", outcomeCounts.getValue(TaskOutcome.EXECUTED))
              row("Up-to-date", outcomeCounts.getValue(TaskOutcome.UP_TO_DATE))
              row("From cache", outcomeCounts.getValue(TaskOutcome.FROM_CACHE))
              row("Skipped", outcomeCounts.getValue(TaskOutcome.SKIPPED))
              row("No source", outcomeCounts.getValue(TaskOutcome.NO_SOURCE))
              row("Failed", outcomeCounts.getValue(TaskOutcome.FAILED))
              row("Cacheable reuse", percent(outcomeCounts.getValue(TaskOutcome.FROM_CACHE), allTasks.size))
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }

        BuildStatsSection.PROJECT_STATS -> {
          if (projectTaskStats.isNotEmpty()) {
            blocks += "Project Stats\n" + table {
              header { row("Project", "Tasks", "Executed", "From cache", "Failed", "Duration", "% build") }
              projectTaskStats.forEach { project ->
                row(
                  project.path,
                  project.taskCount,
                  project.executedCount,
                  project.cacheHitCount,
                  project.failedCount,
                  project.durationMs.milliseconds,
                  percent(project.durationMs, totalDurationMs)
                )
              }
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }

        BuildStatsSection.SLOW_TASKS -> {
          if (slowTasks.isNotEmpty()) {
            blocks += "Slow Tasks\n" + table {
              header { row("Task path", "Outcome", "Duration", "% build") }
              slowTasks.forEach { task ->
                row(
                  buildSlowTaskCell(task),
                  task.outcome.name.lowercase(),
                  task.durationMs.milliseconds,
                  percent(task.durationMs, totalDurationMs)
                )
              }
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }

        BuildStatsSection.LIFECYCLE_TIMINGS -> {
          snapshot.lifecycleTimings?.let { timings ->
            blocks += "Lifecycle Timings$incompatibleSuffix\n" + table {
              header { row("Phase", "Duration") }
              row("Settings script", formatNanos(timings.settingsScriptNanos))
              row("Project loading", formatNanos(timings.projectLoadingNanos))
              row("Project configuration", formatNanos(timings.projectConfigurationNanos))
              row("Task graph calculation", formatNanos(timings.taskGraphCalculationNanos))
              row("Task execution", formatNanos(timings.taskExecutionNanos))
              row("Total", formatNanos(timings.totalNanos))
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }

        BuildStatsSection.PROJECT_CONFIGURATION_TIMINGS -> {
          if (snapshot.projectConfigurationStats.isNotEmpty()) {
            blocks += "Project Configuration Timings$incompatibleSuffix\n" + table {
              header { row("Project", "Duration", "Execution", "Configuration") }
              snapshot.projectConfigurationStats
                .sortedByDescending { it.totalNanos }
                .take(15)
                .forEach { project ->
                  row(
                    buildProjectCell(project),
                    project.totalNanos.nanoseconds,
                    project.executionNanos.nanoseconds,
                    project.configurationNanos.nanoseconds
                  )
                }
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }

        BuildStatsSection.DIAGNOSTICS -> {
          snapshot.diagnostics?.let { diagnostics ->
            blocks += "Build Diagnostics$incompatibleSuffix\n" + table {
              header { row("Signal", "Value") }
              row("Projects discovered", diagnostics.projectCount)
              row("Projects evaluated", diagnostics.evaluatedProjectCount)
              row("Projects failed during evaluation", diagnostics.failedProjectCount)
              row("Tasks scheduled", diagnostics.scheduledTaskCount)
              row("Parallel execution", diagnostics.parallelExecutionEnabled)
              row("Configuration on demand", diagnostics.configurationOnDemand)
              row("Configuration cache requested", diagnostics.configurationCacheRequested?.toString() ?: "unknown")
              row("Build failed", diagnostics.buildFailed)
              row("Warning", diagnostics.warning)
              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          }
        }
      }
    }

    return blocks
  }

  private fun buildProjectCell(project: ProjectConfigurationStat): String =
    listOfNotNull(project.path, project.failureSummary).joinToString("\n")

  private fun buildSlowTaskCell(task: TaskStat): String {
    val reasons = task.executionReasons
      .map(String::trim)
      .filter(String::isNotEmpty)
      .take(3)
      .map { "reason: $it" }

    return listOfNotNull(
      task.path,
      *reasons.toTypedArray()
    ).joinToString("\n")
  }

  private fun formatNanos(value: Long?): String =
    if (value == null || value < 0L) "n/a" else value.nanoseconds.toString()

  private fun percent(part: Int, whole: Int): String = percent(part.toLong(), whole.toLong())

  private fun percent(part: Long, whole: Long): String {
    if (whole <= 0L) return "0.0%"
    return "%.1f%%".format((part.toDouble() * 100.0) / whole.toDouble())
  }
}
