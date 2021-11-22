package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JUnitPointGenerator extends AbstractPointGenerator{

    private static final String JUNIT_SUITE_NAME = "suite_name";
    private static final String JUNIT_TEST_NAME = "test_name";
    private static final String JUNIT_TEST_CLASS_FULL_NAME = "test_class_full_name";
    private static final String JUNIT_PIPELINE_STEP = "pipeline_step";
    private static final String JUNIT_TEST_STATUS = "test_status";
    private static final String JUNIT_TEST_STATUS_ORDINAL = "test_status_ordinal";
    private static final String JUNIT_DURATION = "test_duration";
    private static final String JUNIT_COUNT = "test_count";

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
                    .addField(JUNIT_TEST_NAME, caseResult.getName())
                    .addField(JUNIT_TEST_CLASS_FULL_NAME, caseResult.getClassName())
                    .addField(JUNIT_PIPELINE_STEP, getCaseResultEnclosingFlowNodeString(caseResult))
                    .addField(JUNIT_TEST_STATUS, caseResult.getStatus().toString())
                    .addField(JUNIT_TEST_STATUS_ORDINAL, caseResult.getStatus().ordinal())
                    .addField(JUNIT_DURATION, caseResult.getDuration())
                    .addField(JUNIT_COUNT, 1L)
                    .addTag(JUNIT_SUITE_NAME, caseResult.getSuiteResult().getName())
                    .addTag(JUNIT_TEST_NAME, caseResult.getName())
                    .addTag(JUNIT_TEST_CLASS_FULL_NAME, caseResult.getClassName())
                    .addTag(JUNIT_PIPELINE_STEP, getCaseResultEnclosingFlowNodeString(caseResult))
                    .addTag(JUNIT_TEST_STATUS, caseResult.getStatus().toString());
            points.add(point);
        }

        return points.toArray(new Point[0]);
    }

    private String getCaseResultEnclosingFlowNodeString(CaseResult caseResult) {
        if(!caseResult.getEnclosingFlowNodeNames().isEmpty()) {
            return StringUtils.join(new ReverseListIterator(caseResult.getEnclosingFlowNodeNames()), " / ");
        }
        return "";
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
