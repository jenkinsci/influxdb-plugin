package jenkinsci.plugins.influxdb.generators;

import jenkinsci.plugins.influxdb.models.AbstractPoint;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointGeneratorBaseTest {
    /**
     * Asserts that the given AbstractPoint has the requested fields and tags.
     *
     * @param point  The abstract point to check the fields and tags of.
     * @param fields The expected fields.
     * @param tags   The expected tags.
     */
    public static void checkPointTagsAndFields(AbstractPoint point, TreeMap<String, Object> fields, TreeMap<String, Object> tags) throws NoSuchFieldException, IllegalAccessException {
        TreeMap<String, Object> pointFields = getPointFields(point);
        TreeMap<String, Object> pointTags = getPointTags(point);

        assertMapsAreEqualIgnoreNumberTypes(fields, pointFields);
        assertMapsAreEqualIgnoreNumberTypes(tags, pointTags);
    }

    /**
     * Returns the fields of an AbstractPoint.
     *
     * @param point The point to get the fields from.
     * @return The fields of the point.
     */
    public static TreeMap<String, Object> getPointFields(AbstractPoint point) throws NoSuchFieldException, IllegalAccessException {
        TreeMap<String, Object> v1v2Fields = getPointMap(point.getV1v2Point(), "fields");
        TreeMap<String, Object> v3Fields = getPointMap(point.getV3Point(), "fields");

        assertMapsAreEqualIgnoreNumberTypes(v1v2Fields, v3Fields);
        return v1v2Fields;
    }

    /**
     * Returns the requested point property as map. The property can be either "fields" or "tags".
     *
     * @param point        The V1V2 point to get the property of.
     * @param propertyName The property can be either "fields" or "tags".
     * @return The point's fields or tags.
     */
    public static TreeMap<String, Object> getPointMap(com.influxdb.client.write.Point point, String propertyName) throws NoSuchFieldException, IllegalAccessException {
        Field propertyField = point.getClass().getDeclaredField(propertyName);
        propertyField.setAccessible(true);

        @SuppressWarnings("unchecked")
        TreeMap<String, Object> data = (TreeMap<String, Object>) propertyField.get(point);
        return data;
    }

    /**
     * Returns the requested point property as map. The property can be either "fields" or "tags".
     *
     * @param point        The V3 point to get the property of.
     * @param propertyName The property can be either "fields" or "tags".
     * @return The point's fields or tags.
     */
    public static TreeMap<String, Object> getPointMap(com.influxdb.v3.client.Point point, String propertyName) throws NoSuchFieldException, IllegalAccessException {
        Field privateValues = point.getClass().getDeclaredField("values");
        privateValues.setAccessible(true);

        Object pointValues = privateValues.get(point);

        Field propertyField = pointValues.getClass().getDeclaredField(propertyName);

        propertyField.setAccessible(true);

        @SuppressWarnings("unchecked")
        TreeMap<String, Object> data = (TreeMap<String, Object>) propertyField.get(pointValues);
        return data;
    }

    /**
     * Asserts that two maps are equal, ignoring number types, i.e. 11L == 11i.
     *
     * @param expectedMap The expected map to compare to.
     * @param actualMap   The actual map.
     */
    public static void assertMapsAreEqualIgnoreNumberTypes(TreeMap<String, Object> expectedMap, TreeMap<String, Object> actualMap) {
        for (Map.Entry<String, Object> entry : expectedMap.entrySet()) {
            String expectedKey = entry.getKey();
            assertValuesEqualIgnoreNumberTypes(entry.getValue(), actualMap.get(expectedKey));
        }
        assertEquals(expectedMap.size(), actualMap.size());
    }

    /**
     * Asserts that two values are equal, ignoring number types, i.e. 11L == 11i.
     *
     * @param expected The expected value to compare to.
     * @param actual   The actual value.
     */
    private static void assertValuesEqualIgnoreNumberTypes(Object expected, Object actual) {
        if (expected instanceof Number && actual instanceof Number) {
            assertEquals(((Number) expected).longValue(), ((Number) actual).longValue());
        } else {
            assertEquals(expected, actual);
        }
    }

    /**
     * Returns the tags of an AbstractPoint.
     *
     * @param point The point to get the tags from.
     * @return The tags of the point.
     */
    public static TreeMap<String, Object> getPointTags(AbstractPoint point) throws NoSuchFieldException, IllegalAccessException {
        TreeMap<String, Object> v1v2Tags = getPointMap(point.getV1v2Point(), "tags");
        TreeMap<String, Object> v3Tags = getPointMap(point.getV3Point(), "tags");

        assertMapsAreEqualIgnoreNumberTypes(v1v2Tags, v3Tags);
        return v1v2Tags;
    }

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
