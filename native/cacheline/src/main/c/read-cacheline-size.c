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

int main (int argc, char *argv[])
{
  size_t cacheline_size;

  // We're interested in L1 Data Cache line size (the smallest unit of data
  // trasnfer between cache and main memory).
  // When the CPU reads a single byte from memory, it actually loads an entire
  // cache line (typically 64 bytes on modern systems) because of spatial
  // locality.
  // Note, there's also the L1 Instruction Cache (ICACHE).
  #if defined(__linux__)
    long result = sysconf(_SC_LEVEL1_DCACHE_LINESIZE);
    if (result == -1) {
      perror("sysconf");
      return 1;
    }
    cacheline_size = (size_t)result;
  #elif defined(__APPLE__)
    size_t size = sizeof(cacheline_size);
    if (sysctlbyname("hw.cachelinesize", &cacheline_size, &size, NULL, 0) != 0) {
      perror("sysctlbyname");
      return 1;
    }
  #else
    // TODO
    //  * BSDs : sysctlbyname("machdep.cacheline_size", &cacheline_size, &size, NULL, 0)
    //  * Windows
    //  * ...
    printf("Unsupported platform\n");
    return 1;
  #endif

  printf("Cache line size: %zu bytes\n", cacheline_size);

  // Show CPU model
  #if defined(__linux__)
    FILE *cpuinfo = fopen("/proc/cpuinfo", "r");
    if (cpuinfo) {
      char line[256];
      while (fgets(line, sizeof(line), cpuinfo)) {
        if (strncmp(line, "model name", 10) == 0) {
          char *colon = strchr(line, ':');
          if (colon) {
            printf("CPU model:%s", colon + 1);  // colon+1 includes leading space
          }
          break;
        }
      }
      fclose(cpuinfo);
    }
  #elif defined(__APPLE__)
    char cpu_brand[256];
    size_t cpu_brand_size = sizeof(cpu_brand);
    if (sysctlbyname("machdep.cpu.brand_string", cpu_brand, &cpu_brand_size, NULL, 0) == 0) {
      printf("CPU model: %s\n", cpu_brand);
    }
  #endif

  return 0;
}

