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

package sandbox;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Sparkline {
  public static final Pattern DELIMITER = Pattern.compile(",|\\p{javaWhitespace}+");
  static PrintStream stdout = System.out;
  static InputStream stdin = System.in;

  private static final char[] TICKS = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

  public static void main(String... args) {
    double[] doubles = null;

    if (args.length == 1) {
      switch (args[0]) {
        case "-":
          try (var scanner = new Scanner(new BufferedInputStream(stdin), UTF_8)
              .useDelimiter(DELIMITER)) {
            final var builder = DoubleStream.builder();
            while (scanner.hasNextDouble()) {
              final var t = scanner.nextDouble();
              builder.accept(t);
            }
            doubles = builder.build().toArray();
          }
          break;
        case "-h":
        case "--help":
          help();
          return;
        case "--example-usage":
          example_usage();
          return;
      }
    }
    if (doubles == null) {
      doubles = Arrays.stream(args)
          .flatMap(arg -> Arrays.stream(DELIMITER.split(arg)))
          .mapToDouble(Double::parseDouble)
          .toArray();
    }

    if (doubles.length != 0) {
      stdout.println(Sparkline.of(doubles));
    } else {
      help();
    }
  }

  private static void example_usage() {
    stdout.println(
        """
            # Magnitude of earthquakes worldwide 2.5 and above in the last 24 hours
            curl -s https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.csv |
              sed '1d' |
              cut -d, -f5 |
              java Sparkline.java -
                   
            # Number of commits in a repo, by author
            git shortlog -s | cut -f1 | java Sparkline.java -
                    
            # commits for the las 60 days
            for day in $(seq 60 -1 0); do
                git log --before="${day} days" --after="$[${day}+1] days" --format=oneline |
                wc -l
            done | java Sparkline.java -
                    
            More example here could be found on https://github.com/holman/spark/wiki/Wicked-Cool-Usage
            """
    );
  }

  private static void help() {
    int[] intArray = {1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 1};
    stdout.println("java Sparkline " + Arrays.stream(intArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    stdout.println(Sparkline.of(Arrays.stream(intArray).asDoubleStream().toArray()));

    double[] doubleArray = {1.5f, 0.5f, 3.5f, 2.5f, 5.5f, 4.5f, 7.5f, 6.5f};
    stdout.println("java Sparkline " + Arrays.stream(doubleArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    stdout.println(Sparkline.of(doubleArray));

    stdout.println("shuf -i 0-20 | java Sparkline -");
    stdout.println(Sparkline.of(new Random().ints(20).asDoubleStream().toArray()));

    stdout.println(
        """
        Comma or spaces can be used to separate the data, the dot indicates
        a double number.
        
        java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4
        ▁▂▄▅▃█▂▆▁▃
        
        Note this program use a slightly differently scaling algorithm than
        https://github.com/holman/spark which may produce slightly different rendering.
        
        More example with --example-usage
        """
    );
  }

  public static String of(int[] ints) {
    return Sparkline.of(Arrays.stream(ints).asDoubleStream().toArray());
  }

  public static String of(long[] longs) {
    return Sparkline.of(Arrays.stream(longs).asDoubleStream().toArray());
  }

  public static String of(double[] doubles) {
    final var bounds = Arrays.stream(doubles).collect(
        () -> new Object() {
          double min = Long.MAX_VALUE;
          double max = Long.MIN_VALUE;
        },
        (acc, v) -> {
          if (v < acc.min) acc.min = v;
          if (v > acc.max) acc.max = v;
        },
        (acc, acc2) -> {
          if (acc2.min < acc.min) acc.min = acc2.min;
          if (acc2.max > acc.max) acc.max = acc2.max;
        }
    );

    double range = bounds.max - bounds.min;
    double scale = range / (TICKS.length - 1);
    double mid = TICKS.length / 2d;
    final var line = Arrays.stream(doubles)
        .mapToInt(v -> Double.isNaN(v) ? ' ' : TICKS[(int) (range == 0 ? mid : Math.round((v - bounds.min) / scale))])
        .collect(
            StringBuilder::new,
            (stringBuilder, i) -> stringBuilder.append((char) i),
            StringBuilder::append
        );
    return line.toString();
  }
}