/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2025 SciJava developers.
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

package org.scijava.maven.plugin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.project.MavenProject;
import org.junit.Test;

/**
 * Tests {@link DependencyUtils}.
 *
 * @author Curtis Rueden
 */
public class DependencyUtilsTest {

	/**
	 * A module's {@code <parent>} is often a shared parent POM that knows
	 * nothing about its siblings, so the aggregator that actually lists the
	 * module must be recognized by directory instead.
	 */
	@Test
	public void testDeclaresModule() {
		final File root = new File("/tmp/some-project").getAbsoluteFile();
		final MavenProject aggregator = aggregator(root, "child", "other-child");

		assertTrue(DependencyUtils.declaresModule(aggregator, //
			new File(root, "child")));
		assertTrue(DependencyUtils.declaresModule(aggregator, //
			new File(root, "other-child")));
		assertFalse(DependencyUtils.declaresModule(aggregator, //
			new File(root, "not-a-module")));
		// NB: a module of some *other* aggregator must not match.
		assertFalse(DependencyUtils.declaresModule(aggregator, //
			new File(root.getParentFile(), "child")));
	}

	/** Module paths need not be direct children, and need not be normalized. */
	@Test
	public void testDeclaresModuleNonCanonicalPath() {
		final File root = new File("/tmp/some-project").getAbsoluteFile();
		final MavenProject aggregator = aggregator(root, "nested/child", "../sibling");

		assertTrue(DependencyUtils.declaresModule(aggregator, //
			new File(root, "nested/child")));
		assertTrue(DependencyUtils.declaresModule(aggregator, //
			new File(root.getParentFile(), "sibling")));
	}

	@Test
	public void testDeclaresModuleWithoutBasedir() {
		final MavenProject noBasedir = new MavenProject();
		assertFalse(DependencyUtils.declaresModule(noBasedir, new File("child")));
	}

	private static MavenProject aggregator(final File basedir,
		final String... modules)
	{
		final MavenProject project = new MavenProject();
		project.setFile(new File(basedir, "pom.xml"));
		project.getModel().setModules(Arrays.asList(modules));
		return project;
	}
}
