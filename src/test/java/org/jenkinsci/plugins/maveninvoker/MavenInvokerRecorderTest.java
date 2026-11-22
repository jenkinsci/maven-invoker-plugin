package org.jenkinsci.plugins.maveninvoker;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class MavenInvokerRecorderTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void parse_file() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("foo");

        p.getBuildersList().add(new TestBuilder() {
            @Override
            @SuppressWarnings("null")
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,
                    IOException {
                build.getWorkspace().child("src/test/resources/invoker-reports/BUILD-simple-jsp.xml")
                        .copyFrom(getClass().getResource("/invoker-reports/BUILD-simple-jsp.xml"));
                build.getWorkspace().child("src/test/resources/invoker-reports/BUILD-simple-jsp-fail.xml")
                        .copyFrom(getClass().getResource("/invoker-reports/BUILD-simple-jsp-fail.xml"));
                return true;
            }
        });
        p.getPublishersList().add(new MavenInvokerRecorder("src/test/resources/invoker-reports/*.xml"));
        FreeStyleBuild run = j.assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0));
        MavenInvokerBuildAction mavenInvokerBuildAction = run.getAction(MavenInvokerBuildAction.class);
        assertEquals(2, mavenInvokerBuildAction.getRunTests());
        assertEquals(1, mavenInvokerBuildAction.getPassedTestCount());
        assertEquals(1, mavenInvokerBuildAction.getFailCount());
    }

}
