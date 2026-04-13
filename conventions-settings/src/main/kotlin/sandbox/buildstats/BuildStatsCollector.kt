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

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.tasks.TaskStateInternal
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskState
import kotlin.reflect.full.findAnnotations

typealias ProjectPath = String
typealias TaskPath = String

class BuildStatsCollector(private val buildStatsReporter: Provider<WorkInProgress_BuildStatsReporter>) :
  BuildListener,
  ProjectEvaluationListener,
  TaskExecutionGraphListener,
  // TaskExecutionListener is not supported when configuration caching is enabled.
  TaskExecutionListener {

  private var rootProjectName: String = ""
  private var failed = false

  // timeframe
  val startedAtNanos: Long = System.nanoTime()
  var endedAtNanos: Long = -1

  // val startedAtZD: ZonedDateTime = ZonedDateTime.now()
  // val endedAtZD: ZonedDateTime? = null
  var settingsEvaluatedNanos: Long = -1
  var projectLoadedNanos: Long = -1
  var projectsEvaluatedNanos: Long = -1
  var graphPopulatedNanos: Long = -1

  val projectStats = linkedMapOf<ProjectPath, ProjectStats>()

  // region Collect init timings
  override fun settingsEvaluated(settings: Settings) {
    settingsEvaluatedNanos = System.nanoTime()
    rootProjectName = settings.rootProject.name
  }

  override fun projectsLoaded(gradle: Gradle) {
    projectLoadedNanos = System.nanoTime()
  }

  override fun projectsEvaluated(gradle: Gradle) {
    projectsEvaluatedNanos = System.nanoTime()
  }
  // endregion

  // region Collect per-project timings
  override fun beforeEvaluate(project: Project) {
    projectStats.computeIfAbsent(project.path) { it -> ProjectStats(project) }
  }

  override fun afterEvaluate(project: Project, state: ProjectState) {
    projectStats[project.path]!!.run {
      afterEvaluateAtNanos = System.nanoTime()
      failed = state.failure != null
    }
  }
  // endregion

  // region Collect tasks timings
  override fun graphPopulated(graph: TaskExecutionGraph) {
    graphPopulatedNanos = System.nanoTime()

    graph.allTasks.groupBy { it.project.path }.forEach { (projectPath, tasks) ->
      projectStats[projectPath]!!.tasksToExecute + tasks.map { it.path }
    }
  }

  override fun beforeExecute(task: Task) {
    projectStats[task.project.path]!!.taskStats.computeIfAbsent(task.path) { TaskStats(task) }
  }

  override fun afterExecute(task: Task, state: TaskState) {
    projectStats[task.project.path]!!.taskStats[task.path]!!.run {
      afterExecuteNanos = System.nanoTime()
      captureTaskOutcome(state)
    }
  }
  // endregion

  @Deprecated("Deprecated in Java")
  override fun buildFinished(result: BuildResult) {
    endedAtNanos = System.nanoTime()
    failed = result.failure != null
    buildStatsReporter.get().emitReport(this)
  }

  data class ProjectStats(val project: Project) {
    val beforeEvaluateNanos: Long = System.nanoTime()
    var afterEvaluateAtNanos: Long = beforeEvaluateNanos
    val path = project.path
    val name = project.name
    var failed = false
    val taskStats = mutableMapOf<TaskPath, TaskStats>()
    val tasksToExecute = mutableListOf<TaskPath>()

    val hadTasksToExecute get() = tasksToExecute.isNotEmpty()
    val hasFailedTasks get() = project.state.failure != null || taskStats.values.any { it.hasFailed }
    val hasSucceeded get() = project.state.executed && taskStats.values.all { it.hasExecuted }
  }

  class TaskStats(task: Task) {
    val beforeExecuteNanos: Long = System.nanoTime()
    var afterExecuteNanos: Long = beforeExecuteNanos
    val path = task.path
    val name = task.name

    val isCacheable = task::class.findAnnotations(CacheableTask::class)

    var hasExecuted: Boolean = false
    var wasSkipped: Boolean = false
    var wasUpToDate: Boolean = false
    var didWork: Boolean = false
    var hasNoSource: Boolean = false
    var hasFailed: Boolean = false
    var wasFromCache: Boolean = false

    fun captureTaskOutcome(state: TaskState) {
      hasExecuted = state.executed
      wasSkipped = state.skipped
      wasUpToDate = state.upToDate
      didWork = state.didWork
      hasNoSource = state.noSource
      hasFailed = state.failure != null

      if (state is TaskStateInternal) {
        wasFromCache = state.isFromCache
      }
    }
  }
}
