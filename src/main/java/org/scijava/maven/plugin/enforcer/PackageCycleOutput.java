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

import static org.scijava.maven.plugin.enforcer.CollectionOutput.joinCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.scijava.maven.plugin.enforcer.CollectionOutput.Appender;
import org.scijava.maven.plugin.enforcer.CollectionOutput.StringProvider;

import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;

public class PackageCycleOutput {

	private List<JavaPackage> packages;
	private StringBuilder output;
	private PackageCycleCollector packageCycleCollector;

	public PackageCycleOutput(List<JavaPackage> packages) {
		this.packages = packages;
		packageCycleCollector = new PackageCycleCollector();
	}

	public String getOutput() {
		output = new StringBuilder();
		for (List<JavaPackage> cycle : collectAndSortCycles()) {
			appendOutputForPackageCycle(cycle);
		}
		return output.toString();
	}

	private List<List<JavaPackage>> collectAndSortCycles() {
		List<Set<JavaPackage>> cycles = packageCycleCollector.collectCycles(packages);
		List<List<JavaPackage>> orderedCycles = new ArrayList<List<JavaPackage>>();
		for (Set<JavaPackage> cycle : cycles) {
			ArrayList<JavaPackage> cycleAsList = new ArrayList<JavaPackage>(cycle);
			Collections.sort(cycleAsList, JavaPackageNameComparator.INSTANCE);
			orderedCycles.add(cycleAsList);
		}
		Collections.sort(orderedCycles, JavaPackageListComparator.INSTANCE);
		return orderedCycles;
	}

	private void appendOutputForPackageCycle(List<JavaPackage> cyclicPackages) {
		packages.removeAll(cyclicPackages);
		appendHeaderForPackageCycle(cyclicPackages);
		for (JavaPackage cyclicPackage : cyclicPackages) {
			appendOutputForPackage(cyclicPackage, cyclicPackages);
		}
	}

	private void appendHeaderForPackageCycle(List<JavaPackage> cyclicPackages) {
		output.append("\n\n").append("Package-cycle found involving ");
		output.append(joinCollection(cyclicPackages, new StringProvider<JavaPackage>() {
			public String provide(JavaPackage javaPackage) {
				return javaPackage.getName();
			}
		}, ", "));
		output.append(':');
	}

	private void appendOutputForPackage(final JavaPackage javaPackage, List<JavaPackage> cyclicPackages) {
		output.append("\n    ").append(javaPackage.getName()).append(" depends on:");
		for (JavaPackage cyclicPackage : cyclicPackages) {
			appendOutputForCyclicPackage(javaPackage, cyclicPackage);
		}
	}

	private void appendOutputForCyclicPackage(final JavaPackage javaPackage, JavaPackage cyclicPackage) {
		if (javaPackage.equals(cyclicPackage)) {
			return;
		}
		List<JavaClass> dependentClasses = getOrderedDependentClasses(javaPackage, cyclicPackage);
		if (!dependentClasses.isEmpty()) {
			appendOutputForDependentCyclicPackage(javaPackage, cyclicPackage, dependentClasses);
		}
	}

	private List<JavaClass> getOrderedDependentClasses(JavaPackage javaPackage, JavaPackage cyclicPackage) {
		List<JavaClass> dependentClasses = new ArrayList<JavaClass>();
		Collection<JavaClass> allClasses = javaPackage.getClasses();
		for (JavaClass javaClass : allClasses) {
			if (javaClass.getImportedPackages().contains(cyclicPackage)) {
				dependentClasses.add(javaClass);
			}
		}
		Collections.sort(dependentClasses, JavaClassNameComparator.INSTANCE);
		return dependentClasses;
	}

	private void appendOutputForDependentCyclicPackage(JavaPackage javaPackage, JavaPackage cyclicPackage,
			List<JavaClass> dependentClasses) {
		output.append("\n        ").append(cyclicPackage.getName()).append(" (");
		appendOutputForCyclicPackageClasses(javaPackage, cyclicPackage, dependentClasses);
		output.append(")");
	}

	private void appendOutputForCyclicPackageClasses(JavaPackage javaPackage, final JavaPackage cyclicPackage,
			List<JavaClass> dependentClasses) {
		joinCollection(dependentClasses, output, new Appender<JavaClass>() {
			public void append(JavaClass packageClass) {
				output.append(packageClass.getName().substring(packageClass.getPackageName().length() + 1));
			}
		}, ", ");
	}

}
