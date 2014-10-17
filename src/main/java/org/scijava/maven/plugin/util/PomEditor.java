/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 Board of Regents of the University of
 * Wisconsin-Madison.
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
package org.scijava.maven.plugin.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An editor to modify <tt>pom.xml</tt> files.
 * 
 * @author Johannes Schindelin
 */
public class PomEditor {

	private final Document doc;
	private final String projectTag;
	private final Log log;
	private XPath xpath;

	/**
	 * Parses the specified <tt>pom.xml</tt> file.
	 * 
	 * @param file the <tt>pom.xml</tt> file
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public PomEditor(final InputStream inputStream, final Log log) throws ParserConfigurationException, SAXException, IOException {
		this.log = log;
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setCoalescing(false);
		factory.setExpandEntityReferences(false);
		factory.setIgnoringComments(false);
		factory.setIgnoringElementContentWhitespace(false);
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final ProjectTagExtractor in = new ProjectTagExtractor(inputStream);
		doc = builder.parse(in);
		projectTag = in.toString();
	}

	/**
	 * Writes out the modified <tt>pom.xml</tt>
	 * 
	 * @param file the <tt>pom.xml</tt> file
	 * @throws DOMException
	 * @throws IOException
	 */
	public void write(final Writer writer) throws DOMException, IOException {
		write(writer, doc.getDocumentElement());
		writer.write("\n");
		writer.close();
	}

	/**
	 * Inspect all referenced groupId/artifactId/version triplets, allowing to re-set the version.
	 * 
	 * @return the number of modifications
	 * @throws XPathExpressionException
	 * @throws MojoExecutionException
	 */
	public int visitVersions(final VersionVisitor visitor) throws XPathExpressionException, MojoExecutionException {
		int modified = 0;

		final String parentGroupId = cdata("//project/parent/groupId");
		if (parentGroupId != null) {
			final String artifactId = cdata("//project/parent/artifactId");
			final NodeList versions = xpath("//project/parent/version");
			if (versions == null || versions.getLength() != 1) {
				throw new MojoExecutionException("Could not find parent version");
			}
			final Node node = versions.item(0);
			final String version = node.getTextContent();
			final String newVersion = visitor.visit(parentGroupId, artifactId, version);
			if (!version.equals(newVersion)) {
				log.info(parentGroupId + ":" + artifactId + ":" + version + " -> " + newVersion);
				node.setTextContent(newVersion);
				modified++;
			}
		}

		final NodeList properties = xpath("//project/properties/*");
		OUTER:
		for (int i = 0; i < properties.getLength(); i++) {
			final Node node = properties.item(i);
			switch (node.getNodeType()) {
			case Node.COMMENT_NODE:
				if (node.getTextContent().contains("BEGIN MANUALLY MANAGED VERSIONS")) {
					break OUTER;
				}
				break;
			case Node.ELEMENT_NODE:
				final String propertyName = node.getNodeName();
				if (propertyName == null || !propertyName.endsWith(".version")) break;
				String artifactId = propertyName.substring(0,
					propertyName.length() - ".version".length());
				if ("imagej1".equals(artifactId)) artifactId = "ij";
				final String search = "[artifactId[.='" + artifactId + "']]/groupId";
				NodeList groupIdNodes = xpath("//project/dependencyManagement/dependencies/dependency" + search);
				if (groupIdNodes == null || groupIdNodes.getLength() == 0) {
					groupIdNodes = xpath("//project/dependencies/dependency" + search);
					if (groupIdNodes == null || groupIdNodes.getLength() == 0) {
						groupIdNodes = xpath("//project/build/pluginManagement/plugins/plugin" + search);
						if (groupIdNodes == null || groupIdNodes.getLength() == 0) {
							groupIdNodes = xpath("//project/build/plugins/plugin" + search);
						}
					}
				}
				if (groupIdNodes == null || groupIdNodes.getLength() == 0) {
					log.warn("Could not determine groupId for artifactId '" + artifactId + "'; Skipping");
					continue;
				}
				final String groupId = groupIdNodes.item(0).getTextContent();
				final String version = node.getTextContent();
				final String newVersion = visitor.visit(groupId, artifactId, version);
				if (!version.equals(newVersion)) {
					log.info(groupId + ":" + artifactId + ":" + version + " -> " + newVersion);
					node.setTextContent(newVersion);
					modified++;
				}
				break;
			}
		}

		return modified;
	}

	private String cdata(final String expression) throws XPathExpressionException {
		final NodeList nodes = xpath(expression);
		if (nodes == null || nodes.getLength() == 0) return null;

		final NodeList children = nodes.item(0).getChildNodes();
		if (children == null || children.getLength() == 0) return null;
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			if (child.getNodeType() != Node.TEXT_NODE) continue;
			return child.getNodeValue();
		}
		return null;
	}

	private NodeList xpath(final String expression) throws XPathExpressionException {
		return (NodeList) xpath().evaluate(expression, doc, XPathConstants.NODESET);
	}

	private synchronized XPath xpath() {
		if (xpath == null) xpath = XPathFactory.newInstance().newXPath();
		return xpath;
	}

	private static class ProjectTagExtractor extends FilterInputStream {

		private final static byte[] magic = "<project".getBytes();

		private int counter;
		private final StringBuilder builder = new StringBuilder();

		private ProjectTagExtractor(InputStream in) {
			super(in);
		}

		@Override
		public int read() throws IOException {
			int result = super.read();
			if (result > 0) handle((byte) result);
			return result;
		}

		@Override
		public int read(byte[] buffer) throws IOException {
			int result = super.read(buffer);
			for (int i = 0; i < result; i++) handle(buffer[i]);
			return result;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			int result = super.read(buffer, offset, length);
			for (int i = 0; i < result; i++) handle(buffer[offset + i]);
			return result;
		}

		@Override
		public String toString() {
			return builder.toString();
		}

		private void handle(byte ch) {
			if (counter < 0) return;
			if (counter < magic.length) {
				if (ch == magic[counter]) {
					counter++;
				} else {
					// reset
					counter = 0;
				}
			} else if (ch == '>') {
				// stop extracting
				counter = -1;
			}
			builder.append((char) ch);
		}
	}

	private void write(final Writer writer, final Node node) throws DOMException, IOException {
		if (node.getNodeType() == Node.TEXT_NODE) {
			writer.write(node.getTextContent());
		} else if (node.getNodeType() == Node.COMMENT_NODE) {
			writer.write("<!--" + node.getTextContent() + "-->");
		} else if (node.hasChildNodes()) {
			final String name = node.getNodeName();
			if ("project".equals(name)) writer.write(projectTag);
			else writer.write("<" + name + ">");
			for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
				write(writer, child);
			}
			writer.write("</" + name + ">");
		} else {
			writer.write("<" + node.getNodeName() + " />");
		}
	}


}