package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.*;

public class CustomDataMapPointGenerator extends AbstractPointGenerator {

    private final Run<?, ?> build;
    private final String customPrefix;
    private final Map<String, Map<String, Object>> customDataMap;
    private final Map<String, Map<String, String>> customDataMapTags;

    public CustomDataMapPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix,
                                       Run<?, ?> build, long timestamp, Map<String, Map<String, Object>> customDataMap,
                                       Map<String, Map<String, String>> customDataMapTags, boolean replaceDashWithUnderscore) {
        super(projectNameRenderer, timestamp, replaceDashWithUnderscore);
        this.build = build;
        this.customPrefix = customPrefix;
        this.customDataMap = customDataMap;
        this.customDataMapTags = customDataMapTags;
    }

    public boolean hasReport() {
        return (customDataMap != null && customDataMap.size() > 0);
    }

    public Point[] generate() {
        List<Point> customPoints = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : customDataMap.entrySet()) {
            Point.Builder pointBuilder = buildPoint(measurementName(entry.getKey()), customPrefix, build)
                    .fields(entry.getValue());

            if (customDataMapTags != null) {
                Map<String, String> customTags = customDataMapTags.get(entry.getKey());
                if (customTags != null) {
                    if (customTags.size() > 0){
                        pointBuilder.tag(customTags);
                    }
                }
            }

            Point point = pointBuilder.build();

            customPoints.add(point);
        }
        return customPoints.toArray(new Point[0]);
    }

}
