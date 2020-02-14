/* Copyright (c) 2008-2018, Nathan Sweet
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

import java.lang.reflect.Field;

import org.openjdk.jmh.Main;

public class Benchmarks {
	/** To run from command line: $ mvn clean install exec:java -Dexec.args="-f 4 -wi 5 -i 3 -t 2 -w 2s -r 2s"
	 * <p>
	 * Fork 0 can be used for debugging/development, eg: -f 0 -wi 1 -i 1 -t 1 -w 1s -r 1s [benchmarkClassName]
	 * <p>
	 * Need JMH to generate classes before running in Eclipse: mvn clean compile */
	static public void main (String[] args) throws Exception {
		// Turn off illegal access log messages.
		try {
			Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field loggerField = loggerClass.getDeclaredField("logger");
			Class unsafeClass = Class.forName("sun.misc.Unsafe");
			Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			Object unsafe = unsafeField.get(null);
			Long offset = (Long)unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, loggerField);
			unsafeClass.getMethod("putObjectVolatile", Object.class, long.class, Object.class) //
				.invoke(unsafe, loggerClass, offset, null);
		} catch (Throwable ex) {
			System.out.println("Failed to disable Java 10 access warning:");
			ex.printStackTrace();
		}

		if (args.length == 0) {
			String commandLine = "-f 0 -wi 1 -i 1 -t 1 -w 1s -r 1s -bs 10 " // For development only (fork 0, short runs).
				// + "-bs 2500000 SetBenchmarks" //
				// + "-rf csv SetBenchmarks.cuckooGet SetBenchmarks.linearGet" //
				+ "ContainsBenchmark AddBenchmark RemoveBenchmark" //
			;
			System.out.println(commandLine);
			args = commandLine.split(" ");
		}
		Main.main(args);
	}
}
