package jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import org.influxdb.dto.Point;

import java.awt.*;
import java.util.*;
import java.util.List;

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

    private final AbstractBuild<?, ?> build;
    private final Map<String, RobotTagResult> tagResults;

    public RobotFrameworkPointGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
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
        Point point = Point.measurement("rf_results")
            .field(BUILD_NUMBER, build.getNumber())
            .field(PROJECT_NAME, build.getProject().getName())
            .field(RF_FAILED, robotBuildAction.getResult().getOverallFailed())
            .field(RF_PASSED, robotBuildAction.getResult().getOverallPassed())
            .field(RF_TOTAL, robotBuildAction.getResult().getOverallTotal())
            .field(RF_CRITICAL_FAILED, robotBuildAction.getResult().getCriticalFailed())
            .field(RF_CRITICAL_PASSED, robotBuildAction.getResult().getCriticalPassed())
            .field(RF_CRITICAL_TOTAL, robotBuildAction.getResult().getCriticalTotal())
            .field(RF_CRITICAL_PASS_PERCENTAGE, robotBuildAction.getCriticalPassPercentage())
            .field(RF_PASS_PERCENTAGE, robotBuildAction.getOverallPassPercentage())
            .field(RF_DURATION, robotBuildAction.getResult().getDuration())
            .field(RF_SUITES, robotBuildAction.getResult().getAllSuites().size())
            .build();

        return point;
    }

    private List<Point> generateSubPoints(RobotResult robotResult) {
        List<Point> subPoints = new ArrayList<Point>();
        for(RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            subPoints.add(generateSuitePoint(suiteResult));

            for(RobotCaseResult caseResult : suiteResult.getAllCases()) {
                Point casePoint = generateCasePoint(caseResult);
                if (casePointExists(subPoints, casePoint)) {
                    continue;
                }
                subPoints.add(generateCasePoint(caseResult));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // handle
                }
            }

        }

        for(Map.Entry<String, RobotTagResult> entry : tagResults.entrySet()) {
            subPoints.add(generateTagPoint(entry.getValue()));
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                // handle
            }
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

    private Point generateCasePoint(RobotCaseResult caseResult) {
        Point point = Point.measurement("testcase_point")
            .field(BUILD_NUMBER, build.getNumber())
            .field(PROJECT_NAME, build.getProject().getName())
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

    private Point generateTagPoint(RobotTagResult tagResult) {
        Point point = Point.measurement("tag_point")
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

    private Point generateSuitePoint(RobotSuiteResult suiteResult) {
        Point point = Point.measurement("suite_result")
            .field(BUILD_NUMBER, build.getNumber())
            .field(PROJECT_NAME, build.getProject().getName())
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
