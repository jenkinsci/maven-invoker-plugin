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
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerRecorder extends Recorder implements SimpleBuildStep
{

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final String STORAGE_DIRECTORY = "maven-invoker-plugin-reports";

    @Deprecated
    public transient String filenamePattern;

    public static final String DEFAULT_REPORTS_FILENAME_PATTERN = "target/invoker-reports/BUILD*.xml";

    public String reportsFilenamePattern = DEFAULT_REPORTS_FILENAME_PATTERN;

    public static final String DEFAULT_INVOKER_BUILD_DIR = "target/its";

    public String invokerBuildDir = DEFAULT_INVOKER_BUILD_DIR;

    @Deprecated
    public MavenInvokerRecorder( String reportsFilenamePattern )
    {
        this.reportsFilenamePattern = reportsFilenamePattern;
    }

    @DataBoundConstructor
    public MavenInvokerRecorder( String reportsFilenamePattern, String invokerBuildDir )
    {
        this.reportsFilenamePattern = reportsFilenamePattern;
        this.invokerBuildDir = invokerBuildDir;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.STEP;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {

        perform( run, workspace, launcher, listener, new PipelineDetails() );
    }

    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, PipelineDetails pipelineDetails)
            throws InterruptedException, IOException
    {
        PrintStream logger = listener.getLogger();
        logger.println( "performing MavenInvokerRecorder, reportsFilenamePattern:'" + reportsFilenamePattern + "', invokerBuildDir:'" + invokerBuildDir + "'" );
        if ( workspace != null )
        {
            FilePath[] reportsFilePaths = locateReports( workspace, reportsFilenamePattern );
            FilePath[] logsFilePaths = locateBuildLogs( workspace, invokerBuildDir + "/**" );
            logger.println( "Found reports:" + Arrays.asList( reportsFilePaths ) );
            logger.println( "Found logs:" + Arrays.asList( logsFilePaths ) );
            try
            {
                MavenInvokerResults mavenInvokerResults = parseReports( reportsFilePaths, logsFilePaths, listener, run );
                if(!pipelineDetails.getEnclosingBlockNames().isEmpty()){
                    StringBuilder name = new StringBuilder( );
                    pipelineDetails.getEnclosingBlockNames().stream().forEach( s -> name.append( s ).append( " / " ) );
                    final String pipelinePath = name.toString();
                    mavenInvokerResults.mavenInvokerResults //
                        .stream() //
                        .forEach( mavenInvokerResult -> mavenInvokerResult.project = pipelinePath + mavenInvokerResult.project );
                }
                storeAction(run, mavenInvokerResults);

                // if any failure mark the build as unstable
                for (MavenInvokerResult mavenInvokerResult : mavenInvokerResults.getSortedMavenInvokerResults())
                {
                    if ( !StringUtils.equalsIgnoreCase( mavenInvokerResult.result, BuildJob.Result.SUCCESS)
                        && !StringUtils.equalsIgnoreCase( mavenInvokerResult.result, BuildJob.Result.SKIPPED))
                    {
                        run.setResult( Result.UNSTABLE );
                        return;
                    }
                }

            }
            catch ( Exception e )
            {
                throw new IOException( e.getMessage(), e );
            }
        }
    }

    /**
     *
     *
     */
    private void storeAction(Run<?, ?> run, MavenInvokerResults mavenInvokerResults) {
        synchronized ( run ) {
            MavenInvokerBuildAction action = run.getAction( MavenInvokerBuildAction.class );
            if ( action == null )
            {
                action = new MavenInvokerBuildAction( mavenInvokerResults );
                run.addAction( action );

            }
            else
            {
                action.addResults( mavenInvokerResults );
            }
        }
    }

    static MavenInvokerResults parseReports( FilePath[] reportsFilePaths, FilePath[] logsFilePaths, TaskListener listener, Run<?, ?> run )
        throws Exception
    {
        final PrintStream logger = listener.getLogger();
        MavenInvokerResults mavenInvokerResults = new MavenInvokerResults();
        final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
        saveReports( getMavenInvokerReportsDirectory( run ), reportsFilePaths );
        saveBuildLogs( getMavenInvokerReportsDirectory( run ), Arrays.asList( logsFilePaths ) );
        for ( final FilePath filePath : reportsFilePaths )
        {
            BuildJob buildJob = reader.read( filePath.read() );
            MavenInvokerResult mavenInvokerResult = map( buildJob );
            mavenInvokerResults.mavenInvokerResults.add( mavenInvokerResult );
        }
        logger.println( "Finished parsing Maven Invoker results (found " + mavenInvokerResults.mavenInvokerResults.size()+ ")" );
        return mavenInvokerResults;
    }

    static MavenInvokerResult map( BuildJob buildJob )
    {
        MavenInvokerResult mavenInvokerResult = new MavenInvokerResult();

        mavenInvokerResult.logFilename = buildJob.getProject().replace("/pom.xml", "-build.log");
        mavenInvokerResult.description = buildJob.getDescription();
        mavenInvokerResult.failureMessage = buildJob.getFailureMessage();
        mavenInvokerResult.name = buildJob.getName();
        mavenInvokerResult.project = buildJob.getProject();
        mavenInvokerResult.result = buildJob.getResult();
        mavenInvokerResult.time = buildJob.getTime();
        return mavenInvokerResult;
    }

    /**
     * save reports
     */
    static boolean saveReports( FilePath maveninvokerDir, FilePath[] paths )
    {
        try
        {
            maveninvokerDir.mkdirs();
            int i = 0;
            for ( FilePath report : paths )
            {
                String name = "maven-invoker-result" + ( i > 0 ? "-" + i : "" ) + ".xml";
                i++;
                FilePath dst = maveninvokerDir.child( name );
                report.copyTo( dst );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean saveBuildLogs( FilePath backupDirectory, List<FilePath> paths )
    {
        try
        {
            backupDirectory.mkdirs();
            for ( FilePath buildLog : paths )
            {
                File file = new File( buildLog.getRemote() );
                String name = file.getParentFile().getName() + "-build.log";
                FilePath dst = backupDirectory.child( name );
                buildLog.copyTo( dst );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Gets the directory to store report files
     */
    static FilePath getMavenInvokerReportsDirectory( Run<?, ?> build )
    {
        return new FilePath( new File( build.getRootDir(), STORAGE_DIRECTORY ) );
    }

    static FilePath[] locateBuildLogs( FilePath workspace, String basePath )
        throws IOException, InterruptedException
    {
        return workspace.list( basePath + "/build.log" );
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
        List<FilePath> files = new ArrayList<>();
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
        @Override
        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            return true;
        }

        @Override
        public String getDisplayName()
        {
            return Messages.maveninvoker_DisplayName();
        }
    }

    protected Object readResolve() {
        if (filenamePattern != null) {
           reportsFilenamePattern = filenamePattern;
        }
        return this;
    }

}
