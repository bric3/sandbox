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

import org.gradle.api.Plugin
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Uses Gradle's build-events listener registry so task reporting still works when configuration
 * cache is enabled. Older listener APIs such as BuildListener/TaskExecutionListener do not.
 */
class BuildStatsSettingsPlugin @Inject constructor(
  private val objects: ObjectFactory,
  private val buildEventsListenerRegistry: BuildEventsListenerRegistry,
  private val buildFeatures: BuildFeatures,
) : Plugin<Settings> {
  override fun apply(settings: Settings) {
    val statsExtension = settings.extensions.create(
      BUILD_STATS_EXTENSION,
      BuildStatsExtension::class.java,
      objects
    )
    statsExtension.configurationStats.convention(
      buildFeatures.configurationCache.requested
        .orElse(true)
    )

    val statsListener = settings.gradle.sharedServices.registerIfAbsent(
      STATS_LISTENER_SERVICE,
      StatisticsService::class.java
    ) {
      parameters.enabled.set(statsExtension.enabled)
      parameters.sections.set(statsExtension.sections)
      parameters.minTaskDurationMs.set(statsExtension.minTaskDurationMillis)
    }

    buildEventsListenerRegistry.onTaskCompletion(statsListener)

    settings.gradle.addListener(
      ConfigurationCacheIncompatibleLifecycleCollector(
        settings = settings,
        extension = statsExtension,
        configurationCacheRequested = buildFeatures.configurationCache.requested,
      )
    )
  }

  companion object {
    const val BUILD_STATS_EXTENSION = "buildStats"
    const val STATS_LISTENER_SERVICE = "statsListener"
  }
}
