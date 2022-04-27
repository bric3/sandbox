/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package sandbox.jna;

public class JnaIsATTY {
  public static void main(String args[]) {
    IsATTY_JNA jnaLib = IsATTY_JNA.INSTANCE;

    System.out.printf("stdin : %s%n", jnaLib.isatty(0));
    System.out.printf("stdout: %s%n", jnaLib.isatty(1));
    System.out.printf("stderr: %s%n", jnaLib.isatty(3));
  }
}
