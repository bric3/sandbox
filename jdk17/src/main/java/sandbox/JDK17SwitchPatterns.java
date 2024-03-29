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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JDK17SwitchPatterns {
  public static void main(String[] args) {
    pattern_matching_for_instanceof___jdk16();

    sealed_classes___jdk17();

    if (args.length == 0) {
      System.err.println("Requires at least one argument");
      System.exit(1);
    }
    ;
    switch_expressions___jdk14(args[0]);

    // JDK17: JEP 406: Pattern Matching for switch (Preview)
    // --enable-preview

    Object o = new ArrayList<SoundSystem>();

    var soundSystemSteam = switch (o) {
      case null -> Stream.empty();
      case SoundSystem s -> Stream.of(s);
      // case List<SoundSystem> l -> l.stream();
      // Object cannot be safely cast to List<SoundSystem>
      case List l && l.isEmpty() -> Stream.empty();
      case List l && l.size() == 1 -> Stream.of(l.get(0));
      case List l -> l.stream();
      case SoundSystem[] a && a.length == 0 -> Stream.empty();
      case SoundSystem[] a -> Arrays.stream(a);

      default -> Stream.empty();
    };

    soundSystemSteam.map(Object::toString).collect(Collectors.joining(","));


    // JDK18: JEP 405: Record Patterns & Array Patterns (Preview)
  }

  private static void switch_expressions___jdk14(String arg) {
    // JDK 14: JEP 361: Switch expressions
    var channels = switch (arg.toLowerCase()) {
      case "mono" -> Channels.ONE;
      case "stereo" -> Channels.TWO;
      case "thx", "dolbysurround", "dolbydigital" -> Channels.FIVE_DOT_ONE;
      case "dolbyatmos" -> Channels.MANY;
      default -> Channels.UNKNOWN;
    };
  }

  private static void sealed_classes___jdk17() {
    System.out.println("JDK17: JEP 409: Sealed Classes");
    System.out.printf("SoundSystem sealed               : %s%n", SoundSystem.class.isSealed());
    System.out.printf("SoundSystem permitted subclasses : %s%n", Arrays.stream(SoundSystem.class.getPermittedSubclasses()).map(Class::getSimpleName).collect(Collectors.joining(",")));

    System.out.printf("Letter sealed                    : %s%n", Letter.class.isSealed());
    System.out.printf("Letter permitted subclasses      : %s%n", Arrays.stream(Letter.class.getPermittedSubclasses()).map(Class::getSimpleName).collect(Collectors.joining(",")));
  }

  private static void pattern_matching_for_instanceof___jdk16() {
    System.out.println("JDK16: JEP 394: Pattern matching for instanceof");
    Object a = new ArrayList<>();

    if (a instanceof List l) {
      System.out.println(l.size());
    }
  }

  // JDK16: JEP 395: Records
  record HomeCinema(Set<SoundSystem> soundSystems) {
  }

  enum Channels {
    ONE,
    TWO,
    FIVE_DOT_ONE,
    SEVEN_DOT_ONE,
    MANY,
    UNKNOWN
  }

  // JDK17: JEP 409: Sealed Classes
  // combination of sealed classes and record classes is sometimes referred to as algebraic data types
  sealed interface SoundSystem
      permits Mono, Stereo, THX, DolbySurround, DolbyDigital, DolbyAtmos {

  }

  final class Mono implements SoundSystem {
  }

  record Stereo() implements SoundSystem {
  }

  record THX() implements SoundSystem {
  }

  record DolbySurround() implements SoundSystem {
  }

  record DolbyDigital() implements SoundSystem {
  }

  record DolbyAtmos() implements SoundSystem {
  }


  sealed class Letter {
    final class A extends Letter {
    }

    final class B extends Letter {
    }

    final class C extends Letter {
    }
  }
}