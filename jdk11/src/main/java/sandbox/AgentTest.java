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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;

import java.lang.management.ManagementFactory;

public class AgentTest {

  public static void main(String[] args) {
    Foo foo = new Foo();
    System.out.printf("[%.3fs][stdout] args %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, ProcessHandle.current().info().commandLine());
    System.out.printf("[%.3fs][stdout] m(): %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, foo.m());
    ByteBuddyAgent.install();

    new ByteBuddy()
        .redefine(Bar.class)
        .name(Foo.class.getName())
        .make()
        .load(Foo.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    System.out.printf("[%.3fs][stdout] m(): %s%n", ManagementFactory.getRuntimeMXBean().getUptime() / 1000d, foo.m());
  }

  private static class Foo {
    String m() {
      return "full";
    }
  }

  private static class Bar {
    String m() {
      return "bear";
    }
  }
}