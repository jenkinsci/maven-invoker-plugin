package org.jenkinsci.plugins.maveninvoker.pipeline;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.maveninvoker.MavenInvokerBuildAction;
import org.jenkinsci.plugins.maveninvoker.MavenInvokerRecorder;
import org.jenkinsci.plugins.maveninvoker.PipelineDetails;
import org.jenkinsci.plugins.maveninvoker.results.MavenInvokerResults;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MavenInvokerStepExecution extends SynchronousNonBlockingStepExecution<MavenInvokerResults>
{

    private transient final MavenInvokerStep step;

    public MavenInvokerStepExecution( @Nonnull StepContext context, MavenInvokerStep step )
    {
        super( context );
        this.step = step;
    }

    @Override
    protected MavenInvokerResults run()
        throws Exception
    {
        MavenInvokerRecorder mavenInvokerRecorder =
            new MavenInvokerRecorder(step.getReportsFilenamePattern(), step.getInvokerBuildDir());
        FlowNode node = getContext().get( FlowNode.class);

        String nodeId = node.getId();
        PipelineDetails pipelineDetails = new PipelineDetails();
        pipelineDetails.setNodeId( nodeId );
        pipelineDetails.setEnclosingBlockNames( getEnclosingBlockNames( node.getEnclosingBlocks() ) );
        mavenInvokerRecorder.perform( getContext().get( Run.class ), getContext().get(FilePath.class), //
                                      getContext().get( Launcher.class), getContext().get( TaskListener.class ), //
                                      pipelineDetails );
        return getContext().get( Run.class ).getAction( MavenInvokerBuildAction.class ).getMavenInvokerResults();
    }


    // copy from junit plugin
    @Nonnull
    public static List<String> getEnclosingBlockNames( @Nonnull List<FlowNode> nodes) {
        List<String> names = new ArrayList<>();
        for (FlowNode n : nodes) {
            ThreadNameAction threadNameAction = n.getPersistentAction( ThreadNameAction.class);
            LabelAction labelAction = n.getPersistentAction( LabelAction.class);
            if (threadNameAction != null) {
                // If we're on a parallel branch with the same name as the previous (inner) node, that generally
                // means we're in a Declarative parallel stages situation, so don't add the redundant branch name.
                if (names.isEmpty() || !threadNameAction.getThreadName().equals(names.get(names.size()-1))) {
                    names.add(threadNameAction.getThreadName());
                }
            } else if (labelAction != null) {
                names.add(labelAction.getDisplayName());
            }
        }
        return names;
    }
}
