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
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.9.0")
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
    }
    pluginManagement {
        repositories {
            gradlePluginPortal()
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "conventions"