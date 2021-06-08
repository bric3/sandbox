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

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Library-less Sparkline util
 */
public class Sparkline {
  public static final Pattern DELIMITER = Pattern.compile(",|\\p{javaWhitespace}+");
  static PrintStream stdout = System.out;
  static PrintStream stderr = System.err;
  static InputStream stdin = System.in;

  private static final char[] TICKS = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

  public static void main(String... args) throws IOException {
    double[] doubles = null;

    var argsDeque = new ArrayDeque<>(Arrays.asList(args));
    var parsed = false;
    InputStream stdin = null;
    int samples = terminalColumns();

    while (!parsed && !argsDeque.isEmpty()) {

      String arg = argsDeque.pollFirst();
      switch (arg) {
        case "-f":
          if (stdin != null) {
            stderr.printf("The -f option can only be set once.%n%n");
            help();
            return;
          }
          String fileOrStdin = argsDeque.pollFirst();
          if (fileOrStdin == null) {
            stderr.printf("The -f option requires an argument.%n%n");
            help();
            return;
          }
          if (Objects.equals("-", fileOrStdin)) {
            stdin = Sparkline.stdin;
          } else {
            Path file = Path.of(fileOrStdin);
            if (!Files.isReadable(file)) {
              stderr.printf(file + " is not readable.%n%n");
              help();
              return;
            }
            stdin = Files.newInputStream(file);
          }
          break;

        case "--no-downsample":
          samples = -1;
          break;
        case "-d":
        case "--downsample-to":
          String size = argsDeque.pollFirst();
          if (size == null) {
            stderr.printf("Downsampling requires a positive integer.%n%n");
            help();
            return;
          }
          samples = Integer.parseInt(size);
          break;
        case "-h":
        case "--help":
          help();
          return;
        case "--example-usage":
          example_usage();
          return;
        default:
          argsDeque.addFirst(arg);
          parsed = true;
      }
    }

    if (stdin != null) {
      try (var scanner = new Scanner(new BufferedInputStream(stdin), UTF_8)
          .useDelimiter(DELIMITER)) {
        final var builder = DoubleStream.builder();
        while (scanner.hasNextDouble()) {
          final var t = scanner.nextDouble();
          builder.accept(t);
        }
        doubles = builder.build().toArray();
      }
    }

    if (doubles == null) {
      doubles = argsDeque.stream()
          .flatMap(arg -> Arrays.stream(DELIMITER.split(arg)))
          .mapToDouble(Double::parseDouble)
          .toArray();
    }

    if (doubles.length != 0) {
      if (samples != -1) {
        doubles = largestTriangleThreeBuckets1D(doubles, samples);
      }

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
    var sparkline = ProcessHandle.current().info().arguments().map(a -> a[0]).orElse("Sparkline");

    int[] intArray = {1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 1};
    stdout.println("java " + sparkline + " " + Arrays.stream(intArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    stdout.println(Sparkline.of(Arrays.stream(intArray).asDoubleStream().toArray()));

    double[] doubleArray = {1.5f, 0.5f, 3.5f, 2.5f, 5.5f, 4.5f, 7.5f, 6.5f};
    stdout.println("java " + sparkline + " " + Arrays.stream(doubleArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    stdout.println(Sparkline.of(doubleArray));

    stdout.println("shuf -i 0-20 | java " + sparkline + " -f -");
    stdout.println(Sparkline.of(new Random().ints(20).asDoubleStream().toArray()));

    stdout.println();
    stdout.println();
    stdout.println();

    stdout.println(
        """
        Options
            
            -f,--file FILE
            This option indicates the number will be taken from a file. To use stdin, use '-' as a value.
            
            --no-downsample
            Toggle off downsampling of the data.
            
            -d,--downsample-to SAMPLES
            Toggle on downsampling of the data with the number of SAMPLES.
            
            -h,--help
            This message.
            
            --example-usage
            Additional examples of use
        """
    );

    stdout.println();
    stdout.println();
    stdout.println();

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

  private static int terminalColumns() throws IOException {
    var ttyConfig = getTtyConfig();

    return Arrays.stream(new String[]{
            "\\b([0-9]+)\\s+" + "columns" + "\\b",
            "\\b" + "columns" + "\\s+([0-9]+)\\b",
            "\\b" + "columns" + "\\s*=\\s*([0-9]+)\\b"
        })
        .map(pattern -> Pattern.compile(pattern).matcher(ttyConfig))
        .filter(Matcher::find)
        .findFirst()
        .map(matcher -> Integer.parseInt(matcher.group(1)))
        .orElse(-1);
  }

  private static CharSequence getTtyConfig() throws IOException {
    // straw man check to identify if this is running in a terminal
    // System.console() requires that both stdin and stdout are connected to a terminal
    // which is not always the case (eg with pipes).
    // However, it happens that trying to read from /dev/tty works
    // when the application is connected to a terminal, and fails when not
    // with the message '(Device not configured)'.
    //
    // Unfortunately Files::notExists or Files::isReadable don't fail.
    //noinspection EmptyTryBlock
    try (var ignored = Files.newInputStream(Path.of("/dev/tty"))) {
    } catch (FileSystemException fileSystemException) {
      return "";
    }

    Process p = new ProcessBuilder("stty", "-a")
        .redirectInput(ProcessBuilder.Redirect.from(new File("/dev/tty")))
        .start();

    var output = new StringBuilder();
    try (var stdout = p.getInputStream();
         var stderr = p.getErrorStream();
         var outReader = new BufferedReader(new InputStreamReader(stdout));
         var errReader = new BufferedReader(new InputStreamReader(stderr));
    ) {
      String line;
      while ((line = outReader.readLine()) != null) {
        output.append(line).append("\n");
      }
      while ((line = errReader.readLine()) != null) {
        output.append(line).append("\n");
      }
      p.waitFor();
    } catch (InterruptedException e) {
      throw new InterruptedIOException();
    }
    return output.toString();
  }


  /**
   * Downsample a single dimension vector.
   *
   * <p>
   * Uses a variation of the original <strong>Largest Triangle Three Bucket</strong>
   * algorithm to perform the downsampling algorithm, instead it uses the position
   * in the array as the abscissa.</p>
   *
   * <p>This variation naturally implies that all points are adjacent since a
   * single dimension vector doesn't allow passing arbitrary abscissa.</p>
   *
   * @param data      A single dimensional vector, the point's <em>abscissa</em> (<em>x</em>)
   *                  is the position in the vector, and the value is the <em>ordinate</em> (<em>y</em>).
   *                  E.g. {@code new double[] { 1, -40, 1, 4, 6, 120, 10, 4, -3 } }
   * @param threshold The number of data points to be returned. It needs to be greater than 2.
   * @return The two-dimensional vector downsampled to <em>threshold</em> points.
   */
  public static double[] largestTriangleThreeBuckets1D(double[] data, int threshold) {
    final int START_END_BUCKETS = 2;

    Objects.requireNonNull(data);
    int data_length = data.length;
    if (data_length < 2 || threshold >= data_length) {
      return data;
    }
    if (threshold <= START_END_BUCKETS) {
      throw new IllegalArgumentException("threshold should be higher than 2");
    }


    double[] sampled = new double[threshold];
    int sampleIndex = 0;

    // bucket size.
    double every = (data_length - START_END_BUCKETS) / (double) (threshold - START_END_BUCKETS);

    int a = 0;  // Initially a is the first point in the triangle
    double max_area_point = 0;
    double max_area, area;
    int next_a = 0;

    sampled[sampleIndex++] = data[a]; // Always add the first point

    for (int i = 0; i < threshold - START_END_BUCKETS; i++) {
      // Calculate point average for next bucket (containing c)
      double avg_x = 0, avg_y = 0;
      int avg_range_start = (int) (floor((i + 1) * every) + 1),
          avg_range_end = Math.min((int) (floor((i + 2) * every) + 1), data_length);

      double avg_range_length = avg_range_end - avg_range_start;

      for (; avg_range_start < avg_range_end; avg_range_start++) {
        avg_x += avg_range_start;
        avg_y += data[avg_range_start];
      }
      avg_x /= avg_range_length;
      avg_y /= avg_range_length;

      // Get the range for this bucket
      int range_offs = (int) (floor((i) * every) + 1),
          range_to = (int) (floor((i + 1) * every) + 1);

      // Point a
      double point_a_x = a, point_a_y = data[a];

      max_area = -1;

      for (; range_offs < range_to; range_offs++) {
        // Calculate triangle area over three buckets
        area = abs(
            (point_a_x - avg_x) * (data[range_offs] - point_a_y) - (point_a_x - range_offs) * (avg_y - point_a_y)
        ) * 0.5;
        if (area > max_area) {
          max_area = area;
          max_area_point = data[range_offs];
          next_a = range_offs; // Next a is this b
        }
      }

      sampled[sampleIndex++] = max_area_point; // Pick max area point from the bucket
      a = next_a;

    }
    sampled[sampleIndex] = data[data_length - 1]; // Always add last point

    return sampled;
  }

}