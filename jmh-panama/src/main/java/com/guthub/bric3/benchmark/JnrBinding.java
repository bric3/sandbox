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
