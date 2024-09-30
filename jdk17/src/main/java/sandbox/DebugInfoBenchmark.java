package sandbox;

import java.util.function.IntSupplier;
import java.util.stream.IntStream;

// This code behaves differently whether it has been compiled
// with debug `-g` or without `-g:none`.
// https://bugs.openjdk.org/browse/JDK-8318712
// Source https://twitter.com/tagir_valeev/status/1716760141893116051
public class DebugInfoBenchmark {
    private int compute() {
        IntSupplier s1 = () -> IntStream.range(0, 10000).map(v -> 1).sum();
        IntSupplier s2 = () -> IntStream.range(0, 10000).map(v -> 1).sum();
        IntSupplier s3 = () -> IntStream.range(0, 10000).map(v -> 1).sum();
        return s1.getAsInt() + s2.getAsInt() + s3.getAsInt();
    }

    private void measure() {
        int res = 0;
        // Warmup
        for (int i = 0; i < 20000; i++) {
            res += compute();
        }
        // Measurement
        long start = System.currentTimeMillis();
        for (int i = 0; i < 20000; i++) {
            res += compute();
        }
        long end = System.currentTimeMillis();
        System.out.println(res);
        System.out.println("Duration: " + (end - start) + "ms");
    }

    public static void main(String[] args) {
        new DebugInfoBenchmark().measure();
    }
}