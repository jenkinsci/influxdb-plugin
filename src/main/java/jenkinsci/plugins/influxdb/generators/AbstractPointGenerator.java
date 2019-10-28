package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPointGenerator implements PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_PATH = "project_path";
    public static final String BUILD_NUMBER = "build_number";
    public static final String CUSTOM_PREFIX = "prefix";

    protected final long timestamp;

    private final MeasurementRenderer projectNameRenderer;

    public AbstractPointGenerator(MeasurementRenderer projectNameRenderer, long timestamp) {
        this.projectNameRenderer = Objects.requireNonNull(projectNameRenderer);
        this.timestamp = timestamp;
    }

    @Override
    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build, long timestamp) {
        String projectName = projectNameRenderer.render(build);
        String projectPath = build.getParent().getRelativeNameFrom(Jenkins.getInstance());

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

        return builder;
    }

    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build) {
        return buildPoint(name, customPrefix, build, timestamp);
    }
}
