package sandbox.continuation;

import java.util.Scanner;

/**
 * Run on JDK 21+ with <code>--add-opens java.base/jdk.internal.vm=ALL-UNNAMED --enable-preview</code>
 */
public class ContinuationDemo3 {

    static void generator(Continuation.Scope<Integer> scope) {
        for (int i = 0; ; i++) {
            Continuation.yield(scope, i);
        }
    }

    public static void main(String[] args) {

        var scope = Continuation.<Integer>scope("counter");
        var counter = new Continuation<>(scope, ContinuationDemo3::generator);

        try (var scanner = new Scanner(System.in)) {
            while (!counter.isDone()) {
                scanner.nextLine();
                int value = counter.next(scope);
                System.out.printf("State: %s%n", value);
            }
        }
    }
}
