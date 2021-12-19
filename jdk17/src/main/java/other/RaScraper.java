/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package other;


import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class RaScraper {
  public static void main(String[] args) throws IOException, InterruptedException {
    if (new ProcessBuilder("which", "ebook-convert").inheritIO().start().waitFor() != 0) {
      System.err.println("ebook-convert not found on PATH");
      System.exit(1);
    }

    var raUrl = "https://qntm.org/ra";
    var connection = Jsoup.newSession().userAgent("Java").timeout(20 * 1000);
    var source = connection.newRequest().url(raUrl).get();

//    var dateHeader = source.connection().response().header("Date");
//    System.out.println(LocalDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME));

    var output = Jsoup.parse("<html></html>");
    output.body().appendElement("h1").attr("id", "ra").text("Ra");

    var blockQuote = source.selectXpath("//*/div[@class='page__content']//blockquote");
    output.body().appendElement("div").appendChildren(blockQuote);

    // select list chapter links immediately after h3#sec1
    var freeChapters = source.selectXpath("//*/div[@class='page__content']/h3[@id='sec1']/following-sibling::*[1]/following-sibling::ul[1]/li/a");

    // parallelize chapter fetching
    var getChapters = freeChapters.stream().map(link -> {
      var absLink = link.absUrl("href");
      return CompletableFuture.supplyAsync(() -> {
        try {
          return connection.newRequest().url(absLink).get();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }).toList();

    // Append chapter content
    CompletableFuture.allOf(getChapters.toArray(CompletableFuture[]::new)).thenAccept(unused -> {
      getChapters.stream().map(CompletableFuture::join).forEach(chapterSource -> {
        var chapterTitle = chapterSource.selectXpath("//*/h2[@class='page__h2']").first();
        var content = chapterSource.selectXpath("//*/div[@class='page__content']/p | //*/div[@class='page__content']/h4[text()='*']");

        var chapterDiv = output.body()
                               .appendElement("div").attr("class", "chapter-wrapper");
        chapterDiv.appendElement("h2")
                  .attr("id", URI.create(chapterSource.location()).getPath().replaceAll("/", ""))
                  .attr("class", "chapter") // allow auto-detection of chapters
                  .append(chapterTitle.html());
        chapterDiv.appendElement("div").appendChildren(content.stream().map(element -> {
          if (element.tagName().equals("h4")) {
            return element.html("<center>" + element.html() + "</center>");
          }
          return element;
        }).toList());
      });
    }).join();

    try (var appendable = Files.newBufferedWriter(
        Path.of("/tmp/ra.html"),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
    )) {
      output.html(appendable);
    }

    // Manual: https://manual.calibre-ebook.com/generated/en/ebook-convert.html#structure-detection
    var processBuilder = new ProcessBuilder(
        "ebook-convert", // Calibre
        "/tmp/ra.html",
        "ra.mobi",
        "--title=Ra",
        "--authors=qntm",
        "--language=en",
        "--input-encoding=utf8",
        "--isbn=979-8735007937",
        "--max-levels=0", // disable link recursion
        "--tags=new-ending",

//    "--no-default-epub-cover",
        "--cover", "https://public-files.gumroad.com/variants/w2vv6xweoqkagmp8h3dbza7leljr/3298c3eb001bbed90f1d616da66708480096a0a1b6e81bd4f8a2d6e9b831d301",

//        "--chapter=//*[(name()='h1' or name()='h2')]}, q{--level1-toc=//*[name()='h1']}, q{--level2-toc=//*[name()='h2']}, q{--level3-toc=//*[name()='h3']"
        "--chapter-mark=pagebreak",
        "--level1-toc",
        "--level2-toc"
    ).inheritIO().directory(new File(System.getProperty("user.home")));
    var start = processBuilder.start();
    start.waitFor();
    System.exit(start.exitValue());
  }
}