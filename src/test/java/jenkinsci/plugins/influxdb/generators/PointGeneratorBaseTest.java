package jenkinsci.plugins.influxdb.generators;

import jenkinsci.plugins.influxdb.models.AbstractPoint;

public class PointGeneratorBaseTest {
    /**
     * Test helper function to check if the line protocol of both, v1v2 and v3 points, contains a given string.
     *
     * @param match The string to check for.
     * @return True if the line protocol of both, v1v2 and v3 points contains the given string, false otherwise.
     */
    public static boolean allLineProtocolsContain(AbstractPoint point, String match) {
        return point.getV1v2Point().toLineProtocol().contains(match);
    }

    /**
     * Test helper function to check if the line protocol of both, v1v2 and v3 points, starts with a given string.
     *
     * @param match The string to check for.
     * @return True if the line protocol of both, v1v2 and v3 points starts with the given string, false otherwise.
     */
    public static boolean allLineProtocolsStartWith(AbstractPoint point, String match) {
        return point.getV1v2Point().toLineProtocol().startsWith(match);
    }
}
