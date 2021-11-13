
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CollectionOutput {

	public static <T> String joinCollection(Collection<T> collection, final StringProvider<T> stringProvider,
			String separator) {
		final StringBuilder output = new StringBuilder();
		joinCollection(collection, output, new Appender<T>() {
			public void append(T value) {
				output.append(stringProvider.provide(value));
			}
		}, separator);
		return output.toString();
	}

	public static <T> String joinArray(T[] array, StringProvider<T> stringProvider, String separator) {
		@SuppressWarnings("unchecked")
		List<T> list = Arrays.asList(array);
		return joinCollection(list, stringProvider, separator);
	}

	public static <T> void joinCollection(Collection<T> collection, StringBuilder output, Appender<T> appender,
			String separator) {
		boolean first = true;
		for (T element : collection) {
			if (!first) {
				output.append(separator);
			}
			appender.append(element);
			first = false;
		}
	}

	public interface StringProvider<T> {
		String provide(T value);
	}

	public interface Appender<T> {
		void append(T value);
	}
}
