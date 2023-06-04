/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.property

/**
 * Configure the java version for this project, sets the toolchain, compiler, and java launcher
 *
 * Defaults to `11`.
 */
val Project.javaVersion: Property<Int>
    get() = objects.property(Int::class).convention(11)
//val Project.javaVersion: Property<JavaVersion>
//    get() = objects.property(JavaVersion::class).convention(JavaVersion.VERSION_11)

/**
 * Whether to use the `--release` option for compilation, or use `sourceCompatibility` and `targetCompatibility`.
 *
 * Defaults to `true`.
 */
val Project.javaUseRelease: Property<Boolean>
    get() = objects.property(Boolean::class).convention(true)

/**
 * Tweak the JVM vendor for this project.
 *
 * Unset by default.
 */
val Project.jvmVendor: Property<JvmVendorSpec>
    get() = objects.property(JvmVendorSpec::class) // .convention(JvmVendorSpec.ADOPTIUM)

/**
 * List of modules to add to the module path.
 *
 * Each module will be added as this option `--add-modules=<module>`
 * to the compiler and java exec.
 *
 * Defaults to an empty list.
 */
val Project.addedModules: ListProperty<String>
    get() = objects.listProperty(String::class.java).convention(emptyList())

