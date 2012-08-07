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
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;

import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class InvokerReport
    extends MavenInvokerBuildAction
    implements AggregatableAction
{

    private MavenInvokerResults mavenInvokerResults;

    public InvokerReport( AbstractBuild<?, ?> build, MavenInvokerResults mavenInvokerResults )
    {
        super( build, mavenInvokerResults );
        this.mavenInvokerResults = mavenInvokerResults;
    }

    public MavenAggregatedReport createAggregatedAction( MavenModuleSetBuild build,
                                                         Map<MavenModule, List<MavenBuild>> moduleBuilds )
    {

        /*for ( Map.Entry<MavenModule, List<MavenBuild>> entry : moduleBuilds.entrySet() )
        {
            for ( MavenBuild mavenBuild : entry.getValue() )
            {

                MavenInvokerBuildAction mavenInvokerBuildAction = mavenBuild.getAction( MavenInvokerBuildAction.class );
                if ( mavenInvokerBuildAction != null )
                {
                    MavenInvokerResults mavenInvokerResults = mavenInvokerBuildAction.getMavenInvokerResults();
                    if ( mavenInvokerResults != null )
                    {
                        List<MavenInvokerResult> results = mavenInvokerResults.mavenInvokerResults;
                        if ( results != null )
                        {
                            this.mavenInvokerResults.mavenInvokerResults.addAll( results );
                        }
                    }
                }
            }
        }*/
        InvokerMavenAggregatedReport invokerMavenAggregatedReport =
            build.getAction( InvokerMavenAggregatedReport.class );
        if ( invokerMavenAggregatedReport != null )
        {
            return invokerMavenAggregatedReport;
        }
        return new InvokerMavenAggregatedReport( build );
    }

    @Override
    public MavenInvokerResults getMavenInvokerResults()
    {
        return this.mavenInvokerResults;
    }
}
