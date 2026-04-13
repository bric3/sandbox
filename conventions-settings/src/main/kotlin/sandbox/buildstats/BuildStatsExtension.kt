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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/**
 * DSL for the `buildStats` settings extension.
 *
 * Example:
 * ```
 * buildStats {
 *   enabled.set(true)
 *   sections.set(
 *     listOf(
 *       BuildStatsSection.BUILD_STATS,
 *       BuildStatsSection.SLOW_TASKS,
 *       BuildStatsSection.DIAGNOSTICS,
 *     )
 *   )
 *   minTaskDurationMillis.set(250)
 * }
 * ```
 */
abstract class BuildStatsExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Enables or disables all build-stats reporting.
   */
  val enabled = objects.property(Boolean::class.java).convention(true)

  /**
   * Ordered list of report sections to render.
   *
   * The order of this list is the order used in the final report. Sections that require
   * configuration-cache-incompatible listeners are omitted automatically when configuration
   * cache is requested.
   */
  val sections: ListProperty<BuildStatsSection> = objects.listProperty(BuildStatsSection::class.java)
    .convention(
      listOf(
        BuildStatsSection.BUILD_STATS,
        BuildStatsSection.PROJECT_STATS,
        BuildStatsSection.SLOW_TASKS,
        BuildStatsSection.LIFECYCLE_TIMINGS,
        BuildStatsSection.PROJECT_CONFIGURATION_TIMINGS,
        BuildStatsSection.DIAGNOSTICS,
      )
    )

  /**
   * Minimum task duration, in milliseconds, for a task to appear in the `SLOW_TASKS` section.
   */
  val minTaskDurationMillis = objects.property(Long::class.java).convention(500)
}
