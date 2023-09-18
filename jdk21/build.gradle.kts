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
    openedModules = mapOf(
        "java.base/jdk.internal.vm" to "ALL-UNNAMED"
    )
}

dependencies {
    testImplementation(libs.bundles.junit.jupiter)
    testImplementation(libs.bundles.mockito)
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

// dynamic agent loading
tasks.test {
    jvmArgs(
        // Prevents the following warning:
        // WARNING: A Java agent has been loaded dynamically (/Users/brice.dutheil/.gradle/caches/modules-2/files-2.1/net.bytebuddy/byte-buddy-agent/1.14.4/3bf5ac1104554908cc623e40e58a00be37c35f36/byte-buddy-agent-1.14.4.jar)
        // WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
        // WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
        // WARNING: Dynamic loading of agents will be disallowed by default in a future release
        "-XX:+EnableDynamicAgentLoading",
    )
}
