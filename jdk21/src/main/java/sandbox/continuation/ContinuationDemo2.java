package sandbox.continuation;

import java.util.Scanner;

/**
 * Run on JDK 21+ with <code>--add-opens java.base/jdk.internal.vm=ALL-UNNAMED --enable-preview</code>
 */
public class ContinuationDemo2 {

    static void countUp(Continuation.Scope<?> scope) {
        for (int i = 0; i < 10; i++) {
            System.out.printf("%s: %s%n", scope, i);
            Continuation.yield(scope); // gives up control until continuation is resumed
        }
        System.out.printf("%s: Done%n", scope);
    }

    public static void main(String[] args) {

        var counter1 = new Continuation<>("counter1", ContinuationDemo2::countUp);
        var counter2 = new Continuation<>("counter2", ContinuationDemo2::countUp);

        try (var scanner = new Scanner(System.in)) {
            loop:
            while (!counter1.isDone() || !counter2.isDone()) { // continuation code blocks are not finished
                System.out.print("Enter 1 or 2 to select counter to increment, or 0 to exit: ");
                int counter = scanner.nextInt();
                switch (counter) {
                    case 1:
                        if (!counter1.isDone()) {
                            counter1.run(); // throws "IllegalStateException: Continuation terminated" if already completed
                        }
                        break;
                    case 2:
                        if (!counter2.isDone()) {
                            counter2.run();
                        }
                        break;
                    case 0:
                        break loop;
                }
            }
            System.out.println("No more input.");
        }
    }
}
