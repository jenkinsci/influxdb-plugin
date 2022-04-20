package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;

import com.influxdb.client.write.Point;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class AgentPointGenerator extends AbstractPointGenerator {

    private static final String KUBERNETES_AGENT_LOG_PATTERN = Pattern.quote("Agent .* is provisioned from template .*");
    private static final String DOCKER_AGENT_LOG_PATTERN = Pattern.quote("Building remotely on .* on .* (.*) in workspace .*");
    private static final String AGENT_NAME = "agent_label";

    private Set<String> nodesLabels;
    private String customPrefix;

    public AgentPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer,
            long timestamp, String jenkinsEnvParameterTag, String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.nodesLabels = getNodesLabels();
        this.customPrefix = customPrefix;
        build.getExecutor().getOwner().getNode().getLabelString();
        Jenkins.get().getNode("").getLabelString();
        try {
            EnvVars env = build.getEnvironment(listener);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasReport() {
        return CollectionUtils.isEmpty(nodesLabels);
    }

    @Override
    public Point[] generate() {
        List<Point> points = new ArrayList<>();
        nodesLabels.forEach(nodeName -> {
            Point point = buildPoint("agent_data", customPrefix, build)//
                    .addField(AGENT_NAME, nodeName);
            points.add(point);
        });
        return points.toArray(new Point[0]);
    }

    private Set<String> getNodesLabels() {
        if (null == nodesLabels) {
            return getNodesLabelsFromLogs();
        }
        return nodesLabels;
    }

    public String getFirstAgent() {
        return !CollectionUtils.isEmpty(nodesLabels) ? nodesLabels.iterator().next() : "";
    }

    /**
     * Retrieve agent name in the log of the build
     * 
     * @return agent name
     */
    private Set<String> getNodesLabelsFromLogs() {
        Set<String> agentsLabels = new LinkedHashSet<>();
        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
            String line;
            Matcher match;
            String[] splitLine;
            final Pattern kubernetesAgentPattern = Pattern.compile(KUBERNETES_AGENT_LOG_PATTERN);
            final Pattern dockerAgentPattern = 
            while ((line = br.readLine()) != null) {
                match = kubernetesAgentPattern.matcher(line);
                if (match.matches()) {
                    splitLine = line.split(" ");
                    agentsLabels.add(splitLine[splitLine.length - 1]);
                    break;
                }
            }
        } catch (IOException e) {
        }
        return agentsLabels;
    }

}
