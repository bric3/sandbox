///usr/bin/env jbang "$0" ; exit $?
//JAVA 19
//JAVAC_OPTIONS --enable-preview --source 19
//JAVA_OPTIONS -ea --enable-preview
//DEPS com.vladsch.flexmark:flexmark-all:0.64.0

package sandbox.disqus2giscus;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.teeing;
import static java.util.stream.Collectors.toList;

public class Disqus2Giscus {
  private final GitHub gh;
  private final String discussionCategoryId;
  private final String repoId;
  private boolean convertToMarkdown = true;
  private boolean extractAuthors = false;
  private String mapping = "";
  private String targetCategoryName = "";
  private Path userMappingFile;
  private String repo = "";
  private Path exportFile;
  private String forum;
  private String host = "";

  public static void main(String[] args) throws Exception {
    new Disqus2Giscus(args).run();
  }

  public Disqus2Giscus(String... args) {
    if (args.length == 0) {
      usage();
    }
    var githubToken = System.getenv("GITHUB_TOKEN");
    var dryRun = false;
    for (int argIdx = 0; argIdx < args.length; argIdx++) {
      var arg = args[argIdx];
      switch (arg) {
        case "-h", "--help" -> usage();
        case "-a", "--extract-authors" -> extractAuthors = true;
        case "-e", "--export-file" -> exportFile = expandPathSimple(readOptionValue(++argIdx, arg, args));
        case "-u", "--user-mapping-file" -> userMappingFile = expandPathSimple(readOptionValue(++argIdx, arg, args));
        case "-r", "--repo" -> repo = readOptionValue(++argIdx, arg, args);
        case "-f", "--forum-name" -> forum = readOptionValue(++argIdx, arg, args);
        case "-c", "--target-category" -> targetCategoryName = readOptionValue(++argIdx, arg, args);
        case "-t", "--token" -> githubToken = readOptionValue(++argIdx, arg, args);
        case "-m", "--mapping" -> mapping = readOptionValue(++argIdx, arg, args);
        case "--host" -> {
          host = readOptionValue(++argIdx, arg, args);
          if (!host.endsWith("/")) {
            host += "/";
          }
        }
        case "--convert-to-markdown", "--no-convert-to-markdown" -> convertToMarkdown = !arg.startsWith("--no");
        case "-n", "--dry-run" -> dryRun = true;
      }
    }

    var validMappingValues = Set.of("url", "pathname", "title", "og:title");
    if (!validMappingValues.contains(mapping)) {
      System.err.println("Invalid mapping: " + mapping + ". Supported values are: " + validMappingValues);
      System.exit(1);
    }

    if (Objects.equals(mapping, "pathname") && Objects.equals(host, "")) {
      System.err.println("You must specify a host (e.g. '--host https://example.com') when using the 'pathname' mapping");
      System.exit(1);
    }

    if (exportFile == null || !Files.exists(exportFile)) {
      System.err.println(exportFile == null ? "Export file option not set" : "File " + exportFile + " does not exist");
      System.exit(1);
    }

    if (userMappingFile == null) {
      System.out.println("Hint: User author mapping file option not set");
    }
    if (userMappingFile != null && !Files.exists(userMappingFile)) {
      System.err.println("File " + userMappingFile + " does not exist");
      System.exit(1);
    }

    if (convertToMarkdown) {
      try {
        Class.forName("com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter");
      } catch (ClassNotFoundException e) {
        System.err.println("To use convert HTML comment to markdown, you must add the flexmark-all dependency to your classpath or run it with jbang, or disable the conversion with '--no-convert-to-markdown' option");
        System.exit(1);
      }
    }

    // check repo
    if (!repo.matches("\\w+/\\w+")) {
      System.err.println("Invalid Github repo name " + repo);
      System.exit(1);
    }

    // TODO don't check github or category when extract-authors is set
    gh = new GitHub(githubToken, dryRun);
    repoId = gh.getRepoId(repo);
    if (repoId == null || repoId.isEmpty()) {
      System.err.println("No repository found for " + repo);
      System.exit(1);
    }

    // check discussion category
    discussionCategoryId = gh.getDiscussionCategoryId(repo, targetCategoryName);
    if (discussionCategoryId == null || discussionCategoryId.isEmpty()) {
      System.err.println(targetCategoryName.isBlank() ? "No category name entered" : "No discussion category found for " + targetCategoryName);
      System.exit(1);
    }
  }

  private Path expandPathSimple(String str) {
    var userMappingFile1 = Path.of(str);
    if (userMappingFile1.startsWith("~")) {
      userMappingFile1 = Path.of(System.getProperty("user.home")).resolve(userMappingFile.subpath(1, userMappingFile.getNameCount()));
    }
    return userMappingFile1;
  }

  private void run() throws Exception {
    var document = getXmlDocument();
    var xpath = XPathFactory.newInstance().newXPath();

    System.out.printf(
            """
            Export file : %s
            Forum       : %s      
            """,
            exportFile,
            xpath.evaluate("/disqus/category[1]/forum", document)
    );

    // a forum or website is associated to a "category"
    var categoryId = xpath.evaluate("/disqus/category[./forum = '" + forum + "']/@id", document).trim();

    // get threads for this category (or website)
    var threadNodeList = (NodeList) xpath.compile("/disqus/thread[./category/@id = '" + categoryId + "']").evaluate(document, XPathConstants.NODESET);
    var disqusThreads = IntStream.range(0, threadNodeList.getLength())
                                 .mapToObj(threadNodeList::item)
                                 .map(node -> DisqusThread.disqusThread(xpath, node))
                                 // .peek(System.out::println)
                                 .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(DisqusThread::date))));
    assert disqusThreads.size() == threadNodeList.getLength() :
            "Difference between number of threads in query " + threadNodeList.getLength() + " and mapped threads " + disqusThreads.size();
    if (disqusThreads.isEmpty()) {
      System.out.println("No threads found for forum " + forum);
      return;
    }

    // get non-spam and non-deleted comments
    var postNodeList = (NodeList) xpath.compile("/disqus/post[./isSpam = 'false' and ./isDeleted = 'false']").evaluate(document, XPathConstants.NODESET);
    var result = IntStream.range(0, postNodeList.getLength())
                          .mapToObj(postNodeList::item)
                          .map(node -> DisqusPost.disqusPost(xpath, node))
                          // .peek(System.out::println)
                          .collect(teeing(
                                  toList(),
                                  mapping(
                                          DisqusPost::threadId,
                                          Collectors.toUnmodifiableSet()
                                  ),
                                  Pair::new
                          ));
    var disqusPosts = result.a;
    assert disqusPosts.size() == postNodeList.getLength() :
            "Difference between number of posts in query " + postNodeList.getLength() + " and mapped posts " + disqusPosts.size();
    if (disqusPosts.isEmpty()) {
      System.err.println("No non-deleted, non-spam comments found");
      System.exit(1);
    }

    // Keep only threads with comments
    var threadIds = result.b;
    disqusThreads.removeIf(t -> !threadIds.contains(t.id()));
    if (disqusThreads.isEmpty()) {
      System.err.println("No threads with comments found");
      System.exit(1);
    }

    // Rebuild hierarchic discussions in a "hash tree"
    // [post-id | thread-id, list { post-id, post-id, ... }]
    var discussionsIndex = new LinkedHashMap<String, TreeSet<DisqusPost>>(disqusPosts.size());
    Function<String, TreeSet<DisqusPost>> newChildPosts = k -> new TreeSet<>(comparing(DisqusPost::date));
    disqusPosts.forEach(post -> {
      discussionsIndex.computeIfAbsent(post.id(), newChildPosts);
      if (post.parentId != null && !post.parentId.isBlank()) {
        discussionsIndex.computeIfAbsent(post.parentId, newChildPosts).add(post);
      } else {
        discussionsIndex.computeIfAbsent(post.threadId, newChildPosts).add(post);
      }
    });

    if (extractAuthors) {
      Stream.concat(
              disqusThreads.stream().map(DisqusThread::author),
              disqusPosts.stream().map(DisqusPost::author)
      ).map(Author::name).distinct().sorted().forEach(System.out::println);
      System.exit(0);
    }

    // t: 8296439444
    // p: 5928880724
    var tid = "8296439444";
    flattenChildNodes(discussionsIndex, tid, 0).forEach(pair -> {
      var indent = " ".repeat(pair.a * 2);
      System.out.println(indent + pair.b);
    });

    var authorMapping = readCsv();

    disqusThreads.stream().filter(t -> Objects.equals(t.id, tid)).forEach(t -> {
      var discussionId = gh.createDiscussion(
              repoId,
              discussionCategoryId,
              switch (mapping) {
                case "title", "og:title" -> t.title();
                case "url" -> t.link();
                case "pathname" -> t.link.replaceFirst("^" + host, "");
                default -> throw new IllegalStateException("Unexpected value: " + mapping);
              },
              """
              %s
              %s
              """.formatted(t.title, t.link)
      );

      visitChildNodes(
              discussionsIndex,
              t.id,
              null,  // first level comment are not replying
              (replyToId, post) -> gh.addDiscussionComment(
                      discussionId,
                      replyToId,
                      """
                      Originally posted by %s on %s
                                                       
                      ----
                                                       
                      %s
                      """.formatted(
                              authorMapping.getOrDefault(post.author.name, post.author.name),
                              post.date,
                              new HtmlConverter(convertToMarkdown).convert(post.message) // TODO basic markdown conversion
                      )
              )
      ).forEach(p -> {});
    });

    System.out.println("Target repo : " + repo);

    System.out.printf(
            """
            Giscus Configuration:
                        
            <script src="https://giscus.app/client.js"
                    data-repo="%s"
                    data-repo-id="%s"
                    data-category="%s"
                    data-category-id="%s"
                    data-mapping="%s"
                    data-strict="0"
                    data-reactions-enabled="1"
                    data-emit-metadata="0"
                    data-input-position="top"
                    data-theme="preferred_color_scheme"
                    data-lang="en"
                    data-loading="lazy"
                    crossorigin="anonymous"
                    async>
            </script>
            """,
            repo,
            repoId,
            targetCategoryName,
            discussionCategoryId,
            mapping
    );
  }

  private Map<String, String> readCsv() throws IOException {
    if (userMappingFile == null) {
      return Map.of();
    }
    try (var lines = Files.lines(userMappingFile, StandardCharsets.UTF_8)) {
      return lines.filter(l -> !l.isBlank()).map(l -> l.split(",")).collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim()));
    }
  }

  record Pair<A, B>(A a, B b) {}


  private static Stream<Pair<String, DisqusPost>> visitChildNodes(Map<String, TreeSet<DisqusPost>> index, String id, String visitorArg0, BiFunction<String, DisqusPost, String> visitor) {
    return index.get(id).stream().flatMap(childPost -> {
      var r = visitor.apply(visitorArg0, childPost);
      return Stream.concat(
              Stream.of(new Pair<>(r, childPost)),
              visitChildNodes(index, childPost.id, r, visitor)
      );
    });
  }

  private static Stream<Pair<Integer, DisqusPost>> flattenChildNodes(Map<String, TreeSet<DisqusPost>> index, String id, int level) {
    return index.get(id).stream().flatMap(childPost -> Stream.concat(
            Stream.of(new Pair<>(level, childPost)),
            flattenChildNodes(index, childPost.id, level + 1)
    ));
  }


  private static class GitHub {
    // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String githubToken;
    private final boolean dryRun;

    public GitHub(String githubToken, boolean dryRun) {
      this.githubToken = Objects.requireNonNull(githubToken);
      this.dryRun = dryRun;
    }

    public String getRepoId(String repo) {
      Objects.requireNonNull(repo);
      var owner = repo.substring(0, repo.indexOf('/'));
      var repoName = repo.substring(repo.indexOf('/') + 1);
      var payload = graphQl(
              """
              query($owner: String!, $name: String!) {
                repository(owner: $owner, name: $name) {
                  id
                }
              }
              """,
              Map.of("owner", owner, "name", repoName)
      );

      // lame json parser, assumes the output is minified
      // {"data":{"repository":{"id":"R_kggXA"}}}
      var matcher = Pattern.compile("\\{\"id\":\"(.*?)\"}").matcher(payload);
      if (!matcher.find()) {
        System.err.println(payload);
        return null;
      }
      return matcher.group(1);
    }

    public String getDiscussionCategoryId(String repo, String targetCategoryName) {
      Objects.requireNonNull(repo);
      Objects.requireNonNull(targetCategoryName);
      var owner = repo.substring(0, repo.indexOf('/'));
      var repoName = repo.substring(repo.indexOf('/') + 1);
      var payload = graphQl(
              """
              query($owner: String!, $name: String!) {
                repository(owner: $owner, name: $name) {
                  discussionCategories(first: 20) {
                    nodes {
                      id
                      name
                    }
                  }
                }
              }           
              """,
              Map.of("owner", owner, "name", repoName)
      );

      // lame json parser, assumes the output is minified
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#discussioncategory
      // {"data":{"repository":{"discussionCategories":{"nodes":[{"id":"DIC_kw4CR-","description":"Updates from maintainers"},...]}}}}
      var matcher = Pattern.compile("\\{\"discussionCategories\":\\{\"nodes\":\\[(.*)]}}").matcher(payload);
      if (!matcher.find()) {
        System.err.println(payload);
        return owner;
      }

      var nodes = matcher.group(1);
      if (nodes.isEmpty()) {
        System.out.println("No discussion categories found");
        System.exit(1);
      }

      var targetCategoryId = Pattern.compile("\\{\"id\":\"(.*?)\",\"name\":\"(.*?)\"}")
                                    .matcher(nodes)
                                    .results()
                                    .map(mr -> new Object() {
                                      final String id = mr.group(1);
                                      final String name = mr.group(2);
                                    })
                                    .filter(o -> o.name.equals(targetCategoryName))
                                    .map(o -> o.id)
                                    .findFirst();

      return targetCategoryId.orElse(null);
    }

    public String createDiscussion(String repoId, String discussionCategoryId, String title, String body) {
      if (dryRun) {
        System.out.println("### createDiscussion: " + title);
        return "fake-d-" + UUID.randomUUID(); // return "fake-{random id}" ?
      }
      Objects.requireNonNull(repoId);
      Objects.requireNonNull(discussionCategoryId);
      Objects.requireNonNull(title);
      Objects.requireNonNull(body);
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#creatediscussion
      var payload = graphQl(
              """
              mutation($repoId: ID!, $categoryId: ID!, $title: String!, $body: String!) {
                createDiscussion(input: {repositoryId: $repoId, categoryId: $categoryId, title: $title, body: $body}) {
                  discussion {
                    id
                  }
                }
              }
              """,
              Map.of("repoId", repoId, "categoryId", discussionCategoryId, "title", title, "body", body)
      );
      System.out.println(payload);


      // lame json parser, assumes the output is minified
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#discussion
      var matcher = Pattern.compile("\\{\"id\":\"(.*?)\"}").matcher(payload);
      if (!matcher.find()) {
        System.err.println(payload);
        System.exit(1);
      }
      return matcher.group(1);
    }

    public String addDiscussionComment(String discussionId, String replyToId, String body) {
      if (dryRun) {
        System.out.println("### dry run: addDiscussionComment " + discussionId + " " + replyToId + "\n" + body);

        return "fake-c-" + UUID.randomUUID(); // return "fake-{random id}" ?
      }
      Objects.requireNonNull(discussionId);
      Objects.requireNonNull(body);

      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#adddiscussioncomment
      String payload = switch (replyToId) {
        case null -> graphQl(
                """
                mutation($discussionId: ID!, $body: String!) {
                  addDiscussionComment(input: {discussionId: $discussionId, body: $body}) {
                    comment {
                      id
                    }
                  }
                }
                """,
                Map.of("discussionId", discussionId, "body", body)
        );
        default -> graphQl(
                """
                mutation($discussionId: ID!, $body: String!, $replyToId: ID) {
                  addDiscussionComment(input: {discussionId: $discussionId, body: $body, replyToId: $replyToId}) {
                    comment {
                      id
                    }
                  }
                }
                """,
                Map.of("discussionId", discussionId, "body", body, "replyToId", replyToId)
        );
      };
      System.out.println(payload);

      // lame json parser, assumes the output is minified
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#discussioncomment
      var matcher = Pattern.compile("\\{\"id\":\"(.*?)\"}").matcher(payload);
      if (!matcher.find()) {
        System.err.println(payload);
        System.exit(1);
      }
      return matcher.group(1);
    }

    private String graphQl(String graphQlQuery, Map<String, String> variables) throws UncheckedIOException {
      Objects.requireNonNull(graphQlQuery);
      Objects.requireNonNull(variables);

      Function<String, String> stringEscaper = s -> s.replace("\\", "\\\\")
                                                     .replace("\"", "\\\"")
                                                     .replace("\r", "\\r")
                                                     .replace("\n", "\\n")
                                                     .replace("\t", "\\t");

      var json = """
                 {
                   "query": "%s",
                   "variables": %s
                 }
                 """.formatted(
              graphQlQuery
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n"),
              variables.entrySet()
                       .stream()
                       .map(e -> "\"%s\":\"%s\"".formatted(
                               e.getKey(),
                               stringEscaper.apply(e.getValue())
                       )).collect(joining(",", "{", "}"))
      );

      if (githubToken.isBlank()) {
        System.err.println("GITHUB_TOKEN environment variable is not set, or --token command line option is not set");
        System.exit(1);
      }

      var request = HttpRequest.newBuilder()
                               .uri(URI.create("https://api.github.com/graphql"))
                               .header("Authorization", "token " + githubToken)
                               .header("Accept", "application/json")
                               .header("Content-Type", "application/json")
                               .POST(BodyPublishers.ofString(json))
                               .build();
      var responseBodyHandler = BodyHandlers.ofString();

      try {
        var response = httpClient.send(request, responseBodyHandler);
        while (response.statusCode() == 429) {
          waitIfRateLimited(response.headers().firstValueAsLong("x-ratelimit-reset").orElseThrow());
          response = httpClient.send(request, responseBodyHandler);
        }
        if (response.statusCode() == 400) {
          System.err.printf(
                  """
                  Bad request: %d
                  Response: %s
                  Request: %s
                  """,
                  response.statusCode(),
                  response.body(),
                  json
          );
          System.exit(1);
        }
        if (response.statusCode() >= 400) {
          System.out.println(response.headers());
          response.headers().firstValue("X-RateLimit-Remaining").filter(r -> Objects.equals(r, "0")).ifPresent(r -> System.out.println("Rate limit reached"));
          System.err.printf(
                  """
                  Bad request: %d %s%n
                  """,
                  response.statusCode(),
                  response.body()
          );
          System.exit(1);
        }

        return response.body();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }

    private void waitIfRateLimited(long resetSeconds) throws InterruptedException {
      long resetMilliSeconds = resetSeconds * 1000 - System.currentTimeMillis();
      if (resetMilliSeconds > 0) {
        System.out.printf("Rate limit reached, waiting %d seconds%n", resetSeconds);
        Thread.sleep(resetMilliSeconds);
      }
    }
  }

  private Document getXmlDocument() throws ParserConfigurationException, SAXException, IOException {
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);
    var documentBuilder = dbf.newDocumentBuilder();
    return documentBuilder.parse(exportFile.toFile());
  }


  private static String readOptionValue(int optionValueIdx, String arg, String[] args) {
    if (optionValueIdx > args.length - 1) {
      System.err.printf("Missing value for '%s'%n", arg);
      System.exit(1);
    }

    return args[optionValueIdx];
  }

  private static void usage() {
    System.out.println(
            """
            Usage:
              env GITHUB_TOKEN=... java --source 19 Disqus2Giscus.java -f my-forum -e export.xml -r ghUser/repo -c Announcements
            """
    );
    System.exit(0);
  }

  private record Author(
          String name,
          String username,
          boolean anonymous
  ) {
    private static final Map<String, Author> authorMap = new ConcurrentHashMap<>();

    private static Author author(XPath xpath, Node context) throws XPathExpressionException {
      var authorName = xpath.evaluate("./author/name", context).trim();
      return authorMap.computeIfAbsent(authorName, name -> {
        try {
          return new Author(
                  name,
                  xpath.evaluate("./author/username", context).trim(),
                  Boolean.parseBoolean(xpath.evaluate("./author/isAnonymous", context).trim())
          );
        } catch (XPathExpressionException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private record DisqusThread(
          String id,
          String link,
          String title,
          Author author,
          ZonedDateTime date,
          String categoryId
  ) {
    private static DisqusThread disqusThread(XPath xpath, Node threadNode) {
      try {
        return new DisqusThread(
                xpath.evaluate("@id", threadNode),
                xpath.evaluate("./link", threadNode).trim(),
                xpath.evaluate("./title", threadNode).trim(),
                Author.author(xpath, threadNode),
                ZonedDateTime.parse(xpath.evaluate("./createdAt", threadNode).trim()),
                xpath.evaluate("./category/@id", threadNode)
        );
      } catch (XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private record DisqusPost(
          String id,
          String message,
          Author author,
          ZonedDateTime date,
          String threadId,
          String parentId
  ) {
    private static DisqusPost disqusPost(XPath xpath, Node threadNode) {
      try {
        return new DisqusPost(
                xpath.evaluate("@id", threadNode),
                xpath.evaluate("./message", threadNode).trim(),
                Author.author(xpath, threadNode),
                ZonedDateTime.parse(xpath.evaluate("./createdAt/text()", threadNode).trim()),
                xpath.evaluate("./thread/@id", threadNode).trim(),
                xpath.evaluate("./parent/@id", threadNode).trim()
        );
      } catch (XPathExpressionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class HtmlConverter {
    private MethodHandle convertMH = null;

    public HtmlConverter(boolean convertToMarkdown) {
      if (convertToMarkdown) {
        // FlexmarkHtmlConverter.builder().build();
        try {
          Class<?> convertClass = Class.forName("com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter");
          Class<?> convertBuilderClass = Class.forName("com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter$Builder");

          var builder = MethodHandles.lookup()
                                     .findStatic(convertClass, "builder", MethodType.methodType(convertBuilderClass))
                                     .invoke();

          var converter = MethodHandles.lookup()
                                       .findVirtual(convertBuilderClass, "build", MethodType.methodType(convertClass))
                                       .invoke(builder);

          convertMH = MethodHandles.lookup().findVirtual(convertClass, "convert", MethodType.methodType(String.class, String.class))
                                   .bindTo(converter)
                                   .asType(MethodType.methodType(String.class, String.class));
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }

      }
    }

    public String convert(String message) {
      if (convertMH != null) {
        try {
          return (String) convertMH.invoke(message);
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      } else {
        return message;
      }
    }
  }
}