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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * Inspired by https://github.com/holman/spark/blob/ab88ac6f8f33698f39ece2f109b1117ef39a68eb/spark-test.sh
 */
class SparklineTest {

  private ByteArrayOutputStream stdout;


  @BeforeEach
  void set_stdout() {
    stdout = new ByteArrayOutputStream();
    Sparkline.stdout = new PrintStream(stdout, true, UTF_8);
  }

  @AfterEach
  void restore_stdout() {
    Sparkline.stdout.close();
    Sparkline.stdin = System.in;
    Sparkline.stdout = System.out;
  }

  @Test
  void it_shows_help_with_no_argv() {
    Sparkline.main();
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_help_with_short_help_option() {
    Sparkline.main("-h");
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_help_with_long_help_option() {
    Sparkline.main("--help");
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_examples_with_long_example_usage_option() {
    Sparkline.main("--example-usage");
    assertThat(stdout.toString(UTF_8))
        .contains("# Magnitude of earthquakes worldwide 2.5 and above in the last 24 hours")
        .contains("# commits for the las 60 days");
  }

  @Test
  void it_charts_pipe_data_delimited_by_space() {
    Sparkline.stdin = new ByteArrayInputStream("0 30 55 80 33 150\n".getBytes(UTF_8));
    Sparkline.main("-");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_pipe_data_delimited_by_comma() {
    Sparkline.stdin = new ByteArrayInputStream("0,30,55,80,33,150".getBytes(UTF_8));
    Sparkline.main("-");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_space_delimited_data() {
    Sparkline.main("0 30 55 80 33 150");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_comma_delimited_data() {
    Sparkline.main("0,30,55,80,33,150");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_mixed_space_and_comma_delimited_data() {
    Sparkline.main("0,30,55,80", "33,150", "20,100,2.1 33.4");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█▂▆▁▃");
  }

  @Test
  void it_handles_decimals() {
    Sparkline.main("5.5", "20");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁█");
  }

  @Test
  void it_can_accept_int_arrays() {
    assertThat(Sparkline.of(new int[]{1, 5, 22, 13, 5})).isEqualTo("▁▂█▅▂");
  }

  @Test
  void it_can_accept_long_arrays() {
    assertThat(Sparkline.of(new long[]{1, 5, 22, 13, 5})).isEqualTo("▁▂█▅▂");
  }

  @Test
  void it_charts_100_lt_300() {
    assertThat(Sparkline.of(new int[]{1, 2, 3, 4, 100, 5, 10, 20, 50, 300})).isEqualTo("▁▁▁▁▃▁▁▁▂█");
  }

  @Test
  void it_charts_50_lt_100() {
    assertThat(Sparkline.of(new int[]{1, 50, 100})).isEqualTo("▁▄█");
  }

  @Test
  void it_charts_4_lt_8() {
    assertThat(Sparkline.of(new int[]{2, 4, 8})).isEqualTo("▁▃█");
  }

  @Test
  void it_charts_no_tier_0() {
    assertThat(Sparkline.of(new int[]{1, 2, 3, 4, 5, 6})).isEqualTo("▁▂▄▅▇█");
  }

  @Test
  void it_equalizes_at_midtier_on_same_data() {
    assertThat(Sparkline.of(new int[]{1, 1, 1, 1})).isEqualTo("▅▅▅▅");
  }

  @Test
  void it_skips_NaN() {
    assertThat(Sparkline.of(new double[]{1d, Double.NaN, 1d, Double.NaN, 1d, 1d, 1d})).isEqualTo("▅ ▅ ▅▅▅");
  }
}