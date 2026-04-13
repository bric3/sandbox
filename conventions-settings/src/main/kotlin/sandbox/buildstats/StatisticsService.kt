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
import org.gradle.tooling.events.task.TaskFinishEvent
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

// inspired by https://github.com/gradle/gradle/issues/20151#issuecomment-1751927564
abstract class StatisticsService :
  BuildService<StatisticsService.Parameters>,
    OperationCompletionListener,
  AutoCloseable {

  interface Parameters : BuildServiceParameters {
    val minTaskDurationMs: Property<Long>
  }

  private val timeStatistics = ConcurrentSkipListMap<String, Long>()
  private val firstTaskStartMs = AtomicLong(Long.MAX_VALUE)
  private val lastTaskEndMs = AtomicLong(Long.MIN_VALUE)

  override fun onFinish(event: FinishEvent) {
    if (event is TaskFinishEvent) {
      firstTaskStartMs.accumulateAndGet(event.result.startTime, ::minOf)
      lastTaskEndMs.accumulateAndGet(event.result.endTime, ::maxOf)

      val durationMs = event.result.endTime - event.result.startTime
      timeStatistics[event.descriptor.taskPath] = durationMs
    }
  }

  override fun close() {
    val interestingTasks = timeStatistics.filterValues {
      it >= parameters.minTaskDurationMs.get()
    }

    if (interestingTasks.isEmpty()) {
      return
    }

    val renderedTable = table {
        header {
            row("Task path", "Duration")
        }
        row("Total", (lastTaskEndMs.get() - firstTaskStartMs.get()).milliseconds)
        interestingTasks.forEach { (descriptor, time) ->
            row(descriptor, time.milliseconds)
        }

        cellStyle {
            paddingLeft = 1
            paddingRight = 1
            border = true
        }
    }.renderText(border = TextBorder.ROUNDED)
    println(renderedTable)
  }
}