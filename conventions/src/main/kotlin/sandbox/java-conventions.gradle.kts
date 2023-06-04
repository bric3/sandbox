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

repositories {
    mavenCentral()
}

// Note: simple property assignment enabled in kotlin-dsl since Gradle 8.2 RC1
// https://docs.gradle.org/8.2-rc-1/release-notes.html#kotlin-dsl-improvements

val javaConventions: JavaConventionExtension =
    project.extensions.create(
        "javaConvention",
        JavaConventionExtension::class.java
    )
javaConventions.languageVersion.convention(11)
javaConventions.useRelease.convention(true)

//val javaToolchains: JavaToolchainService = extensions.getByType()

java {
    toolchain {
        languageVersion.set(javaConventions.languageVersion.map(JavaLanguageVersion::of))
        javaConventions.jvmVendor.orNull?.let { vendor.set(it) }
    }
}

tasks {
    // Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
    withType<JavaExec>().configureEach {
        //group = "class-with-main"
        classpath(sourceSets.main.get().runtimeClasspath)

        // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
        jvmArgs(
            "-ea",
            "--enable-preview",
        )

        jvmArgumentProviders.add(
            javaConventions.addedModules
                .map {
                    CommandLineArgumentProvider {
                        it.map { "--add-modules=$it" }
                    }
                }
                .get()
        )
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (javaConventions.useRelease.get()) {
            options.release.set(javaConventions.languageVersion)
        } else {
            sourceCompatibility = javaConventions.languageVersion.get().toString()
            targetCompatibility = javaConventions.languageVersion.get().toString()
        }
        options.compilerArgs.addAll(
            listOf(
                "--enable-preview",
                "-Xlint:preview",
            )
        )
        options.compilerArgumentProviders.add(
            javaConventions.addedModules
                .map {
                    CommandLineArgumentProvider {
                        it.map { "--add-modules=$it" }
                    }
                }
                .get()
        )
    }

    register("javaConvention", PrintJavaConventionTask::class.java) {
        languageVersion.set(javaConventions.languageVersion)
        useRelease.set(javaConventions.useRelease)
        jvmVendor.set(javaConventions.jvmVendor)
        addedModules.set(javaConventions.addedModules)
    }

    register("showToolchain") {
        doLast {
            val launcher = javaToolchains.launcherFor(java.toolchain).get()

            println(launcher.executablePath)
            println(launcher.metadata.installationPath)
        }
    }
}

// workaround for https://youtrack.jetbrains.com/issue/IDEA-316081/Gradle-8-toolchain-error-Toolchain-from-executable-property-does-not-match-toolchain-from-javaLauncher-property-when-different
gradle.taskGraph.whenReady {
    val ideRunTask = allTasks.find { it.name.endsWith(".main()") } as? JavaExec
    // note that javaLauncher property is actually correct
    ideRunTask?.setExecutable(javaToolchains.launcherFor(java.toolchain).get().executablePath.asFile.absolutePath)
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events("skipped", "failed")
    }
}

abstract class PrintJavaConventionTask : DefaultTask() {
    @get:Input
    abstract val languageVersion: Property<Int>

    @get:Input
    abstract val useRelease: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val jvmVendor: Property<JvmVendorSpec>

    @get:Input
    abstract val addedModules: ListProperty<String>

    @TaskAction
    fun run() {
        logger.lifecycle(
            """
            languageVersion: ${languageVersion.get()}
            compile with release flag: ${useRelease.get()}
            jvmVendor: ${jvmVendor.orNull ?: "unset"}
            addedModules: ${addedModules.get()}
        """.trimIndent()
        )
    }
}