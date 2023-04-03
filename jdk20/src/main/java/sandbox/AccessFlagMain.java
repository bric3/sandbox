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

import java.lang.reflect.AccessFlag;

public class AccessFlagMain {
    public static void main(String[] args) {
        // lookout for the new ClassFile API (JDK21 ?)
        AccessFlag.maskToAccessFlags(AccessFlagMain.class.getModifiers(), AccessFlag.Location.CLASS)
                  .forEach(System.out::println);
    }
}
