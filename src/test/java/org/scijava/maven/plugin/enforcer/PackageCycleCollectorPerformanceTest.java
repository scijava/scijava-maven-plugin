
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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdepend.framework.JavaPackage;

import org.junit.Before;
import org.junit.Test;

public class PackageCycleCollectorPerformanceTest {
	private static final int PACKAGE_MATRIX_SIZE = 20;
	private List<JavaPackage> allPackages;

	@Before
	public void setUp() {
		allPackages = new ArrayList<JavaPackage>();
	}

	@Test
	public void collectCycles_PerformanceTest() throws Exception {
		initializePackages();
		List<Set<JavaPackage>> cycles = new PackageCycleCollector().collectCycles(allPackages);
		assertThat(cycles, is(empty()));
	}

	private void initializePackages() {
		JavaPackage rootPackage = createPackage("root");
		Set<JavaPackage> lastPackageLayer = Collections.singleton(rootPackage);
		for (int row = 0; row < PACKAGE_MATRIX_SIZE; row++) {
			lastPackageLayer = createNextPackageLayer(lastPackageLayer, row);
		}
	}

	private Set<JavaPackage> createNextPackageLayer(Set<JavaPackage> lastPackageLayer, int row) {
		Set<JavaPackage> nextPackageLayer = new HashSet<JavaPackage>();
		for (int column = 0; column < PACKAGE_MATRIX_SIZE; column++) {
			JavaPackage layerPackage = createPackage("layer." + row + "." + column);
			nextPackageLayer.add(layerPackage);
			for (JavaPackage javaPackage : lastPackageLayer) {
				javaPackage.dependsUpon(layerPackage);
			}
		}
		return nextPackageLayer;
	}

	private JavaPackage createPackage(String packageName) {
		JavaPackage javaPackage = new JavaPackage(packageName);
		allPackages.add(javaPackage);
		return javaPackage;
	}
}
