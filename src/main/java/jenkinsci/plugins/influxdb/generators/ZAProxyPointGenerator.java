package jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;
import org.jdom.JDOMException;
import org.zaproxy.clientapi.core.Alert;
import org.zaproxy.clientapi.core.AlertsFile;
import org.zaproxy.clientapi.core.ClientApi;

import hudson.FilePath;
import hudson.model.Run;


import java.io.File;
import java.io.IOException;
import java.util.List;


public class ZAProxyPointGenerator extends AbstractPointGenerator {

    private static final String ZAP_REPORT_FILE = "/target/ZAP/zap_report.html";
    private static final String HIGH_ALERTS = "high_alerts";
    private static final String MEDIUM_ALERTS = "medium_alerts";
    private static final String LOW_ALERTS = "low_alerts";
    private static final String INFORMATIONAL_ALERTS = "informational_alerts";

    private final Run<?, ?> build;
    private final File zapFile;
    private ClientApi clientapi;

    public ZAProxyPointGenerator(Run<?, ?> build, FilePath workspace) {
        this.build = build;
        zapFile = new File(workspace + ZAP_REPORT_FILE);
    }

    public boolean hasReport() {
        // TODO
        return true;
        //return (zapFile != null && zapFile.exists() && zapFile.canRead()); 
    }

    public Point[] generate() {
        File file = new File("zap_report.html");
        try {
            List<Alert> highAlerts = AlertsFile.getAlertsFromFile(file, "high");
            List<Alert> mediumAlerts = AlertsFile.getAlertsFromFile(file, "medium");
            List<Alert> lowAlerts = AlertsFile.getAlertsFromFile(file, "low");
            List<Alert> informationalAlerts = AlertsFile.getAlertsFromFile(file, "ínformational");
            Point point = Point.measurement("zap_data")
                .field(BUILD_NUMBER, build.getNumber())
                .field(PROJECT_NAME, build.getParent().getName())
                .field(HIGH_ALERTS, highAlerts.size())
                .field(MEDIUM_ALERTS, mediumAlerts.size())
                .field(LOW_ALERTS, lowAlerts.size())
                .field(INFORMATIONAL_ALERTS, informationalAlerts.size())
                .build();
            return new Point[] {point};
        } catch (JDOMException|IOException e) {
            return null;
        }
    }

}
