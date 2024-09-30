package coroutines.and.flows

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

object DumbCoroutine {
  @JvmStatic
  fun main(vararg args: String) {
    runBlocking {
      launch {
        println("runBlocking: Current thread: ${Thread.currentThread().name}")

        println("runBlocking: $coroutineContext")
      }
    }

    CoroutineScope(CoroutineName("a")).launch {
      println("CoroutineScope(CoroutineName(\"a\")).launch: Current thread: ${Thread.currentThread().name}")
      println("CoroutineScope(CoroutineName(\"a\")).launch: $coroutineContext")
    }

    CoroutineScope(Dispatchers.Unconfined).launch {
      println("CoroutineScope(Dispatchers.Unconfined).launch: Current thread: ${Thread.currentThread().name}")
      println("CoroutineScope(Dispatchers.Unconfined).launch: $coroutineContext")
      withContext(Dispatchers.Default) {
        println("CoroutineScope(Dispatchers.Unconfined).launch Default: Current thread: ${Thread.currentThread().name}")
        println("CoroutineScope(Dispatchers.Unconfined).launch Default: $coroutineContext")
      }
      println("CoroutineScope(Dispatchers.Unconfined).launch after: Current thread: ${Thread.currentThread().name}")
      println("CoroutineScope(Dispatchers.Unconfined).launch after: : $coroutineContext")
    }

    CoroutineScope(Dispatchers.IO).launch {
      println("CoroutineScope(Dispatchers.IO).launch: Current thread: ${Thread.currentThread().name}")
      println("CoroutineScope(Dispatchers.IO).launch: $coroutineContext")
      withContext(Dispatchers.Default) {
        println("CoroutineScope(Dispatchers.IO).launch Default: Current thread: ${Thread.currentThread().name}")
        println("CoroutineScope(Dispatchers.IO).launch Default: $coroutineContext")
      }
      println("CoroutineScope(Dispatchers.IO).launch after: Current thread: ${Thread.currentThread().name}")
      println("CoroutineScope(Dispatchers.IO).launch after: : $coroutineContext")
    }

    runBlocking {
      delay(2.seconds)
    }
  }

  private val backgroundDispatcher = newFixedThreadPoolContext(4, "App Background")
  // At most 2 threads will be processing images as it is really slow and CPU-intensive
  private val imageProcessingDispatcher = backgroundDispatcher. limitedParallelism(2)
  // At most 3 threads will be processing JSON to avoid image processing starvation
  private val jsonProcessingDispatcher = backgroundDispatcher. limitedParallelism(3)
  // At most 1 thread will be doing IO
  private val fileWriterDispatcher = backgroundDispatcher. limitedParallelism(1)

}
