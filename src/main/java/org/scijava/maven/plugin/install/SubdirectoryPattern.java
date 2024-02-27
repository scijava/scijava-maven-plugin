/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2024 SciJava developers.
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

import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * Data structure for mapping GAV patterns to subdirectories.
 * 
 * @author Curtis Rueden
 */
public class SubdirectoryPattern {

	/** The subdirectory into which matching artifacts should be installed. */
	public String subdirectory;

	/**
	 * List of pattern strings. An artifact matching any of these patterns will be
	 * installed into the affiliated subdirectory. Valid pattern syntaxes are:
	 * <ol>
	 * <li>classifier</li>
	 * <li>groupId:artifactId</li>
	 * <li>groupId:artifactId:version</li>
	 * <li>groupId:artifactId:version:classifier</li>
	 * <li>groupId:artifactId:version:classifier:packaging</li>
	 * </ol>
	 * <p>
	 * Additionally, the wildcard character ({@code *}) is allowed and matches
	 * anything. For example, the pattern {@code org.scijava:*:*:natives-*}
	 * would match any artifact with groupId {@code org.scijava} and classifier
	 * beginning with {@code natives-}.
	 * </p>
	 */
	public List<String> patterns;

	/** Returns true iff this pattern matches the given artifact. */
	public boolean matches(final Artifact artifact) {
		return patterns.stream().anyMatch(pattern -> matches(artifact, pattern));
	}

	private static boolean matches(final Artifact artifact, final String pattern) {
		final String[] tokens = pattern.split(":");
		final String g, a, v, c, p;
		if (tokens.length == 1) {
			g = a = v = p = "*";
			c = tokens[0];
		}
		else if (tokens.length == 2) {
			g = tokens[0];
			a = tokens[1];
			v = c = p = "*";
		}
		else if (tokens.length == 3) {
			g = tokens[0];
			a = tokens[1];
			v = tokens[2];
			c = p = "*";
		}
		else if (tokens.length == 4) {
			g = tokens[0];
			a = tokens[1];
			v = tokens[2];
			c = tokens[3];
			p = "*";
		}
		else if (tokens.length == 5) {
			g = tokens[0];
			a = tokens[1];
			v = tokens[2];
			c = tokens[3];
			p = tokens[4];
		}
		else {
			throw new IllegalArgumentException("Invalid subdirectory pattern: " + pattern);
		}
		return matches(artifact.getGroupId(), g) && //
			matches(artifact.getArtifactId(), a) && //
			matches(artifact.getVersion(), v) && //
			matches(artifact.getClassifier(), c) && //
			matches(artifact.getType(), p);
	}

	private static boolean matches(final String string, final String pattern) {
		final String s = string == null ? "" : string;
		final String regex = pattern.replaceAll("\\*", ".*");
		return s.matches(regex);
	}
}
