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

static int load_libjvm_handle(void) {
  // RTLD_LAZY -> resolves libraries symbols lazily
  // RTLD_NOW -> resolves library symbols eagerly

  void* libjvmso_handle = dlopen("libjvm.so", RTLD_LAZY);
  if (libjvmso_handle != NULL) {
    fputs(dlerror(), stderr);
    fputs("\n", stderr);
  }

  void* libjvmdylib_handle = dlopen("libjvm.dylib", RTLD_LAZY);
  if (!libjvmdylib_handle) {
    fputs(dlerror(), stderr);
    fputs("\n", stderr);
    exit(1);
  }
  return 0;
}

static char* exec(const char* command) {
  FILE* fp;
  char* result = NULL;
  size_t len = 0;

  fflush(NULL);
  fp = popen(command, "r");
  if (fp == NULL) {
    printf("failed to execute command: %s\n", command);
    return fp;
  }

  while(getline(&result, &len, fp) != -1) {
    fputs(result, stdout);
  }

  free(result);
  fflush(fp);

  int status = pclose(fp);
  if (status == -1) {
    perror("pclose");
  } else if (WIFSIGNALED(status)) {
    printf("terminating signal: %d", WTERMSIG(status));
  } else if (WIFEXITED(status)) {
    printf("exit with status: %d", WEXITSTATUS(status));
  } else {
    printf("unexpected: %d", status);
  }
  return result;
}

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

  // Another note on the differences between .dylib and .so
  // https://stackoverflow.com/a/29226313/48136
  // The difference between .dylib and .so on mac os x is how they are compiled.
  // For .so files you use -shared and for .dylib you use -dynamiclib. Both .so and .dylib are
  // interchangeable as dynamic library files and either have a type as DYLIB or BUNDLE.

  // run as 
  //   env LD_LIBRARY_PATH=$JAVA_HOME/lib/server build/exes/main/test-dlopen
  load_libjvm_handle();

  char buffer[50];
  sprintf(buffer, "pmap -X -p %d", pid); // no pmap replacement on macos
  printf("Executing: '%s'\n", buffer);
  exec(buffer);

  sprintf(buffer, "ps -p %d -o rss,vsz,command", pid);
  printf("Executing: '%s'\n", buffer);
  exec(buffer);

  return 0;
}
