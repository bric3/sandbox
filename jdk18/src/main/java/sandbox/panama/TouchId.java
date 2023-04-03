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

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;

public class TouchId {

  public static void main(String[] args) throws Throwable {
//    System.err.println(Arrays.toString(ProcessHandle.current().info().arguments().get()));
//    System.err.println(ProcessHandle.current().info().command().get());
//    System.err.println(ProcessHandle.current().info().commandLine().get());
//    System.err.println(Paths.get(".").toAbsolutePath());
//
//    System.load("path/to/libTouchIdDemoLib.dylib");
    System.loadLibrary("TouchIdDemoLib");

    // $ nm swift-library/build/lib/main/debug/libTouchIdDemoLib.dylib
    // ...
    // 00000000000037c0 T _authenticate_user
    // ...
    var authenticate_user = CLinker.systemCLinker()
                                   .downcallHandle(
                                           SymbolLookup.loaderLookup().lookup("authenticate_user_touchid").get(),
                                           FunctionDescriptor.ofVoid()
                                   );


    try (var scope = ResourceScope.newConfinedScope()) {
      authenticate_user.invokeExact();
    }
  }
}
