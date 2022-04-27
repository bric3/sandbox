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

import java.util.Objects;

import static java.lang.Math.abs;
import static java.lang.Math.floor;

/**
 * Implementation of the <strong>Largest Triangle Three Bucket</strong> downsampling algorithm.
 *
 * <p>The basic idea of this algorithm is to select points based
 * on the largest area of a triangle whose points are in 3 three
 * adjacent buckets.</p>
 *
 * <p>This implementation is merely a port of the original work done by <em>Sveinn Steinarsson</em>.</p>
 *
 * <p>While the original algorithm works with 2 dimensional point vectors. This class also features a
 * slight variation of the algorithm that works on a single dimension vector.</p>
 * <br>
 *
 * <p>Here's a few select paragraphs from the original <a href="href="http://skemman.is/stream/get/1946/15343/37285/3/SS_MSthesis.pdf">paper</a>.</p>
 *
 * <p>
 * 4.2. Largest-Triangle-Three-Buckets
 *
 * <p>...</p>
 *
 * <p>The first step is to divide all the data points into buckets of approximately equal
 * size. The first and last buckets however contain only the first and last data points
 * of the original data as shown in figure 2.3 on page 8. This is to ensure that those
 * points will be included in the downsampled data.</p>
 *
 * <p>The next step is to go through all the buckets from the first to the last and select
 * one point from each bucket. The first bucket only contains a single point, so it is
 * selected by default. The next bucket would then normally contain more than one
 * point from which to choose. Here the algorithm differs from the LTOB since the
 * rank of a point is calculated from the effective areas that the point can form with
 * other points in the adjacent buckets.</p>
 *
 * <p>The algorithm works with three buckets at a time and proceeds from left to right. The
 * first point which forms the left corner of the triangle (the effective area) is always
 * fixed as the point that was previously selected and one of the points in the middle
 * bucket shall be selected now. The question is what point should the algorithm use
 * in the last bucket to form the triangle.</p>
 *
 * <p>...</p>
 *
 * <p>Algorithm 4.2 Largest-Triangle-Three-Buckets
 * <pre>
 *   <strong>Require</strong>: <em>data</em>.                                        // The original data
 *   <strong>Require</strong>: <em>threshold</em>.                // Number of data points to be returned
 *
 *     1: Split the <em>data</em> into equal number of <em>buckets</em> as the <em>threshold</em>
 *        but have the first <em>bucket</em> only containing the first data point,
 *        and the last <em>bucket</em> containing only the last data point.
 *
 *     2: Select the point in the first <em>bucket</em>
 *     3: <strong>for</strong> each <em>bucket</em> except the first and last <strong>do</strong>
 *     4:   Rank every point in the <em>bucket</em> by calculating the area of
 *          a triangle it forms with the selected point in the last
 *          <em>bucket</em>, and the average point in the next <em>bucket</em>.
 *     5:   Select the point with the highest rank within the <em>bucket</em>.
 *     6: <strong>end for</strong>
 *     7: Select the point in the last bucket.             // There is only one
 * </pre>
 * </p>
 *
 * </p>
 *
 * @author Brice Dutheil
 * @see <a href="https://github.com/drcrane/downsample">https://github.com/drcrane/downsample</a>
 * @see <a href="https://github.com/52North/sensorweb-server-helgoland/blob/754914608d6e5366b43194b72f3574b6154ed12c/io/src/main/java/org/n52/io/type/quantity/generalize/LargestTriangleThreeBucketsGeneralizer.java">52north.org implmentation</a>
 * @see <a href="http://skemman.is/stream/get/1946/15343/37285/3/SS_MSthesis.pdf">Downsampling Time Series for Visual Representation paper</a>
 */
public class LargestTriangleThreeBuckets {
  public static final int START_END_BUCKETS = 2;


  /**
   * Downsample a two-dimensional vector.
   * <p>
   * Uses the original <strong>Largest Triangle Three Bucket</strong> algorithm
   * to perform the downsampling algorithm.</p>
   *
   * @param data      A two-dimensional vector, where the second array is the point coordinate,
   *                  with index 0 as the <em>abscissa</em> (<em>x</em>),
   *                  index 1 as the <em>ordinate</em> (<em>y</em>). E.g.
   *                  {@code new double[][] { { 1, 2 }, { 0, 5 }, {10, 4 }, } }
   * @param threshold The number of data points to be returned. It needs to be greater than 2.
   * @return The two-dimensional vector downsampled to <em>threshold</em> points.
   */
  public static double[][] largestTriangleThreeBuckets(double[][] data, int threshold) {
    Objects.requireNonNull(data);
    int data_length = data.length;
    if (data_length < START_END_BUCKETS || threshold >= data_length) {
      return data;
    }
    if (threshold <= START_END_BUCKETS) {
      throw new IllegalArgumentException("threshold should be higher than 2");
    }


    double[][] sampled = new double[threshold][];
    int sampleIndex = 0;

    // bucket size.
    double every = (data_length - START_END_BUCKETS) / (double) (threshold - START_END_BUCKETS);

    int a = 0;  // Initially a is the first point in the triangle
    double[] max_area_point = null;
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
        avg_x += data[avg_range_start][0];
        avg_y += data[avg_range_start][1];
      }
      avg_x /= avg_range_length;
      avg_y /= avg_range_length;

      // Get the range for this bucket
      int range_offs = (int) (floor((i) * every) + 1),
          range_to = (int) (floor((i + 1) * every) + 1);

      // Point a
      double point_a_x = data[a][0], point_a_y = data[a][1];

      max_area = -1;

      for (; range_offs < range_to; range_offs++) {
        // Calculate triangle area over three buckets
        area = abs(
            (point_a_x - avg_x) * (data[range_offs][1] - point_a_y) - (point_a_x - data[range_offs][0]) * (avg_y - point_a_y)
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