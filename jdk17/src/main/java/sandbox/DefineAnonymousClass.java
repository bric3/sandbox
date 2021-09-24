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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Replacement for Unsafe::defineAnonymousClass
 *
 * See
 * - https://twitter.com/sundararajan_a/status/1441283846738042884?s=21
 * - https://bugs.openjdk.java.net/browse/JDK-8243287
 */
public class DefineAnonymousClass {
  // This class is never used directly.
  // The .class bytes from the compilation are loaded as a hidden class
  private static class MyClass {
    private static void doSomething() {
      try {
        // access data passed when hidden class was created
        var str = MethodHandles.classData(
            MethodHandles.lookup(),
            "_",
            String.class);
        System.out.printf("data: %s%n", str);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }


  public static void main(String[] args) throws Throwable {
    var data = args.length > 0 ? args[0] : "Hello World";
    MethodHandles.Lookup lookup;
    try (var in = DefineAnonymousClass.class.getResourceAsStream("DefineAnonymousClass$MyClass.class")) {
      var buf = in.readAllBytes();
      // define a new hidden class using the loaded .class bytes
      // and pass data as class data
      lookup = MethodHandles.lookup().defineHiddenClassWithClassData(
          buf,
          data,
          false
      );
    }

    var hiddenClass = lookup.lookupClass();
    System.out.printf("hidden class name: %s%n", hiddenClass.getName());

    // find "doSomething" and invoke it.
    var mh = lookup.findStatic(hiddenClass, "doSomething", MethodType.methodType(void.class));
    mh.invokeExact();
  }
}
