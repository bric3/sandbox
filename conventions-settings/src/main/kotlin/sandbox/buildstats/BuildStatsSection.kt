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

/**
 * Ordered report sections that can be rendered by the `buildStats` extension.
 */
enum class BuildStatsSection {
  /** Summary of observed task outcomes and overall build span. */
  BUILD_STATS,

  /** Per-project aggregation of observed task execution outcomes and time. */
  PROJECT_STATS,

  /** Slowest observed tasks, including execution reasons when Gradle provides them. */
  SLOW_TASKS,

  /** Build lifecycle phase timings gathered from configuration-cache-incompatible listeners. */
  LIFECYCLE_TIMINGS,

  /** Per-project configuration timings gathered from configuration-cache-incompatible listeners. */
  PROJECT_CONFIGURATION_TIMINGS,

  /** Extra build diagnostics such as task count, parallelism, and configuration-cache state. */
  DIAGNOSTICS,
}
