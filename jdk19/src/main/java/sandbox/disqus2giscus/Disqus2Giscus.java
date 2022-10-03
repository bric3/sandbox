///usr/bin/env jbang "$0" ; exit $?
// JAVA 19

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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Disqus2Giscus {
  private final GitHub gh;
  private String targetCategoryName = "";
  private String disqusUserName = "";
  private String repo = "";
  private Path file;
  private String forum;


  public static void main(String[] args) throws Exception {
    new Disqus2Giscus(args).run();
  }

  public Disqus2Giscus(String... args) throws IOException, InterruptedException {
    if (args.length == 0) {
      usage();
    }
    var githubToken = System.getenv("GITHUB_TOKEN");
    for (int argIdx = 0; argIdx < args.length; argIdx++) {
      var arg = args[argIdx];
      switch (arg) {
        case "-h", "--help" -> usage();
        case "-e", "--export-file" -> {
          file = Path.of(readOptionValue(++argIdx, arg, args));
          if (file.startsWith("~")) {
            file = Path.of(System.getProperty("user.home")).resolve(file.subpath(1, file.getNameCount()));
          }
        }
        case "-r", "--repo" -> repo = readOptionValue(++argIdx, arg, args);
        case "-f", "--forum-name" -> forum = readOptionValue(++argIdx, arg, args);
        case "-u", "--disqus-user-name" -> disqusUserName = readOptionValue(++argIdx, arg, args);
        case "-c", "--target-category" -> targetCategoryName = readOptionValue(++argIdx, arg, args);
        case "-t", "--token" -> githubToken = readOptionValue(++argIdx, arg, args);
      }
    }
    gh = new GitHub(githubToken);

    if (file == null || !Files.exists(file)) {
      System.err.println("File " + file + " does not exist");
      System.exit(1);
    }

    // check repo
    if (!repo.matches("\\w+/\\w+")) {
      System.err.println("Invalid Github repo name " + repo);
      System.exit(1);
    }
    var repoId = gh.getRepoId(repo);
    if (repoId == null || repoId.isEmpty()) {
      System.err.println("No repository found for " + repo);
      System.exit(1);
    }

    var discussionCategoryId = gh.getDiscussionCategoryId(repo, targetCategoryName);
    if (discussionCategoryId == null || discussionCategoryId.isEmpty()) {
      System.err.println(targetCategoryName.isBlank() ? "No category name entered" : "No discussion category found for " + targetCategoryName);
      System.exit(1);
    }
  }

  private void run() throws Exception {
    var document = getXmlDocument();
    var xpath = XPathFactory.newInstance().newXPath();

    System.out.printf(
            """
            Export file : %s
            Target repo : %s
            Forum       : %s        
            """,
            file,
            repo,
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
                                 .collect(Collectors.toConcurrentMap(
                                         DisqusThread::id,
                                         Function.identity(),
                                         (a, b) -> a,
                                         () -> new ConcurrentHashMap<>(threadNodeList.getLength())
                                 ));
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
                          .collect(
                                  Collectors.teeing(
                                          Collectors.toConcurrentMap(
                                                  DisqusPost::id,
                                                  Function.identity(),
                                                  (a, b) -> {
                                                    System.err.printf("Duplicate post id a: '%s' b: '%s'%n", a.id(), b.id());
                                                    return a;
                                                  },
                                                  () -> new ConcurrentHashMap<>(postNodeList.getLength())
                                          ),
                                          Collectors.mapping(
                                                  DisqusPost::threadId,
                                                  Collectors.toUnmodifiableSet()
                                          ),
                                          (ps, ts) -> new Object() {
                                            final Map<String, DisqusPost> disqusPosts = ps;
                                            final Set<String> threadIds = ts;
                                          }
                                  )
                          );
    assert result.disqusPosts.size() == postNodeList.getLength() :
            "Difference between number of posts in query " + postNodeList.getLength() + " and mapped posts " + result.disqusPosts.size();
    if (result.disqusPosts.isEmpty()) {
      System.err.println("No non-deleted, non-spam comments found");
      System.exit(1);
    }

    // Keep only threads with comments
    disqusThreads.keySet().retainAll(result.threadIds);
    if (disqusThreads.isEmpty()) {
      System.err.println("No threads with comments found");
      System.exit(1);
    }


  }

  private static class GitHub {
    // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String githubToken;

    public GitHub(String githubToken) {
      this.githubToken = Objects.requireNonNull(githubToken);
    }

    public String getRepoId(String repo) throws IOException, InterruptedException {
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

    public String getDiscussionCategoryId(String repo, String targetCategoryName) throws IOException, InterruptedException {
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

    public String createDiscussion(String repoId, String discussionCategoryId, String title, String body) throws IOException, InterruptedException {
      Objects.requireNonNull(repoId);
      Objects.requireNonNull(discussionCategoryId);
      Objects.requireNonNull(title);
      Objects.requireNonNull(body);
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#creatediscussion
      var payload = graphQl(
              """
              mutation($repoId: ID!, $discussionCategoryId: ID!, $title: String!, $body: String!) {
                createDiscussion(input: {repositoryId: $repoId, discussionCategoryId: $discussionCategoryId, title: $title, body: $body}) {
                  discussion {
                    id
                  }
                }
              }
              """,
              Map.of("repoId", repoId, "discussionCategoryId", discussionCategoryId, "title", title, "body", body)
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

    public String addDiscussionComment(String discussionId, String body, String replyToId) throws IOException, InterruptedException {
      Objects.requireNonNull(discussionId);
      Objects.requireNonNull(body);
      Objects.requireNonNull(replyToId);
      // https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions#adddiscussioncomment
      var payload = graphQl(
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

    private String graphQl(String graphQlQuery, Map<String, String> variables) throws IOException, InterruptedException {
      Objects.requireNonNull(graphQlQuery);
      Objects.requireNonNull(variables);
      var json = """
                 {
                   "query": "%s",
                   "variables": %s
                 }
                 """.formatted(
              graphQlQuery
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n"),
              variables.entrySet().stream().map(e -> "\"%s\":\"%s\"".formatted(e.getKey(), e.getValue())).collect(Collectors.joining(",", "{", "}"))
      );

      if (githubToken.isBlank()) {
        System.err.println("GITHUB_TOKEN environment variable is not set, or --token command line option is not set");
        System.exit(1);
      }

      var response = httpClient.send(
              HttpRequest.newBuilder()
                         .uri(URI.create("https://api.github.com/graphql"))
                         .header("Authorization", "token " + githubToken)
                         .header("Accept", "application/json")
                         .header("Content-Type", "application/json")
                         .POST(BodyPublishers.ofString(json))
                         .build(),
              BodyHandlers.ofString()
      );

      if (response.statusCode() >= 400) {
        System.out.println(response.headers());
        response.headers().firstValue("X-RateLimit-Remaining").filter(r -> Objects.equals(r, "0")).ifPresent(r -> System.out.println("Rate limit reached"));
        System.err.printf("Error: %d %s%n", response.statusCode(), response.body());
        System.exit(1);
      }

      return response.body();
    }
  }

  private Document getXmlDocument() throws ParserConfigurationException, SAXException, IOException {
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);
    var documentBuilder = dbf.newDocumentBuilder();
    return documentBuilder.parse(file.toFile());
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
}
