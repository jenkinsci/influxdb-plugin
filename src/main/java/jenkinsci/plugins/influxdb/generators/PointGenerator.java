package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import org.influxdb.dto.Point;

public interface PointGenerator {

    public boolean hasReport();

    public Point[] generate();

    /**
     * Initializes a basic build point with the basic data already set with a specified timestamp.
     */
    Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build, long timeStamp);

    /**
     * Initializes a basic build point with the basic data already set.
     */
    Point.Builder buildPoint(String name, String customPrefix, Run<?, ?> build);
}
