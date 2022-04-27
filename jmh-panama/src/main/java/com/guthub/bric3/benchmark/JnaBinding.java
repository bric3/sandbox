/*
 * sandbox
 *
 * Copyright (c) 2021,today - Brice Dutheil <brice.dutheil@gmail.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */


package com.guthub.bric3.benchmark;

import com.muquit.libsodiumjna.SodiumLibrary;
import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import com.sun.jna.Platform;

import java.nio.charset.StandardCharsets;

// https://github.com/muquit/libsodium-jna
public class JnaBinding {

  static {
    var libraryPath = switch (Platform.getOSType()) {
      case Platform.LINUX -> "/usr/local/lib/libsodium.so";
      case Platform.MAC -> "/usr/local/lib/libsodium.dylib";
      case Platform.WINDOWS -> "C:/libsodium/libsodium.dll";
      default -> throw new IllegalStateException("Add support for platform");
    };

    SodiumLibrary.setLibraryPath(libraryPath);
    System.out.println("JnaBinding using libsodium " + SodiumLibrary.libsodiumVersionString());
  }

  public byte[] cryptoSealedBox(byte[] message) throws SodiumLibraryException {
    // Recipient creates a long-term key pair
    var keyPair = SodiumLibrary.cryptoBoxKeyPair();

    // Anonymous sender encrypts a message using an ephemeral key pair
    // and the recipient's public key
    byte[] sealedMessage = SodiumLibrary.cryptoBoxSeal(message, keyPair.getPublicKey());

    // Recipient decrypts the ciphertext
    return SodiumLibrary.cryptoBoxSealOpen(sealedMessage, keyPair.getPublicKey(), keyPair.getPrivateKey());
  }

  public static void main(String[] args) throws SodiumLibraryException {
    byte[] bytes = new JnaBinding().cryptoSealedBox("JNA Binding".getBytes(StandardCharsets.UTF_8));

    System.out.println(new String(bytes, StandardCharsets.UTF_8));
  }
}
