package sandbox.virtualthreads;

import java.io.PrintStream;

@SuppressWarnings("unused")
public class LoadingIndicator {
  public static final String BASIC = "|/-\\";
  public static final String BRAILLE = "⡿⣟⣯⣷⣾⣽⣻⢿";
  public static final String CLOCK = "◷◶◵◴";
  public static final String UNICODE_HALF_WIDTH = "▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ";
  public static final String UNICODE_FULL_WIDTH = "▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ";

  public static LoadingIndicator.InfiniteLoaderIndicator infinite(PrintStream printStream) {
    return new LoadingIndicator.InfiniteLoaderIndicator(printStream);
  }

  public static class InfiniteLoaderIndicator extends LoadingIndicator {
    private final PrintStream printStream;
    private String loadingChars = BASIC;
    private String prefix = "";
    private String terminateString = "";

    public InfiniteLoaderIndicator(PrintStream printStream) {
      this.printStream = printStream;
    }

    public InfiniteLoaderIndicator loadingChars(String loadingChars) {
      this.loadingChars = loadingChars;
      return this;
    }

    public InfiniteLoaderIndicator withPrefix(String prefix) {
      if (prefix.indexOf('\n') >= 0) {
        throw new IllegalArgumentException("Prefix cannot contain newline");
      }
      this.prefix = prefix;
      return this;
    }

    public InfiniteLoaderIndicator withTerminateString(String terminateString) {
      this.terminateString = terminateString;
      return this;
    }

    public Thread asVirtualThread() {
      return Thread.ofVirtual().unstarted(() -> startLoadingIndicator(printStream, prefix, terminateString, loadingChars));
    }

    @SuppressWarnings("BusyWait") // loom
    private static void startLoadingIndicator(PrintStream printStream, String prefix, String terminatedString, String chars) {
      while (true) {
        for (char c : chars.toCharArray()) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printStream.print("\r" + prefix + "✅");
            // for some reason using print("\n") introduce a flush and outputs one too many
            // 'new line' along the way, using the char overload to output a new line works
            printStream.print('\n');

            if (!terminatedString.isEmpty()) {
              printStream.println(terminatedString);
            }

            return;
          }

          printStream.print(prefix + c + "\r");
        }
      }
    }
  }
}
