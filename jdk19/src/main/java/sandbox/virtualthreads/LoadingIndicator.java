package sandbox.virtualthreads;

@SuppressWarnings("unused")
public class LoadingIndicator {

  public static final String BASIC = "|/-\\";
  public static final String BRAILLE = "⡿⣟⣯⣷⣾⣽⣻⢿";
  public static final String CLOCK = "◷◶◵◴";
  public static final String UNICODE_HALF_WIDTH = "▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ";

  public static void main(String[] args) {
    var t1 = asVirtualThread(BRAILLE);

    t1.start();
    try {
      Thread.sleep(5000);
      t1.interrupt();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    var t2 = asVirtualThread(BRAILLE);

    t2.start();
    try {
      Thread.sleep(5000);
      t2.interrupt();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static Thread asVirtualThread(String loadingChars) {
    return Thread.ofVirtual().unstarted(() -> startLoadingIndicator(loadingChars));
  }

  @SuppressWarnings("BusyWait") // loom
  private static void startLoadingIndicator(String chars) {
    while (true) {
      for (char c : chars.toCharArray()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.out.print("\r✅");
          // for some reason using print("\n") introduce a new line along the way
          // using the char overload to output a new line works
          System.out.print('\n');

          return;
        }

        System.out.print(c + "\r");
      }
    }
  }
}
