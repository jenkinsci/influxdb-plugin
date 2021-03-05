package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.influxdb.dto.Point;

import java.util.*;

public class CustomDataMapPointGenerator extends AbstractPointGenerator {

    private final String customPrefix;
    private final Map<String, Map<String, Object>> customDataMap;
    private final Map<String, Map<String, String>> customDataMapTags;

    public CustomDataMapPointGenerator(Run<?, ?> build, TaskListener listener,
                                       ProjectNameRenderer projectNameRenderer,
                                       long timestamp, String jenkinsEnvParameterTag,
                                       String customPrefix, Map<String, Map<String, Object>> customDataMap,
                                       Map<String, Map<String, String>> customDataMapTags) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.customDataMap = customDataMap;
        this.customDataMapTags = customDataMapTags;
    }

    public boolean hasReport() {
        return (customDataMap != null && customDataMap.size() > 0);
    }

    public Point[] generate() {
        List<Point> points = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : customDataMap.entrySet()) {
            Point.Builder pointBuilder = buildPoint(entry.getKey(), customPrefix, build).fields(entry.getValue());

            if (customDataMapTags != null) {
                Map<String, String> customTags = customDataMapTags.get(entry.getKey());
                if (customTags != null) {
                    if (customTags.size() > 0) {
                        pointBuilder.tag(customTags);
                    }
                }
            }

            Point point = pointBuilder.build();

            points.add(point);
        }

        return points.toArray(new Point[0]);
    }
}
