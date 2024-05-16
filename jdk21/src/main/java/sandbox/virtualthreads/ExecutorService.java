/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.virtualthreads;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ExecutorService {
  public static void main(String[] args) {
    // Simple executor implementation with Executors.newVirtualThreadPerTaskExecutor()
    var vFactory = Thread.ofVirtual().name("V").factory();
    try (var es = Executors.newThreadPerTaskExecutor(vFactory)) {
      IntStream.range(0, 10).forEach(i -> es.submit(() -> {
        try {
          Thread.sleep(ThreadLocalRandom.current().nextInt(10_000));
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }

        System.out.println("Hello " + Thread.currentThread());
      }));
    }
  }
}
