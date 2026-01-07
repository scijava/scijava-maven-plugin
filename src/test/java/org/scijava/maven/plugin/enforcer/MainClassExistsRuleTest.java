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

package org.scijava.maven.plugin.enforcer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.maven.enforcer.rule.api.EnforcerLogger;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for {@link MainClassExistsRule}.
 *
 * @author Gabriel Selzer
 */
public class MainClassExistsRuleTest {

	/**
	 * Test subclass that provides a mock logger.
	 */
	private class MainClassExistsRuleMock extends MainClassExistsRule {
		public MainClassExistsRuleMock(MavenProject project) {
			super(project);
		}

		@Override
		public EnforcerLogger getLog() {
			return logMock;
		}
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private MavenProject project;
	private File outputDirectory;
	private EnforcerLoggerMock logMock;

	@Before
	public void setUp() throws Exception {
		// Create a temporary output directory
		outputDirectory = temporaryFolder.newFolder("target", "classes");

		// Create a mock MavenProject
		project = new MavenProject();
		Build build = new Build();
		build.setOutputDirectory(outputDirectory.getAbsolutePath());
		project.setBuild(build);

		// Create a mock logger
		logMock = new EnforcerLoggerMock();
	}

	@Test
	public void execute_NoMainClassProperty_Passes() throws Exception {
		// No "main-class" property set
		MainClassExistsRule rule = new MainClassExistsRuleMock(project);
        // Execute the rule and make sure it passes
		rule.execute();
	}

	@Test
	public void execute_MainClassExists_Passes() throws Exception {
		// Set main-class property
		project.getProperties().setProperty("main-class", "com.example.Main");

		// Create the corresponding .class file
		createClassFile("com.example.Main");

        // Execute the rule and make sure it passes
		MainClassExistsRule rule = new MainClassExistsRuleMock(project);
		rule.execute();
	}

	@Test
	public void execute_MainClassDoesNotExist_ThrowsException() throws Exception {
		// Set main-class property...
		project.getProperties().setProperty("main-class", "com.example.NonExistent");

		// ...but don't create the file
		MainClassExistsRule rule = new MainClassExistsRuleMock(project);

        // ...and assert an Exception is thrown.
		Assert.assertThrows(EnforcerRuleException.class, rule::execute);
	}

	/**
	 * Helper method to create a .class file at the appropriate location
	 * based on the fully qualified class name.
	 */
	private void createClassFile(String fullyQualifiedClassName) throws IOException {
		String classFilePath = fullyQualifiedClassName.replace('.', File.separatorChar) + ".class";
		File classFile = new File(outputDirectory, classFilePath);

		// Create parent directories if needed
		classFile.getParentFile().mkdirs();

		// Create the .class file
		assertTrue("Failed to create class file: " + classFile.getAbsolutePath(),
			classFile.createNewFile());
	}
}
