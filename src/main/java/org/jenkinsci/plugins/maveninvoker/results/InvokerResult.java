package org.jenkinsci.plugins.maveninvoker.results;

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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import hudson.model.Run;

/**
 * @author Olivier Lamy
 */
public class InvokerResult
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    public String project;

    public String name;

    public String description;

    public String result;

    public String failureMessage;

    public double time;

    public String logFilename;

    public transient Run<?,?> build;

    public transient String log;

    public InvokerResult()
    {
        // no op
    }

    public String getUrl()
    {
        return project.replace( "/pom.xml", "" );
    }

    public String getLink() throws UnsupportedEncodingException
    {
        return URLEncoder.encode( this.project, "UTF-8" );
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "InvokerResult" );
        sb.append( "{project='" ).append( project ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", result='" ).append( result ).append( '\'' );
        sb.append( ", failureMessage='" ).append( failureMessage ).append( '\'' );
        sb.append( ", time=" ).append( time );
        sb.append( ", logFilename=" ).append( logFilename );
        sb.append( '}' );
        return sb.toString();
    }

}
