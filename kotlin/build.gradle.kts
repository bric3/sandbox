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
  id("sandbox.kotlin-conventions")
}

repositories {
  mavenCentral()
  maven("https://packages.jetbrains.team/maven/p/kds/kotlin-ds-maven")
}

dependencies {
  implementation(kotlin("stdlib", libs.versions.kotlin.get()))
  implementation(kotlin("reflect", libs.versions.kotlin.get()))

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinCoroutines.get()}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${libs.versions.kotlinCoroutines.get()}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${libs.versions.kotlinCoroutines.get()}")

  implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.11.1")
  implementation("org.jetbrains.lets-plot:lets-plot-batik:4.7.3")
  implementation("org.jetbrains.kotlinx:kandy-lets-plot:0.8.0")
  implementation("org.jetbrains.kotlinx:kandy-util:0.8.0")
  // currently on a private repository, see https://github.com/Kotlin/kandy/issues/284
  implementation("org.jetbrains.kotlinx:kotlin-statistics-jvm:0.4.0")

  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinCoroutines.get()}")
  testImplementation(platform("org.junit:junit-bom:${libs.versions.junit.jupiter.get()}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("app.cash.turbine:turbine:1.2.1")

  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.jupiter)
  testImplementation(libs.mockito.kotlin)
}
