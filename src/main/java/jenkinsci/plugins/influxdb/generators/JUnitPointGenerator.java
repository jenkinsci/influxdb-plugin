package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.List;

public class JUnitPointGenerator extends AbstractPointGenerator{

    private static final String JUNIT_SUITE_NAME = "suite_name";
    private static final String JUNIT_TEST_NAME = "test_name";
    private static final String JUNIT_TEST_STATUS = "test_status";
    private static final String JUNIT_TEST_STATUS_ORDINAL = "test_status_ordinal";
    private static final String JUNIT_DURATION = "test_duration";

    private final String customPrefix;
    private final TaskListener listener;

    private final EnvVars env;

    public JUnitPointGenerator(Run<?, ?> build, TaskListener listener,
                               ProjectNameRenderer projectNameRenderer,
                               long timestamp, String jenkinsEnvParameterTag,
                               String customPrefix, EnvVars env) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.listener = listener;
        this.env = env;
    }

    /**
     * @return true, if environment variable LOG_JUNIT_RESULTS is set to true and JUnit Reports exist
     */
    @Override
    public boolean hasReport() {
        return Boolean.parseBoolean(env.getOrDefault("LOG_JUNIT_RESULTS", "false")) && hasTestResults(build);
    }

    @Override
    public Point[] generate() {

        List<Point> points = new ArrayList<>();

        // iterate each caseResult to get suiteName, testName and testStatus
        List<CaseResult> allTestResults = getAllTestResults(build);

        for (CaseResult caseResult : allTestResults) {
            Point point = buildPoint("junit_data", customPrefix, build)
                    .addField(JUNIT_SUITE_NAME, caseResult.getSuiteResult().getName())
                    .addField(JUNIT_TEST_NAME, caseResult.getDisplayName())
                    .addField(JUNIT_TEST_STATUS, caseResult.getStatus().toString())
                    .addField(JUNIT_TEST_STATUS_ORDINAL, caseResult.getStatus().ordinal())
                    .addField(JUNIT_DURATION, caseResult.getDuration())
                    .tag(JUNIT_SUITE_NAME, caseResult.getSuiteResult().getName())
                    .tag(JUNIT_TEST_NAME, caseResult.getDisplayName())
                    .tag(JUNIT_TEST_STATUS, caseResult.getStatus().toString())
                    .build();
            points.add(point);
        }

        return points.toArray(new Point[points.size()]);
    }

    private List<CaseResult> getAllTestResults(Run<?, ?> build) {
        //get tests from build
        AbstractTestResultAction testResultAction = build.getAction(AbstractTestResultAction.class);

        // create a list that contains all tests
        List<CaseResult> allTestResults = new ArrayList<>();
        allTestResults.addAll(testResultAction.getFailedTests());
        allTestResults.addAll(testResultAction.getSkippedTests());
        allTestResults.addAll(testResultAction.getPassedTests());

        return allTestResults;
    }

    private boolean hasTestResults(Run<?, ?> build) {
        return build.getAction(AbstractTestResultAction.class) != null;
    }
}
