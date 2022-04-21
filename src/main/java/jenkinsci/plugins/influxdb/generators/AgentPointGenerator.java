package jenkinsci.plugins.influxdb.generators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.collections.CollectionUtils;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;

import com.influxdb.client.write.Point;

import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class AgentPointGenerator extends AbstractPointGenerator {

    private static final String AGENT_NAME = "agent_name";
    private static final String AGENT_LABEL = "agent_label";

    private List<AgentPoint> agentPoints;
    private String customPrefix;

    public AgentPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer,
            long timestamp, String jenkinsEnvParameterTag, String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.agentPoints = getAgentPoints(build);
        this.customPrefix = customPrefix;
    }

    @Override
    public boolean hasReport() {
        return CollectionUtils.isEmpty(agentPoints);
    }

    private List<AgentPoint> getAgentPoints(Run<?, ?> build) {
        if (build instanceof AbstractBuild) {
            return getAgentFromAbstractBuild((AbstractBuild<?, ?>) build);
        } else if (build instanceof FlowExecutionOwner.Executable) {
            return getAgentsFromPipeline((FlowExecutionOwner.Executable) build);
        }
        return new ArrayList<>();
    }

    private List<AgentPoint> getAgentFromAbstractBuild(AbstractBuild<?, ?> build) {
        List<AgentPoint> agentPointsList = new ArrayList<>();
        Node node = build.getBuiltOn();
        if (node != null) {
            agentPointsList.add(new AgentPoint(node.getDisplayName(), node.getLabelString()));
        }
        return agentPointsList;
    }

    private List<AgentPoint> getAgentsFromPipeline(FlowExecutionOwner.Executable build) {
        List<AgentPoint> agentPointsList = new ArrayList<>();
        FlowExecutionOwner flowExecutionOwner = build.asFlowExecutionOwner();
        if (flowExecutionOwner != null) {
            FlowExecution flowExecution = flowExecutionOwner.getOrNull();
            if (flowExecution != null) {
                FlowGraphWalker graphWalker = new FlowGraphWalker(flowExecution);
                graphWalker.forEach(flowNode -> {
                    WorkspaceAction workspaceAction = flowNode.getAction(WorkspaceAction.class);
                    if (null != workspaceAction) {
                        Set<LabelAtom> labels = workspaceAction.getLabels();
                        StringJoiner labelString = new StringJoiner(", ");
                        labelString.setEmptyValue("");
                        if (null != labels) {
                            labels.forEach(label -> {
                                labelString.add(label.getName());
                            });
                        }
                        String nodeName = workspaceAction.getNode();
                        agentPointsList.add(new AgentPoint(nodeName, labelString.toString()));
                    }
                });
            }
        }
        return agentPointsList;
    }

    @Override
    public Point[] generate() {
        List<Point> points = new ArrayList<>();
        agentPoints.forEach(agentPoint -> {
            Point point = buildPoint("agent_data", customPrefix, build)//
                    .addField(AGENT_NAME, agentPoint.getName())//
                    .addField(AGENT_LABEL, agentPoint.getLabels());
            points.add(point);
        });
        return points.toArray(new Point[0]);
    }

    public String getFirstAgent() {
        return !CollectionUtils.isEmpty(agentPoints) ? agentPoints.get(0).getName() : "";
    }

    private class AgentPoint {

        private String name;
        private String labels;

        public AgentPoint(String name, String labels) {
            this.name = name;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public String getLabels() {
            return labels;
        }
    }

}
