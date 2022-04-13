package sandbox;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

// curl -i localhost:8080/puts/something
// curl --head localhost:8080/puts/something
// curl -i localhost:8080/puts/something --data 'yup'
public class WebServer {
  public static void main(String[] args) throws IOException {
    // var server = SimpleFileServer.createFileServer(
    //         new InetSocketAddress(8080),
    //         Path.of("/some/path"),
    //         OutputLevel.VERBOSE
    // );
    var filter = SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO);

    System.out.println("Starting server on port 0.0.0.0:8080");
    var server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", 8080),
            10,
            "/puts/",
            new SomeHandler(),
            filter);

    var handler = SimpleFileServer.createFileHandler(Path.of(System.getProperty("user.home")));
    server.createContext("/browse", handler);


    server.start();


    // var jsonHeaders = Headers.of("Content-Type", "application/json");
    // var getHandler = HttpHandlers.of(200, jsonHeaders, Files.readString(Paths.get("http-static2/get/get_request.json").toAbsolutePath()));
    // var postHandler = HttpHandlers.of(200, jsonHeaders, Files.readString(Paths.get("http-static2/post/post_request.json").toAbsolutePath()));
    // Predicate<Request> IS_GET = r -> r.getRequestMethod().equals("GET");
    // Predicate<Request> IS_POST = r -> r.getRequestMethod().equals("POST");
    // var notAllowedHandler = HttpHandlers.of(405,
    //                                         Headers.of("Access-Control-Allow-Methods", "GET,POST"),
    //                                         "Sorry, not allowed method");
    // var h1 = HttpHandlers.handleOrElse(IS_GET, getHandler, notAllowedHandler);
    // var h2 = HttpHandlers.handleOrElse(IS_POST, postHandler, h1);
    // var server = HttpServer.create(new InetSocketAddress(8001), 2,
    //                                "/", h2, SimpleFileServer.createOutputFilter(System.out, OutputLevel.INFO));
    server.start();
  }

  private static class SomeHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try (exchange) {
        switch (exchange.getRequestMethod()) {
          case "GET", "HEAD" -> {
            try (var is = exchange.getRequestBody()) {
              is.readAllBytes();
            }
            var response = (exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()).getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (var os = exchange.getResponseBody()) {
              os.write(response);
            }
          }
          case "POST", "PUT" -> {
            try (var bufferedReader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
              bufferedReader.lines().forEach(System.out::println);
            }
            exchange.sendResponseHeaders(201, 0);
          }
          default -> exchange.sendResponseHeaders(400, 0);
        }
        exchange.getHttpContext().getAttributes().forEach((k, v) -> System.out.println(k + ": " + v));

      }


    }
  }
}
