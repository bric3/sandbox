/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import dev.nokee.platform.nativebase.ExecutableBinary
import org.gradle.internal.os.OperatingSystem

plugins {
    id("dev.nokee.c-application")
    java
}

application {
    baseName.set("test-dlopen")
    binaries.configureEach(ExecutableBinary::class.java) {
        linkTask {
            // dynamic loader
            // https://blog.stephenmarz.com/2020/06/22/dynamic-linking/
            linkerArgs.add("-ldl")
        }
    }
}

val javaHome = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(11))
}.map { it.metadata.installationPath }

tasks {
    register<Exec>("run-test-dlopen") {
        dependsOn(assemble)
        group = "other"


        val os = OperatingSystem.current()
        environment = mapOf(
            when {
                os.isLinux -> "LD_LIBRARY_PATH"
                os.isMacOsX -> "DYLD_LIBRARY_PATH"
                else -> throw UnsupportedOperationException("LD LIBRARY PATH is not supported on current os $os")
            } to "${javaHome.get()}/lib/server",
        )

        val binary = application.binaries.withType(ExecutableBinary::class.java).get().single()
        executable(
            layout.buildDirectory.file("exes/${sourceSets.main.name}/${binary.baseName.get()}").get().toString()
        )
    }
}