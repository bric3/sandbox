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
import dev.nokee.platform.nativebase.internal.linking.NativeLinkTaskUtils

plugins {
    id("dev.nokee.c-application")
}

application {
    baseName.set("test-cmem")
//    cSources.setFrom(fileTree("srcs") { include("**/*.c") })		// <2>
//    privateHeaders.setFrom(fileTree("hdrs") { include("**/*.h") })	// <3>
//    publicHeaders.setFrom(fileTree("incs") { include("**/*.h") })	// <4>
}