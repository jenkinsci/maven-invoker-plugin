package org.jenkinsci.plugins.maveninvoker;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;

public class MavenInvokerRecorderTest
{

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void parse_file()
        throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject( "foo");

        p.getBuildersList().add(new TestBuilder() {
            @Override
            @SuppressWarnings("null")
            public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,
                IOException
            {
                build.getWorkspace().child( "src/test/resources/invoker-reports/BUILD-simple-jsp.xml" )
                    .copyFrom( getClass().getResource( "/invoker-reports/BUILD-simple-jsp.xml" ) );
                build.getWorkspace().child( "src/test/resources/invoker-reports/BUILD-simple-jsp-fail.xml" )
                    .copyFrom( getClass().getResource( "/invoker-reports/BUILD-simple-jsp-fail.xml" ) );
                return true;
            }
        });
        p.getPublishersList().add(new MavenInvokerRecorder( "src/test/resources/invoker-reports/*.xml" ));
        FreeStyleBuild run = j.assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0));
        MavenInvokerBuildAction mavenInvokerBuildAction = run.getAction( MavenInvokerBuildAction.class );
        Assert.assertEquals( 2, mavenInvokerBuildAction.getRunTests());
        Assert.assertEquals( 1, mavenInvokerBuildAction.getPassedTestCount());
        Assert.assertEquals( 1, mavenInvokerBuildAction.getFailCount());
    }

}
