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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.management.ManagementFactory;

public class DebugTest {

  public static void main(String[] args) {
    var foo = new Foo();
    System.out.printf("[%.3fs][stdout] args %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, ProcessHandle.current().info().commandLine());
    System.out.printf("[%.3fs][stdout] m(): %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, foo.m());
  }

  private static class Foo {
    @Alpha(v = "bim")
    String m() {
      return new StringBuilder("fable").toString();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Alpha {
    String v();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Beta {
    String val();
  }
}