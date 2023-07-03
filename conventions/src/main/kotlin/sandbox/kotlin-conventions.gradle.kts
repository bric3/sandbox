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
    java
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

kotlinExtension.apply {
    jvmToolchain(19) // kotlin 1.8.21 does not support JDK 20
}

// workaround for https://youtrack.jetbrains.com/issue/IDEA-316081/Gradle-8-toolchain-error-Toolchain-from-executable-property-does-not-match-toolchain-from-javaLauncher-property-when-different
gradle.taskGraph.whenReady {
    val ideRunTask = allTasks.find { it.name.endsWith(".main()") } as? JavaExec
    // note that javaLauncher property is actually correct
    @Suppress("UsePropertyAccessSyntax") // otherwise fails with: 'Val cannot be reassigned'
    ideRunTask?.setExecutable(javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath)
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
    withType<JavaExec>().configureEach {
        // group = "class-with-main"
        classpath(sourceSets.main.get().runtimeClasspath)

        // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }
}

tasks.test {
    useJUnitPlatform()
}
