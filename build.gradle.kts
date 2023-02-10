/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
val jmhProjects = listOf(project(":jmh-stuff"), project(":jmh-panama"))
// lookout for TYPESAFE_PROJECT_ACCESSORS feature preview
val javaProjects = subprojects - jmhProjects - project(":swift-app") - project(":swift-library")

configure(javaProjects) {
  apply(plugin = "java-library")

  repositories {
    mavenCentral()
  }

  tasks.withType<Test>() {
    useJUnitPlatform()
    testLogging {
      showStandardStreams = true
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      events("skipped", "failed")
    }
  }
}
