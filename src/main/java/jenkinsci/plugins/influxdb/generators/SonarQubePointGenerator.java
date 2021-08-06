package jenkinsci.plugins.influxdb.generators;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.influxdb.client.write.Point;
import hudson.EnvVars;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import hudson.model.Run;
import hudson.model.TaskListener;
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
    private static final String SONARQUBE_CODE_SMELLS = "code_smells";
    private static final String SONARQUBE_BUGS = "bugs";
    private static final String SONARQUBE_COVERAGE = "coverage";
    private static final String SONARQUBE_VULNERABILITIES = "vulnerabilities";
    private static final String SONARQUBE_BRANCH_COVERAGE = "branch_coverage";
    private static final String SONARQUBE_LINE_COVERAGE = "line_coverage";
    private static final String SONARQUBE_LINES_TO_COVER = "lines_to_cover";
    private static final String SONARQUBE_ALERT_STATUS = "alert_status"; // Quality Gate Status
    private static final String SONARQUBE_DUPLICATED_LINES_DENSITY = "duplicated_lines_density";
    private static final String SONARQUBE_TECHNICAL_DEBT = "sqale_index";
    private static final String SONARQUBE_TECHNICAL_DEBT_RATIO = "sqale_debt_ratio";

    private static final short DEFAULT_MAX_RETRY_COUNT = 10;
    private static final int DEFAULT_RETRY_SLEEP = 5000;

    private static final String URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ")
            + "(.*)";
    private static final String TASK_ID_PATTERN_IN_LOGS = ".*" + Pattern.quote("More about the report processing at ")
            + "(.*)";

    private static final String SONAR_ISSUES_BASE_URL = "/api/issues/search?ps=1&projects=";

    // SonarQube 5.4+ expects componentKey=, SonarQube 8.1 expects component=, we
    // can make both of them happy
    private static final String SONAR_METRICS_BASE_URL = "/api/measures/component?componentKey=%s&component=%s";
    private static final String SONAR_METRICS_BASE_METRIC = "&metricKeys=";
    private static final OkHttpClient httpClient = new OkHttpClient();

    private String sonarIssuesUrl;
    private String sonarMetricsUrl;
    private String sonarBuildTaskIdUrl = null;

    private final String customPrefix;
    private final TaskListener listener;

    private String sonarBuildLink = null;
    

    private String token = null;

    private EnvVars env = null;

    public SonarQubePointGenerator(Run<?, ?> build, TaskListener listener,
                                   ProjectNameRenderer projectNameRenderer,
                                   long timestamp,
                                   String jenkinsEnvParameterTag,
                                   String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.listener = listener;
    }

    public boolean hasReport() {
        try {

            String[] result = getSonarProjectURLFromBuildLogs(build);

            sonarBuildLink = result[0];
            sonarBuildTaskIdUrl = result[1];

            return !StringUtils.isEmpty(sonarBuildLink);
        } catch (IOException | IndexOutOfBoundsException e) {}

        return false;
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }


    private void waitForQualityGateTask() throws IOException {

        String status = null;
        String output = null;
        String logMessage = null;

        int count = 0;

        int MAX_RETRY_COUNT = DEFAULT_MAX_RETRY_COUNT;

        String max_retry = this.env.get("SONAR_TASK_MAX_RETRY_COUNT");

        if (max_retry != null && !max_retry.isEmpty()) {
            try {
                MAX_RETRY_COUNT = Integer.parseInt(max_retry);
            } catch(NumberFormatException e) {
                MAX_RETRY_COUNT = DEFAULT_MAX_RETRY_COUNT;
            }
        }

        String taskID = sonarBuildTaskIdUrl.split("=")[1];

        do {
            try {
                Thread.sleep(DEFAULT_RETRY_SLEEP);
            } catch(InterruptedException e) {}

            output = getResult(sonarBuildTaskIdUrl);
            JSONObject taskObjects = JSONObject.fromObject(output);
            status = taskObjects.getJSONObject("task").getString("status").toUpperCase();

            ++count;

            if (status.equals("FAILED") || status.equals("CANCELED")) {
                logMessage = "[InfluxDB Plugin] Warning: SonarQube task " + taskID +  " failed. Status is " + status + "! Getting the QG metrics from the latest completed task!";
                listener.getLogger().println(logMessage);                   
                break;
            }

            logMessage = "[InfluxDB Plugin] INFO: SonarQube task " + taskID +  " status is " + status;
            listener.getLogger().println(logMessage);
            
        } while (!status.equals("SUCCESS") && count <= MAX_RETRY_COUNT);

        if(!status.equals("SUCCESS") && count > MAX_RETRY_COUNT) {
            logMessage = "[InfluxDB Plugin] WARNING: Timeout! SonarQube task " + taskID + " is still in progress. Getting the QG metrics from the latest completed task!";
            listener.getLogger().println(logMessage);
        }
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
            sonarMetricsUrl = sonarServer + String.format(SONAR_METRICS_BASE_URL, sonarProjectName, sonarProjectName);
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

            waitForQualityGateTask();

            point = buildPoint("sonarqube_data", customPrefix, build)
                    .addField(BUILD_DISPLAY_NAME, build.getDisplayName())
                    .addField(SONARQUBE_CRITICAL_ISSUES, getSonarIssues(sonarIssuesUrl, "CRITICAL"))
                    .addField(SONARQUBE_BLOCKER_ISSUES, getSonarIssues(sonarIssuesUrl, "BLOCKER"))
                    .addField(SONARQUBE_MAJOR_ISSUES, getSonarIssues(sonarIssuesUrl, "MAJOR"))
                    .addField(SONARQUBE_MINOR_ISSUES, getSonarIssues(sonarIssuesUrl, "MINOR"))
                    .addField(SONARQUBE_INFO_ISSUES, getSonarIssues(sonarIssuesUrl, "INFO"))
                    .addField(SONARQUBE_LINES_OF_CODE, getSonarMetric(sonarMetricsUrl, "ncloc"))
                    .addField(SONARQUBE_CODE_SMELLS, getSonarMetric(sonarMetricsUrl, SONARQUBE_CODE_SMELLS))
                    .addField(SONARQUBE_BUGS, getSonarMetric(sonarMetricsUrl, SONARQUBE_BUGS))
                    .addField(SONARQUBE_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_COVERAGE))
                    .addField(SONARQUBE_VULNERABILITIES, getSonarMetric(sonarMetricsUrl, SONARQUBE_VULNERABILITIES))
                    .addField(SONARQUBE_BRANCH_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_BRANCH_COVERAGE))
                    .addField(SONARQUBE_LINE_COVERAGE, getSonarMetric(sonarMetricsUrl, SONARQUBE_LINE_COVERAGE))
                    .addField(SONARQUBE_LINES_TO_COVER, getSonarMetric(sonarMetricsUrl, SONARQUBE_LINES_TO_COVER))
                    .addField(SONARQUBE_DUPLICATED_LINES_DENSITY, getSonarMetric(sonarMetricsUrl, SONARQUBE_DUPLICATED_LINES_DENSITY))
                    .addField(SONARQUBE_COMPLEXITY, getSonarMetric(sonarMetricsUrl, SONARQUBE_COMPLEXITY))
                    .addField(SONARQUBE_ALERT_STATUS, getSonarMetricStr(sonarMetricsUrl, SONARQUBE_ALERT_STATUS))
                    .addField(SONARQUBE_TECHNICAL_DEBT, getSonarMetric(sonarMetricsUrl, SONARQUBE_TECHNICAL_DEBT))
                    .addField(SONARQUBE_TECHNICAL_DEBT_RATIO, getSonarMetric(sonarMetricsUrl, SONARQUBE_TECHNICAL_DEBT_RATIO));
        } catch (IOException e) {
            String logMessage = "[InfluxDB Plugin] Warning: IOException while fetching SonarQube metrics: " + e.getMessage();
            listener.getLogger().println(logMessage);
            e.printStackTrace(listener.getLogger());
        }
        return new Point[] { point };
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
            ResponseBody body = response.body();
            if (body == null) {
                throw new NullPointerException("Failed : null body from URL : " + url);
            }

            return body.string();
        }
    }

    private String[] getSonarProjectURLFromBuildLogs(Run<?, ?> build) throws IOException {
        String url = null;
        String taskid = null;

        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
            String line;
            Pattern p_url = Pattern.compile(URL_PATTERN_IN_LOGS);
            Pattern p_task = Pattern.compile(TASK_ID_PATTERN_IN_LOGS);
            while ((line = br.readLine()) != null) {
                Matcher match = p_url.matcher(line);
                if (match.matches()) {
                    url = match.group(1);
                } else {
                    match = p_task.matcher(line);
                    if (match.matches()) {
                        taskid = match.group(1);
                        break; // No need to search for other lines
                    } 
                }
            }
        }

        String[] rtr_str_array = {url, taskid}; //return string array

        return rtr_str_array;
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

    public String getSonarMetricStr(String url, String metric) throws IOException {
        return getSonarMetricValue(url, metric);
    }

    public Float getSonarMetric(String url, String metric) throws IOException {
        Float value = null;
        try {
            value = Float.parseFloat(getSonarMetricValue(url, metric));
        } catch (NumberFormatException exp) {}

        return value;
    }

    private String getSonarMetricValue(String url, String metric) throws IOException {

        String value = "";

        String output = getResult(url + SONAR_METRICS_BASE_METRIC + metric);
        JSONObject metricsObjects = JSONObject.fromObject(output);
        try {
            JSONArray array = metricsObjects.getJSONObject("component").getJSONArray("measures");
            JSONObject metricsObject = array.getJSONObject(0);
            value = metricsObject.getString("value");
        } catch (IndexOutOfBoundsException exp) {}

        return value;
    }

    private int getSonarIssues(String url, String severity) throws IOException {
        String output = getResult(url + severity);
        return JSONObject.fromObject(output).getInt("total");
    }
}
