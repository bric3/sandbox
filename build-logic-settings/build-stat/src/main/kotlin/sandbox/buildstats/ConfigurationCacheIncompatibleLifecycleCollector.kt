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
import org.gradle.StartParameter
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.execution.TaskExecutionGraphListener
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

/**
 * Legacy build lifecycle timings, similar in spirit to Gradle's profile report.
 * This relies on listener APIs that are not configuration-cache-compatible, so it is
 * only used when configuration-cache-incompatible sections are explicitly requested.
 */
class ConfigurationCacheIncompatibleLifecycleCollector(
  private val settings: Settings,
  private val extension: BuildStatsExtension,
) : BuildListener, ProjectEvaluationListener, TaskExecutionGraphListener, TaskExecutionListener {

  private val createdAtNanos = System.nanoTime()
  private var settingsEvaluatedAtNanos = -1L
  private var projectsLoadedAtNanos = -1L
  private var projectsEvaluatedAtNanos = -1L
  private var taskGraphReadyAtNanos = -1L
  private var buildFinishedAtNanos = -1L

  private var projectCount = 0
  private var scheduledTaskCount = 0
  private var evaluatedProjectCount = 0
  private var failedProjectCount = 0
  private val projectEvaluationStarts = linkedMapOf<String, Long>()
  private val taskExecutionStarts = linkedMapOf<String, Long>()
  private val projectExecutionNanos = linkedMapOf<String, Long>()
  private val projectConfigurationStats = linkedMapOf<String, ProjectConfigurationStat>()

  override fun settingsEvaluated(settings: Settings) {
    settingsEvaluatedAtNanos = System.nanoTime()
  }

  override fun projectsLoaded(gradle: Gradle) {
    projectsLoadedAtNanos = System.nanoTime()
    projectCount = gradle.rootProject.allprojects.size
  }

  override fun beforeEvaluate(project: Project) {
    projectEvaluationStarts[project.path] = System.nanoTime()
  }

  override fun afterEvaluate(project: Project, state: ProjectState) {
    evaluatedProjectCount += 1
    if (state.failure != null) {
      failedProjectCount += 1
    }
    val startNanos = projectEvaluationStarts[project.path]
    if (startNanos != null) {
      projectConfigurationStats[project.path] = ProjectConfigurationStat(
        path = project.path,
        configurationNanos = System.nanoTime() - startNanos,
        executionNanos = projectExecutionNanos[project.path] ?: 0L,
        failureSummary = state.failure?.let(::renderFailureSummary),
      )
    }
  }

  override fun projectsEvaluated(gradle: Gradle) {
    projectsEvaluatedAtNanos = System.nanoTime()
  }

  override fun graphPopulated(graph: TaskExecutionGraph) {
    taskGraphReadyAtNanos = System.nanoTime()
    scheduledTaskCount = graph.allTasks.size
  }

  override fun beforeExecute(task: Task) {
    taskExecutionStarts[task.path] = System.nanoTime()
  }

  override fun afterExecute(task: Task, state: TaskState) {
    val startNanos = taskExecutionStarts.remove(task.path) ?: return
    val durationNanos = System.nanoTime() - startNanos
    projectExecutionNanos.merge(task.project.path, durationNanos, Long::plus)

    val existing = projectConfigurationStats[task.project.path] ?: return
    projectConfigurationStats[task.project.path] = existing.copy(
      executionNanos = projectExecutionNanos[task.project.path] ?: 0L
    )
  }

  @Deprecated("Deprecated in Java")
  override fun buildFinished(result: BuildResult) {
    buildFinishedAtNanos = System.nanoTime()

    if (!extension.enabled.get()) {
      return
    }

    val startParameter = settings.startParameter
    val snapshot = BuildStatsSnapshot(
      sections = extension.sections.get(),
      lifecycleTimings = LifecycleTimings(
        settingsScriptNanos = elapsed(createdAtNanos, settingsEvaluatedAtNanos),
        projectLoadingNanos = elapsed(settingsEvaluatedAtNanos, projectsLoadedAtNanos),
        projectConfigurationNanos = elapsed(projectsLoadedAtNanos, projectsEvaluatedAtNanos),
        taskGraphCalculationNanos = elapsed(projectsEvaluatedAtNanos, taskGraphReadyAtNanos),
        taskExecutionNanos = elapsed(taskGraphReadyAtNanos, buildFinishedAtNanos),
        totalNanos = elapsed(createdAtNanos, buildFinishedAtNanos),
      ),
      projectConfigurationStats = projectConfigurationStats.values
        .map { project ->
          project.copy(executionNanos = projectExecutionNanos[project.path] ?: project.executionNanos)
        },
      diagnostics = Diagnostics(
        projectCount = projectCount,
        evaluatedProjectCount = evaluatedProjectCount,
        failedProjectCount = failedProjectCount,
        scheduledTaskCount = scheduledTaskCount,
        parallelExecutionEnabled = startParameter.isParallelProjectExecutionEnabled,
        configurationOnDemand = startParameter.isConfigureOnDemand,
        configurationCacheRequested = false,
        buildFailed = result.failure != null,
        warning = diagnosticsWarning(startParameter),
      ),
      configurationCacheIncompatible = true,
    )
    BuildStatsRenderer.render(snapshot).forEach(::println)
  }

  private fun diagnosticsWarning(startParameter: StartParameter): String =
    when {
      scheduledTaskCount == 0 ->
        "No tasks were scheduled, so task execution timing is not representative."

      else -> "none"
    }

  private fun renderFailureSummary(error: Throwable): String {
    val rootCause = generateSequence(error) { it.cause }.last()
    val type = rootCause::class.simpleName ?: rootCause.javaClass.simpleName
    val message = rootCause.message?.replace(Regex("\\s+"), " ")?.trim()
    return listOfNotNull(type, message?.takeIf { it.isNotEmpty() })
      .joinToString(": ")
      .take(180)
  }

  private fun elapsed(start: Long, end: Long): Long? =
    if (start < 0L || end < 0L || end < start) null else end - start
}
