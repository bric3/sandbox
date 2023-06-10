/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
val jmhProjects = setOf(
  project(":jmh-stuff"),
  project(":jmh-panama")
)
val nativeProjects = setOf(
  project(":native:cmem"),
  project(":native:dlopen"),
  project(":swift-app"),
  project(":swift-library")
)
// lookout for TYPESAFE_PROJECT_ACCESSORS feature preview
val javaProjects = subprojects - jmhProjects - nativeProjects

configure(javaProjects) {
  apply(plugin = "java-library")

  repositories {
    mavenCentral()
  }
}
