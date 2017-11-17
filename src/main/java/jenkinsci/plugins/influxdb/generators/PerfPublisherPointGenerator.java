package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.plugins.PerfPublisher.PerfPublisherBuildAction;
import hudson.plugins.PerfPublisher.Report.Metric;
import hudson.plugins.PerfPublisher.Report.ReportContainer;
import hudson.plugins.PerfPublisher.Report.Test;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerfPublisherPointGenerator extends AbstractPointGenerator {
    private final Run<?, ?> build;
    private final String customPrefix;
    private final PerfPublisherBuildAction performanceBuildAction;

    public PerfPublisherPointGenerator(MeasurementRenderer<Run<?,?>> measurementRenderer, String customPrefix, Run<?, ?> build) {
        super(measurementRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
        performanceBuildAction = build.getAction(PerfPublisherBuildAction.class);
    }

    public boolean hasReport() {
        return performanceBuildAction != null && performanceBuildAction.getReport() != null;
    }

    public Point[] generate() {
        ReportContainer reports = performanceBuildAction.getReports();

        List<Point> pointsList = new ArrayList<>();

        pointsList.add(generateSummaryPoint(reports));
        pointsList.addAll(generateMetricsPoints(reports));

        for (Test test : reports.getTests()) {
            pointsList.add(generateTestPoint(test));
            pointsList.addAll(generateTestMetricsPoints(test));
        }

        return pointsList.toArray(new Point[pointsList.size()]);
    }

    private Point generateSummaryPoint(ReportContainer reports) {
        Point.Builder builder = buildPoint(measurementName("perfpublisher_summary"), customPrefix, build)
                .addField("number_of_tests", reports.getNumberOfTest())
                .addField("number_of_executed_tests", reports.getNumberOfExecutedTest())
                .addField("number_of_not_executed_tests", reports.getNumberOfNotExecutedTest())
                .addField("number_of_passed_tests", reports.getNumberOfPassedTest())
                .addField("number_of_failed_tests", reports.getNumberOfFailedTest())
                .addField("number_of_success_tests", reports.getNumberOfSuccessTests())
                .addField("number_of_true_false_tests", reports.getNumberOfTrueFalseTest());

        // compile time
        if (reports.getBestCompileTimeTest().isCompileTime()) {
            builder.addField("best_compile_time_test_value", reports.getBestCompileTimeTestValue())
                    .addField("best_compile_time_test_name", reports.getBestCompileTimeTestName())
                    .addField("worst_compile_time_test_value", reports.getWorstCompileTimeTestValue())
                    .addField("worst_compile_time_test_name", reports.getWorstCompileTimeTestName())
                    .addField("avg_compile_time", reports.getAverageOfCompileTime());
        }

        // performance
        if (reports.getBestPerformanceTest().isPerformance()) {
                builder.addField("best_performance_test_value", reports.getBestPerformanceTestValue())
                    .addField("best_performance_test_name", reports.getBestPerformanceTestName())
                    .addField("worst_performance_test_value", reports.getWorstPerformanceTestValue())
                    .addField("worst_performance_test_name", reports.getWorstPerformanceTestName())
                    .addField("average_performance", reports.getAverageOfPerformance());
        }

        // execution time
        if (reports.getBestExecutionTimeTest().isExecutionTime()) {
            builder.addField("best_execution_time_test_value", reports.getBestExecutionTimeTestValue())
                    .addField("best_execution_time_test_name", reports.getBestExecutionTimeTestName())
                    .addField("worst_execution_time_test_value", reports.getWorstExecutionTimeTestValue())
                    .addField("worst_execution_time_test_name", reports.getWorstExecutionTimeTestName())
                    .addField("avg_execution_time", reports.getAverageOfExecutionTime());
        }

        return builder.build();
    }

    private List<Point> generateMetricsPoints(ReportContainer reports) {

        List<Point> pointsList = new ArrayList<>();

        for (Map.Entry<String, Double> entry : reports.getAverageValuePerMetrics().entrySet())
        {
            String metricName = entry.getKey();
            Point point = buildPoint(measurementName("perfpublisher_metric"), customPrefix, build)
                    .addField("metric_name", metricName)
                    .addField("average", entry.getValue())
                    .addField("worst", reports.getWorstValuePerMetrics().get(metricName))
                    .addField("best", reports.getBestValuePerMetrics().get(metricName))
                    .build();
            pointsList.add(point);
        }
        return pointsList;
    }

    private Point generateTestPoint(Test test) {
        Point.Builder builder = buildPoint(measurementName("perfpublisher_test"), customPrefix, build)
                .addField("test_name", test.getName())
                .addField("successful", test.isSuccessfull())
                .addField("executed", test.isExecuted());

        if (test.getMessage() != null)
            builder.addField("message", test.getMessage());

        if (test.isCompileTime())
            builder.addField("compile_time", test.getCompileTime().getMeasure());

        if (test.isExecutionTime())
            builder.addField("execution_time", test.getExecutionTime().getMeasure());

        if (test.isPerformance())
            builder.addField("performance", test.getPerformance().getMeasure());

        return builder.build();
    }

    private List<Point> generateTestMetricsPoints(Test test) {
        List<Point> pointsList = new ArrayList<>();

        for (Map.Entry<String, Metric> entry : test.getMetrics().entrySet()) {
            String metricName = entry.getKey();
            Metric metric = entry.getValue();

            Point point = buildPoint(measurementName("perfpublisher_test_metric"), customPrefix, build)
                    .addField("test_name", test.getName())
                    .addField("metric_name", metricName)
                    .addField("value", metric.getMeasure())
                    .addField("unit", metric.getUnit())
                    .addField("relevant", metric.isRelevant())
                    .build();

            pointsList.add(point);
        }
        return pointsList;
    }
}
