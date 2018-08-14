package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class RobotFrameworkPointGenerator extends AbstractPointGenerator {

    public static final String RF_NAME = "rf_name";
    public static final String RF_FAILED = "rf_failed";
    public static final String RF_PASSED = "rf_passed";
    public static final String RF_TOTAL = "rf_total";
    public static final String RF_CRITICAL_FAILED = "rf_critical_failed";
    public static final String RF_CRITICAL_PASSED = "rf_critical_passed";
    public static final String RF_CRITICAL_TOTAL = "rf_critical_total";
    public static final String RF_CRITICAL_PASS_PERCENTAGE = "rf_critical_pass_percentage";
    public static final String RF_PASS_PERCENTAGE = "rf_pass_percentage";
    public static final String RF_DURATION = "rf_duration";
    public static final String RF_SUITES = "rf_suites";
    public static final String RF_SUITE_NAME = "rf_suite_name";
    public static final String RF_TESTCASES = "rf_testcases";
    public static final String RF_TAG_NAME = "rf_tag_name";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final Map<String, RobotTagResult> tagResults;
    private MeasurementRenderer<Run<?,?>> projectNameRenderer;

    public RobotFrameworkPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix, Run<?, ?> build, long timestamp) {
        super(projectNameRenderer, timestamp);
        this.projectNameRenderer = projectNameRenderer;
        this.build = build;
        this.customPrefix = customPrefix;
        tagResults = new Hashtable<String, RobotTagResult>();
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Point[] generate() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Point> pointsList = new ArrayList<Point>();
        
        pointsList.add(generateOverviewPoint(robotBuildAction));
        pointsList.addAll(generateSubPoints(robotBuildAction.getResult()));
        
        return pointsList.toArray(new Point[pointsList.size()]);
    }

    private Point generateOverviewPoint(RobotBuildAction robotBuildAction) {
        Point point = buildPoint(measurementName("rf_results"), customPrefix, build)
            .addField(RF_FAILED, robotBuildAction.getResult().getOverallFailed())
            .addField(RF_PASSED, robotBuildAction.getResult().getOverallPassed())
            .addField(RF_TOTAL, robotBuildAction.getResult().getOverallTotal())
            .addField(RF_CRITICAL_FAILED, robotBuildAction.getResult().getCriticalFailed())
            .addField(RF_CRITICAL_PASSED, robotBuildAction.getResult().getCriticalPassed())
            .addField(RF_CRITICAL_TOTAL, robotBuildAction.getResult().getCriticalTotal())
            .addField(RF_CRITICAL_PASS_PERCENTAGE, robotBuildAction.getCriticalPassPercentage())
            .addField(RF_PASS_PERCENTAGE, robotBuildAction.getOverallPassPercentage())
            .addField(RF_DURATION, robotBuildAction.getResult().getDuration())
            .addField(RF_SUITES, robotBuildAction.getResult().getAllSuites().size())
            .build();

        return point;
    }

    private List<Point> generateSubPoints(RobotResult robotResult) {
        List<Point> subPoints = new ArrayList<Point>();
        TimeGenerator suiteResultTime = new TimeGenerator(timestamp);
        for(RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            long caseTimeStamp = suiteResultTime.next();
            subPoints.add(generateSuitePoint(suiteResult, caseTimeStamp));
            // To preserve the existing functionality of the case being timestamps after the
            // suiteResult, seed the new TimeGenerator with the suiteResult's time
            TimeGenerator caseResultTime = new TimeGenerator(caseTimeStamp);
            for(RobotCaseResult caseResult : suiteResult.getAllCases()) {
                Point casePoint = generateCasePoint(caseResult, caseResultTime.next());
                if (casePointExists(subPoints, casePoint)) {
                    continue;
                }
                subPoints.add(casePoint);
            }

        }

        TimeGenerator tagTime = new TimeGenerator(timestamp);
        for(Map.Entry<String, RobotTagResult> entry : tagResults.entrySet()) {
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
        Point point = buildPoint(measurementName("testcase_point"), customPrefix, build, timestamp)
            .field(RF_NAME, caseResult.getName())
            .field(RF_SUITE_NAME, caseResult.getParent().getName())
            .field(RF_CRITICAL_FAILED, caseResult.getCriticalFailed())
            .field(RF_CRITICAL_PASSED, caseResult.getCriticalPassed())
            .field(RF_FAILED, caseResult.getFailed())
            .field(RF_PASSED, caseResult.getPassed())
            .field(RF_DURATION, caseResult.getDuration())
            .build();

        for(String tag : caseResult.getTags()) {
            markTagResult(tag, caseResult);
        }

        return point;
    }
    private static final class RobotTagResult {
        protected final String name;
        protected RobotTagResult(String name) {
            this.name = name;
        }
        protected final List<String> testCases = new ArrayList<String>();
        protected int failed = 0;
        protected int passed = 0;
        protected int criticalFailed = 0;
        protected int criticalPassed = 0;
        protected long duration = 0;
    }


    private void markTagResult(String tag, RobotCaseResult caseResult) {
        if(tagResults.get(tag) == null)
            tagResults.put(tag, new RobotTagResult(tag));

        RobotTagResult tagResult = tagResults.get(tag);
        if(!tagResult.testCases.contains(caseResult.getDuplicateSafeName())) {
            tagResult.failed += caseResult.getFailed();
            tagResult.passed += caseResult.getPassed();
            tagResult.criticalFailed += caseResult.getCriticalFailed();
            tagResult.criticalPassed += caseResult.getCriticalPassed();
            tagResult.duration += caseResult.getDuration();
            tagResult.testCases.add(caseResult.getDuplicateSafeName());
        }
    }

    private Point generateTagPoint(RobotTagResult tagResult, long timestamp) {
        Point point = buildPoint(measurementName("tag_point"), customPrefix, build, timestamp)
            .field(RF_TAG_NAME, tagResult.name)
            .field(RF_CRITICAL_FAILED, tagResult.criticalFailed)
            .field(RF_CRITICAL_PASSED, tagResult.criticalPassed)
            .field(RF_CRITICAL_TOTAL, tagResult.criticalPassed + tagResult.criticalFailed)
            .field(RF_FAILED, tagResult.failed)
            .field(RF_PASSED, tagResult.passed)
            .field(RF_TOTAL, tagResult.passed + tagResult.failed)
            .field(RF_DURATION, tagResult.duration)
            .build();

        return point;
    }

    private Point generateSuitePoint(RobotSuiteResult suiteResult, long timestamp) {
        Point point = buildPoint(measurementName("suite_result"), customPrefix, build, timestamp)
            .field(RF_SUITE_NAME, suiteResult.getName())
            .field(RF_TESTCASES, suiteResult.getAllCases().size())
            .field(RF_CRITICAL_FAILED, suiteResult.getCriticalFailed())
            .field(RF_CRITICAL_PASSED, suiteResult.getCriticalPassed())
            .field(RF_CRITICAL_TOTAL, suiteResult.getCriticalTotal())
            .field(RF_FAILED, suiteResult.getFailed())
            .field(RF_PASSED, suiteResult.getPassed())
            .field(RF_TOTAL, suiteResult.getTotal())
            .field(RF_DURATION, suiteResult.getDuration())
            .build();

        return point;
    }

}
