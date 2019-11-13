package jenkinsci.plugins.influxdb.generators;

import com.checkmarx.jenkins.CxScanResult;

import org.influxdb.dto.Point;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;

public class CheckmarxPointGenerator extends AbstractPointGenerator {
    private static final String MEASUREMENT_NAME = "checkmarx_data";
    private static final String CHECKMARX_HIGH_ISSUES = "high_issues";
    private static final String CHECKMARX_MEDIUM_ISSUES = "medium_issues";
    private static final String CHECKMARX_LOW_ISSUES = "low_issues";
    private static final String CHECKMARX_INFO_ISSUES = "info_issues";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final CxScanResult result;

    public CheckmarxPointGenerator(MeasurementRenderer<Run<?,?>> measurementRenderer, String customPrefix, Run<?, ?> build, long timestamp, boolean replaceDashWithUnderscore) {
        super(measurementRenderer, timestamp, replaceDashWithUnderscore);
        this.build = build;
        this.customPrefix = customPrefix;
        this.result = build.getAction(CxScanResult.class);
    }

    public boolean hasReport() {
        return this.result != null;
    }

    public Point[] generate() {
        Point point = buildPoint(measurementName(MEASUREMENT_NAME), customPrefix, build)
            .addField(CHECKMARX_HIGH_ISSUES, this.result.getHighCount())
            .addField(CHECKMARX_MEDIUM_ISSUES, this.result.getMediumCount())
            .addField(CHECKMARX_LOW_ISSUES, this.result.getLowCount())
            .addField(CHECKMARX_INFO_ISSUES, this.result.getInfoCount())
            .build();
        return new Point[] {point};
    }
}
