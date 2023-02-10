package sandbox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Since("16") // copied from jdk16
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
      static final Pattern pattern = Pattern.compile("^[^ @]+@[^ @]+\\.[^ @]+$");
    }
    System.out.printf("%s -> %s%n", str, str != null && EmailPattern.pattern.matcher(str).matches());
  }
}