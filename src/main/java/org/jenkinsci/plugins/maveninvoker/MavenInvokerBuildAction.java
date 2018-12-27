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

import hudson.FilePath;
import hudson.model.Api;
import hudson.model.Run;
import jenkins.model.RunAction2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.invoker.model.BuildJob;
import org.apache.maven.plugins.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.maveninvoker.results.InvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;

import static org.jenkinsci.plugins.maveninvoker.MavenInvokerRecorder.STORAGE_DIRECTORY;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerBuildAction
    implements RunAction2, Serializable
{

    public static final String URL_NAME = "maven-invoker-plugin-results";

    /**
     * Unique identifier for this class.
     */
    private static final long serialVersionUID = 31415927L;

    private transient Reference<MavenInvokerResults> mavenInvokerResults;

    private transient Run<?, ?> build;

    private transient int passedTestCount;

    private transient int failedTestCount;

    private transient int skippedTestCount;

    private transient int runTests;

//    private PipelineDetails pipelineDetails;

    /**
     * Constructor.
     * @deprecated This is a {@link RunAction2} instance, not need to pass build explicitly
     */
    @Deprecated
    public MavenInvokerBuildAction( Run<?, ?> build, MavenInvokerResults mavenInvokerResults )
    {
        this.build = build;
        this.mavenInvokerResults = new WeakReference<>( mavenInvokerResults );
        initTestCountsFields( mavenInvokerResults );
    }

    public MavenInvokerBuildAction( MavenInvokerResults mavenInvokerResults)
    {
        this.mavenInvokerResults = new WeakReference<>( mavenInvokerResults );
        initTestCountsFields( mavenInvokerResults );
    }

    protected MavenInvokerBuildAction( Run<?, ?> build )
    {
        this.build = build;
    }

    protected synchronized void addResults( MavenInvokerResults mavenInvokerResults )
    {
        MavenInvokerResults invokerResults = new MavenInvokerResults();
        MavenInvokerResults current = this.mavenInvokerResults.get();
        if(current!=null)
        {
            if(current.getInvokerResults()!=null)
            {
                invokerResults.getInvokerResults().addAll( current.getInvokerResults() );
            }
        }
        invokerResults.getInvokerResults().addAll( mavenInvokerResults.getInvokerResults() );
        this.mavenInvokerResults = new WeakReference<>( invokerResults );
        initTestCountsFields( mavenInvokerResults );
    }

    public MavenInvokerResults getMavenInvokerResults()
    {
        if ( build != null )
        {
            if ( mavenInvokerResults == null || mavenInvokerResults.get() == null //
                || mavenInvokerResults.get().getInvokerResults().isEmpty() )
            {
                FilePath directory = new FilePath( new File( build.getRootDir(), STORAGE_DIRECTORY ) );
                FilePath[] paths = null;
                try
                {
                    paths = directory.list( "**/maven-invoker-result*.xml" );
                }
                catch ( Exception e )
                {
                    // FIXME improve logging
                    // ignore this error nothing to show
                }
                if ( paths == null )
                {
                    mavenInvokerResults = new WeakReference<>( new MavenInvokerResults() );
                }
                else
                {
                    mavenInvokerResults = new WeakReference<>( loadResults( paths ) );
                }
            }
            MavenInvokerResults results = mavenInvokerResults.get();
            return results;
        }
        return new MavenInvokerResults();
    }

    @Override
    public String getDisplayName()
    {
        return Messages.maveninvoker_DisplayName();
    }

    @Override
    public String getUrlName()
    {
        return URL_NAME;
    }

    @Override
    public String getIconFileName()
    {
        return "/plugin/maven-invoker-plugin/icons/report.png";
    }

    @Override
    public void onAttached(Run<?, ?> r)
    {
        this.build = r;
    }

    @Override
    public void onLoad(Run<?, ?> r)
    {
        this.build = r;
        init();
    }

    public Api getApi()
    {
        return new Api( getMavenInvokerResults() );
    }

    public int getPassedTestCount()
    {
        return passedTestCount;
    }

    public int getFailedTestCount()
    {
        return failedTestCount;
    }

    public int getSkippedTestCount()
    {
        return skippedTestCount;
    }

    public int getRunTests()
    {
        return runTests;
    }

    public Run<?, ?> getBuild()
    {
        return build;
    }

    public int getFailCount()
    {
        return getFailedTestCount();
    }

    public int getSkipCount()
    {
        return getSkippedTestCount();
    }

    public int getTotalCount()
    {
        return getRunTests();
    }

    public MavenInvokerBuildAction getPreviousResult() {
        Run<?,?> b = build;
        while(true) {
            b = b.getPreviousBuild();
            if(b==null)
                return null;
            MavenInvokerBuildAction r = b.getAction(MavenInvokerBuildAction.class);
            if (r != null) {
                if (r == this) {
                    throw new IllegalStateException(this + " was attached to both " + b + " and " + build);
                }
                if (r.build.number != b.number) {
                    throw new IllegalStateException(r + " was attached to both " + b + " and " + r.build);
                }
                return r;
            }
        }
    }

    public InvokerResult getResult( String url)
    {
        try
        {
            for ( InvokerResult result : getMavenInvokerResults().getInvokerResults() )
            {
                if ( URLDecoder.decode( url, "UTF-8" ).equals( result.project ) )
                {
                    result.build = build;
                    String pattern = "**/" + STORAGE_DIRECTORY + "/" + result.logFilename;
                    FilePath[] logs = new FilePath( build.getRootDir() ).list(pattern);
                    if ( logs.length > 0 )
                    {
                        result.log = logs[0].readToString();
                    }
                    return result;
                }
            }
        }
        catch (IOException | InterruptedException e)
        {
            // FIXME improve
            e.printStackTrace();
        }

        return new InvokerResult();
    }

    private MavenInvokerResults loadResults( FilePath[] paths )
    {
        MavenInvokerResults results = new MavenInvokerResults();
        final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
        for ( FilePath filePath : paths )
        {
            try(FileInputStream fis = new FileInputStream( new File( filePath.getRemote() ) ))
            {
                results.getInvokerResults().add( map( reader.read( fis ) ) );
            }
            catch ( IOException | XmlPullParserException e )
            {
                // FIXME improve
                e.printStackTrace();
            }
        }
        return results;
    }

    private static InvokerResult map( BuildJob buildJob)
    {

        InvokerResult invokerResult = new InvokerResult();

        invokerResult.description = buildJob.getDescription();
        invokerResult.failureMessage = buildJob.getFailureMessage();
        invokerResult.name = buildJob.getName();
        invokerResult.project = buildJob.getProject();
        invokerResult.result = buildJob.getResult();
        invokerResult.time = buildJob.getTime();
        //invokerResult.logFilename = buildJob.getProject().replace( "pom.xml", "build.log");
        invokerResult.logFilename = StringUtils.removeEnd( invokerResult.project, "/pom.xml" );
        invokerResult.logFilename = StringUtils.replace( invokerResult.logFilename, "/", "_" );
        invokerResult.logFilename = invokerResult.logFilename + "/build.log";
        return invokerResult;
    }

    protected void initTestCountsFields( MavenInvokerResults miResults )
    {
        for ( InvokerResult result : miResults.getInvokerResults() )
        {
            String resultStr = result.result;
            if ( StringUtils.equals( resultStr, BuildJob.Result.ERROR ) )
            {
                failedTestCount++;
            }
            else if ( StringUtils.equals( resultStr, BuildJob.Result.SKIPPED ) )
            {
                skippedTestCount++;
            }
            else if ( StringUtils.equals( resultStr, BuildJob.Result.SUCCESS ) )
            {
                passedTestCount++;
            }
            // TODO an other field ?
            else if ( StringUtils.equals( resultStr, BuildJob.Result.FAILURE_BUILD ) )
            {
                failedTestCount++;
            }
            else if ( StringUtils.equals( resultStr, BuildJob.Result.FAILURE_POST_HOOK ) )
            {
                failedTestCount++;
            }
            else if ( StringUtils.equals( resultStr, BuildJob.Result.FAILURE_PRE_HOOK ) )
            {
                failedTestCount++;
            }
            runTests++;
        }
    }

    protected Object readResolve()
    {
        init();
        return this;
    }

    private void init()
    {
        initTestCountsFields( getMavenInvokerResults() );
    }
}
