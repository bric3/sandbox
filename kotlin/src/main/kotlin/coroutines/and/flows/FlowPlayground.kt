/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package coroutines.and.flows

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class NumberCruncher(
    private val calculationTime: Long,
    private val coroutineScope: CoroutineScope,
    private val dispatcherProvider: () -> CoroutineDispatcher
) {
    private val _results = MutableSharedFlow<Int>()
    fun results() = _results.asSharedFlow()
    fun calculate() {
        // 👇 using dispatcher provider avoids hardcoding dispatcher, allowing for us to use a `TestDispatcher` while testing
        coroutineScope.launch(dispatcherProvider.invoke()) {
            val result = longRunningOperation()
            delay(calculationTime)
            _results.emit(result)
        }
    }

    private fun longRunningOperation(): Int = Random.nextInt()
}

class MyEmitter {
    private val _results = MutableSharedFlow<Int>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    fun results(): SharedFlow<Int> = _results

    fun somethingHappened() {
        val tryEmit = _results.tryEmit(longRunningOperation())
        println(tryEmit)
    }

    private fun longRunningOperation(): Int = Random.nextInt()
}
