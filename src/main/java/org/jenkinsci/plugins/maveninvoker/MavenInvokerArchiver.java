package org.jenkinsci.plugins.maveninvoker;

/*
 * Copyright (c) Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import hudson.Extension;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.BuildListener;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Olivier Lamy
 * @since 1.1
 */
public class MavenInvokerArchiver
    extends MavenReporter
{
    @Override
    public boolean postExecute( MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener,
                                Throwable error )
        throws InterruptedException, IOException
    {
        if ( !mojo.is( "org.apache.maven.plugins", "maven-invoker-plugin", "run" ) && !mojo.is(
            "org.apache.maven.plugins", "maven-invoker-plugin", "integration-test" ) )
        {
            return true;
        }

        PrintStream logger = listener.getLogger();
        logger.println( "MavenInvokerArchiver" );
        XmlPlexusConfiguration c = (XmlPlexusConfiguration) mojo.configuration.getChild( "reportsDirectory" );

        return true;
    }


    @Extension
    public static final class DescriptorImpl
        extends MavenReporterDescriptor
    {

        @Override
        public String getDisplayName()
        {
            // FIXME i18n
            return "Maven Invoker Plugin Results";
        }


        @Override
        public MavenReporter newAutoInstance( MavenModule module )
        {
            return new MavenInvokerArchiver();
        }
    }

    private static final long serialVersionUID = 1L;
}
