package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.*;

public class CustomDataMapPointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";

    private final Run<?, ?> build;
    private final String customPrefix;
    Map<String, Map<String, Object>> customDataMap;
    private final Map<String, String> customDataMapTags;

    public CustomDataMapPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix,
                                       Run<?, ?> build, Map<String, Map<String, Object>> customDataMap,
                                       Map<String, String> customDataMapTags) {
        super(projectNameRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
        this.customDataMap = customDataMap;
        this.customDataMapTags = customDataMapTags;
    }

    public boolean hasReport() {
        return (customDataMap != null && customDataMap.size() > 0);
    }

    public Point[] generate() {
        List<Point> customPoints = new ArrayList<Point>();
        Set<String> customKeys = customDataMap.keySet();
        for (String key : customKeys) {
            Point point = buildPoint(measurementName(key), customPrefix, build)
                    .fields(customDataMap.get(key))
                    .tag(customDataMapTags)
                    .build();
            customPoints.add(point);
        }
        return customPoints.toArray(new Point[customPoints.size()]);
    }

}
