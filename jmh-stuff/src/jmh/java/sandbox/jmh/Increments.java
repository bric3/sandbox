/*
 * Author : Richard Startin
 * Source https://github.com/apache/pinot/pull/7442#issuecomment-934674765
 *
 * To be run as
 *   taskset -c 0 java -jar target/benchmarks.jar -wi 5 -i 5 -w 1 -r 1 -f 1 -bm avgt -tu ns Increments
 */
package sandbox.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

@State(Scope.Benchmark)
public class Increments {

  @Param("1024")
  int size;

  private int[] input;
  private int[] output;

  @Setup(Level.Trial)
  public void setup() {
    input = ThreadLocalRandom.current().ints(size).toArray();
    output = new int[size];
  }

  @Benchmark
  public void autovecPre(Blackhole bh) {
    for (int i = 0; i < input.length; ++i) {
      output[i] += input[i];
    }
    bh.consume(output);
  }

  @Benchmark
  public void autovecPost(Blackhole bh) {
    for (int i = 0; i < input.length; i++) {
      output[i] += input[i];
    }
    bh.consume(output);
  }

  @Benchmark
  public int reducePre(Blackhole bh) {
    int sum = 0;
    for (int i = 0; i < input.length; ++i) {
      sum += Integer.bitCount(input[i]);
    }
    return sum;
  }

  @Benchmark
  public int reducePost(Blackhole bh) {
    int sum = 0;
    for (int i = 0; i < input.length; i++) {
      sum += Integer.bitCount(input[i]);
    }
    return sum;
  }

  @Benchmark
  public void blackholedPre(Blackhole bh) {
    for (int i = 0; i < input.length; ++i) {
      bh.consume(i);
    }
  }

  @Benchmark
  public void blackholedPost(Blackhole bh) {
    for (int i = 0; i < input.length; i++) {
      bh.consume(i);
    }
  }

}