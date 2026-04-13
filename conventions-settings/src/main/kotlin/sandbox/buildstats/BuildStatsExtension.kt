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

abstract class BuildStatsExtension @Inject constructor(objects: ObjectFactory) {
  val enabled = objects.property(Boolean::class.java).convention(true)
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

  val minTaskDurationMillis = objects.property(Long::class.java).convention(500)
}
