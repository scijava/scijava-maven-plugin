/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
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

package org.scijava.maven.plugin;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;
import org.scijava.maven.plugin.util.PomEditor;
import org.scijava.maven.plugin.util.VersionVisitor;

/**
 * A couple of test to verify that POM rewriting works as expected.
 * 
 * @author Johannes Schindelin
 */
public class PomEditorTest {

	private static final Log log = new SystemStreamLog();

	private final static String example = "" + //
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
		"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " + //
		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + //
		"xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " + //
		"http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" + //
		"\t<modelVersion>4.0.0</modelVersion>\n" + //
		"\n" + //
		"\t<parent>" + //
		"\t\t<groupId>org.scijava</groupId>\n" + //
		"\t\t<artifactId>pom-scijava</artifactId>\n" + //
		"\t\t<version>2.22</version>\n" + //
		"\t\t<relativePath />\n" + //
		"\t</parent>\n" + //
		"\n" + //
		"\t<properties>\n" + //
		"\t\t<scijava-common.version>2.33.4</scijava-common.version>\n" + //
		"\t</properties>\n" + //
		"\n" + //
		"\t<dependencyManagement>\n" + //
		"\t\t<dependencies>\n" + //
		"\t\t\t<dependency>\n" + //
		"\t\t\t\t<groupId>io.scif</groupId>\n" + //
		"\t\t\t\t<artifactId>pom-scifio</artifactId>\n" + //
		"\t\t\t\t<version>1.11</version>\n" + //
		"\t\t\t\t<type>pom</type>\n" + //
		"\t\t\t\t<scope>import</scope>\n" + //
		"\t\t\t</dependency>\n" + //
		"\n" + //
		"\t\t\t<dependency>\n" + //
		"\t\t\t\t<groupId>org.scijava</groupId>\n" + //
		"\t\t\t\t<artifactId>scijava-common</artifactId>\n" + //
		"\t\t\t\t<version>${scijava-common.version}</version>\n" + //
		"\t\t\t</dependency>\n" + //
		"\t\t</dependencies>\n" + //
		"\t</dependencyManagement>\n" + //
		"\n" + //
		"\t<dependencies>\n" + //
		"\t\t<dependency>\n" + //
		"<!-- Intentionally funny order and formatting -->\n" + //
		"\t\t\t<version>4.12</version>\n" + //
		"\t\t\t<artifactId>junit</artifactId>\n" + //
		"\t\t\t <groupId>org.junit</groupId>\n" + //
		"\t\t</dependency>\n" + //
		"\t</dependencies>\n" + //
		"</project>\n";

	@Test
	public void retainWhitespace() throws Exception {
		final InputStream in = new ByteArrayInputStream(example.getBytes());
		final PomEditor editor = new PomEditor(in, log);
		final StringWriter writer = new StringWriter();
		editor.write(writer);
		assertEquals(example, writer.toString());
	}

	@Test
	public void visitVersions() throws Exception {
		final String[] gavs = { //
			"org.scijava:pom-scijava:2.22", //
				"org.scijava:scijava-common:2.33.4", //
				"io.scif:pom-scifio:1.11" //
			};
		final int[] counter = { 0 };
		final InputStream in = new ByteArrayInputStream(example.getBytes());
		final PomEditor editor = new PomEditor(in, log);
		final int modified = editor.visitVersions(new VersionVisitor() {

			@Override
			public String visit(final String groupId, final String artifactId,
				final String version)
			{
				assertEquals(gavs[counter[0]++], groupId + ":" + artifactId + ":" +
					version);
				return version.replace("11", "13").replace("22", "23").replace("33",
					"67");
			}

		});
		assertEquals(3, modified);
		assertEquals(gavs.length, counter[0]);

		final StringWriter writer = new StringWriter();
		editor.write(writer);
		assertEquals(example.replace("11", "13").replace("22", "23").replace("33",
			"67"), writer.toString());
	}

}
