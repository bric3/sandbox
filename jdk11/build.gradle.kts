/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import java.util.*

plugins {
  alias(libs.plugins.download)
  id("sandbox.java-conventions")
}

dependencies {
  api(libs.commons.math)

  implementation(libs.guava)

  implementation(libs.bundles.bytebuddy)
  implementation(libs.bundles.okhttp)
  implementation(libs.bundles.graal.js)
  implementation(libs.conscrypt)
  implementation(libs.jna)
  implementation(libs.jnr.ffi)

  implementation(files("lib/spring-jdbc-4.1.6.RELEASE.jar"))

  testImplementation(libs.junit.jupiter)
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

  val downloadPegjs by registering(de.undercouch.gradle.tasks.download.Download::class) {
    // https://cdnjs.com/libraries/pegjs/0.9.0
    src("https://cdn.jsdelivr.net/npm/peggy@2.0.1/browser/peggy.min.js")
    dest(file("${sourceSets.main.get().output.resourcesDir}/peg.min.js"))
    onlyIfModified(true)
    useETag(true) // Use the ETag on GH
  }

  val verifyPegjs by registering(de.undercouch.gradle.tasks.download.Verify::class) {
    dependsOn(downloadPegjs)
    src(file("${sourceSets.main.get().output.resourcesDir}/peg.min.js"))
    // https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity
    // https://cdn.jsdelivr.net/npm/peggy@2.0.1/browser/peggy.min.js
    val pegJsSri = "sha256-rc8x8rCn3MX/3jqUGfnqeIurZfNJY2vuHuoP7UfQyV8="
    algorithm(
      when(pegJsSri.substringBefore('-')) {
        "sha256" -> "SHA-256"
        "sha384" -> "SHA-384"
        "sha512" -> "SHA-512"
        else -> throw IllegalArgumentException("Unknown algorithm")
      }
    )
    checksum(
      Base64.getDecoder().decode(pegJsSri.substringAfter('-')).joinToString("") {
        "%02x".format(it)
      }
    )
  }

  processResources {
    dependsOn(verifyPegjs)
  }
}
