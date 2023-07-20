package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public abstract class AbstractPointGenerator implements PointGenerator {

    public static final String PROJECT_NAMESPACE = "project_namespace";
    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_PATH = "project_path";
    public static final String INSTANCE = "instance";
    public static final String BUILD_NUMBER = "build_number";
    public static final String CUSTOM_PREFIX = "prefix";

    protected final long timestamp;
    protected final Run<?, ?> build;
    protected final TaskListener listener;
    private final ProjectNameRenderer projectNameRenderer;
    private final String jenkinsEnvParameterTag;
    private final WritePrecision precision = WritePrecision.NS;

    public AbstractPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer, long timestamp, String jenkinsEnvParameterTag) {
        this.build = build;
        this.listener = listener;
        this.projectNameRenderer = Objects.requireNonNull(projectNameRenderer);
        this.timestamp = timestamp;
        this.jenkinsEnvParameterTag = jenkinsEnvParameterTag;
    }

    @Override
    public Point buildPoint(String name, String customPrefix, Run<?, ?> build, long timestamp) {
        Jenkins instance = Jenkins.getInstanceOrNull();
        String projectName = projectNameRenderer.render(build);
        String projectPath = build.getParent().getRelativeNameFrom(instance);

        Point point = Point
                .measurement(name)
                .addField(PROJECT_NAME, projectName)
                .addField(PROJECT_PATH, projectPath)
                .addField(BUILD_NUMBER, build.getNumber())
                .time(timestamp, precision);

        if (customPrefix != null && !customPrefix.isEmpty()) {
            point.addTag(CUSTOM_PREFIX, customPrefix);
        }

        point.addTag(PROJECT_NAME, projectName);
        point.addTag(PROJECT_PATH, projectPath);
        point.addTag(INSTANCE, instance != null ? instance.getRootUrl() : "");
        point.addTag(PROJECT_NAMESPACE, projectPath.split("/")[0]);


        if (StringUtils.isNotBlank(jenkinsEnvParameterTag)) {
            Properties tagProperties = parsePropertiesString(jenkinsEnvParameterTag);
            Map<String, String> tagMap = resolveEnvParameterAndTransformToMap(tagProperties);
            point.addTags(tagMap);
        }

        return point;
    }

    public Point buildPoint(String name, String customPrefix, Run<?, ?> build) {
        return buildPoint(name, customPrefix, build, timestamp);
    }

    protected Properties parsePropertiesString(String propertiesString) {
        Properties properties = new Properties();
        try {
            StringReader reader = new StringReader(propertiesString);
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    protected Map<String, String> resolveEnvParameterAndTransformToMap(Properties properties) {
        return properties.entrySet().stream().collect(
                Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> {
                            String value = e.getValue().toString();
                            return containsEnvParameter(value) ? resolveEnvParameter(value) : value;
                        }
                )
        );
    }

    private boolean containsEnvParameter(String value) {
        return StringUtils.length(value) > 3 && StringUtils.contains(value, "${");
    }

    private String resolveEnvParameter(String stringValue) {
        try {
            EnvVars envVars = build.getEnvironment(listener);
            return StringSubstitutor.replace(stringValue, envVars);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return stringValue;
    }
}
