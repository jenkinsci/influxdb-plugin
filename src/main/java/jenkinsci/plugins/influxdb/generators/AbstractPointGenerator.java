package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.Objects;

public abstract class AbstractPointGenerator implements PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String PROJECT_PATH = "project_path";
    public static final String BUILD_NUMBER = "build_number";
    public static final String CUSTOM_PREFIX = "prefix";

    private MeasurementRenderer projectNameRenderer;

    public AbstractPointGenerator(MeasurementRenderer projectNameRenderer) {
        this.projectNameRenderer = Objects.requireNonNull(projectNameRenderer);
    }

    @Override
    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build) {
        final String renderedProjectName = projectNameRenderer.render(build);
        Point.Builder builder = Point
                .measurement(name)
                .addField(PROJECT_NAME, renderedProjectName)
                .addField(PROJECT_PATH, build.getParent().getRelativeNameFrom(Jenkins.getInstance()))
                .addField(BUILD_NUMBER, build.getNumber())
                .tag(PROJECT_NAME, renderedProjectName);

        if (customPrefix != null && !customPrefix.isEmpty())
            builder = builder.tag(CUSTOM_PREFIX, measurementName(customPrefix));

        return builder;

    }

    protected String measurementName(String measurement) {
        //influx discourages "-" in measurement names.
        return measurement.replaceAll("-", "_");
    }


}
