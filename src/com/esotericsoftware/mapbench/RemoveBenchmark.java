/* Copyright (c) 2020, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.esotericsoftware.mapbench;

import java.util.HashSet;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import com.badlogic.gdx.utils.ObjectSet;

@BenchmarkMode(Mode.SingleShotTime)
public class RemoveBenchmark {
	@Benchmark
	public void Cuckoo (CuckooState state) {
		ObjectSet set = state.set;
		String[] words = state.words;
		for (int i = 0, n = words.length; i < n; i++)
			set.remove(words[i]);
	}

	@Benchmark
	public void HashSet (HashSetState state) {
		HashSet set = state.set;
		String[] words = state.words;
		for (int i = 0, n = words.length; i < n; i++)
			set.remove(words[i]);
	}

	@Benchmark
	public void MerrySet (MerrySetState state) {
		MerrySet set = state.set;
		String[] words = state.words;
		for (int i = 0, n = words.length; i < n; i++)
			set.remove(words[i]);
	}

	@State(Scope.Thread)
	static public class CuckooState {
		@Param({"100", "1000", "10000", "100000", "1000000"}) public int size; // Words to load from the file.
		@Param({"-1"}) public int seed = -1; // -1: no random shuffling

		public String[] words;
		public final ObjectSet set = new ObjectSet();

		@Setup(Level.Trial)
		public void setup () {
			words = Wordlist.loadWords(size, seed);
			for (int i = 0, n = words.length; i < n; i++)
				set.add(words[i]);
		}
	}

	@State(Scope.Thread)
	static public class HashSetState {
		@Param({"100", "1000", "10000", "100000", "1000000"}) public int size; // Words to load from the file.
		@Param({"-1"}) public int seed = -1; // -1: no random shuffling

		public String[] words;
		public final HashSet set = new HashSet();

		@Setup(Level.Trial)
		public void setup () {
			words = Wordlist.loadWords(size, seed);
			for (int i = 0, n = words.length; i < n; i++)
				set.add(words[i]);
		}
	}

	@State(Scope.Thread)
	static public class MerrySetState {
		@Param({"100", "1000", "10000", "100000", "1000000"}) public int size; // Words to load from the file.
		@Param({"-1"}) public int seed = -1; // -1: no random shuffling

		public String[] words;
		public final MerrySet set = new MerrySet();

		@Setup(Level.Trial)
		public void setup () {
			words = Wordlist.loadWords(size, seed);
			for (int i = 0, n = words.length; i < n; i++)
				set.add(words[i]);
		}
	}
}
