package sandbox;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Sparkline {
  private static final String BARS = "▁▂▃▄▅▆▇█";

  public static void main(String[] args) {
    double[] doubles = new double[0];

    if (args.length == 1) {
      switch (args[0]) {
        case "-":
          try (var scanner = new Scanner(new BufferedInputStream(System.in), StandardCharsets.UTF_8)) {
            final var builder = DoubleStream.builder();
            while (scanner.hasNextDouble()) {
              builder.accept(scanner.nextDouble());
            }
            doubles = builder.build().toArray();
          }
          break;
        case "-h":
        case "--help":
          help();
          System.exit(0);
          break;
        case "--example-usage":
          example_usage();
          System.exit(0);
          break;
      }
    } else {
      doubles = Arrays.stream(args).mapToDouble(Double::parseDouble).toArray();
    }


    if (doubles.length != 0) {
      System.out.println(Sparkline.of(doubles));
    } else {
      help();
    }
  }

  private static void example_usage() {
    System.out.println(
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
    System.out.println("java Sparkline " + Arrays.stream(intArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    System.out.println(Sparkline.of(Arrays.stream(intArray).asDoubleStream().toArray()));

    double[] doubleArray = {1.5f, 0.5f, 3.5f, 2.5f, 5.5f, 4.5f, 7.5f, 6.5f};
    System.out.println("java Sparkline " + Arrays.stream(doubleArray).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
    System.out.println(Sparkline.of(doubleArray));

    System.out.println("shuf -i 0-20 | java Sparkline");
    System.out.println(Sparkline.of(new Random().ints(20).asDoubleStream().toArray()));

    System.out.println();
    System.out.println("More example with --example-usage");
  }

  public static String of(double[] doubles) {
    final var minMax = Arrays.stream(doubles).collect(
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

    double range = minMax.max - minMax.min;
    int num = BARS.length() - 1;
    final var line = Arrays.stream(doubles)
        .mapToInt(v -> BARS.charAt((int) Math.ceil(((v - minMax.min) / range * num))))
        .collect(
            StringBuilder::new,
            (stringBuilder, i) -> stringBuilder.append((char) i),
            StringBuilder::append
        );

    return line.toString();
  }

}
