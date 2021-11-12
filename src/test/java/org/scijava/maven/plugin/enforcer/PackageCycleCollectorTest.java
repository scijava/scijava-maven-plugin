
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import jdepend.framework.JavaPackage;

import org.junit.Before;
import org.junit.Test;

public class PackageCycleCollectorTest {

	private JavaPackage packageA;
	private JavaPackage packageB;
	private JavaPackage packageC;
	private JavaPackage packageD;
	private JavaPackage packageE;
	private PackageCycleCollector collector;
	private List<JavaPackage> packages;

	@Before
	public void setUp() {
		packageA = new JavaPackage("packageA");
		packageB = new JavaPackage("packageB");
		packageC = new JavaPackage("packageC");
		packageD = new JavaPackage("packageD");
		packageE = new JavaPackage("packageE");
		packages = Arrays.asList(packageA, packageB, packageC, packageD, packageE);
		collector = new PackageCycleCollector();
	}

	@Test
	public void collectCycles_NoCycle() throws Exception {
		packageA.dependsUpon(packageB);
		assertCyclesWith(packageA);
	}

	@Test
	public void collectCycles_HasCycle() throws Exception {
		packageA.dependsUpon(packageB);
		packageB.dependsUpon(packageA);
		assertCyclesWith(packageA, packageA, packageB);
	}

	@Test
	public void collectCycles_HasCycleWithThreePackages() throws Exception {
		packageA.dependsUpon(packageB);
		packageB.dependsUpon(packageC);
		packageC.dependsUpon(packageA);
		assertCyclesWith(packageA, packageA, packageB, packageC);
	}

	@Test
	public void collectCycles_HasCycleWithMultiplePaths() throws Exception {
		packageA.dependsUpon(packageB);
		packageB.dependsUpon(packageC);
		packageC.dependsUpon(packageA);
		packageD.dependsUpon(packageB);
		packageB.dependsUpon(packageD);
		packageB.dependsUpon(packageE);
		packageE.dependsUpon(packageC);
		assertCyclesWith(packageA, packageA, packageB, packageC, packageD, packageE);
	}

	@Test
	public void collectCycles_HasCycleWithDependendPackageNotInCycle() throws Exception {
		packageA.dependsUpon(packageB);
		packageB.dependsUpon(packageA);
		packageC.dependsUpon(packageA);
		assertCyclesWith(packageC, packageA, packageB);
	}

	private void assertCyclesWith(JavaPackage rootPackage, JavaPackage... javaPackages) {
		if (javaPackages.length == 0) {
			assertThat(collector.collectCycles(packages), is(empty()));
			return;
		}
		assertThat(collector.collectCycles(packages), hasSize(1));
		assertThat(collector.collectCycles(packages).get(0), containsInAnyOrder(javaPackages));
	}

}
