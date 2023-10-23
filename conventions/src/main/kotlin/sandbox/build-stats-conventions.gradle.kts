/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox

import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

// inspired by https://github.com/gradle/gradle/issues/20151#issuecomment-1751927564

abstract class StatisticsService :
    BuildService<StatisticsService.Parameters>,
    OperationCompletionListener {

    interface Parameters : BuildServiceParameters {
        var lastTask: String
        var timeStatistics: MutableMap<String, Long>
    }

    override fun onFinish(event: FinishEvent) {
        val taskName = event.descriptor.name
        val operationResult = event.result
        val durationSeconds = TimeUnit.MILLISECONDS.toSeconds(operationResult.endTime - operationResult.startTime)

        parameters.timeStatistics.putIfAbsent(taskName, durationSeconds)
        println("[$taskName] time elapsed: ${durationSeconds}s")

        if (operationResult is FailureResult || taskName == parameters.lastTask) {
            println(
                parameters.timeStatistics
                    .toList()
                    .sortedByDescending { (_, duration) -> duration }
                    .toMap()
                    .asSequence()
                    .map { (task, duration) -> println("[${task.padEnd(32)}] : ${duration}s") }
                    .joinToString(prefix = "Task Time Statistics")
            )
        }
    }
}

gradle.taskGraph.whenReady {
    val lastTask = gradle.taskGraph.allTasks.last().path

    val timeStatistics: MutableMap<String, Long> = mutableMapOf()

    val statsListener = gradle.sharedServices.registerIfAbsent(
        "statsListener ", StatisticsService::class
    ) {
        parameters.lastTask = lastTask
        parameters.timeStatistics = timeStatistics
    }

    gradle.serviceOf<BuildEventsListenerRegistry>().onTaskCompletion(statsListener)
}