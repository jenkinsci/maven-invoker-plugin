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

import hudson.maven.AggregatableAction;
import hudson.maven.MavenAggregatedReport;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;

/**
 * @author Olivier Lamy
 */
public class InvokerMavenAggregatedReport
    extends MavenInvokerBuildAction
    implements MavenAggregatedReport
{

    /**
     * Unique identifier for this class.
     */
    private static final long serialVersionUID = 31415927L;

    MavenInvokerResults mavenInvokerResults = new MavenInvokerResults();

    public InvokerMavenAggregatedReport( AbstractBuild<?, ?> build )
    {
        super( build );
    }

    @Override
    public void update( Map<MavenModule, List<MavenBuild>> moduleBuilds, MavenBuild newBuild )
    {
        InvokerReport invokerReport = newBuild.getAction( InvokerReport.class );

        if ( invokerReport != null )
        {
            MavenInvokerResults miResults = invokerReport.getMavenInvokerResults();
            if ( miResults != null )
            {
                List<MavenInvokerResult> results = miResults.mavenInvokerResults;
                mavenInvokerResults.mavenInvokerResults.addAll( results );
                initTestCountsFields( mavenInvokerResults );
            }
        }
    }

    @Override
    public MavenInvokerResults getMavenInvokerResults()
    {
        return mavenInvokerResults;
    }

    @Override
    public Class<? extends AggregatableAction> getIndividualActionType()
    {
        return InvokerReport.class;
    }

    @Override
    public Action getProjectAction( MavenModuleSet moduleSet )
    {
        return this;
    }

    public static final MavenInvokerResultComparator COMPARATOR_INSTANCE = new MavenInvokerResultComparator();

    public static class MavenInvokerResultComparator
        implements Comparator<MavenInvokerResult>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare( MavenInvokerResult mavenInvokerResult, MavenInvokerResult mavenInvokerResult1 )
        {
            if ( mavenInvokerResult.mavenModuleName == null )
            {
                return 0;
            }
            if ( mavenInvokerResult1.mavenModuleName == null )
            {
                return 0;
            }

            return mavenInvokerResult.mavenModuleName.compareTo( mavenInvokerResult1.mavenModuleName );
        }
    }

}
