/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2025 SciJava developers.
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

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.Objects;
import java.util.Properties;

/**
 * Ensures a main class exists if the POM declares one.
 *
 * @author Gabriel Selzer
 */
@Named("mainClassExistsRule")
public class MainClassExistsRule extends AbstractEnforcerRule {

    // Inject needed Maven components
    private final MavenProject project;

    @Inject
    public MainClassExistsRule(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public void execute() throws EnforcerRuleException {
        Properties properties = project.getProperties();
        if (!properties.containsKey("main-class")) {
            return;
        }
        String mainClass = properties.getProperty("main-class");

        // Get the build output directory (e.g., target/classes)
        String outputDirectory = project.getBuild().getOutputDirectory();

        // Convert class name to file path (e.g., com.example.Main -> com/example/Main.class)
        String classFilePath = mainClass.replace('.', File.separatorChar) + ".class";
        // Check if e.g. target/classes/com/example/Main.class exists
        File classFile = new File(outputDirectory, classFilePath);
        if (!classFile.exists()) {
            throw new EnforcerRuleException(
                "Main class '" + mainClass + "' declared in POM does not exist. " +
                "Expected to find: " + classFile.getAbsolutePath()
            );
        }

        getLog().info("Main class '" + mainClass + "' exists at: " + classFile.getAbsolutePath());
    }
}
