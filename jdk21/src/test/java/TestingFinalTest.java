import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class TestingFinalTest {
    record Person(String name, int age) {
        public Person {
            if (age < 0) {
                throw new IllegalArgumentException("age < 0");
            }
        }
    }

    @Test
    void name() {
        mock(Person.class);
    }

}
