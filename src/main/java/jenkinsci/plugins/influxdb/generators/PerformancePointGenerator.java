package jenkinsci.plugins.influxdb.generators;

import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;

import java.util.*;

public class PerformancePointGenerator extends AbstractPointGenerator {

    private static final String PERFORMANCE_ERROR_PERCENT = "error_percent"; // failed / size * 100
    private static final String PERFORMANCE_ERROR_COUNT = "error_count";     // Amount of failed samples
    private static final String PERFORMANCE_AVERAGE = "average"; // Total duration / size
    private static final String PERFORMANCE_90PERCENTILE = "90Percentile";   // 90 Percentile duration
    private static final String PERFORMANCE_MEDIAN = "median";   //median duration
    private static final String PERFORMANCE_MAX = "max";     // max duration
    private static final String PERFORMANCE_MIN = "min";     // min duration
    private static final String PERFORMANCE_TOTAL_TRAFFIC = "total_traffic";
    private static final String PERFORMANCE_SIZE = "size";   // Size of all samples

    private final Run<?, ?> build;
    private final String customPrefix;
    private final PerformanceBuildAction performanceBuildAction;

    public PerformancePointGenerator(MeasurementRenderer<Run<?,?>> measurementRenderer, String customPrefix, Run<?, ?> build,
                                     long timestamp, boolean replaceDashWithUnderscore) {
        super(measurementRenderer, timestamp, replaceDashWithUnderscore);
        this.build = build;
        this.customPrefix = customPrefix;
        performanceBuildAction = build.getAction(PerformanceBuildAction.class);
    }

    public boolean hasReport() {
        return performanceBuildAction != null && performanceBuildAction.getPerformanceReportMap() != null;
    }

    public Point[] generate() {
        Map<String, PerformanceReport> reportMap = performanceBuildAction.getPerformanceReportMap().getPerformanceReportMap();

        List<Point> pointsList = new ArrayList<>();

        for (PerformanceReport report : reportMap.values()) {
            pointsList.add(generateReportPoint(report));
        }

        return pointsList.toArray(new Point[0]);
    }

    private Point generateReportPoint(PerformanceReport performanceReport) {
        Point point = buildPoint(measurementName("performance_data"), customPrefix, build)
            .addField(PERFORMANCE_ERROR_PERCENT, performanceReport.errorPercent())
            .addField(PERFORMANCE_ERROR_COUNT, performanceReport.countErrors())
            .addField(PERFORMANCE_AVERAGE, performanceReport.getAverage())
            .addField(PERFORMANCE_MAX, performanceReport.getMax())
            .addField(PERFORMANCE_MIN, performanceReport.getMin())
            .addField(PERFORMANCE_TOTAL_TRAFFIC, performanceReport.getTotalTrafficInKb())
            .addField(PERFORMANCE_SIZE, performanceReport.samplesCount())
            .addField(PERFORMANCE_90PERCENTILE, performanceReport.get90Line())
            .addField(PERFORMANCE_MEDIAN, performanceReport.getMedian())
            .build();

        return point;
    }

}
