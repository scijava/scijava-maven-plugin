
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

public class NoSubpackageDependenceRule extends AbstractPackageEnforcementRule {

	@Override
	void enforceRule(JDepend jdepend) throws EnforcerRuleException {
		Map<JavaPackage, List<JavaPackage>> subpackageLists = new HashMap<>();
		for (JavaPackage p : jdepend.getPackages()) {
			// create running List of subpackages, evaluate all previously added to
			// the Map
			List<JavaPackage> subpackages = new ArrayList<>();
			for (JavaPackage other : subpackageLists.keySet()) {
				String dString = p.getName();
				String oString = other.getName();
				if (dString == oString) continue;
				if (dString.startsWith(oString)) subpackageLists.get(other).add(p);
				if (oString.startsWith(dString)) subpackages.add(other);
			}
			subpackageLists.put(p, subpackages);
		}
		Map<JavaPackage, List<JavaPackage>> subpackageDependence = new HashMap<>();
		for (Entry<JavaPackage, List<JavaPackage>> e : subpackageLists.entrySet()) {
			Collection<JavaPackage> efferents = e.getKey().getEfferents();
			List<JavaPackage> subpackagesDependedUpon = efferents.stream().filter(e
				.getValue()::contains).collect(Collectors.toList());
			subpackageDependence.put(e.getKey(), subpackagesDependedUpon);
		}
		for (List<JavaPackage> l : subpackageDependence.values()) {
			if (!l.isEmpty()) {
				throw new EnforcerRuleException("Some packages depend on subpackages:" +
					new SubpackageDependenceOutput(subpackageDependence).getOutput());
			}
		}
	}

}
