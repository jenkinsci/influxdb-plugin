package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

public class JenkinsBasePointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    private final Run<?, ?> build;
    private final String customPrefix;

    public JenkinsBasePointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix, Run<?, ?> build) {
        super(projectNameRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate() {
        boolean results = hasTestResults(build);
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;
        Point point = (results) ? generatePointWithTestResults(dt) : generatePointWithoutTestResults(dt);
        return new Point[] {point};
    }

    private Point generatePointWithoutTestResults(long dt) {
        Point point = buildPoint(measurementName("jenkins_data"), customPrefix, build)
            .field(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
            .field(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message)
            .field(PROJECT_BUILD_HEALTH, build.getParent().getBuildHealth().getScore())
            .build();

        return point;
    }

    private Point generatePointWithTestResults(long dt) {
        Point point = buildPoint(measurementName("jenkins_data"), customPrefix , build)
            .field(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
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
