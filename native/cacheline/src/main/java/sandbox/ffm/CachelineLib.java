/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package sandbox.ffm;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Java wrapper for the cacheline native library using FFM API (Java 25).
 */
public class CachelineLib {
    private static final SymbolLookup SYMBOL_LOOKUP;
    private static final MethodHandle GET_CACHELINE_SIZE;
    private static final MethodHandle GET_CPU_MODEL;

    static {
        try {
            // Load the native library
            System.loadLibrary("cacheline");
            SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

            // Define function descriptors
            FunctionDescriptor getCachelineSizeDescriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG  // returns size_t
            );

            FunctionDescriptor getCpuModelDescriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,     // returns int
                ValueLayout.ADDRESS,      // char* buffer
                ValueLayout.JAVA_LONG     // size_t buffer_size
            );

            // Get method handles
            Linker linker = Linker.nativeLinker();

            MemorySegment getCachelineSizeAddr = SYMBOL_LOOKUP.find("get_cacheline_size")
                .orElseThrow(() -> new LinkageError("Could not find get_cacheline_size"));
            GET_CACHELINE_SIZE = linker.downcallHandle(getCachelineSizeAddr, getCachelineSizeDescriptor);

            MemorySegment getCpuModelAddr = SYMBOL_LOOKUP.find("get_cpu_model")
                .orElseThrow(() -> new LinkageError("Could not find get_cpu_model"));
            GET_CPU_MODEL = linker.downcallHandle(getCpuModelAddr, getCpuModelDescriptor);

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Get the CPU cache line size in bytes.
     * @return the cache line size, or 0 if it cannot be determined
     */
    public static long getCachelineSize() {
        try {
            return (long) GET_CACHELINE_SIZE.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call get_cacheline_size", e);
        }
    }

    /**
     * Get the CPU model name.
     * @return the CPU model name, or null if it cannot be determined
     */
    public static String getCpuModel() {
        try (Arena arena = Arena.ofConfined()) {
            // Allocate a buffer for the CPU model string
            MemorySegment buffer = arena.allocate(256);

            int result = (int) GET_CPU_MODEL.invokeExact(buffer, 256L);

            if (result != 0) {
                return null;
            }

            // Convert the C string to Java String
            return buffer.getString(0);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to call get_cpu_model", e);
        }
    }

    /**
     * Main method for testing.
     */
    public static void main(String[] args) {
        System.out.println("Cache line size: " + getCachelineSize() + " bytes");
        System.out.println("CPU model: " + getCpuModel());
    }
}
