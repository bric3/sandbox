/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#if defined(__APPLE__)
  #include <sys/sysctl.h>
#endif

#if defined(_WIN32) || defined(_WIN64)
  #define EXPORT __declspec(dllexport)
#else
  #define EXPORT __attribute__((visibility("default")))
#endif

EXPORT size_t get_cacheline_size(void)
{
  size_t cacheline_size;

  // We're interested in L1 Data Cache line size (the smallest unit of data
  // transfer between cache and main memory).
  // When the CPU reads a single byte from memory, it actually loads an entire
  // cache line (typically 64 bytes on modern systems) because of spatial
  // locality.
  // Note, there's also the L1 Instruction Cache (ICACHE).
  #if defined(__linux__)
    long result = sysconf(_SC_LEVEL1_DCACHE_LINESIZE);
    if (result == -1) {
      return 0;
    }
    cacheline_size = (size_t)result;
  #elif defined(__APPLE__)
    size_t size = sizeof(cacheline_size);
    if (sysctlbyname("hw.cachelinesize", &cacheline_size, &size, NULL, 0) != 0) {
      return 0;
    }
  #else
    // TODO
    //  * BSDs : sysctlbyname("machdep.cacheline_size", &cacheline_size, &size, NULL, 0)
    //  * Windows
    //  * ...
    return 0;
  #endif

  return cacheline_size;
}

EXPORT int get_cpu_model(char *buffer, size_t buffer_size)
{
  if (buffer == NULL || buffer_size == 0) {
    return -1;
  }

  #if defined(__linux__)
    FILE *cpuinfo = fopen("/proc/cpuinfo", "r");
    if (cpuinfo) {
      char line[256];
      while (fgets(line, sizeof(line), cpuinfo)) {
        if (strncmp(line, "model name", 10) == 0) {
          char *colon = strchr(line, ':');
          if (colon) {
            // Skip colon and leading space
            colon += 2;
            // Remove trailing newline
            char *newline = strchr(colon, '\n');
            if (newline) {
              *newline = '\0';
            }
            strncpy(buffer, colon, buffer_size - 1);
            buffer[buffer_size - 1] = '\0';
            fclose(cpuinfo);
            return 0;
          }
          break;
        }
      }
      fclose(cpuinfo);
    }
    return -1;
  #elif defined(__APPLE__)
    char cpu_brand[256];
    size_t cpu_brand_size = sizeof(cpu_brand);
    if (sysctlbyname("machdep.cpu.brand_string", cpu_brand, &cpu_brand_size, NULL, 0) == 0) {
      strncpy(buffer, cpu_brand, buffer_size - 1);
      buffer[buffer_size - 1] = '\0';
      return 0;
    }
    return -1;
  #else
    return -1;
  #endif
}
