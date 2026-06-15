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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskExecutionResult
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong

// inspired by https://github.com/gradle/gradle/issues/20151#issuecomment-1751927564
abstract class StatisticsService :
  BuildService<StatisticsService.Parameters>,
  OperationCompletionListener,
  AutoCloseable {

  interface Parameters : BuildServiceParameters {
    val enabled: Property<Boolean>
    val sections: ListProperty<BuildStatsSection>
    val configurationCacheRequested: Property<Boolean>
    val minTaskDurationMs: Property<Long>
  }

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
      executionReasons = executionReasons(event.result),
      outcome = when (val result = event.result) {
        is TaskFailureResult -> TaskOutcome.FAILED
        is TaskSkippedResult ->
          if (result.skipMessage.equals("NO-SOURCE", ignoreCase = true)) TaskOutcome.NO_SOURCE else TaskOutcome.SKIPPED

        is TaskSuccessResult -> when {
          result.isFromCache -> TaskOutcome.FROM_CACHE
          result.isUpToDate -> TaskOutcome.UP_TO_DATE
          else -> TaskOutcome.EXECUTED
        }

        else -> TaskOutcome.EXECUTED
      }
    )
  }

  private fun executionReasons(result: Any): List<String> =
    if (result is TaskExecutionResult) {
      runCatching { result.executionReasons ?: emptyList() }.getOrDefault(emptyList())
    } else {
      emptyList()
    }

  override fun close() {
    if (!parameters.enabled.get() || taskStats.isEmpty()) {
      return
    }

    val incompatibleSections = parameters.sections.get().filter {
      it == BuildStatsSection.LIFECYCLE_TIMINGS ||
        it == BuildStatsSection.PROJECT_CONFIGURATION_TIMINGS ||
        it == BuildStatsSection.DIAGNOSTICS
    }
    val notices = buildList {
      if (parameters.configurationCacheRequested.get() && incompatibleSections.isNotEmpty()) {
        add(
          "Unavailable sections when Configuration Cache is requested: " +
            incompatibleSections.joinToString(", ")
        )
      }
    }
    val snapshot = BuildStatsSnapshot(
      sections = parameters.sections.get(),
      taskStats = taskStats.values.toList(),
      totalDurationMs = lastTaskEndMs.get() - firstTaskStartMs.get(),
      notices = notices,
      minTaskDurationMs = parameters.minTaskDurationMs.get(),
    )
    BuildStatsRenderer.render(snapshot).forEach(::println)
  }
}
