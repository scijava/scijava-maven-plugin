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
package org.scijava.maven.plugin;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Test;
import org.scijava.maven.plugin.util.PomEditor;

/**
 * A couple of test to verify that POM rewriting works as expected.
 * 
 * @author Johannes Schindelin
 */
public class PomEditorTest {

	private final static String example =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
			"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
			"xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " +
			"http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
			"\t<modelVersion>4.0.0</modelVersion>\n" +
			"\n" +
			"\t<parent>" +
			"\t\t<groupId>org.scijava</groupId>\n" +
			"\t\t<artifactId>pom-scijava</artifactId>\n" +
			"\t\t<version>2.22</version>\n" +
			"\t\t<relativePath />\n" +
			"\t</parent>\n" +
			"</project>\n";

	@Test
	public void retainWhitespace() throws Exception {
		final InputStream in = new ByteArrayInputStream(example.getBytes());
		final PomEditor editor = new PomEditor(in);
		final StringWriter writer = new StringWriter();
		editor.write(writer);
		assertEquals(example, writer.toString());
	}

}
