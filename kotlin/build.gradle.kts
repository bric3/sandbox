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
    java
    kotlin("jvm") version "1.8.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinCoroutines.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${libs.versions.kotlinCoroutines.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${libs.versions.kotlinCoroutines.get()}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinCoroutines.get()}")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("app.cash.turbine:turbine:0.12.1")
}

tasks.test {
    useJUnitPlatform()
}