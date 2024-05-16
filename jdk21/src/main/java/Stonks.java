///usr/bin/env jbang "$0" ; exit $?
//JAVA 21
//JAVAC_OPTIONS --enable-preview --source 21
//JAVA_OPTIONS --enable-preview

/* Usage, with a [free finnhub token](https://finnhub.io/register).
 *
 * With [jbang](https://www.jbang.dev/)
 * env FINNHUB_TOKEN=<token> jbang https://gist.github.com/bric3/2338aad81b5bba614020a17a1fd0884f
 *
 * Or after downloading
 * env FINNHUB_TOKEN=<token> jbang jdk21/src/main/java/Stonks.java DDOG
 *
 * Or even with Java 21 directly
 * env FINNHUB_TOKEN=<token> java --source 21 path/to/Stonks.java NET
 */

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 Run with

 environment variable: FINNHUB_TOKEN={the token}
 JFR: -XX:StartFlightRecording:filename=stonks.jfr,+jdk.VirtualThreadStart#enabled=true,+jdk.VirtualThreadEnd#enabled=true,+jdk.VirtualThreadPinned=true
*/
@SuppressWarnings({"SpellCheckingInspection", "UastIncorrectHttpHeaderInspection"})
public class Stonks {
  public static final List<String> DEFAULT_TICKERS = List.of(
          "DDOG",
          "NFLX",
          "AAPL",
          "TSLA",
          "GOOG",
          "AMZN",
          "MSFT",
          "NET",
          "TWTR",
          "BABA",
          "NDAQ",
          "IBM",
          "UBER",
          "ROKU",
          "NVDA",
          "AMD",
          "INTC",
          "AKAM",
          "XIACF",
          "PHG",
          "ADBE",
          "ORCL",
          "TTE",
          "SNY",
          "PFE",
          "ORAN",
          "CRTO",

          "GC=F", // no company profile for gold
          "SI=F", // no company profile for silver
          "CL=F", // no company profile for crude oil
          "EURUSD=X", // no company profile for euro/usd
          "BTC-USD",
          "BTC-EUR",
          "ETH-USD",
          "ETH-EUR",

          // "A2R82Y.DU", // no company profile for Dow Futures
          // "^IXIC", // no company profile for nasdaq

          // not available in the free tier
          // "NASDAQ",
          // "^HSI",
          // "^FCHI",
          // "DOW J",
          // "^N225",
          // "^GDAXI",
          // "^FTSE",
          ""
  );
  private static final String default_fg = "\u001B[39m";
  private static final String red = "\u001B[31m";
  private static final String green = "\u001B[32m";
  private static final String blue = "\u001B[34m";
  private static final String gray = "\u001B[37m";
  private static final String bold = "\u001B[01m";
  private static final String italic = "\u001B[03m";
  private static final String underline = "\u001B[04m";
  private static final String reset = "\u001B[00m";

  private volatile boolean rateLimitAnnounceInProgress = false;
  private volatile long rateLimitResetSeconds = 0;
  private final Pattern fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(null|\"?(?:-?\\d+(?:.\\d+)?|[^\"]+)\"?)");
  private final HttpClient httpClient;

  public static void main(String[] args) throws Exception {
    // https://finnhub.io/pricing
    // 60 API calls/minute
    var finnhubToken = System.getenv("FINNHUB_TOKEN");
    if (finnhubToken == null || finnhubToken.isBlank() || finnhubToken.equals("null")) {
      System.err.println("Needs non empty token set in the environment variable FINNHUB_TOKEN");
      System.exit(1);
    }

    new Stonks().run(finnhubToken, args);
  }

  public Stonks() {
    var httpClientCarrierThreadsEnv = System.getenv("HTTP_CLIENT_CARRIER_THREADS");
    int httpClientCarrierThreads = switch (httpClientCarrierThreadsEnv) {
      case "null" -> 1;
      case null -> 1;
      default -> Integer.parseInt(httpClientCarrierThreadsEnv);
    };

    httpClient = HttpClient.newBuilder()
                           .executor(Executors.newFixedThreadPool(httpClientCarrierThreads, Thread.ofVirtual().name("HttpClient-virtual").factory()))
                           .connectTimeout(Duration.ofSeconds(10))
                           .build();
  }

  private void run(String finnhubToken, String... tickersArgs) throws InterruptedException, ExecutionException, TimeoutException {
    var tickers = tickersArgs.length > 0 ? List.of(tickersArgs) : DEFAULT_TICKERS;

    try (var es = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("finnhub-fetcher").factory())) {
      var tickerTasks = tickers.stream().filter(Predicate.not(String::isBlank)).map(ticker -> CompletableFuture.runAsync(() -> {
        try {
          var quoteResponse = CompletableFuture.supplyAsync(() -> sendWithRetry(
                  HttpRequest.newBuilder(URI.create("https://finnhub.io/api/v1/quote?symbol=" + URLEncoder.encode(ticker, StandardCharsets.UTF_8)))
                             .GET()
                             .header("X-Finnhub-Token", finnhubToken)
                             .build(),
                  BodyHandlers.ofString()
          ), es);

          var profile2Response = CompletableFuture.supplyAsync(() -> sendWithRetry(
                  HttpRequest.newBuilder(URI.create("https://finnhub.io/api/v1/stock/profile2?symbol=" + URLEncoder.encode(ticker, StandardCharsets.UTF_8)))
                             .GET()
                             .header("X-Finnhub-Token", finnhubToken)
                             .build(),
                  BodyHandlers.ofString()
          ), es);

          displayQuote(
                  ticker,
                  quoteResponse.get(),
                  profile2Response.get()
          );
        } catch (Exception e) {
          System.err.println("Failure on " + ticker);
          e.printStackTrace(System.err);
        }
      }, es)).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(tickerTasks).get(10, TimeUnit.MINUTES);
    }

    // continuous refresh ?
    // clear console "\033[H\033[2J"
  }

  private void waitIfRateLimited(long reset) {
    if (reset > this.rateLimitResetSeconds) {
      this.rateLimitResetSeconds = reset;
    }
    var rateLimitResetSeconds = this.rateLimitResetSeconds;
    var announceInProgress = this.rateLimitAnnounceInProgress;
    if (rateLimitResetSeconds > 0) {
      Thread waitingThread = null;
      var amount = rateLimitResetSeconds - ((int) (System.currentTimeMillis() / 1000));
      if (!announceInProgress) {
        this.rateLimitAnnounceInProgress = true;
        waitingThread = LoadingIndicator.infinite(System.err)
                                        .loadingChars(LoadingIndicator.BRAILLE)
                                        .withPrefix("[Rate limit] Pausing for " + amount + "s")
                                        .withTerminateString("[Rate limit] Resuming")
                                        .asVirtualThread();
        waitingThread.start();
      }
      try {
        Thread.sleep(Duration.of(amount, ChronoUnit.SECONDS));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        if (!announceInProgress) {
          waitingThread.interrupt();
        }
      }
    }
  }

  private <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
    try {
      var response = httpClient.send(request, responseBodyHandler);

      // var limit = response.headers().firstValueAsLong("x-ratelimit-limit").orElseThrow();
      // var remaining = response.headers().firstValueAsLong("x-ratelimit-remaining").orElseThrow();
      // if (remaining < 4) {
      //   System.err.println("[Rate limit] remaining: " + remaining + " / " + limit);
      // }
      while (response.statusCode() == 429) {
        waitIfRateLimited(response.headers().firstValueAsLong("x-ratelimit-reset").orElseThrow());
        response = httpClient.send(request, responseBodyHandler);
      }
      return response;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private void displayQuote(String ticker, HttpResponse<String> quoteResponse, HttpResponse<String> profile2Response) {
    // https://finnhub.io/docs/api/quote
    var matcher = fieldMatcher.matcher(quoteResponse.body());
    var quotePayload = matcher.results().collect(Collectors.toMap(
            m -> m.group(1),
            m -> m.group(2)
    ));

    String error = quotePayload.get("error");
    if ("null".equals(quotePayload.get("d")) || error != null) {
      System.out.println("► " + bold + ticker + reset + " " + gray + "SKIPPED" + reset + "%n");
      if (error != null) {
        System.out.println("  " + red + error + reset + "%n");
      }
      return;
    }

    var currentPrice = Double.parseDouble(quotePayload.get("c"));
    var change = Double.parseDouble(quotePayload.get("d"));
    var percentChange = Double.parseDouble(quotePayload.get("dp"));
    var highPriceOfTheDay = Double.parseDouble(quotePayload.get("h"));
    var lowPriceOfTheDay = Double.parseDouble(quotePayload.get("l"));

    // https://finnhub.io/docs/api/company-profile2
    matcher = fieldMatcher.matcher(profile2Response.body());
    var profile2Payload = matcher.results().collect(Collectors.toMap(
            m -> m.group(1),
            m -> m.group(2)
    ));

    System.out.println(
            "► " + bold + ticker + reset + " " + (change < 0 ? red + "↘︎" : green + "↗︎") + reset + " " + gray + LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(quotePayload.get("t"))), ZoneId.systemDefault()) + reset + "\n" +
            italic + profile2Payload.get("name") + reset + " (" + profile2Payload.get("exchange") + ") currency: " + profile2Payload.get("currency") + "\n" +
            bold + underline + blue + currentPrice + reset + "\n" +
            (change < 0 ? change : "+" + change) + " (" + (change < 0 ? red : green) + percentChange + "%" + reset + ") " + green + "↑" + reset + highPriceOfTheDay + " " + red + "↓" + reset + lowPriceOfTheDay + " " + reset
    );
  }
}

@SuppressWarnings("unused")
class LoadingIndicator {
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
