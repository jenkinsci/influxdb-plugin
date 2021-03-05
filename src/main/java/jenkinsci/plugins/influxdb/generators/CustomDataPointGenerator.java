package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

import java.util.Map;

import static jenkinsci.plugins.influxdb.InfluxDbPublisher.DEFAULT_MEASUREMENT_NAME;

public class CustomDataPointGenerator extends AbstractPointGenerator {

    private static final String BUILD_TIME = "build_time";

    private final String customPrefix;
    private final String measurementName;
    private final Map<String, Object> customData;
    private final Map<String, String> customDataTags;

    public CustomDataPointGenerator(Run<?, ?> build, TaskListener listener,
                                    ProjectNameRenderer projectNameRenderer,
                                    long timestamp, String jenkinsEnvParameterTag,
                                    String customPrefix, Map<String, Object> customData,
                                    Map<String, String> customDataTags, String measurementName) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
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
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;

        Point point = buildPoint(measurementName, customPrefix, build)
                .addField(BUILD_TIME, build.getDuration() == 0 ? dt : build.getDuration())
                .addFields(customData);

        if (customDataTags != null) {
            if (customDataTags.size() > 0) {
                point.addTags(customDataTags);
            }
        }


        return new Point[] {point};
    }
}
