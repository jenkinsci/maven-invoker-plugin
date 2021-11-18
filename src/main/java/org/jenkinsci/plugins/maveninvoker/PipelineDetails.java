package org.jenkinsci.plugins.maveninvoker;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for recording additional Pipeline-related arguments needed for test parsing and test results.
 */
public class PipelineDetails
    implements Serializable
{
    private String nodeId;

    private List<String> enclosingBlockNames = new ArrayList<>( );

    @CheckForNull
    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId( @NonNull String nodeId )
    {
        this.nodeId = nodeId;
    }

    public List<String> getEnclosingBlockNames()
    {
        return enclosingBlockNames;
    }

    public void setEnclosingBlockNames( List<String> enclosingBlockNames )
    {
        this.enclosingBlockNames = enclosingBlockNames;
    }

    private static final long serialVersionUID = 1L;
}
