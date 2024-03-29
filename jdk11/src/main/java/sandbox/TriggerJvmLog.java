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

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Objects;

public class TriggerJvmLog {
  public static void main(String[] args) throws Exception {
//        Arrays.stream(describeDiagnosticCommands().getOperations())
//              .map(op -> op.getName() + " -> \n" + op.toString())
//              .forEach(System.out::println);
    vmLogList();

    configureVmLog("logging*=trace", "uptime,tags,level", "stderr");
    configureVmLog("compilation*=debug", "uptime,tags,level", "stdout");
    configureVmLog("gc*=debug", "uptime,tags,level", "stdout");
    // weird VM.log bug where I cannot set class*=debug with another tag*=debug
    configureVmLog("class*=debug", "uptime,tags,level", "stdout");
    vmLogList();

//        disableVmLog("stderr");
//        resetVmLog();

    vmLogList();

  }

  public static void enableLogForBlock(String what, String decorators, String output, Runnable block) {
    configureVmLog(what, decorators, output);
    try {
      block.run();
    } finally {
      resetVmLog();
    }
  }


  private static String configureVmLog(String what, String decorators, String output) {
    Objects.requireNonNull(what);
    Objects.requireNonNull(decorators);
    Objects.requireNonNull(output);

    String cmdOutput = executeDiagnosticCommand(
        "vmLog",
        "what=" + what,
        "decorators=" + decorators,
        "output=" + output
    );
    System.out.printf("jcmd $(pidof java) VM.log what=%s decorators=%s output=%s%n%s%n", what, decorators, output, cmdOutput);
    return cmdOutput;
  }

  private static String resetVmLog() {
    System.out.printf("jcmd $(pidof java) VM.log disable%n");
    return executeDiagnosticCommand(
        "vmLog",
        "disable"
    );
  }

  private static String vmLogList() {
    String cmdOutput = executeDiagnosticCommand(
        "vmLog",
        "list"
    );
    System.out.printf("jcmd $(pidof java) VM.log list%n%s%n", cmdOutput);
    return cmdOutput;
  }


  private static String executeDiagnosticCommand(String operationName, String... params) {
    try {
      var objectName = new ObjectName("com.sun.management:type=DiagnosticCommand");
      var mbeanServer = ManagementFactory.getPlatformMBeanServer();

      String[] signature = new String[]{String[].class.getName()};

      return (String) mbeanServer.invoke(objectName,
          operationName,
          new Object[]{params},
          signature
      );
    } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static MBeanInfo describeDiagnosticCommands() throws IntrospectionException, InstanceNotFoundException, ReflectionException, MalformedObjectNameException {
    var objectName = new ObjectName("com.sun.management:type=DiagnosticCommand");
    var mbeanServer = ManagementFactory.getPlatformMBeanServer();

    return mbeanServer.getMBeanInfo(objectName);
  }
}