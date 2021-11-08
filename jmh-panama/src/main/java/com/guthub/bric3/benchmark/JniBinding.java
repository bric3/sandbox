/*
 * MIT License
 *
 * Copyright (c) 2021 Brice Dutheil <brice.dutheil@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
