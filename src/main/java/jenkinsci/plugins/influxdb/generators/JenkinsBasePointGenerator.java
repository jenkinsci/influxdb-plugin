package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.List;

public class JenkinsBasePointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    private final Run<?, ?> build;

    public JenkinsBasePointGenerator(Run<?, ?> build) {
        this.build = build;
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate() {
        boolean results = hasTestResults(build);
        Point point = (results) ? generatePointWithTestResults() : generatePointWithoutTestResults();
        return new Point[] {point};
    }

    private Point generatePointWithoutTestResults() {
        Point point = Point.measurement("jenkins_data")
            .field(BUILD_NUMBER, build.getNumber())
            .field(PROJECT_NAME, build.getParent().getName())
            .field(BUILD_TIME, build.getDuration())
            .field(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message)
            .field(PROJECT_BUILD_HEALTH, build.getParent().getBuildHealth().getScore())
            .build();

        return point;
    }

    private Point generatePointWithTestResults() {
        Point point = Point.measurement("jenkins_data")
            .field(BUILD_NUMBER, build.getNumber())
            .field(PROJECT_NAME, build.getParent().getName())
            .field(BUILD_TIME, build.getDuration())
            .field(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message)
            .field(PROJECT_BUILD_HEALTH, build.getParent().getBuildHealth().getScore())
            .field(TESTS_FAILED, build.getAction(AbstractTestResultAction.class).getFailCount())
            .field(TESTS_SKIPPED, build.getAction(AbstractTestResultAction.class).getSkipCount())
            .field(TESTS_TOTAL, build.getAction(AbstractTestResultAction.class).getTotalCount())
            .build();

        return point;
    }

    private boolean hasTestResults(Run<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }
}
