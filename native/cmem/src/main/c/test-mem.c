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
#include <unistd.h>
#include <sys/mman.h>

#define HEAP_SIZE (16 * 1024 * 1024 * sizeof(char))

int main (int argc, char *argv[])
{
//  char *heap1 = malloc(HEAP_SIZE);
//  char *heap2 = mmap(0, HEAP_SIZE, PROT_NONE|PROT_READ|PROT_WRITE, MAP_PRIVATE | MAP_NORESERVE | MAP_ANONYMOUS, -1, 0);

  pid_t pid = getpid();
  printf("pid: %d\n", pid);

  char buffer[50];

  sprintf(buffer, "pmap -X %d", pid);
  printf("Executing: '%s'\n", buffer);
  system(buffer);

  sprintf(buffer, "ps -p %d -o rss,vsz,command", pid);
  printf("Executing: '%s'\n", buffer);
  system(buffer);

  long pagesize = sysconf(_SC_PAGE_SIZE);
  printf("Page size: %ld\n", pagesize);
  printf("Writing to some pages, but not all\n");

//  for (char* i = heap1; i < (heap1 + HEAP_SIZE / 16); i += pagesize) {
//    *i = 0x01;
//  }
//  for (char* i = heap2; i < (heap2 + HEAP_SIZE / 8); i += pagesize) {
//    *i = 0x01;
//  }

  sprintf(buffer, "ps -p %d -o rss,vsz,command", pid);
  printf("Executing: '%s'\n", buffer);
  system(buffer);



//  free(heap1);
//  munmap(heap2, HEAP_SIZE);

  return 0;
}
