///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.jsoup:jsoup:1.14.3
/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Note that Ra is the work of Sam Hughes, and is not licensed under the MIT license.
 *
 * You should definitely try to buy a copy of Ra to support the author, I did.
 * Here's a few links:
 * https://www.amazon.com/Ra-Sam-Hughes/dp/B08YQCQNYM
 * https://gumroad.com/l/Unpv
 *
 * More paid editions are listed here : https://qntm.org/ra
 */

package other;


import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class RaAssembler {
  public static void main(String[] args) throws IOException, InterruptedException {
    var whichEbookConvert = new ProcessBuilder("which", "ebook-convert").start();
    if (whichEbookConvert.waitFor() != 0) {
      System.err.println("ebook-convert not found on PATH");
      System.exit(1);
    }
    System.out.printf("Found '%s'%n", new String(whichEbookConvert.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());

    var raUrl = "https://qntm.org/ra";
    var connection = Jsoup.newSession().userAgent("Java").timeout(20 * 1000);
    var source = connection.newRequest().url(raUrl).get();
    System.out.printf("Fetching Ra at '%s'%n", raUrl);

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
          System.out.printf("Fetching chapter '%s'%n", link.text());
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


    var tempFile = Files.createTempFile(null, "ra.html");
    tempFile.toFile().deleteOnExit();
    try (var appendable = Files.newBufferedWriter(
        tempFile,
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