package jenkinsci.plugins.influxdb.renderer;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import hudson.model.Run;

public class ProjectNameRenderer implements MeasurementRenderer<Run<?, ?>> {

    private String customPrefix;
    private String customProjectName;

    public ProjectNameRenderer(String customPrefix, String customProjectName) {
        this.customPrefix = Strings.emptyToNull(customPrefix);
        this.customProjectName = Strings.emptyToNull(customProjectName);
    }

    @Override
    public String render(Run<?, ?> input) {
        return measurementName(projectName(customPrefix, customProjectName, input));
    }

    protected String projectName(String prefix, String projectName, Run<?, ?> build) {
        if (projectName == null) {
            projectName = build.getParent().getName();
        }
        return Joiner
                .on("_")
                .skipNulls()
                .join(Strings.emptyToNull(prefix), Strings.emptyToNull(projectName));
    }

    protected String measurementName(String measurement) {
        // InfluxDB disallows "-" in measurement names.
        return measurement.replaceAll("-", "_");
    }
}
