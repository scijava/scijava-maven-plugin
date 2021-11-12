package org.scijava.maven.plugin.enforcer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.Before;
import org.junit.Test;

public class NoSubpackageDependenceRuleIntegrationTest {
	private static final URL FITNESSE_TARGET_FOLDER = getResource("fitnesse-target");
	private static final URL FITNESSE_EXPECTED_OUTPUT = getResource("fitnesse-expected-output-subpackage-dependence.txt");
	private static final URL JUNIT_TARGET_FOLDER = getResource("junit-target");
	private static final URL JUNIT_EXPECTED_OUTPUT = getResource("junit-expected-output-subpackage-dependence.txt");

	private NoSubpackageDependenceRule rule;
	private EnforcerRuleHelperMock helper;

	@Before
	public void setUp() throws Exception {
		rule = new NoSubpackageDependenceRule();
		helper = new EnforcerRuleHelperMock();
	}

	@Test
	public void fitnesseIntegrationTest() throws Exception {
		assertPackageCycles(FITNESSE_TARGET_FOLDER, FITNESSE_EXPECTED_OUTPUT);
	}

	@Test
	public void junitIntegrationTest() throws Exception {
		assertPackageCycles(JUNIT_TARGET_FOLDER, JUNIT_EXPECTED_OUTPUT);
	}

	private void assertPackageCycles(URL targetFolder, URL expectedOutput) throws URISyntaxException, IOException {
		helper.setTestClassesDir(new File("non-existent"));
		helper.setClassesDir(new File(targetFolder.toURI()));
		try {
			rule.execute(helper);
			fail("expected EnforcerRuleException");
		} catch (EnforcerRuleException e) {
			// using assertEquals to get a nice comparison editor in eclipse
			assertEquals(getExpectedOutput(expectedOutput), e.getMessage());
		}
	}

	private String getExpectedOutput(URL expectedOutput) throws IOException {
		return IOUtils.toString(expectedOutput.openStream()).replaceAll("\r", "");
	}

	private static URL getResource(String path) {
		return Thread.currentThread().getContextClassLoader().getResource(path);
	}
}
