package org.jenkinsci.plugins.maveninvoker;

/*
 * Copyright (c) Olivier Lamy
 *
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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerRecorder
    extends Recorder
{

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public String filenamePattern = "target/invoker-reports/BUILD*.xml";

    @DataBoundConstructor
    public MavenInvokerRecorder( String filenamePattern )
    {
        this.filenamePattern = filenamePattern;
    }


    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.STEP;
    }

    @Override
    public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener )
        throws InterruptedException, IOException
    {
        PrintStream logger = listener.getLogger();
        logger.println( "performing MavenInvokerRecorder, filenamePattern:'" + filenamePattern + "'" );
        FilePath[] filePaths = locateReports( build.getWorkspace(), this.filenamePattern );
        logger.println( "filePaths:" + Arrays.asList( filePaths ) );
        try
        {
            List<BuildJob> buildJobs = parseReports( filePaths, listener );
        }
        catch ( Exception e )
        {
            throw new IOException( e.getMessage() );
        }
        return true;
    }

    static List<BuildJob> parseReports( FilePath[] filePaths, BuildListener listener )
        throws Exception
    {
        PrintStream logger = listener.getLogger();
        List<BuildJob> buildJobs = new ArrayList<BuildJob>( filePaths.length );
        final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
        for ( final FilePath filePath : filePaths )
        {
            BuildJob buildJob = filePath.act( new Callable<BuildJob, Exception>()
            {
                public BuildJob call()
                    throws Exception
                {
                    String fileName = filePath.getRemote();
                    System.out.println( "fileName:" + fileName );
                    InputStream is = new FileInputStream( fileName );
                    try
                    {
                        return reader.read( is );
                    }
                    finally
                    {
                        is.close();
                    }
                }
            } );
            logger.println(
                "buildJob:" + buildJob.getProject() + ":" + buildJob.getResult() + ":" + buildJob.getTime() );
            buildJobs.add( buildJob );

        }
        return buildJobs;
    }

    static FilePath[] locateReports( FilePath workspace, String filenamePattern )
        throws IOException, InterruptedException
    {

        // First use ant-style pattern
        try
        {
            FilePath[] ret = workspace.list( filenamePattern );
            if ( ret.length > 0 )
            {
                return ret;
            }
        }
        catch ( Exception e )
        {
        }

        // If it fails, do a legacy search
        List<FilePath> files = new ArrayList<FilePath>();
        String parts[] = filenamePattern.split( "\\s*[;:,]+\\s*" );
        for ( String path : parts )
        {
            FilePath src = workspace.child( path );
            if ( src.exists() )
            {
                if ( src.isDirectory() )
                {
                    files.addAll( Arrays.asList( src.list( "**/BUILD*.xml" ) ) );
                }
                else
                {
                    files.add( src );
                }
            }
        }
        return files.toArray( new FilePath[files.size()] );
    }

    public static final class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        /**
         * Do not instantiate DescriptorImpl.
         */
        /*private DescriptorImpl()
        {
            super( Publisher.class );
        }*/

        /*@Override
        public MavenInvokerRecorder newInstance( StaplerRequest req, JSONObject formData )
            throws FormException
        {
            return req.bindJSON( MavenInvokerRecorder.class, formData );
        }*/
        @Override
        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            return true;
        }

        @Override
        public String getDisplayName()
        {
            // FIXME i18n
            return "Maven Invoker Plugin Report";
        }
    }
}
