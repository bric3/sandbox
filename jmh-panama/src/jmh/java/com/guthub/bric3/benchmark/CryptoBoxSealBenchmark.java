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

import com.muquit.libsodiumjna.exceptions.SodiumLibraryException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CryptoBoxSealBenchmark {

	private PanamaBinding panama;
	private JniBinding jni;
	private JnaBinding jna;
	private JnrBinding jnr;


  private final byte[] message = UUID.randomUUID().toString().getBytes();


  @Setup(Level.Trial)
	public void setup() {
    System.out.println("Getting ready");
		panama = new PanamaBinding();
		jni = new JniBinding();
		jna = new JnaBinding();
		jnr = new JnrBinding();
	}

	@Benchmark
	public void jni() {
		jni.cryptoSealedBox(message);
	}

	@Benchmark
	public void panama() throws Throwable {
		panama.cryptoSealedBox(message);
	}

	@Benchmark
	public void jna() throws SodiumLibraryException {
		jna.cryptoSealedBox(message);
	}

	@Benchmark
	public void jnr() {
		jnr.cryptoSealedBox(message);
	}
}
