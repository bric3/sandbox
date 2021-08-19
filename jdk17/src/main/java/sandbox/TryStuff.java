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

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class TryStuff {
  public static void main(String[] args) throws Throwable {
    var tryStuff = new TryStuff();
    System.out.println("pid: " + tryStuff.c_getpid_smokeTest());
    tryStuff.c_printf_smokeTest("Hello C\n");

    tryStuff.thread_dump();
  }

  public long c_printf_smokeTest(String str) throws Throwable {
    MethodHandle printf = CLinker.getInstance()
                                 .downcallHandle(
                                     CLinker.systemLookup().lookup("printf").get(),
                                     MethodType.methodType(long.class, MemoryAddress.class),
                                     FunctionDescriptor.of(CLinker.C_LONG, CLinker.C_POINTER)
                                 );

    MemorySegment stdoutp = CLinker.systemLookup().lookup("__stdoutp")
                                   .map(s -> s.address().asSegment(CLinker.C_POINTER.byteSize(), ResourceScope.newImplicitScope()))
                                   .orElse(null);

    MemoryAddress stdout = (MemoryAddress)
        MemoryHandles.asAddressVarHandle(CLinker.C_POINTER.varHandle(long.class))
                     .get(
                         stdoutp
                     );

    MethodHandle fflush = CLinker.getInstance()
                                 .downcallHandle(
                                     CLinker.systemLookup().lookup("fflush").get(),
                                     MethodType.methodType(long.class, MemoryAddress.class),
                                     FunctionDescriptor.of(CLinker.C_LONG, CLinker.C_POINTER)
                                 );

    try (var scope = ResourceScope.newConfinedScope()) {
      var cString = CLinker.toCString(str, scope);
      return (long) printf.invokeExact(cString.address());
    } finally {
      long ignored = (long) fflush.invokeExact(stdout.address());
    }
  }

  public long c_getpid_smokeTest() throws Throwable {
    var getpid = CLinker.getInstance()
                        .downcallHandle(
                            CLinker.systemLookup().lookup("getpid").get(),
                            MethodType.methodType(int.class),
                            FunctionDescriptor.of(CLinker.C_INT)
                        );

    return (int) getpid.invokeExact();
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

    MethodHandle raise = CLinker.getInstance()
                                .downcallHandle(
                                    CLinker.systemLookup().lookup("raise").get(),
                                    MethodType.methodType(int.class, int.class),
                                    FunctionDescriptor.of(CLinker.C_INT, CLinker.C_INT)
                                );

    MethodHandle kill = CLinker.getInstance()
                               .downcallHandle(
                                   CLinker.systemLookup().lookup("kill").get(),
                                   MethodType.methodType(int.class, long.class, int.class),
                                   FunctionDescriptor.of(CLinker.C_INT, CLinker.C_LONG, CLinker.C_INT)
                               );

    var resultKill = (int) kill.invokeExact(ProcessHandle.current().pid(), 3);
    var resultRaise = (int) raise.invokeExact(3);
//        var ignored = (long) raise.invokeExact(ProcessHandle.current().pid());
  }

}