package org.jenkinsci.plugins.maveninvoker.pipeline;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.maveninvoker.MavenInvokerRecorder;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MavenInvokerStep
    extends Step
{

    private String reportsFilenamePattern;

    private String invokerBuildDir;

    public MavenInvokerStep()
    {
        this.reportsFilenamePattern = MavenInvokerRecorder.DEFAULT_REPORTS_FILENAME_PATTERN;
        this.invokerBuildDir = MavenInvokerRecorder.DEFAULT_INVOKER_BUILD_DIR;
    }

    @DataBoundConstructor
    public MavenInvokerStep( String reportsFilenamePattern, String invokerBuildDir )
    {
        this.reportsFilenamePattern = reportsFilenamePattern == null ? //
            MavenInvokerRecorder.DEFAULT_REPORTS_FILENAME_PATTERN : reportsFilenamePattern;
        this.invokerBuildDir = invokerBuildDir == null ? //
            MavenInvokerRecorder.DEFAULT_INVOKER_BUILD_DIR : invokerBuildDir;
    }

    @Override
    public StepExecution start( StepContext stepContext )
        throws Exception
    {
        return new MavenInvokerStepExecution( stepContext, this );
    }

    public String getReportsFilenamePattern()
    {
        return reportsFilenamePattern;
    }

    public void setReportsFilenamePattern( String reportsFilenamePattern )
    {
        this.reportsFilenamePattern = reportsFilenamePattern;
    }

    public String getInvokerBuildDir()
    {
        return invokerBuildDir;
    }

    public void setInvokerBuildDir( String invokerBuildDir )
    {
        this.invokerBuildDir = invokerBuildDir;
    }

    @Extension
    public static class DescriptorImpl
        extends StepDescriptor
    {
        @Override
        public String getFunctionName()
        {
            return "maven_invoker";
        }

        @Override
        @Nonnull
        public String getDisplayName()
        {
            return "Archive Maven Invoker test results";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext()
        {
            Set<Class<?>> context = new HashSet<>();
            Collections.addAll(context, FilePath.class, FlowNode.class, TaskListener.class, Launcher.class);
            return Collections.unmodifiableSet(context);
        }

    }
}
