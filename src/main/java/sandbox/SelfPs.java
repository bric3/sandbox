package sandbox;

import java.io.*;

public class SelfPs {
  public static void main(String[] args) throws Exception {
    var h = new ProcessBuilder("ps",
        "--no-header",
        "-orss,vsz",
        Long.toString(ProcessHandle.current().pid()))
        .start();
    try (var br = new BufferedReader(new InputStreamReader(h.getInputStream()))) {
      System.out.println(br.readLine());
    }
  }
}
