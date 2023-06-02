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

plugins {
    id("java-library")
}

// simple property assignment enabled in kotlin-dsl since Gradle 8.2 RC1
// https://docs.gradle.org/8.2-rc-1/release-notes.html#kotlin-dsl-improvements

//val javaToolchains: JavaToolchainService = extensions.getByType()


fun JavaPluginExtension.configureJavaToolChain(
    javaVersion: Int,
    useRelease: Boolean = true,
    vendorSpec: JvmVendorSpec?
) {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
        vendorSpec?.let { vendor = it }
    }

    tasks.withType<JavaCompile> {
        if (useRelease) {
            options.release = javaVersion
        } else {
            targetCompatibility = javaVersion.toString()
            sourceCompatibility = javaVersion.toString()
        }
        options.compilerArgs.addAll(
            listOf(
                "--enable-preview",
                "-Xlint:preview",
            )
        )
    }

    tasks.withType<JavaExec>().configureEach {
        group = "class-with-main"
        classpath(sourceSets.main.get().runtimeClasspath)

        // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
        jvmArgs(
            "-ea",
            "--enable-preview",
        )
    }

    // workaround for https://youtrack.jetbrains.com/issue/IDEA-316081/Gradle-8-toolchain-error-Toolchain-from-executable-property-does-not-match-toolchain-from-javaLauncher-property-when-different
    gradle.taskGraph.whenReady {
        val ideRunTask = allTasks.find { it.name.endsWith(".main()") } as? JavaExec
        // note that javaLauncher property is actually correct
        ideRunTask?.setExecutable(javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

//tasks {
//    withType<JavaExec> {
//
//    }
//
//    withType<JavaCompile> {
//        options.encoding = "UTF-8"
//        options.release.set(20)
//    }
//}

tasks.test {
    useJUnitPlatform()
}