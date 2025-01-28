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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Paths;
import java.util.Arrays;

@SuppressWarnings("preview")
public class TouchId {

  public static void main(String[] args) throws Throwable {
    // System.err.println(Arrays.toString(ProcessHandle.current().info().arguments().get()));
    // System.err.println(ProcessHandle.current().info().command().get());
    // System.err.println(ProcessHandle.current().info().commandLine().get());
    // System.err.println(Paths.get(".").toAbsolutePath());

    // System.load("path/to/libTouchIdDemoLib.dylib");
    System.loadLibrary("TouchIdDemoLib"); // => SymbolLookup.loaderLookup()

    try (var arena = Arena.ofConfined()) {
      // $ nm swift-library/build/lib/main/debug/libTouchIdDemoLib.dylib
      // ...
      // 00000000000037c0 T _authenticate_user
      // ...
      var authenticate_user =
              Linker.nativeLinker()
                    .downcallHandle(
                            SymbolLookup.loaderLookup()
                                        .find("authenticate_user_touchid").get(),
                            FunctionDescriptor.ofVoid()
                    );

      authenticate_user.invokeExact();
    }
  }
}