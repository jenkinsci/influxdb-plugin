package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import org.influxdb.dto.Point;

public abstract class AbstractPointGenerator implements  PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    @Override
    public Point.Builder buildPoint(String name, Run<?,?> build) {
        return Point
                .measurement(name)
                .addField(PROJECT_NAME, build
                        .getParent()
                        .getName())
                .addField(BUILD_NUMBER, build.getNumber())
                .tag(PROJECT_NAME, build
                        .getParent()
                        .getName());
    }

    protected String measurementName(String measurement) {
        //influx disallows "-" in measurement names.
        return measurement.replaceAll("-", "_");
    }
}
