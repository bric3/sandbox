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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * Inspired by https://github.com/holman/spark/blob/ab88ac6f8f33698f39ece2f109b1117ef39a68eb/spark-test.sh
 */
class SparklineTest {

  private ByteArrayOutputStream stdout;
  private ByteArrayOutputStream stderr;


  @BeforeEach
  void set_stdout() {
    stdout = new ByteArrayOutputStream();
    Sparkline.stdout = new PrintStream(stdout, true, UTF_8);
    stderr = new ByteArrayOutputStream();
    Sparkline.stderr = new PrintStream(stderr, true, UTF_8);
  }

  @AfterEach
  void restore_stdout() {
    Sparkline.stdout.close();
    Sparkline.stderr.close();
    Sparkline.stdin = System.in;
    Sparkline.stdout = System.out;
    Sparkline.stderr = System.err;
  }

  @Test
  void it_shows_help_with_no_argv() throws IOException {
    Sparkline.main();
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_help_with_short_help_option() throws IOException {
    Sparkline.main("-h");
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_help_with_long_help_option() throws IOException {
    Sparkline.main("--help");
    assertThat(stdout.toString(UTF_8))
        .contains("java Sparkline 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1")
        .contains("java Sparkline 1.5 0.5 3.5 2.5 5.5 4.5 7.5 6.5")
        .contains("java Sparkline 0,30,55,80 33,150 20,100,2.1 33.4")
        .contains("shuf -i 0-20 | java Sparkline -");
  }

  @Test
  void it_shows_examples_with_long_example_usage_option() throws IOException {
    Sparkline.main("--example-usage");
    assertThat(stdout.toString(UTF_8))
        .contains("# Magnitude of earthquakes worldwide 2.5 and above in the last 24 hours")
        .contains("# commits for the las 60 days");
  }

  @Test
  void report_missing_value_for_file_option() throws IOException {
    Sparkline.main("-f");
    assertThat(stderr.toString(UTF_8))
        .contains("The -f option requires an argument.");
  }

  @Test
  void report_too_many_file_option() throws IOException {
    Sparkline.main("-f", "-", "-f", "file");
    assertThat(stderr.toString(UTF_8))
        .contains("The -f option can only be set once.");
  }

  @Test
  void report_unreadable_file_option() throws IOException {
    Sparkline.main("-f", "not_there");
    assertThat(stderr.toString(UTF_8))
        .contains("not_there is not readable");
  }

  @Test
  void report_missing_sample_value_option() throws IOException {
    Sparkline.main("-d");
    assertThat(stderr.toString(UTF_8))
        .contains("Downsampling requires a positive integer.");
  }

  @Test
  void it_charts_pipe_data_delimited_by_space() throws IOException {
    Sparkline.stdin = new ByteArrayInputStream("0 30 55 80 33 150\n".getBytes(UTF_8));
    Sparkline.main("-f", "-");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_pipe_data_delimited_by_comma() throws IOException {
    Sparkline.stdin = new ByteArrayInputStream("0,30,55,80,33,150".getBytes(UTF_8));
    Sparkline.main("-f", "-");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_space_delimited_data() throws IOException {
    Sparkline.main("0 30 55 80 33 150");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_comma_delimited_data() throws IOException {
    Sparkline.main("0,30,55,80,33,150");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█");
  }

  @Test
  void it_charts_args_mixed_space_and_comma_delimited_data() throws IOException {
    Sparkline.main("0,30,55,80", "33,150", "20,100,2.1 33.4");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁▂▄▅▃█▂▆▁▃");
  }

  @Test
  void it_handles_decimals() throws IOException {
    Sparkline.main("5.5", "20");

    assertThat(stdout.toString(UTF_8)).isEqualToIgnoringNewLines("▁█");
  }

  @Test
  void it_can_downsample() throws IOException {
    Sparkline.main(Stream.concat(
        Stream.of("-d", "100"),
        new SecureRandom().doubles(1000).mapToObj(String::valueOf)
    ).toArray(String[]::new));

    assertThat(stdout.toString(UTF_8).trim().codePoints().count()).isEqualTo(100);
  }

  @Test
  void it_does_not_downsample_if_not_in_a_terminal_by_default() throws IOException {
    Sparkline.main(new SecureRandom().doubles(1000).mapToObj(String::valueOf).toArray(String[]::new));

    assertThat(stdout.toString(UTF_8).trim().codePoints().count()).isEqualTo(1000);
  }

  @Test
  void it_does_not_downsample_if_disabled() throws IOException {
    Sparkline.main(Stream.concat(
        Stream.of("--no-downsample"),
        new SecureRandom().doubles(1000).mapToObj(String::valueOf)
    ).toArray(String[]::new));

    assertThat(stdout.toString(UTF_8).trim().codePoints().count()).isEqualTo(1000);
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