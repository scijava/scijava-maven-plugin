
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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class LogMock implements Log {
	private final List<String> info = new ArrayList<String>();

	public List<String> getInfo() {
		return info;
	}

	public void debug(CharSequence arg0) {
	}

	public void debug(Throwable arg0) {
	}

	public void debug(CharSequence arg0, Throwable arg1) {
	}

	public void error(CharSequence arg0) {
	}

	public void error(Throwable arg0) {
	}

	public void error(CharSequence arg0, Throwable arg1) {
	}

	public void info(CharSequence arg0) {
		info.add(arg0.toString());
	}

	public void info(Throwable arg0) {
	}

	public void info(CharSequence arg0, Throwable arg1) {
	}

	public boolean isDebugEnabled() {
		return false;
	}

	public boolean isErrorEnabled() {
		return false;
	}

	public boolean isInfoEnabled() {
		return false;
	}

	public boolean isWarnEnabled() {
		return false;
	}

	public void warn(CharSequence arg0) {
	}

	public void warn(Throwable arg0) {
	}

	public void warn(CharSequence arg0, Throwable arg1) {
	}

}
