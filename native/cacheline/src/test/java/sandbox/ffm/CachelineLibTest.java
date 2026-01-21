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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CachelineLibTest {

    @Test
    void testGetCachelineSize() {
        long cachelineSize = CachelineLib.getCachelineSize();
        assertTrue(cachelineSize > 0, "Cache line size should be greater than 0");
        // Most modern CPUs have 64 or 128 byte cache lines
        assertTrue(cachelineSize >= 32 && cachelineSize <= 256,
            "Cache line size should be between 32 and 256 bytes, got: " + cachelineSize);
    }

    @Test
    void testGetCpuModel() {
        String cpuModel = CachelineLib.getCpuModel();
        assertNotNull(cpuModel, "CPU model should not be null");
        assertFalse(cpuModel.isEmpty(), "CPU model should not be empty");
        System.out.println("CPU Model: " + cpuModel);
    }
}
