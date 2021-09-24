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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Inner classes can now have static fields
 *
 * See
 * - https://twitter.com/sundararajan_a/status/1439227184229482498?s=21
 * - https://twitter.com/sundararajan_a/status/1439433889538187270?s=21
 */
public class StaticFieldInnerClass {

  public static void main(String[] args) {
    StaticFieldInnerClass instance = new StaticFieldInnerClass();

    for (int i = 0; i < 12; i++) {
      System.out.printf("%d -> %d%n", i, instance.fibonacci(i));
    }

    instance.isValidEmail(null);
    instance.isValidEmail("");
    instance.isValidEmail("yup@yup.com");
  }

  private int fibonacci(int n) {
    if (n == 0 || n == 1) return 1;

    class Cache {
      static final int capacity = Integer.getInteger("fibonacci.cache.size", 100);
      static final Map<Integer, Integer> map = new LinkedHashMap<>(16,0.75f,true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
          return size() > capacity;
        }
      };
    }

    var cache = Cache.map;
    if (cache.containsKey(n)) {
      System.out.printf("cache hit for %d%n", n);
      return cache.get(n);
    }

    var res = fibonacci(n - 1) + fibonacci(n - 2);
    cache.put(n, res);
    return res;
  }


  private void isValidEmail(String str) {
    class EmailPattern {
      static final Pattern pattern = Pattern.compile("^[^\s@]+@[^\s@]+\\.[^\s@]+$");
    }
    System.out.printf("%s -> %s%n", str, str != null && EmailPattern.pattern.matcher(str).matches());
  }
}
