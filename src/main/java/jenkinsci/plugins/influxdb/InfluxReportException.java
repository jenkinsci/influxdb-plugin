package jenkinsci.plugins.influxdb;

/**
 * Generic Exception thrown whenever an Exception occurs writing to InfluxDB.
 */
public class InfluxReportException extends RuntimeException {

    public InfluxReportException(String message) {
        super(message);
    }

    public InfluxReportException(String message, Throwable cause) {
        super(message, cause);
    }

    public InfluxReportException(Throwable cause) {
        super(cause);
    }

    public InfluxReportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
