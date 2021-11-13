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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.List;

import jdepend.framework.JDepend;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class NoPackageCyclesRuleTest {

	private class NoPackageCyclesRuleMock extends NoPackageCyclesRule {
		@Override
		protected JDepend createJDepend(EnforcerRuleHelper helper) {
			return jdependMock;
		}
	}

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private NoPackageCyclesRule rule;
	private JDependMock jdependMock;
	private EnforcerRuleHelperMock helper;

	@Before
	public void setUp() throws Exception {
		jdependMock = new JDependMock();
		rule = new NoPackageCyclesRuleMock();
		helper = new EnforcerRuleHelperMock();
		helper.setClassesDir(temporaryFolder.newFolder("classes"));
		helper.setTestClassesDir(temporaryFolder.newFolder("test-classes"));
	}

	@Test
	public void cacheId_IsEmpty() throws Exception {
		assertThat(rule.getCacheId(), is(""));
	}

	@Test
	public void isNot_cacheable() {
		assertThat(rule.isCacheable(), is(false));
	}

	@Test
	public void result_IsNotValid() {
		assertThat(rule.isResultValid(null), is(false));
	}

	@Test
	public void execute_checkNotNecessary_ClassesDirNotFound() throws Exception {
		File nonExistentClassesFolder = new File(temporaryFolder.getRoot(), "non-existent-classes-dir");
		helper.setClassesDir(nonExistentClassesFolder);
		File nonExistentTestClassesFolder = new File(temporaryFolder.getRoot(), "non-existent-test-classes-dir");
		helper.setTestClassesDir(nonExistentTestClassesFolder);
		rule.execute(helper);
		List<String> infoLogs = helper.getLogMock().getInfo();
		assertThat(infoLogs, hasSize(3));
		assertThat(infoLogs.get(0),	is("Directory " + nonExistentClassesFolder.getAbsolutePath() + " could not be found."));
		assertThat(infoLogs.get(1),	is("Directory " + nonExistentTestClassesFolder.getAbsolutePath() + " could not be found."));
		assertThat(infoLogs.get(2), is("No directories with classes to check for cycles found."));
	}

	@Test
	public void execute_MavenEvaluationFailed_ThrowsException() throws Exception {
		helper.setEvaluateThrowsException(true);
		expectedException.expect(EnforcerRuleException.class);
		expectedException.expectMessage(containsString("Unable to lookup an expression"));
		rule.execute(helper);
	}

	@Test
	public void execute_JdependAddDirectoryFailed_ThrowsException() throws Exception {
		jdependMock.setAddDirectoryThrowsException(true);
		expectedException.expect(EnforcerRuleException.class);
		expectedException.expectMessage(containsString("Unable to access target directory"));
		rule.execute(helper);
	}

	@Test
	public void execute_ContainsNoCycles() throws Exception {
		rule.execute(helper);
		List<String> infoLogs = helper.getLogMock().getInfo();
		assertThat(infoLogs, hasSize(2));
		assertThat(infoLogs.get(0), is("Adding directory " + classesFolder().getAbsolutePath() + " for package cycles search."));
		assertThat(infoLogs.get(1), is("Adding directory " + testClassesFolder().getAbsolutePath() + " for package cycles search."));
	}

	@Test
	public void execute_CanExcludeTests() throws Exception {
		rule.setIncludeTests(false);
		rule.execute(helper);
		List<String> infoLogs = helper.getLogMock().getInfo();
		assertThat(infoLogs, hasSize(1));
		assertThat(infoLogs.get(0), is("Adding directory " + classesFolder().getAbsolutePath() + " for package cycles search."));
	}

	@Test
	public void execute_ContainsNoCyclesWithoutTestClasses() throws Exception {
		File nonExistentTestClassesDir = new File(temporaryFolder.getRoot(), "does not exist");
		helper.setTestClassesDir(nonExistentTestClassesDir);
		rule.execute(helper);
		List<String> infoLogs = helper.getLogMock().getInfo();
		assertThat(infoLogs, hasSize(2));
		assertThat(infoLogs.get(0), is("Adding directory " + classesFolder().getAbsolutePath() + " for package cycles search."));
		assertThat(infoLogs.get(1), is("Directory " + nonExistentTestClassesDir.getAbsolutePath() + " could not be found."));
	}

	@Test
	public void execute_ContainsCycles() throws Exception {
		jdependMock.setContainsCycles(true);
		expectedException.expect(EnforcerRuleException.class);
		expectedException.expectMessage(containsString("There are package cycles"));
		rule.execute(helper);
	}

	private File testClassesFolder() {
		return new File(temporaryFolder.getRoot(), "test-classes");
	}

	private File classesFolder() {
		return new File(temporaryFolder.getRoot(), "classes");
	}
}
