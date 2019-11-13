package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.InterruptedException;

import hudson.EnvVars;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SonarQubePointGenerator extends AbstractPointGenerator {

    private static final String BUILD_DISPLAY_NAME = "display_name";
    private static final String SONARQUBE_LINES_OF_CODE = "lines_of_code";
    private static final String SONARQUBE_COMPLEXITY = "complexity";
    private static final String SONARQUBE_CRITICAL_ISSUES = "critical_issues";
    private static final String SONARQUBE_MAJOR_ISSUES = "major_issues";
    private static final String SONARQUBE_MINOR_ISSUES = "minor_issues";
    private static final String SONARQUBE_INFO_ISSUES = "info_issues";
    private static final String SONARQUBE_BLOCKER_ISSUES = "blocker_issues";

    private static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ")
            + "(.*)";

    private static final String SONAR_ISSUES_BASE_URL = "/api/issues/search?ps=500&projectKeys=";

    private static final String SONAR_METRICS_BASE_URL = "/api/measures/component?metricKeys=ncloc,complexity,violations&componentKey=";

    private static final OkHttpClient httpClient = new OkHttpClient();

    private String sonarIssuesUrl;
    private String sonarMetricsUrl;

    private final Run<?, ?> build;
    private final String customPrefix;
    private final TaskListener listener;

    private String sonarBuildLink = null;
    private String token = null;

    private EnvVars env = null;

    public SonarQubePointGenerator(MeasurementRenderer<Run<?, ?>> projectNameRenderer, String customPrefix,
                                   Run<?, ?> build, long timestamp, TaskListener listener) {
        super(projectNameRenderer, timestamp);
        this.build = build;
        this.customPrefix = customPrefix;
        this.listener = listener;
    }

    public boolean hasReport() {
        try {
            sonarBuildLink = getSonarProjectURLFromBuildLogs(build);
            return !StringUtils.isEmpty(sonarBuildLink);
        } catch (IOException e) {
            //
        }

        return false;
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }

    private void setSonarDetails(String sonarBuildLink) {
        try {
            String sonarProjectName = getSonarProjectName(sonarBuildLink);
            // Use SONAR_HOST_URL environment variable if possible
            String url = env.get("SONAR_HOST_URL");

            String sonarServer;
            if (url != null && !url.isEmpty()) {
                sonarServer = url;
                String logMessage = "[InfluxDB Plugin] INFO: Using SonarQube host URL found in environment variable SONAR_HOST_URL.";
                listener.getLogger().println(logMessage);
            } else {
                String logMessage = "[InfluxDB Plugin] INFO: No SonarQube host URL found in environment variable SONAR_HOST_URL. Using build log instead.";
                listener.getLogger().println(logMessage);
                if (sonarBuildLink.indexOf("/dashboard?id=" + sonarProjectName) > 0) {
                    sonarServer = sonarBuildLink.substring(0,
                            sonarBuildLink.indexOf("/dashboard?id=" + sonarProjectName));
                } else {
                    sonarServer = sonarBuildLink.substring(0,
                            sonarBuildLink.indexOf("/dashboard/index/" + sonarProjectName));
                }
            }
            sonarIssuesUrl = sonarServer + SONAR_ISSUES_BASE_URL + sonarProjectName + "&resolved=false&severities=";
            sonarMetricsUrl = sonarServer + SONAR_METRICS_BASE_URL + sonarProjectName;
        } catch (URISyntaxException e) {
            //
        }

        token = env.get("SONAR_AUTH_TOKEN");
        if (token != null) {
            String logMessage = "[InfluxDB Plugin] INFO: Using SonarQube auth token found in environment variable SONAR_AUTH_TOKEN";
            listener.getLogger().println(logMessage);
        } else {
            String logMessage = "[InfluxDB Plugin] WARNING: No SonarQube auth token found in environment variable SONAR_AUTH_TOKEN. Depending on access rights, this might result in a HTTP/401.";
            listener.getLogger().println(logMessage);
        }
    }

    public Point[] generate() {
        setSonarDetails(sonarBuildLink);

        Point point = null;
        try {
            point = buildPoint("sonarqube_data", customPrefix, build)
                    .addField(BUILD_DISPLAY_NAME, build.getDisplayName())
                    .addField(SONARQUBE_CRITICAL_ISSUES, getSonarIssues(sonarIssuesUrl, "CRITICAL"))
                    .addField(SONARQUBE_BLOCKER_ISSUES, getSonarIssues(sonarIssuesUrl, "BLOCKER"))
                    .addField(SONARQUBE_MAJOR_ISSUES, getSonarIssues(sonarIssuesUrl, "MAJOR"))
                    .addField(SONARQUBE_MINOR_ISSUES, getSonarIssues(sonarIssuesUrl, "MINOR"))
                    .addField(SONARQUBE_INFO_ISSUES, getSonarIssues(sonarIssuesUrl, "INFO"))
                    .addField(SONARQUBE_LINES_OF_CODE, getLinesOfCode(sonarMetricsUrl))
                    .build();
        } catch (IOException e) {
            // handle
        }
        return new Point[] { point };
    }

    private String getResult(String url) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .get()
                .url(url)
                .header("Accept", "application/json");

        if (token != null) {
            String credential = Credentials.basic(token, "", StandardCharsets.UTF_8);
            requestBuilder.header("Authorization", credential);
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (response.code() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.code() + " from URL : " + url);
            }

            return response.body().string();
        }
    }

    private String getSonarProjectURLFromBuildLogs(Run<?, ?> build) throws IOException {
        String url = null;
        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
            String line;
            Pattern p = Pattern.compile(URL_PATTERN_IN_LOGS);
            while ((line = br.readLine()) != null) {
                Matcher match = p.matcher(line);
                if (match.matches()) {
                    url = match.group(1);
                }
            }
        }
        return url;
    }

    String getSonarProjectName(String url) throws URISyntaxException {
        //String sonarVersion = getResult("api/server/version");
        URI uri = new URI(url);
        String[] projectUrl;
        try {
            projectUrl = uri.getRawQuery().split("id=");
        } catch (NullPointerException e) {
            projectUrl = uri.getRawPath().split("/");
        }
        return projectUrl.length > 1 ? projectUrl[projectUrl.length - 1] : "";
    }

    private int getLinesOfCode(String url) throws IOException {
        String output = getResult(url);
        JSONObject metricsObjects = JSONObject.fromObject(output);
        int linesOfCodeCount = 0;
        JSONArray array = metricsObjects.getJSONObject("component").getJSONArray("measures");
        for (int i = 0; i < array.size(); i++) {
            JSONObject metricsObject = array.getJSONObject(i);
            if (metricsObject.get("metric").equals("ncloc")) {
                linesOfCodeCount = metricsObject.getInt("value");
            }
        }

        return linesOfCodeCount;
    }

    private int getSonarIssues(String url, String severity) throws IOException {
        String output = getResult(url + severity);
        return JSONObject.fromObject(output).getInt("total");
    }
}
