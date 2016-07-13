package jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;

import java.util.List;

public abstract class AbstractPointGenerator implements  PointGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected void addJenkinsProjectName(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(PROJECT_NAME);
        values.add(build.getProject().getName());
    }

    protected void addJenkinsBuildNumber(AbstractBuild<?, ?> build, List<String> columnNames, List<Object> values) {
        columnNames.add(BUILD_NUMBER);
        values.add(build.getNumber());
    }
}
