/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
#ifndef CACHELINE_H
#define CACHELINE_H

#include <stddef.h>

#if defined(_WIN32) || defined(_WIN64)
  #define EXPORT __declspec(dllexport)
#else
  #define EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Get the CPU cache line size in bytes.
 * Returns 0 if the cache line size cannot be determined.
 */
EXPORT size_t get_cacheline_size(void);

/**
 * Get the CPU model name.
 * Returns 0 on success, -1 on error.
 */
EXPORT int get_cpu_model(char *buffer, size_t buffer_size);

#ifdef __cplusplus
}
#endif

#endif // CACHELINE_H
