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
import org.gradle.kotlin.dsl.the

/**
 * https://github.com/gradle/gradle/issues/15383
 */
val Project.libs
    get() = the<org.gradle.accessors.dm.LibrariesForLibs>()
