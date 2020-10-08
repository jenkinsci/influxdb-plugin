package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.influxdb.dto.Point;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractPointGenerator implements PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_PATH = "project_path";
    public static final String BUILD_NUMBER = "build_number";
    public static final String CUSTOM_PREFIX = "prefix";

    protected final long timestamp;
    protected final Run<?, ?> build;
    protected final TaskListener listener;
    private final MeasurementRenderer projectNameRenderer;
    private final String jenkinsEnvParameterTag;

    public AbstractPointGenerator(Run<?, ?> build, TaskListener listener, MeasurementRenderer projectNameRenderer, long timestamp, String jenkinsEnvParameterTag) {
        this.build = build;
        this.listener = listener;
        this.projectNameRenderer = Objects.requireNonNull(projectNameRenderer);
        this.timestamp = timestamp;
        this.jenkinsEnvParameterTag = jenkinsEnvParameterTag;
    }

    @Override
    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build, long timestamp) {
        String projectName = projectNameRenderer.render(build);
        String projectPath = build.getParent().getRelativeNameFrom(Jenkins.get());

        Point.Builder builder = Point
                .measurement(name)
                .addField(PROJECT_NAME, projectName)
                .addField(PROJECT_PATH, projectPath)
                .addField(BUILD_NUMBER, build.getNumber())
                .time(timestamp, TimeUnit.NANOSECONDS);

        if (customPrefix != null && !customPrefix.isEmpty()) {
            builder.tag(CUSTOM_PREFIX, customPrefix);
        }

        builder.tag(PROJECT_NAME, projectName);
        builder.tag(PROJECT_PATH, projectPath);


        if (StringUtils.isNotBlank(jenkinsEnvParameterTag)) {
            Properties tagProperties = parsePropertiesString(jenkinsEnvParameterTag);
            Map tagMap = resolveEnvParameterAndTransformToMap(tagProperties);
            builder.tag(tagMap);
        }

        return builder;
    }

    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build) {
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
            return StrSubstitutor.replace(stringValue, envVars);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return stringValue;
    }
}
