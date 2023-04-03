/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package sandbox.panama;

import jdk.incubator.foreign.*;

import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class TryStuff {
  public static void main(String[] args) throws Throwable {
    var tryStuff = new TryStuff();
    System.out.println("pid: " + tryStuff.c_getpid_smokeTest());
    tryStuff.c_printf_smokeTest("Hello C");
    tryStuff.c_strlen("Hello C");

    tryStuff.thread_dump();
  }

  private void c_strlen(String str) throws Throwable {
    var linker = CLinker.systemCLinker();
    MethodHandle strlen = linker.downcallHandle(
            linker.lookup("strlen").get(),
            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    );

    try (var scope = ResourceScope.newConfinedScope()) {
      var cString = MemorySegment.allocateNative(str.length() + 1, scope);
      cString.setUtf8String(0, str);
      long len = (long) strlen.invoke(cString);

      assert len == str.getBytes(StandardCharsets.UTF_8).length : "strlen returned wrong length";
    }
  }

  public long c_printf_smokeTest(String str) throws Throwable {
    MethodHandle printf = CLinker.systemCLinker()
                                 .downcallHandle(
                                         CLinker.systemCLinker().lookup("printf").get(),
                                         FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
                                 );

    try (var scope = ResourceScope.newConfinedScope()) {
      var allocator = SegmentAllocator.nativeAllocator(scope);
      return (long) printf.invoke(allocator.allocateUtf8String(str).address());
    }
  }

  public long c_getpid_smokeTest() throws Throwable {
    var getpid = CLinker.systemCLinker()
                        .downcallHandle(
                                CLinker.systemCLinker().lookup("getpid").get(),
                                FunctionDescriptor.of(ValueLayout.JAVA_LONG)
                        );

    return (long) getpid.invokeExact();
  }

  public void thread_dump() throws Throwable {
    // Idea :
    // #include <signal.h>
    // #include <stdio.h>
    //
    // void signal_catchfunc(int);
    //
    // int main () {
    //    int ret;
    //
    //    ret = signal(SIGINT, signal_catchfunc);
    //
    //    if( ret == SIG_ERR) {
    //       printf("Error: unable to set signal handler.\n");
    //       exit(0);
    //    }
    //    printf("Going to raise a signal\n");
    //    ret = raise(SIGINT);
    //
    //    if( ret !=0 ) {
    //       printf("Error: unable to raise SIGINT signal.\n");
    //       exit(0);
    //    }
    //
    //    printf("Exiting...\n");
    //    return(0);
    // }
    //
    // void signal_catchfunc(int signal) {
    //    printf("!! signal caught !!\n");
    // }

    MethodHandle raise = CLinker.systemCLinker()
                                .downcallHandle(
                                        CLinker.systemCLinker().lookup("raise").get(),
                                        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
                                );

    MethodHandle kill = CLinker.systemCLinker()
                               .downcallHandle(
                                       CLinker.systemCLinker().lookup("kill").get(),
                                       FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
                               );

    var resultKill = (int) kill.invoke(ProcessHandle.current().pid(), 3);
    var resultRaise = (int) raise.invoke(3);
//        var ignored = (long) raise.invokeExact(ProcessHandle.current().pid());
  }

}
