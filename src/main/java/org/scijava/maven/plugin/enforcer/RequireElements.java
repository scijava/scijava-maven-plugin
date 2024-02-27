/*
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

package org.scijava.maven.plugin.enforcer;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This rule checks that particular XML elements are set in the project POM
 * (<em>not</em> inherited from an ancestor!).
 *
 * @author Curtis Rueden
 */
public class RequireElements implements EnforcerRule {

	/** The required elements. Must be given. */
	private String[] elements;

	private Document doc;
	private XPath xpath;

	private final String ruleName = //
		StringUtils.lowercaseFirstLetter(getClass().getSimpleName());

	// -- EnforcerRule methods --

	/**
	 * Execute the rule.
	 *
	 * @param helper the helper
	 * @throws EnforcerRuleException the enforcer rule exception
	 */
	@Override
	public void execute(final EnforcerRuleHelper helper)
		throws EnforcerRuleException
	{
		final Log log = helper.getLog();

		final MavenProject project = getMavenProject(helper);

		// Validate rule inputs.
		if (elements == null || elements.length == 0) {
			fail("no elements were specified");
		}
		for (final String element : elements) {
			if (element.matches("[^A-Za-z0-9_/]")) {
				fail("invalid character in element name '" + element + "'");
			}
		}

		// Locate the project's POM file.
		final File pomFile = project.getFile();
		if (pomFile == null) {
			fail("cannot locate project POM");
		}

		// Parse the project POM directly. We do this, rather than leaning on the
		// MavenProject, because we do _not_ want to interpolate the POM. Instead,
		// we want to know if this specific POM includes the requisite element.
		try {
			doc = loadXML(pomFile);
		}
		catch (final ParserConfigurationException exc) {
			log.warn("Cannot parse project POM", exc);
			return;
		}
		catch (final SAXException exc) {
			log.warn("Cannot parse project POM", exc);
			return;
		}
		catch (final IOException exc) {
			log.warn("Cannot parse project POM", exc);
			return;
		}

		xpath = XPathFactory.newInstance().newXPath();
		final StringBuilder errors = new StringBuilder();
		for (final String element : elements) {
			// Find matching element(s).
			final NodeList nodes = xpath("//project/" + element);
			if (nodes == null || nodes.getLength() <= 0) {
				errors.append("* " + element + ": element is missing\n");
				continue;
			}

			// Ensure first element has actual content beneath it.
			final String text = nodes.item(0).getTextContent();
			if (text == null || text.trim().isEmpty()) {
				errors.append("* " + element + ": element has no content\n");
			}
		}
		if (errors.length() > 0) {
			throw new EnforcerRuleException(
				"The following required elements have errors:\n" + errors);
		}
	}

	@Override
	public String getCacheId() {
		return "0";
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public boolean isResultValid(final EnforcerRule cachedRule) {
		return false;
	}

	// -- Helper methods --

	private void fail(final String message) throws EnforcerRuleException {
		throw new EnforcerRuleException(ruleName + ": " + message);
	}

	private MavenProject getMavenProject(final EnforcerRuleHelper helper)
		throws EnforcerRuleException
	{
		try {
			return (MavenProject) helper.evaluate("${project}");
		}
		catch (final ExpressionEvaluationException exc) {
			throw new EnforcerRuleException("Unable to get project.", exc);
		}
	}

	private NodeList xpath(final String expression) throws EnforcerRuleException {
		final Object o;
		try {
			o = xpath.evaluate(expression, doc, XPathConstants.NODESET);
		}
		catch (final XPathExpressionException exc) {
			throw new EnforcerRuleException("Cannot parse xpath expression", exc);
		}
		if (!(o instanceof NodeList)) {
			final String className = o == null ? "null" : o.getClass().getName();
			fail("Unexpected xpath result type: " + className);
		}
		return (NodeList) o;
	}

	private static Document loadXML(final File file)
		throws ParserConfigurationException, SAXException, IOException
	{
		return createBuilder().parse(file.getAbsolutePath());
	}

	private static DocumentBuilder createBuilder()
		throws ParserConfigurationException
	{
		return DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}
}
