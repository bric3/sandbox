/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // doc: https://docs.gradle.org/current/userguide/kotlin_dsl.html
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("buildStats") {
            id = "sandbox.build-stats"
            implementationClass = "sandbox.buildstats.BuildStatsSettingsPlugin"
        }
    }
}

dependencies {
  // Expose typed accessors for external plugins used by precompiled settings scripts.
  implementation(libs.gradleplugin.foojay.resolver.convention)
  implementation(libs.gradleplugin.develocity)
  // implementation(libs.gradleplugin.kotlin.jvm)
  implementation("com.jakewharton.picnic:picnic:0.7.0")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

java {
    javaToolchains {
        version = JavaLanguageVersion.of(17)
    }
}
