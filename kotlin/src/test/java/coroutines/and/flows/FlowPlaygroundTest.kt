package coroutines.and.flows

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class NameCruncherTest {
    @Test
    fun name() = runTest {
        // UnconfinedTestDispatcher allow to skip the delay operator
        val numberCruncher = NumberCruncher(this, { UnconfinedTestDispatcher(testScheduler) })
        numberCruncher.calculate()
        assertEquals(0, numberCruncher.results().first())
    }
}

class MyEmitterTest {
    @Test
    fun usingDispatcher() = runTest(dispatchTimeoutMs = 500) {
        val numberCruncher = MyEmitter()

        var c = 0
        val job = numberCruncher.results().onEach {
            c++
        }.launchIn(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        for (i in 1..4) {
            numberCruncher.somethingHappened()
        }

        assertEquals(4, c)
        job.cancel()
    }

    @Test
    fun usingTurbine() = runTest {
        val numberCruncher = MyEmitter()
        numberCruncher.results().test(timeout = 500.milliseconds) {
            repeat(4) {
                numberCruncher.somethingHappened()
                awaitItem()
            }

            expectNoEvents()
        }
    }
}
