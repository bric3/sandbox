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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.Date;

/**
 * java -cp ./lib/conscrypt-openjdk-uber-2.2.1.jar -javaagent:newrelic-agent.jar -Djavax.net.debug=all sandbox.PokeHttpServer.java 4000 2>&1
 */
public class PokeHttpServer {
  static int port = 8080;

  public static void main(String[] args) {
    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    }
//    Security.insertProviderAt(Conscrypt.newProvider(), 1);
    try {
      var serverSocket = new ServerSocket(port);
      System.out.printf("Starts listening on %d%n", port);

      while (true) {
        try (var connection = serverSocket.accept();
             var in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             var pout = new PrintStream(new BufferedOutputStream(connection.getOutputStream()));) {

          var requestLine = in.readLine();
          if (requestLine == null) continue;

          while (true) {
            var skipped = in.readLine();
            if (skipped == null || skipped.length() == 0) break;
          }

          if (!requestLine.startsWith("GET ") ||
              !(requestLine.endsWith(" HTTP/1.0") || requestLine.endsWith(" HTTP/1.1"))) {
            pout.print("HTTP/1.0 400 Bad Request\r\n\r\n");
          } else {
            var responsePayload = "Hello, World!";

            pout.print("HTTP/1.0 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Date: " + new Date() + "\r\n"
                + "Content-length: " + responsePayload.length() + "\r\n\r\n"
                + responsePayload);
          }
        } catch (Throwable throwable) {
          System.err.println("Error handling request: " + throwable);
        }
      }
    } catch (Throwable throwable) {
      System.err.println("Could not start server: " + throwable);
      System.exit(1);
    }
  }
}