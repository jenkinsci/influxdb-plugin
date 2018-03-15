package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

public class JenkinsBasePointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";

    /* BUILD_RESULT BUILD_RESULT_ORDINAL BUILD_IS_SUCCESSFUL - explanation
     * SUCCESS   0 true  - The build had no errors.
     * UNSTABLE  1 true  - The build hadsome errors but they were not fatal. For example, some tests failed.
     * FAILURE   2 false - The build had a fatal error.
     * NOT_BUILT 3 false - The module was not built.
     * ABORTED   4 false - The build was manually aborted.
     */
    public static final String BUILD_RESULT = "build_result";
    public static final String BUILD_RESULT_ORDINAL = "build_result_ordinal";
    public static final String BUILD_IS_SUCCESSFUL = "build_successful";

    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String PROJECT_LAST_SUCCESSFUL = "last_successful_build";
    public static final String PROJECT_LAST_STABLE = "last_stable_build";
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
        // Build is not finished when running with pipelines. Duration must be calculated manually
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;
        // Build is not finished when running with pipelines. Set build status as unknown and ordinal
        // as something not predefined
        String result;
        int ordinal;
        if (build.getResult() == null) {
            result = "?";
            ordinal = 5;
        } else {
            result = build.getResult().toString();
            ordinal = build.getResult().ordinal;
        }

        Point.Builder point = buildPoint(measurementName("jenkins_data"), customPrefix, build);

        point.addField(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
            .addField(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message)
            .addField(BUILD_RESULT, result)
            .addField(BUILD_RESULT_ORDINAL, ordinal)
            .addField(BUILD_IS_SUCCESSFUL, ordinal < 2 ? true : false)
            .addField(PROJECT_BUILD_HEALTH, build.getParent().getBuildHealth().getScore())
            .addField(PROJECT_LAST_SUCCESSFUL, getLastSuccessfulBuild())
            .addField(PROJECT_LAST_STABLE, getLastStableBuild())
            .tag(BUILD_RESULT, result);

        if(hasTestResults(build)) {
            point.addField(TESTS_FAILED, build.getAction(AbstractTestResultAction.class).getFailCount());
            point.addField(TESTS_SKIPPED, build.getAction(AbstractTestResultAction.class).getSkipCount());
            point.addField(TESTS_TOTAL, build.getAction(AbstractTestResultAction.class).getTotalCount());
        }

        return new Point[] {point.build()};
    }

    private boolean hasTestResults(Run<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    private int getLastSuccessfulBuild() {
        if (build.getParent().getLastSuccessfulBuild() != null)
            return build.getParent().getLastSuccessfulBuild().getNumber();
        else
            return 0;
    }

    private int getLastStableBuild() {
        if (build.getParent().getLastStableBuild() != null)
            return build.getParent().getLastStableBuild().getNumber();
        else
            return 0;
    }
}
