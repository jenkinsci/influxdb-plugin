package jenkinsci.plugins.influxdb.renderer;

public interface MeasurementRenderer<T> {

    String render(T input);
}
