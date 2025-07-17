package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import jenkinsci.plugins.influxdb.models.AbstractPoint;

public interface PointGenerator {

    boolean hasReport();

    AbstractPoint[] generate();

    /**
     * Initializes a basic build point with the basic data already set with a specified timestamp.
     */
    AbstractPoint buildPoint(String name, String customPrefix, Run<?, ?> build, long timeStamp);

    /**
     * Initializes a basic build point with the basic data already set.
     */
    AbstractPoint buildPoint(String name, String customPrefix, Run<?, ?> build);
}
