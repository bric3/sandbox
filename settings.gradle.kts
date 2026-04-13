/*
 * sandbox
 *
 * Copyright (c) 2021, today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

pluginManagement {
  includeBuild("conventions-settings")
}

plugins {
  id("sandbox.conventions")
  id("sandbox.nokee-conventions")
}

// Doc https://docs.gradle.org/7.2/userguide/platforms.html
// API https://docs.gradle.org/7.2/javadoc/org/gradle/api/initialization/dsl/VersionCatalogBuilder.html

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "sandbox"

includeBuild("conventions")
include("kotlin")
include("jmh-stuff", "jmh-panama")
include("jdk11", "jdk17", "jdk21", "jdk25")
include("native:cmem", "native:dlopen", "native:cacheline")
include("jmh-panama")
include("graal:run-with-graal", "graal:run-with-libgraal")
include("swing")

// Swift language broken on sequoia (swift 6)
// See https://github.com/gradle/gradle-native/issues/1116
val os = DefaultNativePlatform.getCurrentOperatingSystem()
if (os.isMacOsX) {
  include(
    "swift-app",
    "swift-library",
  )
}
