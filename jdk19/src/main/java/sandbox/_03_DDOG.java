package sandbox;

import java.io.IOException;
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

public class _03_DDOG {
  private volatile boolean rateLimitExceeded = false;
  private volatile long rateLimitResetSeconds = 0;
  private final Pattern fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"?(-?\\d+(?:.\\d+)?|[^\"]+)\"?");
  private HttpClient httpClient;

  public static void main(String[] args) throws Exception {
    // https://finnhub.io/pricing
    // 60 API calls/minute
    if (args.length == 0) {
      System.out.println("Needs token");
      System.exit(1);
    }
    var finnhubToken = args[0];
    if (finnhubToken.isBlank()) {
      System.out.println("Needs non empty token");
      System.exit(1);
    }

    new _03_DDOG().run(finnhubToken);
  }

  private void run(String finnhubToken) throws InterruptedException, ExecutionException, TimeoutException {
    var tickers = List.of(
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

            "GC=F", // TODO no company profile for gold

            // not available in the free tier
            // "NASDAQ",
            // "^HSI",
            // "^FCHI",
            // "DOW J",
            // "^N225",
            // "^GDAXI",
            // "^FTSE",
            // "BTC-USD",
            // "BTC-EUR",
            // "ETH-USD",
            // "ETH-EUR",
            // "EUR/USD",
            ""
    );

    httpClient = HttpClient.newBuilder()
                           .connectTimeout(Duration.ofSeconds(10))
                           .build();

    try (var es = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("finnhub-fetcher").factory())) {
      var tickerTasks = tickers.stream().filter(Predicate.not(String::isBlank)).map(ticker -> CompletableFuture.runAsync(() -> {
        try {
          var quoteResponse = CompletableFuture.supplyAsync(() -> sendWithRetry(
                  ticker,
                  HttpRequest.newBuilder(URI.create("https://finnhub.io/api/v1/quote?symbol=" + URLEncoder.encode(ticker, StandardCharsets.UTF_8)))
                             .GET()
                             .header("X-Finnhub-Token", finnhubToken)
                             .build(),
                  BodyHandlers.ofString()
          ), es);

          var profile2Response = CompletableFuture.supplyAsync(() -> sendWithRetry(
                  ticker,
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
          e.printStackTrace(System.err);
        }
      }, es)).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(tickerTasks).get(10, TimeUnit.MINUTES);
    }
  }

  private void waitIfRateLimited(String ticker) {
    if (rateLimitExceeded) {
      var rateLimitResetSeconds = this.rateLimitResetSeconds;
      assert rateLimitResetSeconds > 0;
      var amount = rateLimitResetSeconds - ((int) (System.currentTimeMillis() / 1000));
      System.err.println("[Rate limit] Waiting for " + ticker + ", reset in " + amount + "s");
      var start = System.currentTimeMillis();
      try {
        Thread.sleep(Duration.of(amount, ChronoUnit.SECONDS));
      } catch (InterruptedException e) {
        System.err.printf("VirtualThread %s - %s interrupted%n", ticker, Thread.currentThread().getName());
      } finally {
        // System.err.println("[Rate limit] Waited for " + ticker + " " + (System.currentTimeMillis() - start) / 1000 + "s");
        System.err.printf("[Rate limit] VirtualThread %s - %s woke up, slept %dms%n", ticker, Thread.currentThread().getName(), (System.currentTimeMillis() - start));
      }
    }
  }

  private <T> HttpResponse<T> sendWithRetry(String ticker, HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
    try {
      var response = httpClient.send(request, responseBodyHandler);

      var limit = response.headers().firstValueAsLong("x-ratelimit-limit").orElseThrow();
      var remaining = response.headers().firstValueAsLong("x-ratelimit-remaining").orElseThrow();
      if (remaining < 4) {
        System.err.println("[Rate limit] remaining: " + remaining + " / " + limit);
      }
      while (response.statusCode() == 429) {
        rateLimitExceeded = true;
        rateLimitResetSeconds = response.headers().firstValueAsLong("x-ratelimit-reset").orElseThrow();
        waitIfRateLimited(ticker);
        response = httpClient.send(request, responseBodyHandler);
      }
      rateLimitExceeded = false;
      return response;
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private void displayQuote(String ticker, HttpResponse<String> quoteResponse, HttpResponse<String> profile2Response) {
    var red = "\u001B[00;31m";
    var green = "\u001B[00;32m";
    var blue = "\u001B[00;34m";
    var gray = "\u001B[00;37m";
    var bold = "\u001B[01m";
    var italic = "\u001B[03m";
    var underline = "\u001B[04m";
    var reset = "\u001B[00m";

    // https://finnhub.io/docs/api/quote
    var matcher = fieldMatcher.matcher(quoteResponse.body());
    var quotePayload = matcher.results().collect(Collectors.toMap(
            m -> m.group(1),
            m -> m.group(2)
    ));

    var currentPrice = Double.parseDouble(quotePayload.get("c"));
    var change = Double.parseDouble(quotePayload.get("d"));
    var percentChange = Double.parseDouble(quotePayload.get("dp"));
    var highPriceOfTheDay = Double.parseDouble(quotePayload.get("h"));
    var lowPriceOfTheDay = Double.parseDouble(quotePayload.get("l"));

    https:
// finnhub.io/docs/api/company-profile2
    matcher = fieldMatcher.matcher(profile2Response.body());
    var profile2Payload = matcher.results().collect(Collectors.toMap(
            m -> m.group(1),
            m -> m.group(2)
    ));

    System.out.printf(
            """
            ► %s %s %s\u001B[00m
            %s (%s) currency: %s\u001B[00m
            %s
            %s (%s) \u001B[00;32m↑\u001B[00m %s \u001B[00;31m↓\u001B[00m %s
            """,
            // line 1
            bold + ticker + reset,
            (change < 0 ? red + "↘︎" : green + "↗︎") + reset,
            gray + LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(quotePayload.get("t"))), ZoneId.systemDefault()) + reset,

            // line 2
            italic + profile2Payload.get("name") + reset,
            profile2Payload.get("exchange"),
            profile2Payload.get("currency"),

            // Line 3
            bold + underline + blue + currentPrice + reset,

            // line 4
            change < 0 ? change : "+" + change,
            (change < 0 ? red + percentChange + "%" : green + "+" + percentChange + "%") + reset,
            highPriceOfTheDay,
            lowPriceOfTheDay
    );
  }
}
