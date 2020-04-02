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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
		final Map<String, List<String>> patterns = new HashMap<>();

		for (final String family : KnownPlatforms.FAMILIES) {
			for (final String arch : KnownPlatforms.ARCHES) {
				// NB: Convert family+arch to short name --
				// e.g. win32, win64, macosx, linux32, linux64.
				final String shortName = KnownPlatforms.shortName(family, arch);
				if (shortName == null) continue;
				addClassifier(patterns, "jars/" + shortName, family + "-" + arch);
			}
			// NB: Convert family alone (no arch) to short name --
			// e.g. windows -> win64, osx -> macosx, linux -> linux64.
			final String shortName = KnownPlatforms.shortName(family, null);
			if (shortName == null) continue;
			addClassifier(patterns, "jars/" + shortName, family);
		}

		return patterns.entrySet().stream() //
			.map(entry -> pattern(entry.getKey(), entry.getValue())) //
			.collect(Collectors.toList());
	}

	private static void addClassifier(final Map<String, List<String>> patterns,
		final String name, final String classifier)
	{
		final String[] prefixes = { "", "native-", "natives-" };
		final List<String> classifiers = //
			patterns.computeIfAbsent(name, l -> new ArrayList<>());
		for (final String prefix : prefixes) {
			classifiers.add(prefix + classifier);
		}
	}

	private static SubdirectoryPattern pattern(final String name,
		final List<String> classifiers)
	{
		final SubdirectoryPattern pattern = new SubdirectoryPattern();
		pattern.name = name;
		pattern.classifiers = classifiers;
		return pattern;
	}
}
