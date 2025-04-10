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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class EnforcerRuleHelperMock implements EnforcerRuleHelper {

	private File classesDir;
	private File testClassesDir;
	private boolean evaluateThrowsException;
	private final LogMock logMock = new LogMock();

	public LogMock getLogMock() {
		return logMock;
	}

	public void setEvaluateThrowsException(boolean evaluateThrowsException) {
		this.evaluateThrowsException = evaluateThrowsException;
	}

	public void setClassesDir(File targetDir) {
		this.classesDir = targetDir;
	}

	public void setTestClassesDir(File testClassesDir) {
		this.testClassesDir = testClassesDir;
	}
	
	@Override
	public File alignToBaseDirectory(File arg0) {
		return null;
	}

	@Override
	public Object evaluate(String variable) throws ExpressionEvaluationException {
		if (evaluateThrowsException) {
			throw new ExpressionEvaluationException("");
		}
		if (DirectoriesWithClasses.MAVEN_PROJECT_BUILD_OUTPUT_DIRECTORY_VAR.equals(variable)) {
			return classesDir.getPath();
		}
		if (DirectoriesWithClasses.MAVEN_PROJECT_BUILD_TEST_OUTPUT_DIRECTORY_VAR.equals(variable)) {
			return testClassesDir.getPath();
		}
		return null;
	}

	@Override
	public Object getCache(String s, Supplier<?> supplier) {
		return null;
	}

	@Override
	public <T> T getComponent(Class<T> arg0) throws ComponentLookupException {
		return null;
	}

	@Override
	public Object getComponent(String arg0) throws ComponentLookupException {
		return null;
	}

	@Override
	public Object getComponent(String arg0, String arg1) throws ComponentLookupException {
		return null;
	}

	@Override
	public <T> T getComponent(Class<T> clazz, String roleHint) throws ComponentLookupException {
		return null;
	}

	@Override
	public List<?> getComponentList(String arg0) throws ComponentLookupException {
		return null;
	}

	@Override
	public Map<String, ?> getComponentMap(String arg0) throws ComponentLookupException {
		return null;
	}

	@Override
	public PlexusContainer getContainer() {
		return null;
	}

	@Override
	public Log getLog() {
		return logMock;
	}

}
