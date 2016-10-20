package jenkinsci.plugins.influxdb.generators;

import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.plugins.performance.PerformanceBuildAction;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.PerformanceReport;

import java.util.*;

public class PerformancePointGenerator extends AbstractPointGenerator {

    public static final String PERFORMANCE_ERROR_PERCENT = "error_percent"; // failed / size * 100
    public static final String PERFORMANCE_ERROR_COUNT = "error_count";     // Amount of failed samples
    public static final String PERFORMANCE_AVERAGE = "average"; // Total duration / size
    public static final String PERFORMANCE_MAX = "max";     // max duration
    public static final String PERFORMANCE_MIN = "min";     // min duration
    public static final String PERFORMANCE_TOTAL_TRAFFIC = "total_traffic";
    public static final String PERFORMANCE_SIZE = "size";   // Size of all samples

    private final Run<?, ?> build;
    private final String customPrefix;
    private final PerformanceBuildAction performanceBuildAction;

    public PerformancePointGenerator(MeasurementRenderer<Run<?,?>> measurementRenderer, String customPrefix, Run<?, ?> build) {
        super(measurementRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
        performanceBuildAction = build.getAction(PerformanceBuildAction.class);
    }

    public boolean hasReport() {
        return performanceBuildAction != null && performanceBuildAction.getPerformanceReportMap() != null;
    }

    public Point[] generate() {
        Map<String, PerformanceReport> reportMap = performanceBuildAction.getPerformanceReportMap().getPerformanceReportMap();
        
        List<Point> pointsList = new ArrayList<Point>();

        for (String key : reportMap.keySet()) {
            pointsList.add(generateReportPoint(reportMap.get(key)));
        }

        return pointsList.toArray(new Point[pointsList.size()]);
    }

    private Point generateReportPoint(PerformanceReport performanceReport) {
        Point point = buildPoint(measurementName(performanceReport.getReportFileName() + "_data"), customPrefix, build)
            .field(PERFORMANCE_ERROR_PERCENT, performanceReport.errorPercent())
            .field(PERFORMANCE_ERROR_COUNT, performanceReport.countErrors())
            .field(PERFORMANCE_AVERAGE, performanceReport.getAverage())
            .field(PERFORMANCE_MAX, performanceReport.getMax())
            .field(PERFORMANCE_MIN, performanceReport.getMin())
            .field(PERFORMANCE_TOTAL_TRAFFIC, performanceReport.getTotalTrafficInKb())
            .field(PERFORMANCE_SIZE, performanceReport.size())
            .build();

        return point;
    }

}
