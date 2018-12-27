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
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Writer;
import org.jenkinsci.plugins.maveninvoker.results.InvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerRecorder
    extends Recorder
    implements SimpleBuildStep
{

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOGGER = LoggerFactory.getLogger( MavenInvokerRecorder.class );

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
    public void perform( Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener )
        throws InterruptedException, IOException
    {

        perform( run, workspace, launcher, listener, new PipelineDetails() );
    }

    public void perform( Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener,
                         PipelineDetails pipelineDetails )
        throws InterruptedException, IOException
    {
        LOGGER.info( "performing MavenInvokerRecorder, reportsFilenamePattern:'{}', invokerBuildDir:'{}'", //
                     reportsFilenamePattern, invokerBuildDir);
        if ( workspace != null )
        {
            FilePath[] reportsFilePaths = locateReports( workspace, reportsFilenamePattern );
            LOGGER.info( "Found reports: {}", Arrays.asList( reportsFilePaths ) );
            try
            {
                MavenInvokerResults mavenInvokerResults =
                    parseReports( reportsFilePaths, run, pipelineDetails, workspace );
                storeAction( run, mavenInvokerResults );

                // if any failure mark the build as unstable
                for ( InvokerResult invokerResult : mavenInvokerResults.getInvokerResults() )
                {
                    if ( !StringUtils.equalsIgnoreCase( invokerResult.result, BuildJob.Result.SUCCESS )
                        && !StringUtils.equalsIgnoreCase( invokerResult.result, BuildJob.Result.SKIPPED ) )
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
     */
    private void storeAction( Run<?, ?> run, MavenInvokerResults mavenInvokerResults )
    {
        synchronized ( run )
        {
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

    private MavenInvokerResults parseReports( FilePath[] reportsFilePaths, Run<?, ?> run,
                                              PipelineDetails pipelineDetails, FilePath workspace )
        throws Exception
    {
        MavenInvokerResults mavenInvokerResults = new MavenInvokerResults();
        final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
        final BuildJobXpp3Writer writer = new BuildJobXpp3Writer();

        for ( final FilePath filePath : reportsFilePaths )
        {
            BuildJob buildJob = reader.read( filePath.read() );
            String originalProjectName = buildJob.getProject();
            InvokerResult invokerResult = map( buildJob, pipelineDetails );
            writer.write( filePath.write(), buildJob );
            mavenInvokerResults.getInvokerResults().add( invokerResult );
            saveReport( getMavenInvokerReportsDirectory( run, pipelineDetails, originalProjectName ), //
                        filePath, originalProjectName, workspace );
        }

        LOGGER.info(
            "Finished parsing Maven Invoker results (found {})", mavenInvokerResults.getInvokerResults().size());
        return mavenInvokerResults;

    }


    /**
     * save report
     */
    private boolean saveReport( FilePath maveninvokerDir, FilePath report, String originalProjectName,
                                FilePath workspace)
    {
        try
        {
            // save report file
            maveninvokerDir.mkdirs();
            String name = "maven-invoker-result.xml";
            FilePath dst = maveninvokerDir.child( name );
            report.copyTo( dst );
            // save build log file
            String logsPattern = this.invokerBuildDir + "/" + //
                StringUtils.replace( originalProjectName, "pom.xml", "*build.log" );
            FilePath[] logs = workspace.list( logsPattern );

            LOGGER.debug( "found files {} for pattern: {} and workspace: {}", Arrays.asList( logs ), logsPattern, workspace);

            for ( FilePath log : logs )
            {
                LOGGER.debug( "save file {} to {}", log, maveninvokerDir + log.getName() );
                dst = maveninvokerDir.child( log.getName() );
                log.copyTo( dst );
            }

            if(logs.length<1){
                String searchPattern = "**/*build.log";
                logs = workspace.list( searchPattern );
                LOGGER.debug( "found files {} for pattern: {} and workspace: {}", Arrays.asList( logs ), searchPattern, workspace);
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            return false;
        } return true;
    }

    private InvokerResult map( BuildJob buildJob, PipelineDetails pipelineDetails )
    {

        String pipelinePath = pipelinePath( pipelineDetails );

        InvokerResult invokerResult = new InvokerResult();

        invokerResult.description = buildJob.getDescription();
        invokerResult.failureMessage = buildJob.getFailureMessage();
        invokerResult.name = pipelinePath == null ? buildJob.getName() : pipelinePath + "/" + buildJob.getName();
        invokerResult.project =
            pipelinePath == null ? buildJob.getProject() : pipelinePath + "/" + buildJob.getProject();
        invokerResult.result = buildJob.getResult();
        invokerResult.time = buildJob.getTime();
        // transform the current buildJob as well
        if ( pipelinePath != null )
        {
            buildJob.setName( pipelinePath + "/" + buildJob.getName() );
            buildJob.setProject( pipelinePath + "/" + buildJob.getProject() );
        }

        invokerResult.logFilename = StringUtils.removeEnd( invokerResult.project, "/pom.xml" );
        invokerResult.logFilename = StringUtils.replace( invokerResult.logFilename, "/", "_" );
        invokerResult.logFilename = invokerResult.logFilename + "/build.log";

        return invokerResult;
    }

    private static String pipelinePath( PipelineDetails pipelineDetails )
    {
        String pipelinePath = null;
        if ( pipelineDetails != null && !pipelineDetails.getEnclosingBlockNames().isEmpty() )
        {
            StringBuilder name = new StringBuilder();
            pipelineDetails.getEnclosingBlockNames().stream().forEach( s -> name.append( s ).append( " / " ) );
            pipelinePath = name.toString();
        }
        return StringUtils.removeEnd( StringUtils.trim( pipelinePath ), "/" );
    }

    /**
     * Gets the directory to store report files
     */
    private FilePath getMavenInvokerReportsDirectory( Run<?, ?> build, PipelineDetails pipelineDetails, String projectName )
    {
        String pipelinePath = pipelinePath( pipelineDetails );
        if ( pipelinePath == null )
        {
            return new FilePath( new File( build.getRootDir(), STORAGE_DIRECTORY ) );
        }
        pipelinePath = StringUtils.removeEnd( pipelinePath + "/" + StringUtils.removeEnd( projectName, "pom.xml" ), "/");
        return new FilePath( new File( build.getRootDir(), STORAGE_DIRECTORY + "/" //
            + StringUtils.replace( pipelinePath, "/", "_" ) ) );
    }

    private FilePath[] locateReports( FilePath workspace, String filenamePattern )
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

    protected Object readResolve()
    {
        if ( filenamePattern != null )
        {
            reportsFilenamePattern = filenamePattern;
        }
        return this;
    }

}
