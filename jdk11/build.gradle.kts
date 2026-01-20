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
  id("sandbox.java-conventions")
}

dependencies {
  api(libs.commons.math)

  implementation(libs.guava)

  implementation(libs.bundles.bytebuddy)
  implementation(libs.bundles.okhttp)
  implementation(libs.conscrypt)
  implementation(libs.jna)
  implementation(libs.jnr.ffi)

  implementation(files("lib/spring-jdbc-4.1.6.RELEASE.jar"))

  testImplementation(libs.bundles.junit.jupiter)
  testImplementation(libs.assertj)
  testImplementation(libs.testcontainers)
}


javaConvention {
    languageVersion = 11
}

tasks {
  // pass args this way ./gradlew runSparkline --args="-f numbers"
  register<JavaExec>("runSparkline") {
    dependsOn(compileJava)
    mainClass.set("sandbox.Sparkline")
    classpath(configurations.runtimeClasspath)
  }

  register<JavaExec>("runIsATTY") {
    dependsOn(compileJava)
    mainClass.set("sandbox.IsATTY")
    classpath(configurations.runtimeClasspath)
  }
}
