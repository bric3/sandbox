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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GithubMavenPackagesGraphQL {

  public static void main(String[] args) throws IOException, InterruptedException {
    final HttpClient build = HttpClient.newBuilder().build();

    if (args.length != 4) {
      System.err.println("First arg is the github token");
      System.exit(1);
    }
    var owner = args[0];
    var repo = args[1];
    var version = args[2];
    var token = args[3];

    // https://developer.github.com/v4/explorer/
    // https://graphql.org/learn/queries/
    // https://github.community/t/package-search-api/14891
    var graphQL = "{" +
        "\"query\":\"query RepoPackages($owner: String!, $repo: String!, $version: String!) { \\n" +
        "  repository(owner:$owner, name:$repo) {\\n" +
        "    id,\\n" +
        "    packages(\\n" +
        "      last: 30, \\n" +
        "      packageType: MAVEN,\\n" +
        "      orderBy: {\\n" +
        "        direction: DESC,\\n" +
        "        field: CREATED_AT\\n" +
        "      }\\n" +
        "    ) {\\n" +
        "      edges {\\n" +
        "        node {\\n" +
        "          name,\\n" +
        "          # statistics {\\n" +
        "          #   downloadsTotalCount\\n" +
        "          # },\\n" +
        "          latestVersion {\\n" +
        "            version\\n" +
        "          },\\n" +
        "          version(version: $version) {\\n" +
        "            version\\n" +
        "          }\\n" +
        "        }\\n" +
        "      }\\n" +
        "    }\\n" +
        "  }\\n" +
        "}\"," +
        "\"variables\":{" +
        "  \"owner\":\"" + owner + "\"," +
        "  \"repo\":\"" + repo + "\"," +
        "  \"version\":\"" + version + "\"" +
        "}," +
        "\"operationName\":\"RepoPackages\"" +
        "}";
    var request = HttpRequest.newBuilder(URI.create("https://api.github.com/graphql"))
        .header("Content-Type", "application/json")
        .header("Authorization", "bearer " + token)
        .POST(HttpRequest.BodyPublishers.ofString(graphQL))
        .build();
    var httpResponse = build.send(request, HttpResponse.BodyHandlers.ofString());
    httpResponse.headers().map().forEach((k, v) -> System.out.printf("%s: %s%n", k, v));
    System.out.println(jsonPrettyPrinter(httpResponse.body()));
  }

  public static StringBuilder jsonPrettyPrinter(String json) {
    var sb = new StringBuilder(json.length());
    int indentationLevel = 0;
    for (char c : json.toCharArray()) {
      switch (c) {
        case '[':
        case '{':
          indentationLevel += 2;
          sb.append(c)
              .append('\n')
              .append(" ".repeat(Math.max(0, indentationLevel)));
          break;
        case ']':
        case '}':
          indentationLevel -= 2;
          sb.append('\n')
              .append(" ".repeat(Math.max(0, indentationLevel)))
              .append(c);
          break;
        case ',':
          sb.append(c)
              .append('\n')
              .append(" ".repeat(Math.max(0, indentationLevel)));
          break;
        case ':':
          sb.append(c).append(' ');
          break;
        default:
          sb.append(c);
      }
    }
    return sb;
  }
}