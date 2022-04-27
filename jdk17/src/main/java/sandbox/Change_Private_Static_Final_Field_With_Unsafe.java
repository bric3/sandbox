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

import sun.misc.Unsafe;

import java.lang.reflect.Field;

// run with -ea
public class Change_Private_Static_Final_Field_With_Unsafe {
  // javac will inline static final Strings, so let's say it's Object
  private static final Object theField = "origin";

  public static void main(String... args) throws Exception {
    System.out.println("Java version: " + System.getProperty("java.version"));

    final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
    unsafeField.setAccessible(true);
    final Unsafe unsafe = (Unsafe) unsafeField.get(null);

    assert "origin".equals(theField) : "theField should have the value: 'origin'";
    System.out.println("before = " + theField);

    final Field ourField = Change_Private_Static_Final_Field_With_Unsafe.class.getDeclaredField("theField");
    final Object staticFieldBase = unsafe.staticFieldBase(ourField);
    final long staticFieldOffset = unsafe.staticFieldOffset(ourField);
    unsafe.putObject(staticFieldBase, staticFieldOffset, "changed");

    //noinspection ConstantConditions
    assert "changed".equals(theField) : "theField should have the value: 'changed'";
    System.out.println("after = " + theField);
  }
}

