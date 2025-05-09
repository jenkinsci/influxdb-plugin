package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private final String customPrefix;
    private final PerformanceBuildAction performanceBuildAction;

    public PerformancePointGenerator(Run<?, ?> build, TaskListener listener,
                                     ProjectNameRenderer projectNameRenderer,
                                     long timestamp, String jenkinsEnvParameterTag,
                                     String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        performanceBuildAction = build.getAction(PerformanceBuildAction.class);
    }

    public boolean hasReport() {
        return performanceBuildAction != null && performanceBuildAction.getPerformanceReportMap() != null;
    }

    public AbstractPoint[] generate() {
        Map<String, PerformanceReport> reportMap = performanceBuildAction.getPerformanceReportMap().getPerformanceReportMap();

        List<AbstractPoint> points = new ArrayList<>();

        for (PerformanceReport report : reportMap.values()) {
            points.add(generateReportPoint(report));
        }

        return points.toArray(new AbstractPoint[0]);
    }

    private AbstractPoint generateReportPoint(PerformanceReport performanceReport) {
        return buildPoint("performance_data", customPrefix, build)
                .addField(PERFORMANCE_ERROR_PERCENT, performanceReport.errorPercent())
                .addField(PERFORMANCE_ERROR_COUNT, performanceReport.countErrors())
                .addField(PERFORMANCE_AVERAGE, performanceReport.getAverage())
                .addField(PERFORMANCE_MAX, performanceReport.getMax())
                .addField(PERFORMANCE_MIN, performanceReport.getMin())
                .addField(PERFORMANCE_TOTAL_TRAFFIC, performanceReport.getTotalTrafficInKb())
                .addField(PERFORMANCE_SIZE, performanceReport.samplesCount())
                .addField(PERFORMANCE_90PERCENTILE, performanceReport.get90Line())
                .addField(PERFORMANCE_MEDIAN, performanceReport.getMedian());
    }
}
