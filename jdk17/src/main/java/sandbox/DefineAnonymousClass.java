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
