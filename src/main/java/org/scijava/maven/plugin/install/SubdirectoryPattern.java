/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2020 SciJava developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.maven.plugin.install;

import java.util.Arrays;
import java.util.List;

/**
 * Data structure for mapping classifiers to subdirectories.
 * <p>
 * The default use case is for handling installation of native classifier
 * artifacts to separate subdirectories. But this class makes the mappings
 * configurable downstream.
 * </p>
 * 
 * @author Curtis Rueden
 */
public class SubdirectoryPattern {

	public String name;
	public List<String> classifiers;

	public static List<SubdirectoryPattern> defaultPatterns() {
		return Arrays.asList(
			pattern("jars/win64",
				"windows",
				"windows-amd64",
				"windows-x86_64",
				"native-windows",
				"native-windows-amd64",
				"native-windows-x86_64",
				"natives-windows",
				"natives-windows-amd64",
				"natives-windows-x64_64"),
			pattern("jars/win32",
				"windows-x86",
				"windows-x86_32",
				"native-windows-x86",
				"native-windows-x86_32",
				"natives-windows-x86",
				"natives-windows-x86_32"),
			pattern("jars/macosx",
				"macos",
				"macos-amd64",
				"macos-universal",
				"macos-x86_64",
				"macosx",
				"macosx-amd64",
				"macosx-universal",
				"macosx-x86_64",
				"osx",
				"osx-amd64",
				"osx-universal",
				"osx-x86_64",
				"native-macos",
				"native-macos-amd64",
				"native-macos-universal",
				"native-macos-x86_64",
				"native-macosx",
				"native-macosx-amd64",
				"native-macosx-universal",
				"native-macosx-x86_64",
				"native-osx",
				"native-osx-amd64",
				"native-osx-universal",
				"native-osx-x86_64",
				"natives-macos",
				"natives-macos-amd64",
				"natives-macos-universal",
				"natives-macos-x86_64",
				"natives-macosx",
				"natives-macosx-amd64",
				"natives-macosx-universal",
				"natives-macosx-x86_64",
				"natives-osx",
				"natives-osx-amd64",
				"natives-osx-universal",
				"natives-osx-x86_64"),
			pattern("jars/linux64",
				"linux",
				"linux-amd64",
				"linux-x86_64",
				"native-linux",
				"native-linux-amd64",
				"native-linux-x86_64",
				"natives-linux",
				"natives-linux-amd64",
				"natives-linux-x64_64"),
			pattern("jars/linux32",
				"linux-x86",
				"linux-x86_32",
				"native-linux-x86",
				"native-linux-x86_32",
				"natives-linux-x86",
				"natives-linux-x86_32")
		);
	}

	private static SubdirectoryPattern pattern(final String name,
		final String... classifiers)
	{
		final SubdirectoryPattern pattern = new SubdirectoryPattern();
		pattern.name = name;
		pattern.classifiers = Arrays.asList(classifiers);
		return pattern;
	}
}
