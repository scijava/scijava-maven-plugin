/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2021 SciJava developers.
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

/* ========================================================================
 * This file was adapted from the no-package-cycles-enforcer-rule project:
 * https://github.com/andrena/no-package-cycles-enforcer-rule
 *
 * Copyright 2013 - 2018 David Burkhart, Ben Romberg, Daniel Galan y Martins,
 * Bastian Feigl, Marc Philipp, and Carsten Otto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ======================================================================== */

package org.scijava.maven.plugin.enforcer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.Before;
import org.junit.Test;

public class NoSubpackageDependenceRuleIntegrationTest {
	private static final URL FITNESSE_TARGET_FOLDER = getResource("fitnesse-target");
	private static final URL FITNESSE_EXPECTED_OUTPUT = getResource("fitnesse-expected-output-subpackage-dependence.txt");
	private static final URL JUNIT_TARGET_FOLDER = getResource("junit-target");
	private static final URL JUNIT_EXPECTED_OUTPUT = getResource("junit-expected-output-subpackage-dependence.txt");

	private NoSubpackageDependenceRule rule;
	private EnforcerRuleHelperMock helper;

	@Before
	public void setUp() throws Exception {
		rule = new NoSubpackageDependenceRule();
		helper = new EnforcerRuleHelperMock();
	}

	@Test
	public void fitnesseIntegrationTest() throws Exception {
		assertPackageCycles(FITNESSE_TARGET_FOLDER, FITNESSE_EXPECTED_OUTPUT);
	}

	@Test
	public void junitIntegrationTest() throws Exception {
		assertPackageCycles(JUNIT_TARGET_FOLDER, JUNIT_EXPECTED_OUTPUT);
	}

	private void assertPackageCycles(URL targetFolder, URL expectedOutput) throws URISyntaxException, IOException {
		helper.setTestClassesDir(new File("non-existent"));
		helper.setClassesDir(new File(targetFolder.toURI()));
		try {
			rule.execute(helper);
			fail("expected EnforcerRuleException");
		} catch (EnforcerRuleException e) {
			// using assertEquals to get a nice comparison editor in eclipse
			assertEquals(getExpectedOutput(expectedOutput), e.getMessage());
		}
	}

	private String getExpectedOutput(URL expectedOutput) throws IOException {
		return IOUtils.toString(expectedOutput.openStream()).replaceAll("\r", "");
	}

	private static URL getResource(String path) {
		return Thread.currentThread().getContextClassLoader().getResource(path);
	}
}
