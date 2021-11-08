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
