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

enum class TaskOutcome {
  EXECUTED,
  UP_TO_DATE,
  FROM_CACHE,
  SKIPPED,
  NO_SOURCE,
  FAILED,
}

data class TaskStat(
  val path: String,
  val durationMs: Long,
  val outcome: TaskOutcome,
) {
  val projectPath: String
    get() = path.substringBeforeLast(":", missingDelimiterValue = ":").ifBlank { ":" }
}

data class ProjectTaskStat(
  val path: String,
  val durationMs: Long,
  val taskCount: Int,
  val executedCount: Int,
  val cacheHitCount: Int,
  val failedCount: Int,
)

data class ProjectConfigurationStat(
  val path: String,
  val configurationNanos: Long,
  val executionNanos: Long,
  val failureSummary: String?,
) {
  val totalNanos: Long
    get() = configurationNanos + executionNanos
}

data class LifecycleTimings(
  val settingsScriptNanos: Long?,
  val projectLoadingNanos: Long?,
  val projectConfigurationNanos: Long?,
  val taskGraphCalculationNanos: Long?,
  val taskExecutionNanos: Long?,
  val totalNanos: Long?,
)

data class Diagnostics(
  val projectCount: Int,
  val evaluatedProjectCount: Int,
  val failedProjectCount: Int,
  val scheduledTaskCount: Int,
  val parallelExecutionEnabled: Boolean,
  val configurationOnDemand: Boolean,
  val configurationCacheRequested: Boolean?,
  val buildFailed: Boolean,
  val warning: String,
)

data class BuildStatsSnapshot(
  val sections: List<BuildStatsSection>,
  val taskStats: List<TaskStat> = emptyList(),
  val totalDurationMs: Long? = null,
  val lifecycleTimings: LifecycleTimings? = null,
  val projectConfigurationStats: List<ProjectConfigurationStat> = emptyList(),
  val diagnostics: Diagnostics? = null,
  val configurationCacheIncompatible: Boolean = false,
  val minTaskDurationMs: Long = 500,
)
