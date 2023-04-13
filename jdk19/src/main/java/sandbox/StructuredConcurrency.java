/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox;

import jdk.incubator.concurrent.StructuredTaskScope;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StructuredConcurrency {
  public static void main(String[] args) {
    try (var executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
      IntStream.range(0, 10).forEach(i -> executorService.submit(() -> {
        System.out.println("Hello " + Thread.currentThread());
      }));
    }

    Instant deadline = Instant.now().plus(10, java.time.temporal.ChronoUnit.SECONDS);
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
      var future1 = scope.fork(() -> query("left"));
      var future2 = scope.fork(() -> query("right"));
      scope.joinUntil(deadline);
      scope.throwIfFailed(e -> new RuntimeException("oups", e));
      // both tasks completed successfully
      String result = Stream.of(future1, future2)
                            .map(Future::resultNow)
                            .map(Object::toString)
                            .collect(Collectors.joining(", ", "{ ", " }"));

    } catch (InterruptedException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private static String query(String queryString) {
    return "";
  }
}
