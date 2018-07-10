package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.Map;

public class CustomDataPointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";

    private final Run<?, ?> build;
    private final String customPrefix;
    private final String measurementName;
    Map<String, Object> customData;
    Map<String, String> customDataTags;

    public CustomDataPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix,
                                    Run<?, ?> build, Map customData, Map<String, String> customDataTags, String measurementName) {
        super(projectNameRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
        this.customData = customData;
        this.customDataTags = customDataTags;
        // Extra logic to retain compatibility with existing "jenkins_custom_data" tables
        this.measurementName = measurementName.equals("jenkins_data") ? "jenkins_custom_data" : "custom_" + measurementName;
    }

    public boolean hasReport() {
        return (customData != null && customData.size() > 0);
    }

    public Point[] generate() {
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;

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
