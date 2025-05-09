package jenkinsci.plugins.influxdb.models;

import javax.annotation.Nonnull;
import java.util.Map;

public class AbstractPoint {
    private final com.influxdb.client.write.Point v1v2Point;

    public AbstractPoint(@Nonnull String measurement) {
        this.v1v2Point = new com.influxdb.client.write.Point(measurement);
    }

    public com.influxdb.client.write.Point getV1v2Point() {
        return v1v2Point;
    }


    public AbstractPoint addField(String field, boolean value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, int value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, long value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, double value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, Number value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, String value) {
        this.v1v2Point.addField(field, value);
        return this;
    }

    public AbstractPoint addFields(Map<String, Object> fields) {
        this.v1v2Point.addFields(fields);
        return this;
    }

    public AbstractPoint addTag(String name, String value) {
        this.v1v2Point.addTag(name, value);
        return this;
    }

    public AbstractPoint addTags(Map<String, String> tags) {
        this.v1v2Point.addTags(tags);
        return this;
    }

    public AbstractPoint time(long time, com.influxdb.client.domain.WritePrecision precision) {
        this.v1v2Point.time(time, precision);
        return this;
    }
}
