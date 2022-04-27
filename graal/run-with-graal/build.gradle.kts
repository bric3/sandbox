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
  // Playing with graal compiler
  id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"
}

graal {
  version = libs.versions.graalvm.get()
}