/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.structured_concurrency;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings("preview")
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
      var result = Stream.of(future1, future2)
                         .map(Subtask::get)
                         .map(Object::toString)
                         .collect(Collectors.joining(", ", "{ ", " }"));

      System.out.println(result);
    } catch (InterruptedException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  private static String query(String queryString) {
    return queryString + ":" + Instant.now().toString();
  }
}
