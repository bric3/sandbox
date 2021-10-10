/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
  id("me.champeau.jmh") version "0.6.6"
}

// taskset -c 0 java -jar target/benchmarks.jar -wi 5 -i 5 -w 1 -r 1 -f 1 -bm avgt -tu ns Increments
jmh {
  warmupIterations.set(5) // -wi 5
  iterations.set(5) // -i 5
  warmup.set("1") // -w 1
  timeOnIteration.set("1") // -r 1
  fork.set(1) // -f 1
  benchmarkMode.set(listOf("avgt")) // -bm avgt
  timeUnit.set("ns") // -tu ns
}


tasks.create("increments") {
  jmh.includes.add("Increments")
  jmh.resultsFile.set(project.file("${project.buildDir}/reports/jmh/increments-results.txt"))
  finalizedBy("jmh")
}

