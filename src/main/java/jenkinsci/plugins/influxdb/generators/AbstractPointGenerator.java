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
    protected final boolean replaceDashWithUnderscore;

    private final MeasurementRenderer projectNameRenderer;

    public AbstractPointGenerator(MeasurementRenderer projectNameRenderer, long timestamp, boolean replaceDashWithUnderscore) {
        this.projectNameRenderer = Objects.requireNonNull(projectNameRenderer);
        this.timestamp = timestamp;
        this.replaceDashWithUnderscore = replaceDashWithUnderscore;
    }

    @Override
    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build, long timestamp) {
        String projectName;

        if (replaceDashWithUnderscore) {
            projectName = projectNameRenderer.render(build);
        } else if (customPrefix != null) {
            projectName = customPrefix + "_" + build.getParent().getName();
        } else {
            projectName = build.getParent().getName();
        }

        String projectPath = build.getParent().getRelativeNameFrom(Jenkins.getInstance());

        Point.Builder builder = Point
                .measurement(name)
                .addField(PROJECT_NAME, projectName)
                .addField(PROJECT_PATH, projectPath)
                .addField(BUILD_NUMBER, build.getNumber())
                .time(timestamp, TimeUnit.NANOSECONDS);

        if (customPrefix != null && !customPrefix.isEmpty()) {
            builder.tag(CUSTOM_PREFIX, replaceDashWithUnderscore ? measurementName(customPrefix) : customPrefix);
        }

        builder.tag(PROJECT_NAME, projectName);
        builder.tag(PROJECT_PATH, projectPath);

        return builder;
    }

    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build) {
        return buildPoint(name, customPrefix, build, timestamp);
    }

    protected String measurementName(String measurement) {
        // InfluxDB discourages "-" in measurement names.
        return measurement.replace('-', '_');
    }
}
