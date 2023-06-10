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

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlinExtension.apply {
    jvmToolchain(19) // kotlin 1.8.21 does not support JDK 20
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            //jvmTarget.set(project.JVM_LANGUAGE_LEVEL.map { JvmTarget.fromTarget(it.toString()) })
            //
            // Genral compiler options https://kotlinlang.org/docs/compiler-reference.html
            // Default methods in interfaces https://kotlinlang.org/docs/java-to-kotlin-interop.html#default-methods-in-interfaces
            // JSR 305 for null safety https://kotlinlang.org/docs/reference/java-interop.html#compiler-configuration
            freeCompilerArgs.addAll(
                listOf(
                    "-Xjvm-default=all-compatibility",
                    "-java-parameters",
                    "-Xjsr305=strict",
                    "-Xopt-in=kotlin.RequiresOptIn",
                )
            )
        }
    }
}

tasks.test {
    useJUnitPlatform()
}