
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.scijava.maven.plugin.enforcer.CollectionOutput.joinArray;
import static org.scijava.maven.plugin.enforcer.CollectionOutput.joinCollection;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.scijava.maven.plugin.enforcer.CollectionOutput.Appender;
import org.scijava.maven.plugin.enforcer.CollectionOutput.StringProvider;

public class CollectionOutputTest {
	private final DummyAppender appender = new DummyAppender();
	private static final DummyStringProvider STRING_PROVIDER = new DummyStringProvider();
	private static final String SEPARATOR = ", ";

	private class DummyAppender implements Appender<String> {
		public void append(String value) {
			output.append(value);
		}
	}

	private static class DummyStringProvider implements StringProvider<String> {
		public String provide(String value) {
			return value;
		}
	}

	private StringBuilder output;

	@Before
	public void setUp() {
		output = new StringBuilder();
	}

	@Test
	public void joinCollection_WithStringProvider_WithEmptyCollection() throws Exception {
		assertThat(joinCollection(Collections.<String> emptyList(), STRING_PROVIDER, SEPARATOR), is(""));
	}

	@Test
	public void joinCollection_WithStringProvider_WithSingletonCollection() throws Exception {
		assertThat(joinCollection(singletonList("entry1"), STRING_PROVIDER, SEPARATOR), is("entry1"));
	}

	@Test
	public void joinCollection_WithStringProvider_WithCollectionHavingMultipleEntries() throws Exception {
		assertThat(joinCollection(asList("entry1", "entry2"), STRING_PROVIDER, SEPARATOR), is("entry1, entry2"));
	}

	@Test
	public void joinArray_WithStringProvider_WithEmptyArray() throws Exception {
		assertThat(joinArray(new String[0], STRING_PROVIDER, SEPARATOR), is(""));
	}

	@Test
	public void joinArray_WithStringProvider_WithSingletonArray() throws Exception {
		assertThat(joinArray(new String[] { "entry1" }, STRING_PROVIDER, SEPARATOR), is("entry1"));
	}

	@Test
	public void joinArray_WithStringProvider_WithArrayHavingMultipleEntries() throws Exception {
		assertThat(joinArray(new String[] { "entry1", "entry2" }, STRING_PROVIDER, SEPARATOR), is("entry1, entry2"));
	}

	@Test
	public void joinCollection_WithAppender_WithEmptyCollection() throws Exception {
		joinCollection(Collections.<String> emptyList(), output, appender, SEPARATOR);
		assertThat(output.toString(), is(""));
	}

	@Test
	public void joinCollection_WithAppender_WithSingletonCollection() throws Exception {
		joinCollection(singletonList("entry1"), output, appender, SEPARATOR);
		assertThat(output.toString(), is("entry1"));
	}

	@Test
	public void joinCollection_WithAppender_WithCollectionHavingMultipleEntries() throws Exception {
		joinCollection(asList("entry1", "entry2"), output, appender, SEPARATOR);
		assertThat(output.toString(), is("entry1, entry2"));
	}

	@Test
	public void joinCollection_WithAppender_WithNewlineSeparator() throws Exception {
		joinCollection(asList("entry1", "entry2"), output, appender, "\n");
		assertThat(output.toString(), is("entry1\nentry2"));
	}

}
