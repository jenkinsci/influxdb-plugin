package jenkinsci.plugins.influxdb.generators;

import hudson.model.*;
import hudson.model.labels.LabelAtom;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mathieu Delrocq
 */
class AgentPointGeneratorTest extends PointGeneratorBaseTest {

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

    @BeforeEach
    void before() {
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
        flowNodeList = new ArrayList<>();
        flowNodeList.add(flowNode1);
        flowNodeList.add(flowNode2);
        workspaceAction1 = Mockito.mock(WorkspaceAction.class);
        workspaceAction2 = Mockito.mock(WorkspaceAction.class);
        Set<LabelAtom> labels = new HashSet<>();
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
    void pipeline_agent_present() {
        AgentPointGenerator gen = new AgentPointGenerator(pipelineBuild, listener, measurementRenderer, currTime,
                StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(gen.hasReport());
        AbstractPoint[] points = gen.generate();
        assertTrue(points != null && points.length != 0);
        assertTrue(points[0].getV1v2Point().hasFields());
        assertTrue(allLineProtocolsContain(points[0], "agent_name=\"node_name2\""));
        assertTrue(allLineProtocolsContain(points[0], "agent_label=\"node_label\""));
        assertTrue(allLineProtocolsContain(points[1], "agent_name=\"node_name\""));
        assertTrue(allLineProtocolsContain(points[1], "agent_label=\"node_label\""));
    }

    @Test
    void abstractbuild_agent_present() {
        AgentPointGenerator gen = new AgentPointGenerator(abstractBuild, listener, measurementRenderer, currTime,
                StringUtils.EMPTY, CUSTOM_PREFIX);
        assertTrue(gen.hasReport());
        AbstractPoint[] points = gen.generate();
        assertTrue(points != null && points.length != 0);
        assertTrue(points[0].getV1v2Point().hasFields());
        assertTrue(allLineProtocolsContain(points[0], "agent_name=\"node_name\""));
        assertTrue(allLineProtocolsContain(points[0], "agent_label=\"node_label\""));
    }
}
