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
    `java-base`
}

val graalCompilerModulePath by configurations.creating {
    description = "Graal compiler module path"
    isCanBeResolved = true
}

val graalDepsModulePath by configurations.creating {
    description = "Graal SDK and Truffle module path"
    isCanBeResolved = true
}

dependencies {
    graalCompilerModulePath(libs.bundles.graal.compiler)
    graalDepsModulePath(libs.bundles.graal.deps)
}

tasks {
    val prepareGraalCompilerModulePath by registering(Copy::class) {
        from(graalCompilerModulePath.files)
        into(project.layout.buildDirectory.dir("graal-compiler-path"))
    }

    val prepareGraalDepsModulePath by registering(Copy::class) {
        from(graalCompilerModulePath.files)
        into(project.layout.buildDirectory.dir("graal-deps-path"))
    }

    withType(JavaExec::class) {
        dependsOn(prepareGraalCompilerModulePath, prepareGraalDepsModulePath)

        // https://www.graalvm.org/23.1/reference-manual/js/RunOnJDK/
        jvmArgs(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+EnableJVMCI", // enables the interface, but does not enable the compiler - except for GraalVM workloads (Truffle-based language engines, like Graal.js).
            "-XX:+UseJVMCICompiler", // uses the compiler provided via JVMCI (GraalVM Compiler, most likely) for all workloads.
            "--module-path=${prepareGraalDepsModulePath.get().destinationDir}",
            "--upgrade-module-path=${prepareGraalCompilerModulePath.get().destinationDir}",
        )
    }
}