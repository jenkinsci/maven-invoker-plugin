package org.jenkinsci.plugins.maveninvoker.pipeline;

import hudson.FilePath;
import hudson.model.Result;
import org.jenkinsci.plugins.maveninvoker.MavenInvokerBuildAction;
import org.jenkinsci.plugins.maveninvoker.results.InvokerResult;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.net.URLEncoder;
import java.util.List;

import static org.junit.Assert.*;

public class MavenInvokerStepTest
{

    @Rule
    public final JenkinsRule rule = new JenkinsRule();

    @Test
    public void two_invoke_on_action_configured_patterns()
        throws Exception
    {
        WorkflowJob j = rule.jenkins.createProject( WorkflowJob.class, "configured_patterns" );

        j.setDefinition( new CpsFlowDefinition( "stage('Invoker') {\n" + //
                                                    "  node {\n" + //
                                                    "     stage ('First Call'){ \n" + //
                                                    "      maven_invoker(reportsFilenamePattern: 'reports_dir', invokerBuildDir: 'builds_dir')\n" + //
                                                    "     } \n" + //
                                                    "     stage ('Second Call'){ \n" + //
                                                    "       maven_invoker(reportsFilenamePattern: 'reports_dir', invokerBuildDir: 'builds_dir')\n" + //
                                                    "     } \n" + //
                                                    "  }\n" + //
                                                    "}\n", true ) );

        // copy test resources
        FilePath ws = rule.jenkins.getWorkspaceFor( j);

        FilePath reports = new FilePath( new File( "src/test/resources/invoker-reports") );
        FilePath reportsDir = ws.child( "reports_dir" );
        reportsDir.mkdirs();
        reports.copyRecursiveTo( reportsDir );

        FilePath builds = new FilePath( new File( "src/test/resources/it") );
        FilePath buildsDir = ws.child( "builds_dir" );
        buildsDir.mkdirs();
        builds.copyRecursiveTo( buildsDir );


        WorkflowRun r = j.scheduleBuild2( 0).waitForStart();
        rule.assertBuildStatus( Result.UNSTABLE, rule.waitForCompletion( r));
        List<MavenInvokerBuildAction> actions = r.getActions( MavenInvokerBuildAction.class );
        assertEquals(1,actions.size());

        MavenInvokerBuildAction mavenInvokerBuildAction = r.getAction( MavenInvokerBuildAction.class );
        assertEquals( 4, mavenInvokerBuildAction.getRunTests());
        assertEquals( 2, mavenInvokerBuildAction.getPassedTestCount());
        assertEquals( 2, mavenInvokerBuildAction.getFailCount());

        MavenInvokerResults results = mavenInvokerBuildAction.getMavenInvokerResults();
        assertTrue( !results.getInvokerResults().isEmpty() );

        InvokerResult invokerResult = results.getInvokerResults().get( 0 );
        InvokerResult found = mavenInvokerBuildAction.getResult( URLEncoder.encode( invokerResult.project, "UTF-8"));
        assertNotNull( found );
        assertEquals( invokerResult.name, found.name );
        assertNotNull( invokerResult.log );



    }

    @Test
    public void two_invoke_on_action_default_configuration()
        throws Exception
    {
        WorkflowJob j = rule.jenkins.createProject( WorkflowJob.class, "configured_patterns" );

        j.setDefinition( new CpsFlowDefinition( "stage('first') {\n" + //
                                                    "  node {\n" + //
                                                    "     stage ('First Call'){ \n" + //
                                                    "      maven_invoker() \n" + //
                                                    "     } \n" + //
                                                    "     stage ('Second Call'){ \n" + //
                                                    "       maven_invoker() \n" + //
                                                    "     } \n" + //
                                                    "  }\n" + //
                                                    "}\n", true ) );

        // copy test resources
        FilePath ws = rule.jenkins.getWorkspaceFor( j);

        FilePath reports = new FilePath( new File( "src/test/resources/invoker-reports") );
        FilePath reportsDir = ws.child( "target" ).child( "invoker-reports" );
        reportsDir.mkdirs();
        reports.copyRecursiveTo( reportsDir );

        FilePath builds = new FilePath( new File( "src/test/resources/it") );
        FilePath buildsDir = ws.child( "target" ).child( "it" );
        buildsDir.mkdirs();
        builds.copyRecursiveTo( buildsDir );


        WorkflowRun r = j.scheduleBuild2( 0).waitForStart();
        rule.assertBuildStatus( Result.UNSTABLE, rule.waitForCompletion( r));
        List<MavenInvokerBuildAction> actions = r.getActions( MavenInvokerBuildAction.class );
        assertEquals(1,actions.size());

        MavenInvokerBuildAction mavenInvokerBuildAction = r.getAction( MavenInvokerBuildAction.class );
        Assert.assertEquals( 4, mavenInvokerBuildAction.getRunTests());
        Assert.assertEquals( 2, mavenInvokerBuildAction.getPassedTestCount());
        Assert.assertEquals( 2, mavenInvokerBuildAction.getFailCount());

        MavenInvokerResults results = mavenInvokerBuildAction.getMavenInvokerResults();
        assertTrue( !results.getInvokerResults().isEmpty() );

        InvokerResult invokerResult = results.getInvokerResults().get( 0 );
        InvokerResult found = mavenInvokerBuildAction.getResult( URLEncoder.encode( invokerResult.project, "UTF-8"));
        assertNotNull( found );
        assertEquals( invokerResult.name, found.name );
        assertNotNull( invokerResult.log );

    }

}
