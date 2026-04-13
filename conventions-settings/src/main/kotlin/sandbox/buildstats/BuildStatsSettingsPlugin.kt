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
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import javax.inject.Inject

/**
 * This whole plugin is completely experimental, beware if you get inspired from it.
 */
class BuildStatsSettingsPlugin @Inject constructor(
  private val objects: ObjectFactory,
  private val buildEventsListenerRegistry: BuildEventsListenerRegistry
) : Plugin<Settings> {
  override fun apply(settings: Settings) {
    val statsExtension = settings.extensions.create(
      BUILD_STATS_EXTENSION,
      BuildStatsExtension::class.java,
      objects
    )

    settings.gradle.taskGraph.whenReady {
      val statsListener = settings.gradle.sharedServices.registerIfAbsent(
        STATS_LISTENER_SERVICE,
        StatisticsService::class.java
      ) {
        parameters.minTaskDurationMs.set(statsExtension.minTaskDurationMillis)
      }

      buildEventsListenerRegistry.onTaskCompletion(statsListener)
    }

    val statsListener = settings.gradle.sharedServices.registerIfAbsent(
      STATS_REPORTER_SERVICE,
      WorkInProgress_BuildStatsReporter::class.java
    ) {
      parameters.enabled.set(statsExtension.enabled)
    }.map {
      it.settings = settings
      it
    }

    settings.gradle.addListener(BuildStatsCollector(statsListener))

    settings.gradle.addListener(object : ProgressListener {
      override fun statusChanged(event: ProgressEvent) {
        println("${event::class.simpleName} ~> $event")
      }
    })
  }

  companion object {
    const val BUILD_STATS_EXTENSION = "buildStats"
    const val STATS_REPORTER_SERVICE = "statsReporter"
    const val STATS_LISTENER_SERVICE = "statsListener"
  }
}

