/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2021 SciJava developers.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link NoSubpackageDependenceRule} on a dummy project
 * @author Gabriel Selzer
 */
public class NoSubpackageDependenceRuleIntegrationTest {
    // Subpackage dependence test files
    private static final URL SUBPACKAGE_DEPENDENT_TARGET_FOLDER = //
            getResource("subpackage-dependent-target");
    private static final URL SUBPACKAGE_DEPENDENT_EXPECTED_OUTPUT = //
            getResource("subpackage-dependent-expected-output.txt");
    // Control test files
    private static final URL CONTROL_TARGET_FOLDER = getResource("control-target");

    private NoSubpackageDependenceRule rule;
    private EnforcerRuleHelperMock helper;

    @Before
    public void setUp() throws Exception {
        rule = new NoSubpackageDependenceRule();
        helper = new EnforcerRuleHelperMock();
    }

    /**
     * Test that a subpackage dependence throws an error
     */
    @Test
    public void subpackageDependentIntegrationTest() throws URISyntaxException, IOException {
        helper.setTestClassesDir(new File("non-existent"));
        helper.setClassesDir(new File(SUBPACKAGE_DEPENDENT_TARGET_FOLDER.toURI()));
        try {
            rule.execute(helper);
            fail("expected EnforcerRuleException");
        } catch (EnforcerRuleException e) {
            String expected = IOUtils.toString(SUBPACKAGE_DEPENDENT_EXPECTED_OUTPUT.openStream(), (Charset) null) //
                    .replaceAll("\r", "") //
                    .trim();
            String actual = e.getMessage().trim();
            assertEquals(expected, actual);
        }
    }

    /**
     * Test no error thrown for no subpackage dependence
     */
    @Test
    public void controlIntegrationTest() throws URISyntaxException {
        helper.setTestClassesDir(new File("non-existent"));
        helper.setClassesDir(new File(CONTROL_TARGET_FOLDER.toURI()));
        try {
            rule.execute(helper);
        } catch (EnforcerRuleException e) {
            fail("expected EnforcerRuleException");
        }
    }

    private static URL getResource(String path) {
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }
}

