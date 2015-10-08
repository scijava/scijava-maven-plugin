/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2015 Board of Regents of the University of
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

package org.scijava.maven.plugin.enforcer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Bans duplicate classes on the classpath.
 */
public class BanDuplicateClasses
    extends AbstractResolveDependencies
{

    /**
     * The failure message
     */
    private String message;
    
    /**
     * List of classes to ignore. Wildcard at the end accepted
     */
    private String[] ignoreClasses;

    /**
     * If {@code false} then the rule will fail at the first duplicate, if {@code true} then the rule will fail at
     * the end.
     */
    private boolean findAllDuplicates;

    private List<Dependency> dependencies;
    
    /**
     * Only verify dependencies with one of these scopes
     */
    private List<String> scopes;

    @Override
    protected void handleArtifacts( Set<Artifact> artifacts ) throws EnforcerRuleException
    {
        List<IgnorableDependency> ignorableDependencies = new ArrayList<IgnorableDependency>();
        if ( ignoreClasses != null )
        {
            IgnorableDependency ignorableDependency = new IgnorableDependency();
            ignorableDependency.applyIgnoreClasses( ignoreClasses, false );
            ignorableDependencies.add( ignorableDependency );
        }
        if ( dependencies != null )
        {
            for ( Dependency dependency : dependencies )
            {
                getLog().info( "Adding ignorable dependency: " + dependency );
                IgnorableDependency ignorableDependency = new IgnorableDependency();
                if ( dependency.getGroupId() != null )
                {
                    ignorableDependency.groupId = Pattern.compile( asRegex( dependency.getGroupId() ) );
                }
                if ( dependency.getArtifactId() != null )
                {
                    ignorableDependency.artifactId = Pattern.compile( asRegex( dependency.getArtifactId() ) );
                }
                if ( dependency.getType() != null )
                {
                    ignorableDependency.type = Pattern.compile( asRegex( dependency.getType() ) );
                }
                if ( dependency.getClassifier() != null )
                {
                    ignorableDependency.classifier = Pattern.compile( asRegex( dependency.getClassifier() ) );
                }
                ignorableDependency.applyIgnoreClasses( dependency.getIgnoreClasses(), true );
                ignorableDependencies.add( ignorableDependency );
            }
        }
        
        Map<String, Artifact> classNames = new HashMap<String, Artifact>();
        Map<String, Set<Artifact>> duplicates = new HashMap<String, Set<Artifact>>();
        for ( Artifact o : artifacts )
        {
            if( scopes != null && !scopes.contains( o.getScope() ) )
            {
                if( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Skipping " + o.toString() + " due to scope" );
                }
                continue;
            }
            File file = o.getFile();
            getLog().debug( "Searching for duplicate classes in " + file );
            if ( file == null || !file.exists() )
            {
                getLog().warn( "Could not find " + o + " at " + file );
            }
            else if ( file.isDirectory() )
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    final List<String> fileNames = FileUtils.getFileNames( file, null, null, false );
                    for ( String name : fileNames )
                    {
                        getLog().debug( "  " + name );
                        checkAndAddName( o, name, classNames, duplicates, ignorableDependencies );
                    }
                }
                catch ( IOException e )
                {
                    throw new EnforcerRuleException(
                        "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(), e );
                }
            }
            else if ( file.isFile() && "jar".equals( o.getType() ) )
            {
                try
                {
                    //@todo use UnArchiver as defined per type
                    JarFile jar = new JarFile( file );
                    try
                    {
                        for ( JarEntry entry : Collections.<JarEntry>list( jar.entries() ) )
                        {
                            checkAndAddName( o, entry.getName(), classNames, duplicates, ignorableDependencies );
                        }
                    }
                    finally
                    {
                        try
                        {
                            jar.close();
                        }
                        catch ( IOException e )
                        {
                            // ignore
                        }
                    }
                }
                catch ( IOException e )
                {
                    throw new EnforcerRuleException(
                        "Unable to process dependency " + o.toString() + " due to " + e.getLocalizedMessage(), e );
                }
            }
        }
        if ( !duplicates.isEmpty() )
        {
            Map<Set<Artifact>, List<String>> inverted = new HashMap<Set<Artifact>, List<String>>();
            for ( Map.Entry<String, Set<Artifact>> entry : duplicates.entrySet() )
            {
                List<String> s = inverted.get( entry.getValue() );
                if ( s == null )
                {
                    s = new ArrayList<String>();
                }
                s.add( entry.getKey() );
                inverted.put( entry.getValue(), s );
            }
            StringBuilder buf = new StringBuilder( message == null ? "Duplicate classes found:" : message );
            buf.append( '\n' );
            for ( Map.Entry<Set<Artifact>, List<String>> entry : inverted.entrySet() )
            {
                buf.append( "\n  Found in:" );
                for ( Artifact a : entry.getKey() )
                {
                    buf.append( "\n    " );
                    buf.append( a );
                }
                buf.append( "\n  Duplicate classes:" );
                for ( String className : entry.getValue() )
                {
                    buf.append( "\n    " );
                    buf.append( className );
                }
                buf.append( '\n' );
            }
            throw new EnforcerRuleException( buf.toString() );
        }

    }

    private void checkAndAddName( Artifact artifact, String name, Map<String, Artifact> classNames,
                                  Map<String, Set<Artifact>> duplicates, Collection<IgnorableDependency> ignores )
        throws EnforcerRuleException
    {
        if ( !name.endsWith( ".class" ) )
        {
            return;
        }
        
        for ( IgnorableDependency c : ignores )
        {
            if ( c.matchesArtifact( artifact ) && c.matches( name ) )
            {
                if( classNames.containsKey( name ) )
                {
                    getLog().debug( "Ignoring excluded class " + name );
                }
                return;
            }
        }

        if ( classNames.containsKey( name ) )
        {
            Artifact dup = classNames.put( name, artifact );
            if ( !( findAllDuplicates && duplicates.containsKey( name ) ) )
            {
                for ( IgnorableDependency c : ignores )
                {
                    if ( c.matchesArtifact( artifact ) && c.matches(name) )
                    {
                        getLog().debug( "Ignoring duplicate class " + name );
                        return;
                    }
                }
            }
            
            if ( findAllDuplicates )
            {
                Set<Artifact> dups = duplicates.get( name );
                if ( dups == null )
                {
                    dups = new LinkedHashSet<Artifact>();
                    dups.add( dup );
                }
                dups.add( artifact );
                duplicates.put( name, dups );
            }
            else
            {
                StringBuilder buf = new StringBuilder( message == null ? "Duplicate class found:" : message );
                buf.append( '\n' );
                buf.append( "\n  Found in:" );
                buf.append( "\n    " );
                buf.append( dup );
                buf.append( "\n    " );
                buf.append( artifact );
                buf.append( "\n  Duplicate classes:" );
                buf.append( "\n    " );
                buf.append( name );
                buf.append( '\n' );
                buf.append( "There may be others but <findAllDuplicates> was set to false, so failing fast" );
                throw new EnforcerRuleException( buf.toString() );
            }
        }
        else 
        {
            classNames.put( name, artifact );
        }
    }
}
