package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import org.influxdb.dto.Point;

import java.util.HashMap;
import java.util.Map;

public class CustomDataMapPointGenerator extends AbstractPointGenerator {

    public static final String BUILD_TIME = "build_time";

    private final Run<?, ?> build;
    private final String customPrefix;
    Map<String, Object> customDataMap;

    public CustomDataMapPointGenerator(MeasurementRenderer<Run<?,?>> projectNameRenderer, String customPrefix, Run<?, ?> build, Map CustomDataMap) {
        super(projectNameRenderer);
        this.build = build;
        this.customPrefix = customPrefix;
        this.customDataMap = CustomDataMap;
    }

    public boolean hasReport() {
        return (customDataMap != null && customDataMap.size() > 0);
    }

    public Map<String, Point[]> generate() {
        long startTime = build.getTimeInMillis();
        long currTime = System.currentTimeMillis();
        long dt = currTime - startTime;
        Map<String, Point[]> customPoints = new HashMap<String, Point[]>;
        while (customDataMap.keySet().iterator().hasNext() {
            String key = customDataMap.keySet().iterator().next();
            Point point = buildPoint(measurementName(key, customPrefix, build)
                    .fields(customDataMap.get(key))
                    .build();
            customPoints.put(customDataMap.keySet().iterator().next(), customDataMap.get())
        }
        return new Point[] {point};
    }

}
