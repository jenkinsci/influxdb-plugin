package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.influxdb.client.write.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

public class AgentPointGenerator extends AbstractPointGenerator {

    public AgentPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer,
            long timestamp, String jenkinsEnvParameterTag) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean hasReport() {
        return false;
    }

    @Override
    public Point[] generate() {
        // TODO Auto-generated method stub
        return null;
    }
    
//    private String getNodeName() {
//        if(StringUtils.isEmpty(nodeName)) {
//            nodeName = getNodeNameFromLogs();
//        }
//        return nodeName;
//    }
//
//    /**
//     * Retrieve agent name in the log of the build
//     * 
//     * @return agent name
//     */
//    private String getNodeNameFromLogs() {
//        String agentName = "";
//        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
//            String line;
//            Matcher match;
//            String[] splitLine;
//            final Pattern agentPattern = Pattern.compile(AGENT_LOG_PATTERN);
//
//            while ((line = br.readLine()) != null) {
//                match = agentPattern.matcher(line);
//                if (match.matches()) {
//                    splitLine = line.split(" ");
//                    agentName = splitLine.length >= 3 ? splitLine[2] : "";
//                    break;
//                }
//            }
//        } catch (IOException e) {}
//        return agentName;
//    }

}
