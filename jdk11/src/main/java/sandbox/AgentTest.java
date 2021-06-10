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