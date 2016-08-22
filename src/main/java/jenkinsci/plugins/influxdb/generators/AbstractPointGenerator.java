package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;

import java.util.List;

public abstract class AbstractPointGenerator implements  PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected void addJenkinsProjectName(Run<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getParent().getName());
    }

    protected void addJenkinsBuildNumber(Run<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
    }

    protected String measurementName(String measurement) {
        //influx disallows "-" in measurement names.
        return measurement.replaceAll("-", "_");
    }
}
