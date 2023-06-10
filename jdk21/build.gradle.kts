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
    id("sandbox.java-conventions")
}

javaConvention {
    languageVersion = 21
}

dependencies {
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
    group = "class-with-main"

    environment = mapOf(
        "JAVA_LIBRARY_PATH" to sourceSets.main.get().output.resourcesDir!!, // for IntelliJ run main class
        // "JAVA_LIBRARY_PATH" to ".:/usr/local/lib",
        "FINNHUB_TOKEN" to System.getenv("FINNHUB_TOKEN"),
        "HTTP_CLIENT_CARRIER_THREADS" to System.getenv("HTTP_CLIENT_CARRIER_THREADS"),
    )
}
