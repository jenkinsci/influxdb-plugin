package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.Run;

public interface PointGenerator {

    boolean hasReport();

    Point[] generate();

    /**
     * Initializes a basic build point with the basic data already set with a specified timestamp.
     */
    Point buildPoint(String name, String customPrefix, Run<?, ?> build, long timeStamp);

    /**
     * Initializes a basic build point with the basic data already set.
     */
    Point buildPoint(String name, String customPrefix, Run<?, ?> build);
}
