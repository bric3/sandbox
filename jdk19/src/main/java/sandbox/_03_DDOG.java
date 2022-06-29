package sandbox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class _03_DDOG {
  volatile boolean rateLimitExceeded = false;
  private Pattern fieldMatcher = Pattern.compile("\"(\\w+)\"\\s*:\\s*(-?\\d+(?:.\\d+)?)");

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
            "SPY",
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

    var httpClient = HttpClient.newBuilder()
                               .connectTimeout(Duration.ofSeconds(10))
                               .build();
    
    try (var es = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("finnhub-fetcher").factory())) {
      var tickerTasks = tickers.stream().filter(Predicate.not(String::isBlank)).map(ticker -> CompletableFuture.runAsync(() -> {
        if (rateLimitExceeded) {
          return;
        }

        var request = HttpRequest.newBuilder(URI.create("https://finnhub.io/api/v1/quote?symbol=" + ticker + "&token=" + finnhubToken))
                                 .GET()
                                 // .header("X-Finnhub-Token", finnhubToken)
                                 .build();
        try {
          System.out.println("Fetching " + ticker);
          var response = httpClient.send(request, BodyHandlers.ofString());
          switch (response.statusCode()) {
            case 200 -> displayQuote(ticker, response);
            case 429 -> {
              rateLimitExceeded = true;
              System.out.println("Rate limit exceeded.");
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }, es)).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(tickerTasks).get(10, TimeUnit.MINUTES);
    }
  }

  private void displayQuote(String ticker, HttpResponse<String> response) {
    var red = "\u001B[00;31m";
    var green = "\u001B[00;32m";
    var blue = "\u001B[00;34m";
    var gray = "\u001B[01;30m";
    var reset = "\u001B[00m";

    // https://finnhub.io/docs/api/quote
    var body = response.body();
    System.out.println("Got " + ticker + ": " + blue + body + reset);
    var matcher = fieldMatcher.matcher(body);

    var quotePayload = matcher.results().collect(Collectors.toMap(
            m -> m.group(1),
            m -> m.group(2)
    ));

    var currentPrice = Double.parseDouble(quotePayload.get("c"));
    var change = Double.parseDouble(quotePayload.get("d"));
    var percentChange = Double.parseDouble(quotePayload.get("dp"));
    var highPriceOfTheDay = Double.parseDouble(quotePayload.get("h"));
    var lowPriceOfTheDay = Double.parseDouble(quotePayload.get("l"));


    System.out.printf(
            """
            ► %s %s %s\u001B[00m
            %s
            %s (%s) \u001B[00;32m↑\u001B[00m %s \u001B[00;31m↓\u001B[00m %s
            """,
            ticker,
            (change < 0 ? red + "↘︎" : green + "↗︎") + reset,
            gray + LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(quotePayload.get("t"))), ZoneId.systemDefault()) + reset,
            currentPrice,
            change < 0 ? change : "+" + change,
            (change < 0 ? red + percentChange + "%" : green + "+" + percentChange + "%") + reset,
            highPriceOfTheDay,
            lowPriceOfTheDay
    );
  }
}
