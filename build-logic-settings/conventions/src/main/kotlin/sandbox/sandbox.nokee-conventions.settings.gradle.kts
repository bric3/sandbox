/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

// Required for nokee...
// See
// * https://github.com/nokeedev/gradle-native/issues/349
// * https://github.com/nokeedev/gradle-native/issues/350
pluginManagement {
  repositories {
    maven {
      name = "Nokee Release Repository"
      url = uri("https://repo.nokee.dev/release")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    maven {
      name = "Nokee Snapshot Repository"
      url = uri("https://repo.nokee.dev/snapshot")
      mavenContent {
        includeGroupByRegex("dev\\.nokee.*")
        includeGroupByRegex("dev\\.gradleplugins.*")
      }
    }
    gradlePluginPortal()
  }
  val nokeeVersion = "0.4.3129-202303171612.d413fb13"  // found on https://services.nokee.dev/versions/latest-snapshot.json
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id.startsWith("dev.nokee.")) {
        useModule("${requested.id.id}:${requested.id.id}.gradle.plugin:${nokeeVersion}")
      }
    }
  }
}
