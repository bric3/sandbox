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
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskState
import kotlin.time.Duration.Companion.nanoseconds

/**
 * Legacy build lifecycle timings, similar in spirit to Gradle's profile report.
 * This relies on listener APIs that are not configuration-cache-compatible, so it is
 * only enabled when buildStats.configurationStats is set to false.
 */
class ConfigurationCacheIncompatibleLifecycleCollector(
  private val settings: Settings,
  private val extension: BuildStatsExtension,
  private val configurationCacheRequested: Provider<Boolean>,
) : BuildListener, ProjectEvaluationListener, TaskExecutionGraphListener, TaskExecutionListener {

  private data class ProjectConfigurationStat(
    val path: String,
    val configurationNanos: Long,
    val executionNanos: Long,
    val failureSummary: String?,
  ) {
    val totalNanos: Long
      get() = configurationNanos + executionNanos
  }

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

    if (!extension.enabled.get() || !extension.configurationStats.get()) {
      return
    }

    extension.sections.get().forEach { section ->
      when (section) {
        BuildStatsSection.LIFECYCLE_TIMINGS -> {
          println("Lifecycle Timings (Configuration Cache Incompatible)")
          println(
            table {
              header {
                row("Phase", "Duration")
              }
              row("Settings script", duration(createdAtNanos, settingsEvaluatedAtNanos))
              row("Project loading", duration(settingsEvaluatedAtNanos, projectsLoadedAtNanos))
              row("Project configuration", duration(projectsLoadedAtNanos, projectsEvaluatedAtNanos))
              row("Task graph calculation", duration(projectsEvaluatedAtNanos, taskGraphReadyAtNanos))
              row("Task execution", duration(taskGraphReadyAtNanos, buildFinishedAtNanos))
              row("Total", duration(createdAtNanos, buildFinishedAtNanos))

              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          )
        }

        BuildStatsSection.PROJECT_CONFIGURATION_TIMINGS -> {
          val projectsByDuration = projectConfigurationStats.values
            .map { project ->
              project.copy(executionNanos = projectExecutionNanos[project.path] ?: project.executionNanos)
            }
            .sortedByDescending { it.totalNanos }
            .take(15)

          if (projectsByDuration.isNotEmpty()) {
            println("Project Configuration Timings (Configuration Cache Incompatible)")
            println(
              table {
                header {
                  row("Project", "Duration", "Execution", "Configuration")
                }
                projectsByDuration.forEach { project ->
                  row(
                    buildProjectCell(project),
                    project.totalNanos.nanoseconds,
                    project.executionNanos.nanoseconds,
                    project.configurationNanos.nanoseconds,
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

        BuildStatsSection.DIAGNOSTICS -> {
          val startParameter = settings.startParameter
          val ccRequested = configurationCacheRequested.orNull ?: reflectiveConfigurationCacheRequested(startParameter)
          println("Build Diagnostics (Configuration Cache Incompatible)")
          println(
            table {
              header {
                row("Signal", "Value")
              }
              row("Projects discovered", projectCount)
              row("Projects evaluated", evaluatedProjectCount)
              row("Projects failed during evaluation", failedProjectCount)
              row("Tasks scheduled", scheduledTaskCount)
              row("Parallel execution", startParameter.isParallelProjectExecutionEnabled)
              row("Configuration on demand", startParameter.isConfigureOnDemand)
              row("Configuration cache requested", ccRequested?.toString() ?: "unknown")
              row("Build failed", result.failure != null)
              row("Warning", diagnosticsWarning(startParameter, ccRequested))

              cellStyle {
                paddingLeft = 1
                paddingRight = 1
                border = true
              }
            }.renderText(border = TextBorder.ROUNDED)
          )
        }

        BuildStatsSection.BUILD_STATS,
        BuildStatsSection.PROJECT_STATS,
        BuildStatsSection.SLOW_TASKS -> Unit
      }
    }
  }

  private fun duration(start: Long, end: Long): String {
    if (start < 0L || end < 0L || end < start) {
      return "n/a"
    }
    return (end - start).nanoseconds.toString()
  }

  private fun diagnosticsWarning(startParameter: StartParameter, configurationCacheRequested: Boolean?): String =
    when {
      configurationCacheRequested == true ->
        "These lifecycle timings rely on deprecated listeners and may not be reliable with configuration cache."

      scheduledTaskCount == 0 ->
        "No tasks were scheduled, so task execution timing is not representative."

      else -> "none"
    }

  private fun reflectiveConfigurationCacheRequested(startParameter: StartParameter): Boolean? =
    runCatching {
      startParameter.javaClass.methods
        .firstOrNull { it.name == "isConfigurationCacheRequested" && it.parameterCount == 0 }
        ?.invoke(startParameter) as? Boolean
    }.getOrNull()

  private fun buildProjectCell(project: ProjectConfigurationStat): String =
    listOfNotNull(project.path, project.failureSummary).joinToString("\n")

  private fun renderFailureSummary(error: Throwable): String {
    val rootCause = generateSequence(error) { it.cause }.last()
    val type = rootCause::class.simpleName ?: rootCause.javaClass.simpleName
    val message = rootCause.message?.replace(Regex("\\s+"), " ")?.trim()
    return listOfNotNull(type, message?.takeIf { it.isNotEmpty() })
      .joinToString(": ")
      .take(180)
  }
}
