package jenkinsci.plugins.influxdb.generators;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.dto.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SonarQubePointGenerator extends AbstractPointGenerator {

    private static final String BUILD_DISPLAY_NAME = "display_name";
    private static final String SONARQUBE_LINES_OF_CODE = "lines_of_code";
    private static final String SONARQUBE_LINES = "lines";
    private static final String SONARQUBE_COMPLEXITY = "complexity";
    private static final String SONARQUBE_CRITICAL_ISSUES = "critical_issues";
    private static final String SONARQUBE_MAJOR_ISSUES = "major_issues";
    private static final String SONARQUBE_MINOR_ISSUES = "minor_issues";
    private static final String SONARQUBE_INFO_ISSUES = "info_issues";
    private static final String SONARQUBE_BLOCKER_ISSUES = "blocker_issues";
    private static final String SONARQUBE_CODE_SMELLS = "code_smells";
    private static final String SONARQUBE_BUGS = "bugs";
    private static final String SONARQUBE_COVERAGE = "coverage";
    private static final String SONARQUBE_VULNERABILITIES = "vulnerabilities";
    private static final String SONARQUBE_BRANCH_COVERAGE = "branch_coverage";
    private static final String SONARQUBE_LINE_COVERAGE = "line_coverage";
    private static final String SONARQUBE_LINES_TO_COVER = "lines_to_cover";
    private static final String SONARQUBE_DUPLICATED_LINES_DENSITY = "duplicated_lines_density";

    private static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ")
            + "(.*)";

    private static final String SONAR_ISSUES_BASE_URL = "/api/issues/search?ps=500&projectKeys=";

    private static final String SONAR_METRICS_BASE_URL = "/api/measures/component?componentKey=";
    private static final String SONAR_METRICS_BASE_METRIC = "&metricKeys=";
    private static final OkHttpClient httpClient = new OkHttpClient();

    private String sonarIssuesUrl;
    private String sonarMetricsUrl;

    private final String customPrefix;
    private final TaskListener taskListener;

    private String sonarBuildLink = null;
    private String token = null;

    private EnvVars env = null;

    public SonarQubePointGenerator(Run<?, ?> build, TaskListener listener,
                                   MeasurementRenderer<Run<?, ?>> projectNameRenderer,
                                   long timestamp, String jenkinsEnvParameterTag,
                                   String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.taskListener = listener;
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
                taskListener.getLogger().println(logMessage);
            } else {
                String logMessage = "[InfluxDB Plugin] INFO: No SonarQube host URL found in environment variable SONAR_HOST_URL. Using build log instead.";
                taskListener.getLogger().println(logMessage);
                if (sonarBuildLink.contains("/dashboard?id=" + sonarProjectName)) {
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
            taskListener.getLogger().println(logMessage);
        } else {
            String logMessage = "[InfluxDB Plugin] WARNING: No SonarQube auth token found in environment variable SONAR_AUTH_TOKEN. Depending on access rights, this might result in a HTTP/401.";
            taskListener.getLogger().println(logMessage);
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
                    .addField(SONARQUBE_LINES_OF_CODE, getSonarMetric(sonarMetricsUrl, "ncloc"))
                    .addField(SONARQUBE_LINES, getSonarMetric(sonarMetricsUrl, SONARQUBE_LINES))
                    .addField(SONARQUBE_CODE_SMELLS, getSonarMetric(sonarMetricsUrl, SONARQUBE_CODE_SMELLS))
                    .addField(SONARQUBE_BUGS, getSonarMetric(sonarMetricsUrl, SONARQUBE_BUGS))
                    .addField(SONARQUBE_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_COVERAGE))
                    .addField(SONARQUBE_VULNERABILITIES, getSonarMetric(sonarMetricsUrl, SONARQUBE_VULNERABILITIES))
                    .addField(SONARQUBE_BRANCH_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_BRANCH_COVERAGE))
                    .addField(SONARQUBE_LINE_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_LINE_COVERAGE))
                    .addField(SONARQUBE_LINES_TO_COVER, getSonarMetric(sonarMetricsUrl, SONARQUBE_LINES_TO_COVER))
                    .addField(SONARQUBE_DUPLICATED_LINES_DENSITY, getSonarMetric(sonarMetricsUrl, SONARQUBE_DUPLICATED_LINES_DENSITY))
                    .addField(SONARQUBE_COMPLEXITY, getSonarMetric(sonarMetricsUrl, SONARQUBE_COMPLEXITY))
                    .build();
        } catch (IOException e) {
            // handle
        }
        return new Point[]{point};
    }

    protected String getResult(String url) throws IOException {
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

            return Objects.requireNonNull(response.body()).string();
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
        URI uri = new URI(url);
        String[] projectUrl;
        try {
            projectUrl = uri.getRawQuery().split("id=");
        } catch (NullPointerException e) {
            projectUrl = uri.getRawPath().split("/");
        }
        return projectUrl.length > 1 ? projectUrl[projectUrl.length - 1] : "";
    }

    public float getSonarMetric(String url, String metric) throws IOException {
        String output = getResult(url + SONAR_METRICS_BASE_METRIC + metric);
        JSONObject metricsObjects = JSONObject.fromObject(output);
        JSONArray array = metricsObjects.getJSONObject("component").getJSONArray("measures");
        JSONObject metricsObject = array.getJSONObject(0);
        try {
            return Float.parseFloat(metricsObject.getString("value"));
        } catch (NumberFormatException exp) {
            return -1;
        }
    }

    private int getSonarIssues(String url, String severity) throws IOException {
        String output = getResult(url + severity);
        return JSONObject.fromObject(output).getInt("total");
    }
}
