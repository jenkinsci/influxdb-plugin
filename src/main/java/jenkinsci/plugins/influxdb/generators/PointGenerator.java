package jenkinsci.plugins.influxdb.generators;

import hudson.model.Run;
import org.influxdb.dto.Point;

public interface PointGenerator {

    public boolean hasReport();

    public Point[] generate();

}
