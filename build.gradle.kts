/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
plugins {
  id("com.github.ben-manes.versions") version ("0.42.0")
}

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

  tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    fun isNonStable(version: String): Boolean {
      val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
      val regex = "^[0-9,.v-]+(-r)?$".toRegex()
      val isStable = stableKeyword || regex.matches(version)
      return isStable.not()
    }

    rejectVersionIf {
      // disallow release candidates as upgradable versions from stable versions
      isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
  }
}
