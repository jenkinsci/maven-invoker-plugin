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
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerRecorder
    extends Recorder
{

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public final String filenamePattern;

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
        listener.getLogger().println( "performing MavenInvokerRecorder, filenamePattern:'" + filenamePattern + "'" );
        return true;
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
