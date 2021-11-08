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

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;
import org.apache.commons.lang3.SystemUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;

import static jdk.incubator.foreign.CLinker.C_CHAR;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;

public class PanamaBinding {
  private static final SymbolLookup libsodiumLookup;

  static {
    System.load(switch (SystemUtils.OS_NAME) {
      case "Linux" -> "/usr/local/lib/libsodium.so";
      case "Mac OS X" -> "/usr/local/lib/libsodium.dylib";
      case "Windows" -> "C:/libsodium/libsodium.dll";
      default -> throw new IllegalStateException(SystemUtils.OS_NAME);
    });
    libsodiumLookup = SymbolLookup.loaderLookup();

    MethodHandle crypto_box_sealbytes =
        CLinker.getInstance()
               .downcallHandle(
                   libsodiumLookup.lookup("sodium_version_string").get(),
                   MethodType.methodType(MemoryAddress.class),
                   FunctionDescriptor.of(C_POINTER)
               );

    try {
      var v = (MemoryAddress) crypto_box_sealbytes.invokeExact();
      System.out.println("PanamaBinding using libsodium " + CLinker.toJavaString(v));
    } catch (Throwable throwable) {
      throw new ExceptionInInitializerError(throwable);
    }
  }

  // size_t  crypto_box_publickeybytes(void);
  public static final MethodHandle crypto_box_publickeybytes = CLinker.getInstance()
                                                                 .downcallHandle(
                                                                     libsodiumLookup.lookup("crypto_box_publickeybytes").get(),
                                                                     MethodType.methodType(int.class),
                                                                     FunctionDescriptor.of(C_INT)
                                                                 );
  // crypto_box_secretkeybytes(void);
  public static final MethodHandle crypto_box_secretkeybytes = CLinker.getInstance()
                                                                 .downcallHandle(
                                                                     libsodiumLookup.lookup("crypto_box_secretkeybytes").get(),
                                                                     MethodType.methodType(int.class),
                                                                     FunctionDescriptor.of(C_INT)
                                                                 );
  // int crypto_box_keypair(unsigned char *pk, unsigned char *sk)
  // __attribute__ ((nonnull));
  public static final MethodHandle crypto_box_keypair = CLinker.getInstance().downcallHandle(
      libsodiumLookup.lookup("crypto_box_keypair").get(),
      MethodType.methodType(
          void.class,
          MemoryAddress.class, // pk
          MemoryAddress.class  // sk
      ),
      FunctionDescriptor.ofVoid(C_POINTER, C_POINTER)
  );

  // size_t crypto_box_sealbytes(void);
  private static final MethodHandle crypto_box_sealbytes =
      CLinker.getInstance()
             .downcallHandle(
                 libsodiumLookup.lookup("crypto_box_sealbytes").get(),
                 MethodType.methodType(int.class),
                 FunctionDescriptor.of(C_INT));

  public static final MethodHandle crypto_box_seal = CLinker.getInstance().downcallHandle(
      libsodiumLookup.lookup("crypto_box_seal").get(),
      // src/libsodium/include/sodium/crypto_box.h
      // SODIUM_EXPORT
      // int crypto_box_seal(unsigned char *c, const unsigned char *m,
      //                    unsigned long long mlen, const unsigned char *pk)
      //            __attribute__ ((nonnull(1, 4)));
      //
      // "(
      //   Ljdk/incubator/foreign/MemoryAddress;
      //   Ljdk/incubator/foreign/MemoryAddress;
      //   J
      //   Ljdk/incubator/foreign/MemoryAddress;
      // )"
      //
      // c.address(), m.address(), mlen, pk.address()
      MethodType.methodType(int.class,
                            MemoryAddress.class, // cipherText, output buffer
                            MemoryAddress.class, // message
                            long.class,          // message length
                            MemoryAddress.class  // publicKey
      ),
      FunctionDescriptor.of(C_INT,
                            C_POINTER,
                            C_POINTER,
                            C_LONG_LONG,
                            C_POINTER)

  );
  
  public static final MethodHandle crypto_box_seal_open = CLinker.getInstance().downcallHandle(
      libsodiumLookup.lookup("crypto_box_seal_open").get(),
      // "(Ljdk/incubator/foreign/MemoryAddress;
      //   Ljdk/incubator/foreign/MemoryAddress;
      //   J
      //   Ljdk/incubator/foreign/MemoryAddress;
      //   Ljdk/incubator/foreign/MemoryAddress;)I"
      MethodType.methodType(int.class,
                            MemoryAddress.class, // message
                            MemoryAddress.class, // cipherText
                            long.class,          // cipherText.length
                            MemoryAddress.class, // public key
                            MemoryAddress.class  // secret key
      ),
      FunctionDescriptor.of(C_INT,
                            C_POINTER,
                            C_POINTER,
                            C_LONG_LONG,
                            C_POINTER,
                            C_POINTER
      )
  );


  public byte[] cryptoSealedBox(byte[] message) throws Throwable {
    // Recipient creates a long-term key pair
    var keyPair = crypto_box_keypair();

    // Anonymous sender encrypts a message using an ephemeral key pair
    // and the recipient's public key
    var cipherText = crypto_box_seal(message, keyPair.publicKey);

    // Recipient decrypts the ciphertext
    return crypto_box_seal_open(cipherText, keyPair.publicKey, keyPair.secretKey);
  }


  public int crypto_box_sealbytes() throws Throwable {
    return (int) crypto_box_sealbytes.invokeExact();
  }

  public int crypto_box_publickeybytes() throws Throwable {
    return (int) crypto_box_publickeybytes.invokeExact();
  }

  public int crypto_box_secretkeybytes() throws Throwable {
    return (int) crypto_box_secretkeybytes.invokeExact();
  }


  public CryptoBoxKeyPair crypto_box_keypair() throws Throwable {
    try (var scope = ResourceScope.newConfinedScope()) {
      var segmentAllocator = SegmentAllocator.ofScope(scope);
      var recipientPublicKey = segmentAllocator.allocate(crypto_box_publickeybytes());
      var recipientSecretKey = segmentAllocator.allocate(crypto_box_secretkeybytes());

      crypto_box_keypair.invokeExact(recipientPublicKey.address(),
                                     recipientSecretKey.address());

      return new CryptoBoxKeyPair(
          recipientPublicKey.toByteArray(),
          recipientSecretKey.toByteArray()
      );
    }
  }


  public byte[] crypto_box_seal(byte[] message,
                                byte[] publicKey
  ) throws Throwable {
    try (var scope = ResourceScope.newConfinedScope()) {
      var segmentAllocator = SegmentAllocator.ofScope(scope);
      var nativeMessage = segmentAllocator.allocateArray(C_CHAR, message);
      var cipherText = segmentAllocator.allocate(crypto_box_sealbytes() + nativeMessage.byteSize());
      var ret = (int) crypto_box_seal.invokeExact(
          cipherText.address(),
          nativeMessage.address(),
          (long) nativeMessage.byteSize(),
          segmentAllocator.allocateArray(C_CHAR, publicKey).address());
      return cipherText.toByteArray();
    }
  }

  public byte[] crypto_box_seal_open(byte[] cipherText,
                                     byte[] publicKey,
                                     byte[] secretKey
  ) throws Throwable {
    try (var scope = ResourceScope.newConfinedScope()) {
      var segmentAllocator = SegmentAllocator.arenaAllocator(scope);
      var decipheredText = segmentAllocator.allocateArray(C_CHAR, cipherText.length - crypto_box_sealbytes());
      var ret = (int) crypto_box_seal_open.invokeExact(decipheredText.address(),
                                                       segmentAllocator.allocateArray(C_CHAR, cipherText).address(),
                                                       (long) cipherText.length,
                                                       segmentAllocator.allocateArray(C_CHAR, publicKey).address(),
                                                       segmentAllocator.allocateArray(C_CHAR, secretKey).address());

      return decipheredText.toByteArray();
    }
  }

  private record CryptoBoxKeyPair(byte[] publicKey, byte[] secretKey) {
  }

  public static void main(String[] args) throws Throwable {
    byte[] bytes = new PanamaBinding().cryptoSealedBox("Panama JEP-412 Binding".getBytes(StandardCharsets.UTF_8));

    System.out.println(new String(bytes, StandardCharsets.UTF_8));
  }
}
