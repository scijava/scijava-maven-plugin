/*-
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import jdepend.framework.JDepend;
import jdepend.framework.PackageFilter;

/**
 * Common functionality for writing an {@link EnforcerRule} dealing with the
 * package hierarchy.
 * 
 * @author Ben Romberg
 * @author David Burkhart
 * @author Gabriel Selzer
 */
public abstract class AbstractPackageEnforcementRule implements EnforcerRule {

	private List<String> excludedPackages = new ArrayList<>();
	private List<String> includedPackages = new ArrayList<>();
	private boolean includeTests = true;

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		try {
			executePackageCycleCheckIfNecessary(helper);
		}
		catch (ExpressionEvaluationException e) {
			throw new EnforcerRuleException("Unable to lookup an expression " + e
				.getLocalizedMessage(), e);
		}
		catch (IOException e) {
			throw new EnforcerRuleException("Unable to access target directory " + e
				.getLocalizedMessage(), e);
		}
	}

	@Override
	public String getCacheId() {
		return "";
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public boolean isResultValid(EnforcerRule arg0) {
		return false;
	}

	public void setExcludedPackages(List<String> excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	public void setIncludedPackages(List<String> includedPackages) {
		this.includedPackages = includedPackages;
	}

	public void setIncludeTests(boolean includeTests) {
		this.includeTests = includeTests;
	}

	private void executePackageCheck(EnforcerRuleHelper helper,
		Iterable<File> directories) throws IOException, EnforcerRuleException
	{
		JDepend jdepend = createJDepend(helper);
		for (File directory : directories) {
			jdepend.addDirectory(directory.getAbsolutePath());
		}
		jdepend.analyze();

		enforceRule(jdepend);
	}

	private void executePackageCycleCheckIfNecessary(EnforcerRuleHelper helper)
		throws ExpressionEvaluationException, IOException, EnforcerRuleException
	{
		DirectoriesWithClasses directories = new DirectoriesWithClasses(helper,
			"package cycles", includeTests);
		if (directories.directoriesWithClassesFound()) {
			executePackageCheck(helper, directories);
		}
		else {
			helper.getLog().info(
				"No directories with classes to check for cycles found.");
		}
	}

	protected JDepend createJDepend(EnforcerRuleHelper helper) {
		if (!includedPackages.isEmpty()) {
			helper.getLog().warn(
				"Package cycles rule check is restricted to check only these packages: " +
					includedPackages);
		}
		if (!excludedPackages.isEmpty()) {
			helper.getLog().warn(
				"These packages were excluded from package cycle rule check: " +
					excludedPackages);
		}
		return new JDepend(PackageFilter.all().including(includedPackages)
			.excluding(excludedPackages));
	}

	/**
	 * Enforces the rule
	 * 
	 * @param jdepend a {@link JDepend} containing relevant package information
	 * @throws EnforcerRuleException iff the rule is invalid for the packages
	 *           described by {@code jdepend}
	 */
	abstract void enforceRule(JDepend jdepend) throws EnforcerRuleException;

}
