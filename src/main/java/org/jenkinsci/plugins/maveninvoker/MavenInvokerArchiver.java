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
import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenModule;
import hudson.maven.MavenReporter;
import hudson.maven.MavenReporterDescriptor;
import hudson.maven.MojoInfo;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        if ( !mojo.is( "org.apache.maven.plugins", "maven-invoker-plugin", "run" )
            && !mojo.is( "org.apache.maven.plugins", "maven-invoker-plugin", "integration-test" ) )
        {
            return true;
        }
        final String buildDirectory = new File( pom.getBuild().getDirectory() ).getName();

        final PrintStream logger = listener.getLogger();
        logger.println( "MavenInvokerArchiver" );
        File[] reports = new File[0];
        try
        {
            // projectsDirectory
            final File projectsDirectory = mojo.getConfigurationValue( "projectsDirectory", File.class );

            // cloneProjectsTo
            final File cloneProjectsTo = mojo.getConfigurationValue( "cloneProjectsTo", File.class );

            final File reportsDir = mojo.getConfigurationValue( "reportsDirectory", File.class );
            reports = reportsDir.listFiles( new FilenameFilter()
            {
                @Override
                public boolean accept( File file, String s )
                {
                    return s.startsWith( "BUILD" );
                }
            } );

            if ( reports != null )
            {
                logger.println( "found reports:" + Arrays.asList( reports ) );
            }
            else
            {
                logger.println( "no reports found" );
                return true;
            }
            final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();

            final MavenInvokerResults mavenInvokerResults = new MavenInvokerResults();
            // TODO
            // saveReports

            for ( File f : reports )
            {
                InputStream is = new FileInputStream( f );
                try
                {
                    BuildJob buildJob = reader.read( is );
                    MavenInvokerResult mavenInvokerResult = MavenInvokerRecorder.map( buildJob );
                    mavenInvokerResult.mavenModuleName = pom.getArtifactId();

                    logger.println( "mavenInvokerResult:" + mavenInvokerResult );

                    mavenInvokerResults.mavenInvokerResults.add( mavenInvokerResult );
                }
                catch ( XmlPullParserException e )
                {
                    e.printStackTrace( listener.fatalError( "failed to parse report" ) );
                    build.setResult( Result.FAILURE );
                    return true;
                }
                finally
                {
                    IOUtils.closeQuietly( is );
                }
            }

            int failedCount = build.execute( new MavenBuildProxy.BuildCallable<Integer, IOException>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Integer call( MavenBuild aBuild )
                    throws IOException, IOException, InterruptedException
                {

                    FilePath[] reportsPaths =
                        MavenInvokerRecorder.locateReports( aBuild.getWorkspace(),
                                                            buildDirectory + "/" + reportsDir.getName() + "/BUILD*.xml" );

                    FilePath backupDirectory = MavenInvokerRecorder.getMavenInvokerReportsDirectory( aBuild );

                    MavenInvokerRecorder.saveReports( backupDirectory, reportsPaths );

                    List<FilePath> allBuildLogs = new ArrayList<FilePath>();

                    for ( MavenInvokerResult mavenInvokerResult : mavenInvokerResults.mavenInvokerResults )
                    {

                        // search build.log files

                        File invokerBuildDir = cloneProjectsTo == null ? projectsDirectory : cloneProjectsTo;

                        File projectDir = new File( invokerBuildDir, mavenInvokerResult.project );

                        FilePath[] buildLogs =
                            MavenInvokerRecorder.locateBuildLogs( aBuild.getWorkspace(), "**/"
                                + projectDir.getParentFile().getName() );

                        if ( buildLogs != null )
                        {
                            allBuildLogs.addAll( Arrays.asList( buildLogs ) );
                        }
                    }

                    // backup all build.log
                    MavenInvokerRecorder.saveBuildLogs( backupDirectory, allBuildLogs );

                    InvokerReport invokerReport = new InvokerReport( aBuild, mavenInvokerResults );
                    aBuild.addAction( invokerReport );
                    int failed = invokerReport.getFailedTestCount();
                    return failed;
                }
            } );

            return true;

        }
        catch ( ComponentConfigurationException e )
        {
            e.printStackTrace( listener.fatalError( "failed to find report directory" ) );
            build.setResult( Result.FAILURE );
            return true;
        }

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
