
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
	
	public File alignToBaseDirectory(File arg0) {
		return null;
	}

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

	public Object getComponent(@SuppressWarnings("rawtypes") Class arg0) throws ComponentLookupException {
		return null;
	}

	public Object getComponent(String arg0) throws ComponentLookupException {
		return null;
	}

	public Object getComponent(String arg0, String arg1) throws ComponentLookupException {
		return null;
	}

	public List<?> getComponentList(String arg0) throws ComponentLookupException {
		return null;
	}

	public Map<?, ?> getComponentMap(String arg0) throws ComponentLookupException {
		return null;
	}

	public PlexusContainer getContainer() {
		return null;
	}

	public Log getLog() {
		return logMock;
	}

}
