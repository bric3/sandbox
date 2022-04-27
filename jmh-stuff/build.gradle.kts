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
  id("me.champeau.jmh") version "0.6.6"
  id("java")
}

repositories {
  mavenCentral()
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

