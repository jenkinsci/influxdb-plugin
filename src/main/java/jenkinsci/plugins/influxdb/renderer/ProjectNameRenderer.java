package jenkinsci.plugins.influxdb.renderer;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import hudson.model.Run;

public class ProjectNameRenderer implements MeasurementRenderer<Run<?, ?>> {

    private String customPrefix;

    public ProjectNameRenderer(String customPrefix) {
        this.customPrefix = Strings.emptyToNull(customPrefix);
    }

    @Override
    public String render(Run<?, ?> input) {
        return measurementName(projectName(customPrefix, input));
    }

    protected String projectName(String prefix, Run<?, ?> build) {
        return Joiner
                .on("_")
                .join(Strings.emptyToNull(prefix), build
                        .getParent()
                        .getName());
    }

    protected String measurementName(String measurement) {
        //influx disallows "-" in measurement names.
        return measurement.replaceAll("-", "_");
    }
}
