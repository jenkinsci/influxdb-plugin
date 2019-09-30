package jenkinsci.plugins.influxdb.renderer;

import hudson.model.Run;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectNameRenderer implements MeasurementRenderer<Run<?, ?>> {

    private final String customPrefix;
    private final String customProjectName;

    public ProjectNameRenderer(String customPrefix, String customProjectName) {
        this.customPrefix = StringUtils.trimToNull(customPrefix);
        this.customProjectName = StringUtils.trimToNull(customProjectName);
    }

    @Override
    public String render(Run<?, ?> input) {
        return measurementName(projectName(customPrefix, customProjectName, input));
    }

    private String projectName(String prefix, String projectName, Run<?, ?> build) {
        if (projectName == null) {
            projectName = StringUtils.trimToNull(build.getParent().getName());
        }

        return Stream.of(prefix, projectName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));
    }

    private String measurementName(String measurement) {
        // InfluxDB discourages "-" in measurement names.
        return measurement.replace('-', '_');
    }
}
