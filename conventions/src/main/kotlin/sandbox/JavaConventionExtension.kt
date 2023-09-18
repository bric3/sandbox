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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JvmVendorSpec

/**
 * Extension for configuring the java conventions for a project.
 */
interface JavaConventionExtension {
    /**
     * Configure the java version for this project, sets the toolchain, compiler, and java launcher
     *
     * Defaults to `11`.
     */
    val languageVersion: Property<Int>

    //val languageVersion: Property<JavaVersion>

    /**
     * Whether to use the `--release` option for compilation, or use `sourceCompatibility` and `targetCompatibility`.
     *
     * Defaults to `true`.
     */
    val useRelease: Property<Boolean>

    /**
     * Whether to use the `--enable-preview` option for compilation and execution.
     *
     * Defaults to `true`.
     */
    val enablePreview: Property<Boolean>

    /**
     * Tweak the JVM vendor for this project.
     *
     * Unset by default.
     */
    val jvmVendor: Property<JvmVendorSpec>

    /**
     * List of modules to add to the module path.
     *
     * Each module will be added using `--add-modules={module}` option
     * to the **compiler** and **java** exec.
     *
     * Defaults to an empty list.
     */
    val addedModules: ListProperty<String>

    /**
     * List of module package to open and their target modules.
     *
     * Each module will be opened using `--open-modules={module package}={target module}` option
     * to the **compiler** and **java** exec.
     *
     * Defaults to an empty list.
     */
    val openedModules: MapProperty<String, String>
}
