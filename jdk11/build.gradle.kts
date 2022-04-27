/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

dependencies {
  api(libs.commons.math)

  implementation(libs.guava)

  implementation(libs.bundles.bytebuddy)
  implementation(libs.bundles.okhttp)
  implementation(libs.conscrypt)
  implementation(libs.jna)
  implementation(libs.jnr.ffi)

  implementation(files("lib/spring-jdbc-4.1.6.RELEASE.jar"))

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj)
  testImplementation(libs.testcontainers)
}


java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

tasks.withType<JavaCompile>() {
  options.release.set(11)
}

// pass args this way ./gradlew runSparkline --args="-f numbers"
tasks.register<JavaExec>("runSparkline") {
  dependsOn(tasks.compileJava)
  mainClass.set("sandbox.Sparkline")
  classpath(configurations.runtimeClasspath)
}

tasks.register<JavaExec>("runIsATTY") {
  dependsOn(tasks.compileJava)
  mainClass.set("sandbox.IsATTY")
  classpath(configurations.runtimeClasspath)
}