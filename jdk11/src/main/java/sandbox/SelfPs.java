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

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SelfPs {
  public static void main(String[] args) throws Exception {
    var h = new ProcessBuilder("ps",
        "--no-header",
        "-orss,vsz",
        Long.toString(ProcessHandle.current().pid()))
        .start();
    try (var br = new BufferedReader(new InputStreamReader(h.getInputStream()))) {
      System.out.println(br.readLine());
    }
  }
}