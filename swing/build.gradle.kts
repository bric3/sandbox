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
    id("sandbox.kotlin-conventions")
    id("sandbox.test-conventions")
}

javaConvention {
    languageVersion = 21
}

// Downloaded on 2024-04-05 from https://www.jhlabs.com/ip/filters/download.html
sourceSets {
  create("jhLabs")
}

dependencies {
  // implementation("com.jhlabs:filters:2.0.235-1")
  implementation(sourceSets["jhLabs"].output)
}

