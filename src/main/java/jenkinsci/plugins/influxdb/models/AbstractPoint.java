package jenkinsci.plugins.influxdb.models;

import javax.annotation.Nonnull;
import java.util.Map;

public class AbstractPoint {
    private final com.influxdb.client.write.Point v1v2Point;
    private final com.influxdb.v3.client.Point v3Point;

    public AbstractPoint(@Nonnull String measurement) {
        this.v1v2Point = new com.influxdb.client.write.Point(measurement);
        this.v3Point = new com.influxdb.v3.client.Point(measurement);
    }

    public String getName() {
        String v1v2Name = this.v1v2Point.toLineProtocol().split(",")[0];
        String v3Name = this.v3Point.toLineProtocol().split(",")[0];
        if (!v1v2Name.equals(v3Name)) {
            throw new RuntimeException("V1V2 point name '%s' differs from V3 point name '%s'".formatted(v1v2Name, v3Name));
        }
        return v1v2Name;
    }

    public com.influxdb.client.write.Point getV1v2Point() {
        return v1v2Point;
    }

    public com.influxdb.v3.client.Point getV3Point() {
        return v3Point;
    }

    public AbstractPoint addField(String field, boolean value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, int value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, long value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, double value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, Number value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addField(String field, String value) {
        this.v1v2Point.addField(field, value);
        this.v3Point.setField(field, value);
        return this;
    }

    public AbstractPoint addFields(Map<String, Object> fields) {
        this.v1v2Point.addFields(fields);
        this.v3Point.setFields(fields);
        return this;
    }

    public AbstractPoint addTag(String name, String value) {
        this.v1v2Point.addTag(name, value);
        this.v3Point.setTag(name, value);
        return this;
    }

    public AbstractPoint addTags(Map<String, String> tags) {
        this.v1v2Point.addTags(tags);
        this.v3Point.setTags(tags);
        return this;
    }

    public AbstractPoint time(long time, com.influxdb.v3.client.write.WritePrecision precision) {
        com.influxdb.client.domain.WritePrecision v1v2WritePrecision =
                com.influxdb.client.domain.WritePrecision.valueOf(precision.name());
        this.v1v2Point.time(time, v1v2WritePrecision);
        this.v3Point.setTimestamp(time, precision);
        return this;
    }

    public AbstractPoint time(long time, com.influxdb.client.domain.WritePrecision precision) {
        com.influxdb.v3.client.write.WritePrecision v3WritePrecision =
                com.influxdb.v3.client.write.WritePrecision.valueOf(precision.name());
        this.v1v2Point.time(time, precision);
        this.v3Point.setTimestamp(time, v3WritePrecision);
        return this;
    }
}
