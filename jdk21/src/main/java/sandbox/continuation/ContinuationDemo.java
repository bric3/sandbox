package sandbox.continuation;

import java.util.Scanner;

/**
 * Run on JDK 21+ with <code>--add-opens java.base/jdk.internal.vm=ALL-UNNAMED --enable-preview</code>
 */
public class ContinuationDemo {

    static void countUp(Continuation.Scope<?> scope) {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
            Continuation.yield(scope); // gives up control until continuation is resumed
        }
        System.out.println("Done");
    }

    public static void main(String[] args) {

        var continuation = new Continuation<>("myscope", ContinuationDemo::countUp);

        try (var scanner = new Scanner(System.in)) {
            while (!continuation.isDone()) { // continuation code block is not finished
                System.out.print("Press enter to run one more step: ");
                scanner.nextLine();
                continuation.run(); // resume continuation
            }
            System.out.println("No more input.");
        }
    }
}
