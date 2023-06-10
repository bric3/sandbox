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
    kotlin("jvm") version "1.8.22"
}

kotlin {
    jvmToolchain(19) // kotlin 1.8.21 does not support JDK 20
}

dependencies {
    implementation("com.jhlabs:filters:2.0.235-1")
}
