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
    // Keep the embedded Kotlin JVM plugin on the included-build parent classpath so
    // `build-stat` can apply it without loading a second Kotlin plugin classpath
    // alongside the `kotlin-dsl` plugin used by `conventions`.
    embeddedKotlin("jvm") apply false
}
