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
  // Playing with graal compiler
  // commented because it messes with the plugin convention
  //id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
}

javaConvention {
  languageVersion = 19
  addedModules = setOf("jdk.incubator.concurrent")
}

dependencies {
  implementation(libs.bundles.flightRecorder)
  implementation(libs.flexmark.all)
}

tasks.withType<JavaExec>().configureEach {
  group = "class-with-main"
  environment.putAll(
    mapOf(
      "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!, // for IntelliJ run main class
      // "JAVA_LIBRARY_PATH" to ".:/usr/local/lib",
      "FINNHUB_TOKEN" to System.getenv("FINNHUB_TOKEN"),
      "HTTP_CLIENT_CARRIER_THREADS" to System.getenv("HTTP_CLIENT_CARRIER_THREADS"),
    )
  )
}

//graal {
//    version = libs.versions.graalvm.get()
//}
