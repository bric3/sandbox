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

import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.api.tasks.testing.TestResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Only logs test output when a test fails.
 *
 * Example configuration
 *
 * ```kotlin
 * tasks.test {
 *     useJUnitPlatform()
 *     testLogging {
 *         showStackTraces = true
 *         exceptionFormat = TestExceptionFormat.FULL
 *         events = setOf(
 *             TestLogEvent.FAILED,
 *             TestLogEvent.SKIPPED,
 *         )
 *     }
 *     logStdOutOnFailure()
 * }
 * ```
 */
class FailedTestLogger : TestListener, TestOutputListener {
    private val testOutputs: MutableMap<TestDescriptor, MutableList<TestOutputEvent>> = ConcurrentHashMap()
    override fun beforeSuite(test: TestDescriptor) {
    }

    override fun afterSuite(testDescriptor: TestDescriptor, testResult: TestResult) {
        testOutputs.clear()
    }

    override fun beforeTest(testDescriptor: TestDescriptor) {}

    override fun afterTest(test: TestDescriptor, result: TestResult) {
        if (result.resultType === TestResult.ResultType.FAILURE) {
            LOGGER.error(buildString {
                append("## FAILURE: ")
                test.className?.let {
                    append(it)
                    append(".")
                }
                append(test.displayName)
            })
            for (output in testOutputs.getOrDefault(test, listOf())) {
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA") // never null
                when (output.destination) {
                    TestOutputEvent.Destination.StdOut -> print(output.message)
                    TestOutputEvent.Destination.StdErr -> System.err.print(output.message)
                }

            }
            for (exception in result.exceptions) {
                exception.printStackTrace()
            }
        }
        testOutputs.remove(test)
    }

    override fun onOutput(test: TestDescriptor, outputEvent: TestOutputEvent) {
        testOutputs.compute(test) { _, value ->
            (value ?: mutableListOf()).also {
                it.add(outputEvent)
            }
        }
    }

    companion object {
        private val LOGGER = Logging.getLogger(FailedTestLogger::class.java)

        /**
         * Configure the test task to only logs test output when a test fails.
         *
         * Should be used at the end.
         */
        fun AbstractTestTask.logStdOutOnFailure() {
            if (!project.providers.systemProperty("idea.active").map { it.toBoolean() }.getOrElse(false)) {
                testLogging {
                    showStandardStreams = false
                }

                FailedTestLogger().also {
                    addTestListener(it)
                    addTestOutputListener(it)
                }
            }
        }
    }
}