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

package org.scijava.maven.plugin.enforcer;

import static org.scijava.maven.plugin.enforcer.CollectionOutput.joinCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.scijava.maven.plugin.enforcer.CollectionOutput.Appender;

import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;

/**
 * TODO
 *
 * @author Gabriel Selzer
 */
public class SubpackageDependenceOutput {

	private Map<JavaPackage, List<JavaPackage>> packages;
	private StringBuilder output;

	public SubpackageDependenceOutput(Map<JavaPackage, List<JavaPackage>> packages) {
		this.packages = packages;
	}

	public String getOutput() {
		output = new StringBuilder();
		for (Map.Entry<JavaPackage, List<JavaPackage>> e : collectAndSortCycles()) {
			if (!e.getValue().isEmpty()) appendOutputForSubpackageDependence(e);
		}
		return output.toString();
	}

	private List<Map.Entry<JavaPackage, List<JavaPackage>>> collectAndSortCycles() {
		List<Map.Entry<JavaPackage, List<JavaPackage>>> orderedList = new ArrayList<>();
		for (Map.Entry<JavaPackage, List<JavaPackage>> e : packages.entrySet()) {
			Collections.sort(e.getValue(), JavaPackageNameComparator.INSTANCE);
			orderedList.add(e);
		}
		Collections.sort(orderedList, (e1, e2) -> JavaPackageNameComparator.INSTANCE.compare(e1.getKey(), e2.getKey()));
		return orderedList;
	}

	private void appendOutputForSubpackageDependence(Map.Entry<JavaPackage, List<JavaPackage>> subpackageDependence) {
		JavaPackage p = subpackageDependence.getKey();
		packages.remove(p);
		appendHeaderForSubpackageDependence(subpackageDependence);
		for (JavaPackage cyclicPackage : subpackageDependence.getValue()) {
			appendOutputForSubPackage(p, cyclicPackage);
		}
	}

	private void appendHeaderForSubpackageDependence(Map.Entry<JavaPackage, List<JavaPackage>> entry) {
		output.append("\n\n").append("Package " + entry.getKey().getName() + " depends on subpackages: ");
	}

	private void appendOutputForSubPackage(final JavaPackage javaPackage, JavaPackage subpackage) {
		if (javaPackage.equals(subpackage)) {
			return;
		}
		List<JavaClass> dependentClasses = getOrderedDependentClasses(javaPackage, subpackage);
		if (!dependentClasses.isEmpty()) {
			appendOutputForDependentCyclicPackage(subpackage, dependentClasses);
		}
	}

	private List<JavaClass> getOrderedDependentClasses(JavaPackage javaPackage, JavaPackage cyclicPackage) {
		List<JavaClass> dependentClasses = new ArrayList<>();
		Collection<JavaClass> allClasses = javaPackage.getClasses();
		for (JavaClass javaClass : allClasses) {
			if (javaClass.getImportedPackages().contains(cyclicPackage)) {
				dependentClasses.add(javaClass);
			}
		}
		Collections.sort(dependentClasses, JavaClassNameComparator.INSTANCE);
		return dependentClasses;
	}

	private void appendOutputForDependentCyclicPackage(JavaPackage cyclicPackage,
			List<JavaClass> dependentClasses) {
		output.append("\n        ").append(cyclicPackage.getName()).append(" (");
		appendOutputForCyclicPackageClasses(dependentClasses);
		output.append(")");
	}

	private void appendOutputForCyclicPackageClasses(List<JavaClass> dependentClasses) {
		joinCollection(dependentClasses, output, new Appender<JavaClass>() {
			@Override
			public void append(JavaClass packageClass) {
				output.append(packageClass.getName().substring(packageClass.getPackageName().length() + 1));
			}
		}, ", ");
	}

}
