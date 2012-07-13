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
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
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
    implements Action, Serializable
{

    private Reference<MavenInvokerResults> mavenInvokerResults;

    private final AbstractBuild<?, ?> build;

    public MavenInvokerBuildAction( AbstractBuild<?, ?> build, MavenInvokerResults mavenInvokerResults )
    {
        this.build = build;
        this.mavenInvokerResults = new WeakReference<MavenInvokerResults>( mavenInvokerResults );
    }

    public String getIconFileName()
    {
        return null;
    }

    public Reference<MavenInvokerResults> getMavenInvokerResults()
    {
        if ( mavenInvokerResults == null )
        {
            FilePath directory = MavenInvokerRecorder.getMavenInvokerReportsDirectory( this.build );
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
                this.mavenInvokerResults = new WeakReference<MavenInvokerResults>( new MavenInvokerResults() );
            }
            else
            {

                this.mavenInvokerResults = new WeakReference<MavenInvokerResults>( loadResults( paths ) );
            }
        }
        return mavenInvokerResults;
    }

    public String getDisplayName()
    {
        // FIXME i18n
        return "Maven Invoker Plugin Results";
    }

    public String getUrlName()
    {
        return "maven-invoker-plugin-resuls";
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
}
