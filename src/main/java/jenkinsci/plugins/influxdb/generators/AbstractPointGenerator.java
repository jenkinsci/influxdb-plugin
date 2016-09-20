package jenkinsci.plugins.influxdb.generators;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import hudson.model.Run;
import org.influxdb.dto.Point;

public abstract class AbstractPointGenerator implements PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    @Override
    public Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build) {
        return Point
                .measurement(name)
                .addField(PROJECT_NAME, projectName(customPrefix, build))
                .addField(BUILD_NUMBER, build.getNumber())
                .tag(PROJECT_NAME, build
                        .getParent()
                        .getName());
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
