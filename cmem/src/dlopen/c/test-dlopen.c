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

#include <dlfcn.h>

int main (int argc, char *argv[])
{
  pid_t pid = getpid();
  printf("pid: %d\n", pid);

  // If the library you want to dlopen is not in the standard search path you have a number of options:
  // . Specify the full path to the file in dlopen
  //     dlopen("/full/path/to/libfile.so");
  // . Add the path to the library via `LD_LIBRARY_PATH`
  //     LD_LIBRARY_PATH=/path/to/library/ ./executable
  // . use the ld `-rpath` option to add a library path to the application.
  //     g++ -link stuff- -Wl,-rpath=/path/to/library/
  //
  // Note that options 1 & 3 hardcode the library path into your application. `-rpath` does have 
  // an option to specify a relative path, i.e.
  //     -Wl,-rpath=$ORIGIN/../lib/
  //
  // Will embed a relative path into the application.
  //
  // 
  // Debug dynamic libaries loading (http://tldp.org/HOWTO/Program-Library-HOWTO/shared-libraries.html)
  //     LD_DEBUG=libs ./the_binary

  // run as 
  //   env LD_LIBRARY_PATH=$JAVA_HOME/lib/server build/exe/dlopen/test-dlopen
  void* libjava_handle=dlopen("libjvm.so", RTLD_LAZY);
  if (!libjava_handle) {
    fputs (dlerror(), stderr);
    exit(1);
  }

  char buffer[50];
  sprintf(buffer, "pmap -X -p %d", pid);
  printf("Executing: '%s'\n", buffer);
  system(buffer);

  return 0;
}
