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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data structure enumerating known platform strings.
 * 
 * @author Curtis Rueden
 */
public final class KnownPlatforms {

	public static final List<String> FAMILIES = Arrays.asList(
		"android",
		"ios",
		"linux",
		"macos",
		"macosx",
		"osx",
		"windows"
	);

	public static final List<String> ARCHES = Arrays.asList(
		"aarch64",
		"all",
		"amd64",
		"arm",
		"arm64",
		"armhf",
		"i586",
		"ppc64le",
		"universal",
		"x86",
		"x86_64"
	);

	public static String shortName(final String family, final String arch) {
		if (isMacOS(family)) return "macosx";
		if (isWindows(family) && isArch32(arch)) return "win32";
		if (isWindows(family) && isArch64(arch)) return "win64";
		if (isLinux(family) && isArch32(arch)) return "linux32";
		if (isLinux(family) && isArch64(arch)) return "linux64";
		return null;
	}

	/**
	 * Gets a list of {@link SubdirectoryPattern}s corresponding to known native
	 * classifiers. E.g.:
	 * <ul>
	 * <li>win32 &rarr; windows-x86, natives-windows-i586, etc.</li>
	 * <li>win64 &rarr; windows-x86_64, natives-windows-amd64, etc.</li>
	 * <li>macosx &rarr; macosx-x86_64, natives-macosx-universal, etc.</li>
	 * <li>linux32 &rarr; linux-x86, natives-linux-i586, etc.</li>
	 * <li>linux64 &rarr; linux-x86_64, natives-linux-amd64, etc.</li>
	 * </ul>
	 */
	public static List<SubdirectoryPattern> nativeClassifierPatterns() {
		final Map<String, List<String>> patterns = new HashMap<>();

		for (final String family : FAMILIES) {
			for (final String arch : ARCHES) {
				// NB: Convert family+arch to short name --
				// e.g. win32, win64, macosx, linux32, linux64.
				final String shortName = shortName(family, arch);
				if (shortName == null) continue;
				addClassifier(patterns, "jars/" + shortName, family + "-" + arch);
			}
			// NB: Convert family alone (no arch) to short name --
			// e.g. windows -> win64, osx -> macosx, linux -> linux64.
			final String shortName = shortName(family, null);
			if (shortName == null) continue;
			addClassifier(patterns, "jars/" + shortName, family);
		}

		return patterns.entrySet().stream() //
			.map(entry -> pattern(entry.getKey(), entry.getValue())) //
			.collect(Collectors.toList());
	}

	private static boolean isWindows(final String family) {
		return "windows".equals(family);
	}
	private static boolean isLinux(final String family) {
		return "linux".equals(family);
	}
	private static boolean isMacOS(final String family) {
		final String[] macFamily = {"macos", "macosx", "osx"};
		return Arrays.asList(macFamily).contains(family);
	}
	private static boolean isArch32(final String arch) {
		return "i586".equals(arch) || "x86".equals(arch);
	}
	private static boolean isArch64(final String arch) {
		return arch == null || "amd64".equals(arch) || "x86_64".equals(arch);
	}

	private static void addClassifier(final Map<String, List<String>> patterns,
		final String subdirectory, final String classifier)
	{
		final String[] prefixes = { "", "native-", "natives-" };
		final List<String> classifiers = //
			patterns.computeIfAbsent(subdirectory, l -> new ArrayList<>());
		for (final String prefix : prefixes) {
			classifiers.add("*:*:*:" + prefix + classifier);
		}
	}

	private static SubdirectoryPattern pattern(final String subdirectory,
		final List<String> patterns)
	{
		final SubdirectoryPattern pattern = new SubdirectoryPattern();
		pattern.subdirectory = subdirectory;
		pattern.patterns = patterns;
		return pattern;
	}
}
