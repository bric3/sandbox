/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public class CheckEnableJVMCI {
  public static void main(String[] args) {
    System.out.format(
            """
            os               : %s %s
            jvm              : %s %s
            """,
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version")
    );

    var commandArgs = ProcessHandle.current().info().arguments().map(Set::of).orElseThrow();

    assert commandArgs.contains("-XX:+UnlockExperimentalVMOptions") : "Missing -XX:+UnlockExperimentalVMOptions";
    assert commandArgs.contains("-XX:+EnableJVMCI") : "Missing -XX:+EnableJVMCI";
    assert commandArgs.stream().anyMatch(arg -> arg.startsWith("--module-path")) : "Missing graal --module-path";
    assert commandArgs.stream().anyMatch(arg -> arg.startsWith("--upgrade-module-path")) : "Missing graal --upgrade-module-path";

    commandArgs.stream()
               .filter(arg -> arg.startsWith("--module-path"))
               .map(arg -> arg.substring(arg.indexOf("=") + 1))
               .flatMap(path -> {
                 try {
                   return Stream.concat(Stream.of("Graal modules in : " + path), Files.list(Path.of(path)));
                 } catch (IOException e) {
                   throw new UncheckedIOException(e);
                 }
               })
               .forEach(System.out::println);
  }
}
