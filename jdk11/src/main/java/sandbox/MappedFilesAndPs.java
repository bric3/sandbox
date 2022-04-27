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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// docker run --memory-swap=600m --memory=600m --memory-reservation=50m --mount=type=bind,source="$HOME/Downloads",target=/data --mount=type=bind,source=/Users/brice.dutheil/opensource/sandbox/jdk11/src/main/java/sandbox/MappedFilesAndPs.java,target=/MappedFilesAndPs.java --rm -it --privileged azul/zulu-openjdk:11
// apt-get update
// apt-get install -y lsof
public class MappedFilesAndPs {
    public static void main(String[] args) throws Exception {
        Path src = null;
        String mode = null;
        switch (args.length) {
            case 1:
                src = Paths.get(args[0]);
                mode = "full_load";
                if (!Files.exists(src)) {
                    System.exit(1);
                }
                break;
            case 2:
                src = Paths.get(args[1]);
                mode = args[0];
                if (!Files.exists(src)) {
                    System.exit(1);
                }
                break;
            case 0:
            default:
                System.err.println("Usage");
                System.err.printf("   java %s <file>%n", MappedFilesAndPs.class.getName());
                System.err.printf("   java %s <full_load|split_load|buffer> <file>%n", MappedFilesAndPs.class.getName());
                System.exit(1);
                break;
        }
        ps();
        var scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        var scheduledFuture = scheduledExecutorService
                .scheduleAtFixedRate(MappedFilesAndPs::ps, 10, 10, TimeUnit.SECONDS);

        try (var fileChannel = FileChannel.open(src, StandardOpenOption.READ)) {
            switch (mode) {
                case "mapped_full_load": {
                    var mappedByteBuffer = fileChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            0,
                            fileChannel.size());
                    mappedByteBuffer.load();
                    // Address           Kbytes     RSS   Dirty Mode  Mapping
                    // 00007fe51913b000  572180   20280       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz

                    // funny thing without interacting with the mapped buffer, this can run on a cgroup with lower
                    // memory than the size of the file (eg: memory.max = 100 MiB, file size = 500 MiB)
                    break;
                }
                case "mapped_buffer_read": {
                    var mappedByteBuffer = fileChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            0,
                            fileChannel.size());
                    var position = 0;
                    var size = 8192;

                    while (position + size < fileChannel.size()) {
                        // then get bytes from buffer
                        var bytes = new byte[size];
                        mappedByteBuffer.get(bytes);

                        position += size;
                        size = (int) Math.min(size, fileChannel.size() - position);
                    }
                    // Address           Kbytes     RSS   Dirty Mode  Mapping
                    // 00007f3db913b000  572180  459808       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz

                    // Since it is accounted in RSS, this can lead to the process being oom_killed
                    break;
                }
                case "mapped_split_load": {
                    var position = 0;
                    var size = 8192;

                    while (position + size < fileChannel.size()) {
                        var mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                        mappedByteBuffer.load(); // high context switch, high IO

                        position += size;
                        size = (int) Math.min(size, fileChannel.size() - position);
                    }

                    // Address           Kbytes     RSS   Dirty Mode  Mapping
                    // 00007f45fc001000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc003000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc005000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc007000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc009000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc00b000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f45fc00d000       8       0       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz

                    // Notice the RSS of these mappings is 0, because these memory pages where reclaimed.

                    // Since MappedByteBuffer are not freed until GC, this could lead to the process being oom_killed
                    break;
                }
                case "mapped_split_read": {
                    var position = 0;
                    var size = 8192;

                    while (position + size < fileChannel.size()) {
                        var mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
                        // then get bytes from buffer
                        var bytes = new byte[size];
                        mappedByteBuffer.get(bytes);

                        position += size;
                        size = (int) Math.min(size, fileChannel.size() - position);
                    }
                    // Address           Kbytes     RSS   Dirty Mode  Mapping
                    // 00007f7453a70000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a72000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a74000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a76000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a78000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a7a000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a7c000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a7e000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    // 00007f7453a82000       8       8       0 r--s- clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz
                    break;
                }
                case "fc_buffer_read": {
                    var buffer = ByteBuffer.allocateDirect(8192);
                    int read = 0;
                    while ((read = fileChannel.read(buffer)) >= 0) {
                        buffer.flip();
                        // then get bytes from buffer
                        // var bytes = new byte[read];
                        // buffer.get(bytes);

                        buffer.clear();
                    }
                    // not mmaped, reading from the OS page cache
                    break;
                }
                default:
                    System.err.println("Unknown mode: " + mode);
                    System.exit(1);
                    break;
            }
            new ProcessBuilder("pmap", "-x", Long.toString(ProcessHandle.current().pid()))
                    .inheritIO()
                    .start()
                    .waitFor();
        }

        ps();
        scheduledFuture.cancel(false);
        scheduledExecutorService.shutdown();
    }

    private static void ps() {
        try {
            new ProcessBuilder("ps", "--no-header", "-o", "rss,vsz,min_flt,maj_flt",
                               Long.toString(ProcessHandle.current().pid()))
                    .inheritIO()
                    .start()
                    .waitFor();
            new ProcessBuilder("lsof", "-ad", "mem,txt", "/data/clang+llvm-14.0.0-x86_64-apple-darwin.tar.xz")
                    .inheritIO()
                    .start()
                    .waitFor();
            new ProcessBuilder("vmstat", "--wide", "--one-header")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
