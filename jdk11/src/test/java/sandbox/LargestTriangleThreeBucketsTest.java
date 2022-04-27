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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static sandbox.LargestTriangleThreeBuckets.largestTriangleThreeBuckets;
import static sandbox.LargestTriangleThreeBuckets.largestTriangleThreeBuckets1D;

class LargestTriangleThreeBucketsTest {

  public static double[][] sineWaveWithOutliers() {
    double[][] data = new double[8000][];
    for (int i = 0; i < data.length; i++) {
      double[] sample = new double[2];
      sample[0] = ((long) i + 1000000L);
      sample[1] = (Math.sin((double) i / 300)) * 300;
      data[i] = sample;
    }
    data[430][1] = 50.0D;
    data[1500][1] = 0.0D;
    data[2000][1] = 50.0D;
    return data;
  }


  @Test
  public void raise_iae_on_invalid_threshold() {
    double[][] input = {
        {0, 0},
        {1, 1},
        {2, 0},
    };

    assertThatIllegalArgumentException().isThrownBy(() ->
        largestTriangleThreeBuckets(input, 1)
    );
    assertThatIllegalArgumentException().isThrownBy(() ->
        largestTriangleThreeBuckets(input, 2)
    );
    assertThatIllegalArgumentException().isThrownBy(() ->
        largestTriangleThreeBuckets(input, 0)
    );
  }

  @Test
  public void one_bucket_with_two_points_with_same_area_produces_the_first_point() {
    double[][] input = {
        {0, 0},
        {1, 1},     // 1 and -1 are equivalent
        {2, -1},
        {3, 0},
    };

    assertThat(largestTriangleThreeBuckets(input, 3)).isEqualTo(new double[][]{
        {0, 0},
        {1, 1},
        {3, 0},
    });
  }

  @Test
  public void one_bucket_with_two_points_with_different_area_produces_the_point_that_generates_max_area() {
    double[][] input = {
        {0, 0},
        {1, 1},
        {2, 2},  // point that generates the max area
        {3, 0},
    };

    assertThat(largestTriangleThreeBuckets(input, 3)).isEqualTo(new double[][]{
        {0, 0},
        {2, 2},
        {3, 0},
    });
  }

  @Test
  public void two_buckets_non_full_middle_buckets() {
    double[][] input = {
        {0, 0},
        {1, 1},
        {2, 2},
        {3, 1},
        {4, 5},
    };

    assertThat(largestTriangleThreeBuckets(input, 4)).isEqualTo(new double[][]{
        {0, 0},
        {1, 1},
        {3, 1},
        {4, 5},
    });
  }

  @Test
  public void two_buckets_full_middle_buckets() {
    double[][] input = {
        {0, 0},
        {1, 1},
        {2, 3},
        {3, 1},
        {4, 3},
        {5, 2},
        {6, 0},
    };

    assertThat(largestTriangleThreeBuckets(input, 4)).isEqualTo(new double[][]{
        {0, 0},
        {2, 3},
        {4, 3},
        {6, 0},
    });
  }

  @Test
  public void peaks_and_troughs() {
    double[][] input = {
        {0, 0},
        {1, 1},
        {2, 10},
        {3, 1},
        {4, 3},
        {5, 30},
        {6, 0},
        {7, 0},
        {8, 1},
        {9, 3},
        {10, 3},
        {11, 3},
        {12, 3},
        {13, -20},
        {14, 3},
        {15, 3},
        {16, 3},
        {17, 1},
        {18, -2},
        {19, 1},
        {20, -7},
    };

    assertThat(largestTriangleThreeBuckets(input, 4)).isEqualTo(new double[][]{
        {0, 0},
        {5, 30},
        {13, -20},
        {20, -7},
    });
    assertThat(largestTriangleThreeBuckets(input, 11)).isEqualTo(new double[][]{
        {0, 0},
        {2, 10},
        {3, 1},
        {5, 30},
        {7, 0},
        {9, 3},
        {12, 3},
        {13, -20},
        {15, 3},
        {19, 1},
        {20, -7},
    });
  }

  @Test
  public void two_buckets_full_middle_buckets1D() {
    double[] input = {0, 1, 3, 1, 3, 2, 0,};

    assertThat(largestTriangleThreeBuckets1D(input, 4)).isEqualTo(
        new double[]{0, 3, 3, 0,}
    );
  }


  @Test
  public void peaks_and_troughs_1D() {
    double[] input = {0, 1, 10, 1, 3, 30, 0, 0, 1, 3, 3, 3, 3, -20, 3, 3, 3, 1, -2, 1, -7,};

    assertThat(largestTriangleThreeBuckets1D(input, 4)).isEqualTo(
        new double[]{0, 30, -20, -7,}
    );
    assertThat(largestTriangleThreeBuckets1D(input, 11)).isEqualTo(
        new double[]{0, 10, 1, 30, 0, 3, 3, -20, 3, 1, -7,}
    );
  }
}