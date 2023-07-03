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
    void initiateMock() {
        mock(Person.class);
    }
}
