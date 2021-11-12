
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

/**
 * Detects the presence of cycles in the package hierarchy.
 *
 * @author Gabriel Selzer
 */
public class NoPackageCyclesRule extends AbstractPackageEnforcementRule {

	private String getPackageCycles(JDepend jdepend) {
		Collection<JavaPackage> packages = jdepend.getPackages();
		return new PackageCycleOutput(new ArrayList<>(packages)).getOutput();
	}

	@Override
	void enforceRule(JDepend jdepend) throws EnforcerRuleException {
		if (jdepend.containsCycles()) {
			throw new EnforcerRuleException("There are package cycles:" +
				getPackageCycles(jdepend));
		}
	}

}
