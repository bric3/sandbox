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

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.LibraryLookup;
import jdk.incubator.foreign.MemoryAddress;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Try16Stuff {
    public static void main(String[] args) throws Throwable {
        var tryStuff = new Try16Stuff();
        System.out.println("pid: " + tryStuff.c_getpid_smokeTest());
        tryStuff.c_printf_smokeTest("Hello C", StandardCharsets.UTF_8);

        tryStuff.thread_dump();
    }

    public long c_printf_smokeTest(String str, Charset charset) throws Throwable {
        MethodHandle printf = CLinker.getInstance()
                                     .downcallHandle(
                                             LibraryLookup.ofDefault().lookup("printf").get(),
                                             MethodType.methodType(long.class, MemoryAddress.class),
                                             FunctionDescriptor.of(CLinker.C_LONG, CLinker.C_POINTER)
                                     );

        try (var cString = CLinker.toCString(str, charset)) {
            return (long) printf.invokeExact(cString.address());
        }
    }

    public long c_getpid_smokeTest() throws Throwable {
        var getpid = CLinker.getInstance()
                            .downcallHandle(
                                    LibraryLookup.ofDefault().lookup("getpid").get(),
                                    MethodType.methodType(long.class),
                                    FunctionDescriptor.of(CLinker.C_LONG)
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

        MethodHandle raise = CLinker.getInstance()
                                    .downcallHandle(
                                            LibraryLookup.ofDefault().lookup("raise").get(),
                                            MethodType.methodType(int.class, int.class),
                                            FunctionDescriptor.of(CLinker.C_INT, CLinker.C_INT)
                                    );

        MethodHandle kill = CLinker.getInstance()
                                   .downcallHandle(
                                           LibraryLookup.ofDefault().lookup("kill").get(),
                                           MethodType.methodType(int.class, long.class, int.class),
                                           FunctionDescriptor.of(CLinker.C_INT, CLinker.C_LONG, CLinker.C_INT)
                                   );

        var resultKill = (int) kill.invokeExact(ProcessHandle.current().pid(), 3);
        var resultRaise = (int) raise.invokeExact(3);
//        var ignored = (long) raise.invokeExact(ProcessHandle.current().pid());
    }

}