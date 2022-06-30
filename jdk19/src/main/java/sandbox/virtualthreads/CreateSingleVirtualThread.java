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

public class CreateSingleVirtualThread {
  public static void main(String[] args) throws InterruptedException {
    Runnable task = () -> System.out.println("Hello " + Thread.currentThread());

    var t1 = Thread.startVirtualThread(task);
    var t2 = Thread.ofVirtual()
                          .name("vthread-", 42)
                          .unstarted(task);
    t2.start();

    t1.join(10);
    t2.join(10);
  }
}
