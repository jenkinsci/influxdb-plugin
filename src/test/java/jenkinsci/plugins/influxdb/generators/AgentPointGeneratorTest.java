package jenkinsci.plugins.influxdb.generators;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.influxdb.client.write.Point;

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

/**
 * @author Mathieu Delrocq
 */
public class AgentPointGeneratorTest {

    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String JOB_NAME = "job_name";

    private static final String NODE_NAME = "node_name";
    private static final String NODE_LABEL = "node_label";

    private Run<?, ?> abstractBuild;
    private WorkflowRun pipelineBuild;
    private Node node;
    private FlowNode flowNode1;
    private FlowNode flowNode2;
    private List<FlowNode> flowNodeList;
    private FlowExecutionOwner flowExecutionOwner;
    private FlowExecution flowExecution;
    private WorkspaceAction workspaceAction1;
    private WorkspaceAction workspaceAction2;
    private TaskListener listener;
    private long currTime;
    private ProjectNameRenderer measurementRenderer;

    @Before
    public void before() throws Exception {
        // Global Mocks
        listener = Mockito.mock(TaskListener.class);
        currTime = System.currentTimeMillis();
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        Job<?, ?> job = Mockito.mock(Job.class);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(job.getBuildHealth()).thenReturn(new HealthReport());

        // Mocks for AbstractBuild
        abstractBuild = Mockito.mock(AbstractBuild.class);
        Mockito.doReturn(job).when(abstractBuild).getParent();
        node = Mockito.mock(Node.class);
        Mockito.when(((AbstractBuild<?, ?>) abstractBuild).getBuiltOn()).thenReturn(node);
        Mockito.when(node.getDisplayName()).thenReturn(NODE_NAME);
        Mockito.when(node.getLabelString()).thenReturn(NODE_LABEL);

        // Mock for Pipeline
        pipelineBuild = Mockito.mock(WorkflowRun.class);
        Mockito.doReturn(job).when(pipelineBuild).getParent();
        flowNode1 = Mockito.mock(FlowNode.class);
        flowNode2 = Mockito.mock(FlowNode.class);
        flowExecutionOwner = Mockito.mock(FlowExecutionOwner.class);
        flowExecution = Mockito.mock(FlowExecution.class);
        flowNodeList = new ArrayList<FlowNode>();
        flowNodeList.add(flowNode1);
        flowNodeList.add(flowNode2);
        workspaceAction1 = Mockito.mock(WorkspaceAction.class);
        workspaceAction2 = Mockito.mock(WorkspaceAction.class);
        Set<LabelAtom> labels = new HashSet<LabelAtom>();
        LabelAtom label = new LabelAtom(NODE_LABEL);
        labels.add(label);
        Mockito.when(pipelineBuild.asFlowExecutionOwner()).thenReturn(flowExecutionOwner);
        Mockito.when(flowExecutionOwner.getOrNull()).thenReturn(flowExecution);
        Mockito.when(flowExecution.getCurrentHeads()).thenReturn(flowNodeList);
        Mockito.when(flowNode1.getAction(WorkspaceAction.class)).thenReturn(workspaceAction1);
        Mockito.when(flowNode2.getAction(WorkspaceAction.class)).thenReturn(workspaceAction2);
        Mockito.when(workspaceAction1.getNode()).thenReturn(NODE_NAME);
        Mockito.when(workspaceAction1.getLabels()).thenReturn(labels);
        Mockito.when(workspaceAction2.getNode()).thenReturn(NODE_NAME + "2");
        Mockito.when(workspaceAction2.getLabels()).thenReturn(labels);
    }

    @Test
    public void pipeline_agent_present() {
        AgentPointGenerator gen = new AgentPointGenerator(pipelineBuild, listener, measurementRenderer, currTime,
                StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(gen.hasReport());
        Point[] points = gen.generate();
        assertTrue(points != null && points.length != 0);
        assertTrue(points[0].hasFields());
        String lineProtocol1 = points[0].toLineProtocol();
        String lineProtocol2 = points[1].toLineProtocol();
        assertTrue(lineProtocol1.contains("agent_name=\"node_name2\""));
        assertTrue(lineProtocol1.contains("agent_label=\"node_label\""));
        assertTrue(lineProtocol2.contains("agent_name=\"node_name\""));
        assertTrue(lineProtocol2.contains("agent_label=\"node_label\""));
    }

    @Test
    public void abstractbuild_agent_present() {
        AgentPointGenerator gen = new AgentPointGenerator(abstractBuild, listener, measurementRenderer, currTime,
                StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(gen.hasReport());
        Point[] points = gen.generate();
        assertTrue(points != null && points.length != 0);
        assertTrue(points[0].hasFields());
        String lineProtocol = points[0].toLineProtocol();
        assertTrue(lineProtocol.contains("agent_name=\"node_name\""));
        assertTrue(lineProtocol.contains("agent_label=\"node_label\""));
    }
}
