/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
@file:Suppress("UnusedImport")

package sandbox

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.assign

fun JavaPluginExtension.dumbConfigureJavaToolChain() {
    toolchain {
        // simple property assignment enabled in kotlin-dsl since Gradle 8.2 RC1
        // https://docs.gradle.org/8.2-rc-1/release-notes.html#kotlin-dsl-improvements
        languageVersion = JavaLanguageVersion.of(11)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}