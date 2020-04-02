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
}
