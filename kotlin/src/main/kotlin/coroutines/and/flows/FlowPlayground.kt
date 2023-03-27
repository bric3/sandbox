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
    private val coroutineScope: CoroutineScope,
    private val dispatcherProvider: () -> CoroutineDispatcher
) {
    private val _results = MutableSharedFlow<Int>()
    fun results() = _results.asSharedFlow()
    fun calculate() {
        // 👇 using dispatcher provider avoids hardcoding dispatcher, allowing for us to use a `TestDispatcher` while testing
        coroutineScope.launch(dispatcherProvider.invoke()) {
            val result = longRunningOperation()
            delay(5_000)
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
