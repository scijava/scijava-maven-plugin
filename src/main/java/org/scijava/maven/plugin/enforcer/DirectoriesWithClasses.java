
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

import static java.util.Collections.unmodifiableList;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

public class DirectoriesWithClasses implements Iterable<File>{

	public static final String MAVEN_PROJECT_BUILD_OUTPUT_DIRECTORY_VAR = "${project.build.outputDirectory}";
	public static final String MAVEN_PROJECT_BUILD_TEST_OUTPUT_DIRECTORY_VAR = "${project.build.testOutputDirectory}";

	private final List<File> directories = new LinkedList<File>();
	
	public DirectoriesWithClasses(EnforcerRuleHelper helper, String rule, boolean includeTests) throws ExpressionEvaluationException {
		addDirectoryIfExists(helper, MAVEN_PROJECT_BUILD_OUTPUT_DIRECTORY_VAR, rule);
		if (includeTests) {
			addDirectoryIfExists(helper, MAVEN_PROJECT_BUILD_TEST_OUTPUT_DIRECTORY_VAR, rule);
		}
	}

	private void addDirectoryIfExists(EnforcerRuleHelper helper, String variable, String rule)
			throws ExpressionEvaluationException {
		File directory = new File((String) helper.evaluate(variable));
		if(directory.exists()) {
			helper.getLog().info("Adding directory " + directory.getAbsolutePath() + " for " + rule + " search.");
			directories.add(directory);
		} else {			
			helper.getLog().info("Directory " + directory.getAbsolutePath() + " could not be found.");
		}
	}

	public boolean directoriesWithClassesFound() {
		return !directories.isEmpty();
	}

	@Override
	public Iterator<File> iterator() {
		return unmodifiableList(directories).iterator();
	}
}
