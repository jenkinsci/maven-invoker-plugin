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
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Api;
import hudson.model.Run;
import jenkins.model.RunAction2;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerBuildAction
    implements Action, RunAction2, Serializable
{

    /**
     * Unique identifier for this class.
     */
    private static final long serialVersionUID = 31415927L;

    private transient Reference<MavenInvokerResults> mavenInvokerResults;

    private transient AbstractBuild<?, ?> build;

    private transient int passedTestCount;

    private transient int failedTestCount;

    private transient int skippedTestCount;

    private transient int runTests;

    public MavenInvokerBuildAction( AbstractBuild<?, ?> build, MavenInvokerResults mavenInvokerResults )
    {
        this.build = build;
        this.mavenInvokerResults = new WeakReference<MavenInvokerResults>( mavenInvokerResults );
        initTestCountsFields( mavenInvokerResults );
    }

    protected MavenInvokerBuildAction( AbstractBuild<?, ?> build )
    {
        this.build = build;
    }

    public MavenInvokerResults getMavenInvokerResults()
    {
        if ( mavenInvokerResults == null || mavenInvokerResults.get() == null
            || mavenInvokerResults.get().mavenInvokerResults.isEmpty() )
        {
            FilePath directory = MavenInvokerRecorder.getMavenInvokerReportsDirectory( build );
            FilePath[] paths = null;
            try
            {
                paths = directory.list( "maven-invoker-result*.xml" );
            }
            catch ( Exception e )
            {
                // FIXME improve logging
                // ignore this error nothing to show
            }
            if ( paths == null )
            {
                mavenInvokerResults = new WeakReference<MavenInvokerResults>( new MavenInvokerResults() );
            }
            else
            {
                mavenInvokerResults = new WeakReference<MavenInvokerResults>( loadResults( paths ) );
            }
        }
        MavenInvokerResults results = mavenInvokerResults.get();
        return results;
    }

    @Override
    public String getDisplayName()
    {
        // FIXME i18n
        return "Maven Invoker Plugin Results";
    }

    @Override
    public String getUrlName()
    {
        return "maven-invoker-plugin-results";
    }

    @Override
    public String getIconFileName()
    {
        return "/plugin/maven-invoker-plugin/icons/report.png";
    }

    @Override
    public void onAttached(Run<?, ?> r)
    {
        this.build = r instanceof AbstractBuild ? (AbstractBuild<?,?>) r : null;
    }

    @Override
    public void onLoad(Run<?, ?> r)
    {
        this.build = r instanceof AbstractBuild ? (AbstractBuild<?,?>) r : null;
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

    public AbstractBuild<?, ?> getBuild()
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

    MavenInvokerResults loadResults( FilePath[] paths )
    {
        MavenInvokerResults results = new MavenInvokerResults();
        final BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
        for ( FilePath filePath : paths )
        {
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream( new File( filePath.getRemote() ) );
                results.mavenInvokerResults.add( MavenInvokerRecorder.map( reader.read( fis ) ) );
            }
            catch ( IOException e )
            {
                // FIXME improve
                e.printStackTrace();
            }
            catch ( XmlPullParserException e )
            {
                // FIXME improve
                e.printStackTrace();
            }
            finally
            {
                IOUtils.closeQuietly( fis );
            }
        }
        return results;
    }

    protected void initTestCountsFields( MavenInvokerResults miResults )
    {
        for ( MavenInvokerResult result : miResults.mavenInvokerResults )
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
        initTestCountsFields( getMavenInvokerResults() );

        return this;
    }

}
