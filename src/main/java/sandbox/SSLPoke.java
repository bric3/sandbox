/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package sandbox;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Establish a SSL connection to a host and port, writes a byte and
 * prints the response. See
 * http://confluence.atlassian.com/display/JIRA/Connecting+to+SSL+services
 * <p>
 * java -Dhttps.proxyHost=169.254.255.254 -Dhttps.proxyPort=8800 sandbox.SSLPoke.java google.com 443
 * java -cp BOOT-INF/lib/conscrypt-openjdk-uber-2.2.1.jar -Dhttps.proxyHost=169.254.255.254 -Dhttps.proxyPort=8800 sandbox.SSLPoke.java google.com 443
 */
public class SSLPoke {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: " + SSLPoke.class.getName() + " <host> <port>");
      System.exit(1);
    }
//        Security.insertProviderAt(Conscrypt.newProvider(), 1);
//        Conscrypt.setDefaultHostnameVerifier(new ConscryptHostnameVerifier() {
//            @Override
//            public boolean verify(String hostname, SSLSession session) {
//                return true;
//            }
//        });

    Pattern.compile(".*\\d{1,5}\\s\\p{Cs}\\n\\{");

    var tunnelHost = System.getProperty("https.proxyHost", null);
    var tunnelPort = Integer.getInteger("https.proxyPort", 0);
    var sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    try (SSLSocket sslSocket = connectTlsSocket(
        sslSocketFactory,
        tunnelHost,
        tunnelPort,
        args[0],
        Integer.parseInt(args[1]))) {
      var sslParams = new SSLParameters();
      sslParams.setEndpointIdentificationAlgorithm("HTTPS");
      sslSocket.setSSLParameters(sslParams);
      var in = sslSocket.getInputStream();
      var out = sslSocket.getOutputStream();
      out.write(1); // Write a test byte to get a reaction :)
      while (in.available() > 0) {
        System.out.print(in.read());
      }
      System.out.println("Successfully connected");
    } catch (Exception exception) {
      exception.printStackTrace();
      System.exit(1);
    }
  }

  private static SSLSocket connectTlsSocket(SSLSocketFactory sslSocketFactory, String tunnelHost, int tunnelPort, String host, int port)
      throws IOException {
    if (tunnelHost != null || tunnelPort != 0) {
      var tunnel = new Socket(tunnelHost, tunnelPort);
      connectTunnel(tunnel, host, port);
      return (SSLSocket) sslSocketFactory.createSocket(tunnel, host, port, true);
    }

    return (SSLSocket) sslSocketFactory.createSocket(host, port);
  }

  private static void connectTunnel(Socket proxySocket, String proxyHost, int proxyPort)
      throws IOException {
    var bw = new BufferedWriter(new OutputStreamWriter(proxySocket.getOutputStream(), "ASCII7"));
    bw.write("CONNECT " + proxyHost + ":" + proxyPort + " HTTP/1.1\n"
        + "Proxy-Connection: Keep-Alive"
        + "User-Agent: " + SSLPoke.class.getName()
        + "\r\n\r\n");
    bw.flush();

    var br = new BufferedReader(new InputStreamReader(proxySocket.getInputStream(), "ASCII7"));
    var statusLine = br.lines()
        .findFirst()
        .orElseThrow(() -> new IOException("Unexpected EOF from proxy"));

    if (!statusLine.startsWith("HTTP/1.1 200")) {
      throw new IOException("Unable to tunnel through "
          + proxySocket.getInetAddress().getHostAddress() + ":" + proxySocket.getPort()
          + ".  Proxy returns \"" + statusLine + "\"");
    }
  }
}