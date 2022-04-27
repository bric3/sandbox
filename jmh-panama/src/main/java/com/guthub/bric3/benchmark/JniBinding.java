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

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.nio.charset.StandardCharsets;

// https://github.com/joshjdevl/libsodium-jni
public class JniBinding {
  static {
    NaCl.sodium();

    System.out.println("JniBinding using libsodium " + Sodium.sodium_version_string());
  }

  public byte[] cryptoSealedBox(byte[] message) {
    // Recipient creates a long-term key pair
    byte[] public_key=new byte[Sodium.crypto_box_publickeybytes()];
    byte[] private_key=new byte[Sodium.crypto_box_secretkeybytes()];
    Sodium.crypto_box_keypair(public_key, private_key);


    // Anonymous sender encrypts a message using an ephemeral key pair
    // and the recipient's public key
    byte[] sealedMessage = new byte[Sodium.crypto_box_sealbytes() + message.length];
    Sodium.crypto_box_seal(sealedMessage, message, message.length, public_key);

    // Recipient decrypts the ciphertext
    byte[] decryptedMessage = new byte[message.length];
    Sodium.crypto_box_seal_open(decryptedMessage, sealedMessage, sealedMessage.length, public_key, private_key);

    return decryptedMessage;
  }

  public static void main(String[] args) {
    byte[] bytes = new JniBinding().cryptoSealedBox("JNI Binding".getBytes(StandardCharsets.UTF_8));

    System.out.println(new String(bytes, StandardCharsets.UTF_8));
  }
}
