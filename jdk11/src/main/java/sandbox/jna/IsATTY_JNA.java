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

import com.sun.jna.Library;
import com.sun.jna.Native;


public interface IsATTY_JNA extends Library {
  IsATTY_JNA INSTANCE = (IsATTY_JNA) Native.load("c", IsATTY_JNA.class); // (1)
  boolean isatty(int fileDescriptor); // (2)
}