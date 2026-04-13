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
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

// inspired by https://github.com/gradle/gradle/issues/20151#issuecomment-1751927564
abstract class StatisticsService :
  BuildService<StatisticsService.Parameters>,
  OperationCompletionListener,
  AutoCloseable {

  interface Parameters : BuildServiceParameters {
    val enabled: Property<Boolean>
    val showBuildStats: Property<Boolean>
    val showProjectStats: Property<Boolean>
    val showSlowTasks: Property<Boolean>
    val minTaskDurationMs: Property<Long>
  }

  private enum class Outcome {
    EXECUTED,
    UP_TO_DATE,
    FROM_CACHE,
    SKIPPED,
    NO_SOURCE,
    FAILED,
  }

  private data class TaskStat(
    val path: String,
    val durationMs: Long,
    val outcome: Outcome,
  ) {
    val projectPath: String
      get() = path.substringBeforeLast(":", missingDelimiterValue = ":")
        .ifBlank { ":" }
  }

  private data class ProjectStat(
    val path: String,
    val durationMs: Long,
    val taskCount: Int,
    val executedCount: Int,
    val cacheHitCount: Int,
    val failedCount: Int,
  )

  private val taskStats = ConcurrentSkipListMap<String, TaskStat>()
  private val firstTaskStartMs = AtomicLong(Long.MAX_VALUE)
  private val lastTaskEndMs = AtomicLong(Long.MIN_VALUE)

  override fun onFinish(event: FinishEvent) {
    if (event !is TaskFinishEvent) {
      return
    }

    firstTaskStartMs.accumulateAndGet(event.result.startTime, ::minOf)
    lastTaskEndMs.accumulateAndGet(event.result.endTime, ::maxOf)

    val durationMs = event.result.endTime - event.result.startTime
    taskStats[event.descriptor.taskPath] = TaskStat(
      path = event.descriptor.taskPath,
      durationMs = durationMs,
      outcome = when (val result = event.result) {
        is TaskFailureResult -> Outcome.FAILED
        is TaskSkippedResult ->
          if (result.skipMessage.equals("NO-SOURCE", ignoreCase = true)) Outcome.NO_SOURCE else Outcome.SKIPPED

        is TaskSuccessResult -> when {
          result.isFromCache -> Outcome.FROM_CACHE
          result.isUpToDate -> Outcome.UP_TO_DATE
          else -> Outcome.EXECUTED
        }

        else -> Outcome.EXECUTED
      }
    )
  }

  override fun close() {
    if (!parameters.enabled.get() || taskStats.isEmpty()) {
      return
    }

    val allTasks = taskStats.values.toList()
    val totalDurationMs = lastTaskEndMs.get() - firstTaskStartMs.get()
    val totalDuration = totalDurationMs.milliseconds
    val outcomeCounts = Outcome.entries.associateWith { outcome ->
      allTasks.count { it.outcome == outcome }
    }
    val projectStats = allTasks
      .groupBy { it.projectPath }
      .map { (projectPath, tasks) ->
        ProjectStat(
          path = projectPath,
          durationMs = tasks.sumOf { it.durationMs },
          taskCount = tasks.size,
          executedCount = tasks.count { it.outcome == Outcome.EXECUTED },
          cacheHitCount = tasks.count { it.outcome == Outcome.FROM_CACHE },
          failedCount = tasks.count { it.outcome == Outcome.FAILED },
        )
      }
      .sortedByDescending { it.durationMs }
      .take(10)
    val slowTasks = allTasks
      .filter { it.durationMs >= parameters.minTaskDurationMs.get() }
      .sortedByDescending { it.durationMs }
      .take(15)

    if (parameters.showBuildStats.get()) {
      println("Build Stats")
      println(
        table {
          header {
            row("Metric", "Value")
          }
          row("Observed tasks", allTasks.size)
          row("Build span", totalDuration)
          row("Executed", outcomeCounts.getValue(Outcome.EXECUTED))
          row("Up-to-date", outcomeCounts.getValue(Outcome.UP_TO_DATE))
          row("From cache", outcomeCounts.getValue(Outcome.FROM_CACHE))
          row("Skipped", outcomeCounts.getValue(Outcome.SKIPPED))
          row("No source", outcomeCounts.getValue(Outcome.NO_SOURCE))
          row("Failed", outcomeCounts.getValue(Outcome.FAILED))
          row(
            "Cacheable reuse",
            percent(outcomeCounts.getValue(Outcome.FROM_CACHE), allTasks.size)
          )

          cellStyle {
            paddingLeft = 1
            paddingRight = 1
            border = true
          }
        }.renderText(border = TextBorder.ROUNDED)
      )
    }

    if (parameters.showProjectStats.get()) {
      println("Project Stats")
      println(
        table {
          header {
            row("Project", "Tasks", "Executed", "From cache", "Failed", "Duration", "% build")
          }
          projectStats.forEach { project ->
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
      )
    }

    if (parameters.showSlowTasks.get() && slowTasks.isNotEmpty()) {
      println("Slow Tasks")
      println(
        table {
          header {
            row("Task path", "Outcome", "Duration", "% build")
          }
          slowTasks.forEach { task ->
            row(
              task.path,
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
      )
    }
  }

  private fun percent(part: Int, whole: Int): String =
    percent(part.toLong(), whole.toLong())

  private fun percent(part: Long, whole: Long): String {
    if (whole <= 0L) {
      return "0.0%"
    }
    return "%.1f%%".format((part.toDouble() * 100.0) / whole.toDouble())
  }
}
