package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.Map;

import static jenkinsci.plugins.influxdb.InfluxDbPublisher.DEFAULT_MEASUREMENT_NAME;

public class CustomDataPointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final String measurementName;
    Map<String, Object> customData;
    Map<String, String> customDataTags;

    public CustomDataPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix,
                                    Run<?, ?> build, long timestamp, Map customData, Map<String, String> customDataTags, String measurementName ) {
        super(projectNameRenderer, timestamp);
        this.build = build;
        this.customPrefix = customPrefix;
        this.customData = customData;
        this.customDataTags = customDataTags;
        // Extra logic to retain compatibility with existing "jenkins_custom_data" tables
        this.measurementName = DEFAULT_MEASUREMENT_NAME.equals(measurementName) ? "jenkins_custom_data" : "custom_" + measurementName;
    }

    public boolean hasReport() {
        return (customData != null && customData.size() > 0);
    }

    public Point[] generate() {
        long startTime = build.getTimeInMillis();
        long dt = timestamp - startTime;

        Point.Builder pointBuilder = buildPoint(measurementName(measurementName), customPrefix, build)
                .addField(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
                .fields(customData);

        if (customDataTags != null) {
            if (customDataTags.size() > 0) {
                pointBuilder = pointBuilder.tag(customDataTags);
            }
        }

        Point point = pointBuilder.build();

        return new Point[] {point};
    }

}
