package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class RobotFrameworkPointGenerator extends AbstractPointGenerator {

    private static final String RF_NAME = "rf_name";
    private static final String RF_FAILED = "rf_failed";
    private static final String RF_PASSED = "rf_passed";
    private static final String RF_SKIPPED = "rf_skipped";
    private static final String RF_TOTAL = "rf_total";
    private static final String RF_CRITICAL_FAILED = "rf_critical_failed";
    private static final String RF_CRITICAL_PASSED = "rf_critical_passed";
    private static final String RF_CRITICAL_TOTAL = "rf_critical_total";
    private static final String RF_CRITICAL_PASS_PERCENTAGE = "rf_critical_pass_percentage";
    private static final String RF_PASS_PERCENTAGE = "rf_pass_percentage";
    private static final String RF_SKIP_PERCENTAGE = "rf_skip_percentage";
    private static final String RF_DURATION = "rf_duration";
    private static final String RF_SUITES = "rf_suites";
    private static final String RF_SUITE_NAME = "rf_suite_name";
    private static final String RF_TESTCASES = "rf_testcases";
    private static final String RF_TAG_NAME = "rf_tag_name";

    private final String customPrefix;
    private final Map<String, RobotTagResult> tagResults;

    public RobotFrameworkPointGenerator(Run<?, ?> build, TaskListener listener,
                                        ProjectNameRenderer projectNameRenderer,
                                        long timestamp, String jenkinsEnvParameterTag,
                                        String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        tagResults = new Hashtable<>();
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Point[] generate() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Point> points = new ArrayList<>();

        points.add(generateOverviewPoint(robotBuildAction));
        points.addAll(generateSubPoints(robotBuildAction.getResult()));

        return points.toArray(new Point[0]);
    }

    private Point generateOverviewPoint(RobotBuildAction robotBuildAction) {
        return buildPoint("rf_results", customPrefix, build)
            .addField(RF_FAILED, robotBuildAction.getResult().getOverallFailed())
            .addField(RF_PASSED, robotBuildAction.getResult().getOverallPassed())
            .addField(RF_TOTAL, robotBuildAction.getResult().getOverallTotal())
            .addField(RF_SKIPPED, robotBuildAction.getResult().getOverallSkipped())
            .addField(RF_CRITICAL_FAILED, robotBuildAction.getResult().getCriticalFailed())
            .addField(RF_CRITICAL_PASSED, robotBuildAction.getResult().getCriticalPassed())
            .addField(RF_CRITICAL_TOTAL, robotBuildAction.getResult().getCriticalTotal())
            .addField(RF_CRITICAL_PASS_PERCENTAGE, robotBuildAction.getCriticalPassPercentage())
            .addField(RF_PASS_PERCENTAGE, robotBuildAction.getOverallPassPercentage())
            .addField(RF_SKIP_PERCENTAGE, robotBuildAction.getResult().getSkipPercentage())
            .addField(RF_DURATION, robotBuildAction.getResult().getDuration())
            .addField(RF_SUITES, robotBuildAction.getResult().getAllSuites().size());
    }

    private List<Point> generateSubPoints(RobotResult robotResult) {
        List<Point> subPoints = new ArrayList<>();
        TimeGenerator suiteResultTime = new TimeGenerator(timestamp);

        for (RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            long caseTimeStamp = suiteResultTime.next();
            subPoints.add(generateSuitePoint(suiteResult, caseTimeStamp));
            // To preserve the existing functionality of the case being timestamps after the
            // suiteResult, seed the new TimeGenerator with the suiteResult's time
            TimeGenerator caseResultTime = new TimeGenerator(caseTimeStamp);
            for (RobotCaseResult caseResult : suiteResult.getAllCases()) {
                Point casePoint = generateCasePoint(caseResult, caseResultTime.next());
                if (casePointExists(subPoints, casePoint)) {
                    continue;
                }
                subPoints.add(casePoint);
            }
        }

        TimeGenerator tagTime = new TimeGenerator(timestamp);
        for (Map.Entry<String, RobotTagResult> entry : tagResults.entrySet()) {
            subPoints.add(generateTagPoint(entry.getValue(), tagTime.next()));
        }
        return subPoints;
    }

    private boolean casePointExists(List<Point> subPoints, Point point) {
        for (Point p : subPoints) {
            try {
                // CasePoints are the same if all the fields are equal
                String pFields = p.toString().substring(p.toString().indexOf("fields="));
                String pointFields = point.toString().substring(point.toString().indexOf("fields="));
                if (pFields.equals(pointFields)) {
                    return true;
                }
            } catch (StringIndexOutOfBoundsException e) {
                // Handle exception
            }
        }
        return false;
    }

    private Point generateCasePoint(RobotCaseResult caseResult, long timestamp) {
        Point point = buildPoint("testcase_point", customPrefix, build, timestamp)
            .addTag(RF_NAME, caseResult.getName())
            .addField(RF_NAME, caseResult.getName())
            .addField(RF_SUITE_NAME, caseResult.getParent().getName())
            .addField(RF_CRITICAL_FAILED, caseResult.getCriticalFailed())
            .addField(RF_CRITICAL_PASSED, caseResult.getCriticalPassed())
            .addField(RF_FAILED, caseResult.getFailed())
            .addField(RF_PASSED, caseResult.getPassed())
            .addField(RF_SKIPPED, caseResult.getSkipped())
            .addField(RF_DURATION, caseResult.getDuration());

        for (String tag : caseResult.getTags()) {
            markTagResult(tag, caseResult);
        }

        return point;
    }

    private static final class RobotTagResult {

        private final String name;
        private final List<String> testCases = new ArrayList<>();
        private int failed = 0;
        private int passed = 0;
        private int skipped = 0;
        private long criticalFailed = 0;
        private long criticalPassed = 0;
        private long duration = 0;

        private RobotTagResult(String name) {
            this.name = name;
        }
    }

    private void markTagResult(String tag, RobotCaseResult caseResult) {
        if (tagResults.get(tag) == null)
            tagResults.put(tag, new RobotTagResult(tag));

        RobotTagResult tagResult = tagResults.get(tag);
        if (!tagResult.testCases.contains(caseResult.getDuplicateSafeName())) {
            tagResult.failed += caseResult.getFailed();
            tagResult.passed += caseResult.getPassed();
            tagResult.skipped += caseResult.getSkipped();
            tagResult.criticalFailed += caseResult.getCriticalFailed();
            tagResult.criticalPassed += caseResult.getCriticalPassed();
            tagResult.duration += caseResult.getDuration();
            tagResult.testCases.add(caseResult.getDuplicateSafeName());
        }
    }

    private Point generateTagPoint(RobotTagResult tagResult, long timestamp) {
        return buildPoint("tag_point", customPrefix, build, timestamp)
            .addTag(RF_TAG_NAME, tagResult.name)
            .addField(RF_TAG_NAME, tagResult.name)
            .addField(RF_CRITICAL_FAILED, (int)tagResult.criticalFailed)    // Send data as int for backwards compatibility
            .addField(RF_CRITICAL_PASSED, (int)tagResult.criticalPassed)    // Send data as int for backwards compatibility
            .addField(RF_CRITICAL_TOTAL, (int)(tagResult.criticalPassed + tagResult.criticalFailed))    // Send data as int for backwards compatibility
            .addField(RF_FAILED, tagResult.failed)
            .addField(RF_PASSED, tagResult.passed)
            .addField(RF_SKIPPED, tagResult.skipped)
            .addField(RF_TOTAL, tagResult.passed + tagResult.failed)
            .addField(RF_DURATION, tagResult.duration);
    }

    private Point generateSuitePoint(RobotSuiteResult suiteResult, long timestamp) {
        return buildPoint("suite_result", customPrefix, build, timestamp)
            .addTag(RF_SUITE_NAME, suiteResult.getName())
            .addField(RF_SUITE_NAME, suiteResult.getName())
            .addField(RF_TESTCASES, suiteResult.getAllCases().size())
            .addField(RF_CRITICAL_FAILED, suiteResult.getCriticalFailed())
            .addField(RF_CRITICAL_PASSED, suiteResult.getCriticalPassed())
            .addField(RF_CRITICAL_TOTAL, suiteResult.getCriticalTotal())
            .addField(RF_FAILED, suiteResult.getFailed())
            .addField(RF_PASSED, suiteResult.getPassed())
            .addField(RF_SKIPPED, suiteResult.getSkipped())
            .addField(RF_TOTAL, suiteResult.getTotal())
            .addField(RF_DURATION, suiteResult.getDuration());
    }
}
