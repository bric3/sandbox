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

import org.apache.tuweni.crypto.sodium.Box;
import org.apache.tuweni.crypto.sodium.Sodium;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class JnrBinding {
  static {
    var libraryPath = Path.of(switch (jnr.ffi.Platform.getNativePlatform().getOS()) {
      case LINUX -> "/usr/local/lib/libsodium.so";
      case DARWIN -> "/usr/local/lib/libsodium.dylib";
      case WINDOWS -> "C:/libsodium/libsodium.dll";
      default -> throw new IllegalStateException("Add support for platform");
    });

    Sodium.loadLibrary(libraryPath);
    System.out.println("JnrBinding using libsodium " + Sodium.version());
  }

  public byte[] cryptoSealedBox(byte[] message) {
    // Recipient creates a long-term key pair
    var keyPair = Box.KeyPair.random();

    // Anonymous sender encrypts a message using an ephemeral key pair
    // and the recipient's public key
    byte[] sealedMessage = Box.encryptSealed(message, keyPair.publicKey());

    // Recipient decrypts the ciphertext
    return Box.decryptSealed(sealedMessage, keyPair.publicKey(), keyPair.secretKey());
  }

  public static void main(String[] args) {
    byte[] bytes = new JnrBinding().cryptoSealedBox("JNR-FFI Binding".getBytes(StandardCharsets.UTF_8));

    System.out.println(new String(bytes, StandardCharsets.UTF_8));
  }
}
