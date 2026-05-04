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
  id("sandbox.build-stats")
  id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
  id("com.gradle.develocity") version "4.4.1"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    // termsOfUseAgree is handled by .gradle/init.d/configure-develocity.init.gradle.kts
  }
}
