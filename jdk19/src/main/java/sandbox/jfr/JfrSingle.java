/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package sandbox.jfr;


import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.openjdk.jmc.flightrecorder.writer.api.Recordings;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class JfrSingle {
    public static void main(String[] args) throws IOException, ParseException {
        System.out.println("Hello World!");

      try(var r = Recordings.newRecording(Path.of("recording.jfr"))) {
        r.registerEventType("jdk.ExecutionSample");

        for (int i = 0; i < 1000; i++) {
          System.out.print(".");
        }
      }

      var c = """
              <configuration version="2.0">
                <event name="jdk.VirtualThreadStart">
                  <setting name="enabled">true</setting>
                  <setting name="stackTrace">true</setting>
                </event>
  
                <event name="jdk.VirtualThreadEnd">
                  <setting name="enabled">true</setting>
                </event>
  
                <event name="jdk.VirtualThreadPinned">
                  <setting name="enabled">true</setting>
                  <setting name="stackTrace">true</setting>
                  <setting name="threshold">20 ms</setting>
                </event>
  
                <event name="jdk.VirtualThreadSubmitFailed">
                  <setting name="enabled">true</setting>
                  <setting name="stackTrace">true</setting>
                </event>
              </configuration>            
              """;

      try (var r = new Recording(Configuration.create(new StringReader(c)))) {
        r.start();

        try (var es = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("dot-printer").factory())) {
          IntStream.range(0, 10_000).forEach(i -> es.submit(() -> System.out.print(".")));
        }

        r.dump(Path.of("recording2.jfr"));
      }
    }
}
