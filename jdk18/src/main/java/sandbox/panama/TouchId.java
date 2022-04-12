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
