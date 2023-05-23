package jenkinsci.plugins.influxdb.generators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String SONARQUBE_QUALITY_GATE_DETAILS = "quality_gate_details";
    private static final String SONARQUBE_DUPLICATED_LINES_DENSITY = "duplicated_lines_density";
    private static final String SONARQUBE_TECHNICAL_DEBT = "sqale_index";
    private static final String SONARQUBE_TECHNICAL_DEBT_RATIO = "sqale_debt_ratio";

    private static final short DEFAULT_MAX_RETRY_COUNT = 10;
    private static final int DEFAULT_RETRY_SLEEP = 5000;

    // Default SonarQube report file name
    private static final String SONARQUBE_DEFAULT_BUILD_REPORT_NAME = "report-task.txt";
    // Patterns used for data extraction from SonarQube report file
    private static final String PROJECT_KEY_PATTERN_IN_REPORT = "projectKey=(.*)";
    private static final String URL_PATTERN_IN_REPORT = "serverUrl=(.*)";
    private static final String TASK_ID_PATTERN_IN_REPORT = "ceTaskId=(.*)";
    private static final String TASK_URL_PATTERN_IN_REPORT = "ceTaskUrl=(.*)";
    // Patterns used for data extraction from build log
    private static final String URL_PATTERN_IN_LOGS_ANALYSIS = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can browse ")
            + "(.*)";
    private static final String TASK_URL_PATTERN_IN_LOGS = ".*" + Pattern.quote("More about the report processing at ")
            + "(.*)";
    private static final String PROJECT_NAME_PATTERN_IN_LOGS = ".*" + Pattern.quote("Project key: ")
            + "(.*)";
    private static final String URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS = ".*" + Pattern.quote("QUALITY GATE STATUS: ") 
            + "(.*)" + Pattern.quote(" - View details on ") + "(.*)";
    private static final String URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS_v4_8 = ".*" + Pattern.quote("ANALYSIS SUCCESSFUL, you can find the results at: ") 
            + "(.*)";
    private static final String URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS_TIMEOUT = ".*" + Pattern.quote("Quality Gate check timeout exceeded - View details on ") 
            + "(.*)";


            
    private String projectKey = null;
    private String sonarBuildURL = null;
    private String sonarBuildTaskIdUrl = null;
    private String sonarBuildTaskId = null;

    // https://sonarcloud.io/web_api/api/issues
    private static final String SONAR_ISSUES_BASE_URL = "/api/issues/search?ps=1";

    // SonarQube 5.4+ expects componentKey=, SonarQube 8.1 expects component=, we
    // can make both of them happy
    // https://sonarcloud.io/web_api/api/measures
    private static final String SONAR_METRICS_BASE_URL = "/api/measures/component?componentKey=%s&component=%s";
    private static final String SONAR_METRICS_BASE_METRIC = "&metricKeys=";
    private static final OkHttpClient httpClient = new OkHttpClient();

    private String sonarIssuesUrl;
    private String sonarMetricsUrl;  

    private final String customPrefix;
    private final TaskListener listener;

    private String token = null;

    private EnvVars env;

    public SonarQubePointGenerator(Run<?, ?> build, TaskListener listener,
                                   ProjectNameRenderer projectNameRenderer,
                                   long timestamp,
                                   String jenkinsEnvParameterTag,
                                   String customPrefix,
                                   EnvVars env) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.listener = listener;
        this.env = env;
    }

    /**
     * @return true, if environment variable LOG_SONAR_QUBE_RESULTS is set to true and SQ Reports exist
     */
    public boolean hasReport() {
 
        String[] result = null;
 
        try {
            result = getSonarProjectFromBuildReport();
                projectKey = result[0];
                sonarBuildURL = result[1];
                sonarBuildTaskId = result[2];
                sonarBuildTaskIdUrl = result[3];
 
                return !StringUtils.isEmpty(sonarBuildURL);
        } catch (IOException | IndexOutOfBoundsException | UncheckedIOException ignored) {}
 
        try {
            //try build logs
            result = getSonarProjectFromBuildLog(build);
 
            if (!StringUtils.isEmpty(result[1])){
                projectKey = result[0];
                sonarBuildURL = result[1];
                sonarBuildTaskId = result[2];
                sonarBuildTaskIdUrl = result[3];
 
                return !StringUtils.isEmpty(sonarBuildURL);
            }
        } catch (IOException | IndexOutOfBoundsException | UncheckedIOException ignored) {}
 
        return false;
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }

    private void waitForQualityGateTask() throws IOException {
        String status;
        String output;
        String logMessage;
        int count = 0;
        int MAX_RETRY_COUNT = DEFAULT_MAX_RETRY_COUNT;

        String max_retry = this.env.get("SONAR_TASK_MAX_RETRY_COUNT");

        if (max_retry != null && !max_retry.isEmpty()) {
            try {
                MAX_RETRY_COUNT = Integer.parseInt(max_retry);
            } catch(NumberFormatException ignored) { }
        }

        do {
            try {
                Thread.sleep(DEFAULT_RETRY_SLEEP);
            } catch(InterruptedException ignored) {}

            output = getResult(sonarBuildTaskIdUrl);
            JSONObject taskObjects = JSONObject.fromObject(output);
            status = taskObjects.getJSONObject("task").getString("status").toUpperCase();

            ++count;

            if (status.equals("FAILED") || status.equals("CANCELED")) {
                logMessage = "[InfluxDB Plugin] Warning: SonarQube task " + sonarBuildTaskId +  " failed. Status is " + status + "! Getting the QG metrics from the latest completed task!";
                listener.getLogger().println(logMessage);                   
                break;
            }

            logMessage = "[InfluxDB Plugin] INFO: SonarQube task " + sonarBuildTaskId +  " status is " + status;
            listener.getLogger().println(logMessage);
            
        } while (!status.equals("SUCCESS") && count <= MAX_RETRY_COUNT);

        if(!status.equals("SUCCESS") && count > MAX_RETRY_COUNT) {
            logMessage = "[InfluxDB Plugin] WARNING: Timeout! SonarQube task " + sonarBuildTaskId + " is still in progress. Getting the QG metrics from the latest completed task!";
            listener.getLogger().println(logMessage);
        }
    }

    private void setSonarDetails(String sonarBuildURL) {
        // Use SONAR_HOST_URL environment variable if provided, sonarBuildURL otherwise
        String sonarServer = env.get("SONAR_HOST_URL", sonarBuildURL);
        listener.getLogger().println("[InfluxDB Plugin] INFO: Using SonarQube host URL: " + sonarServer);
        
        // https://sonarcloud.io/web_api/api/issues
        sonarIssuesUrl = sonarServer + SONAR_ISSUES_BASE_URL 
                            + "&componentKeys=" + projectKey 
                            + "&resolved=false&severities=";

        sonarMetricsUrl = sonarServer + String.format(SONAR_METRICS_BASE_URL, projectKey, projectKey);

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
        setSonarDetails(sonarBuildURL);

        Point point = null;
        try {
            if(sonarBuildTaskId != null){
                waitForQualityGateTask();
            }

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
                    .addField(SONARQUBE_QUALITY_GATE_DETAILS, getSonarMetricStr(sonarMetricsUrl, SONARQUBE_QUALITY_GATE_DETAILS))
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

    private String[] getSonarProjectFromBuildLog(Run<?, ?> build) throws IOException {
 
        String projName = null;
        String url = null;
        String taskId = null;
        String taskUrl = null;
 
        try (BufferedReader br = new BufferedReader(build.getLogReader())) {
            String line;
            Matcher match;
            Pattern p_analysis_url = Pattern.compile(URL_PATTERN_IN_LOGS_ANALYSIS);
            Pattern p_qg_url = Pattern.compile(URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS);
            Pattern p_qg_url_timeout = Pattern.compile(URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS_TIMEOUT);
            Pattern p_qg_url_v4_8 = Pattern.compile(URL_PATTERN_IN_LOGS_QUALITY_GATE_STATUS_v4_8);
        
            Pattern p_taskUrl = Pattern.compile(TASK_URL_PATTERN_IN_LOGS);
            Pattern p_projName = Pattern.compile(PROJECT_NAME_PATTERN_IN_LOGS);

            while ((line = br.readLine()) != null) {
                match = p_projName.matcher(line);
                if (match.matches()) {
                    projName = match.group(1);
                    continue;
                }
                match = p_qg_url.matcher(line);
                if (match.matches()) {
                    url  = match.group(2);
                    //Task already executed.  No need to search for other lines
                    break; 
                }
                match = p_qg_url_v4_8.matcher(line);
                if (match.matches()) {
                    url  = match.group(1);
                    //Task already executed.  No need to search for other lines
                    break; 
                }
                match = p_qg_url_timeout.matcher(line);
                if (match.matches()) {
                    url  = match.group(1);
                    //Task already executed.  No need to search for other lines
                    break; 
                }
                match = p_analysis_url.matcher(line);
                if (match.matches()) {
                    url  = match.group(1);
                    continue;
                }
                match = p_taskUrl.matcher(line);
                if (match.matches()) {
                    taskUrl = match.group(1);
                    taskId = taskUrl.split("=")[1];
                    break; // No need to search for other lines
                }
            }
        }
        return new String[]{projName, url, taskId, taskUrl};
    }

    private String[] getSonarProjectFromBuildReport() throws IOException, UncheckedIOException {
        String projName = null;
        String url = null;
        String taskId = null;
        String taskUrl = null;

        String workspaceDir = env.get("WORKSPACE");
        List<Path> reportsPaths = workspaceDir == null ? null : this.findReportByFileName(workspaceDir);
        
        if (reportsPaths == null || reportsPaths.size() != 1) {
            return new String[]{null, null, null, null};
        }
        String reportFilePath = reportsPaths.get(0).toFile().getPath();
        
        try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(reportFilePath), StandardCharsets.UTF_8))) {
            String line;
            
            Pattern p_proj_name = Pattern.compile(PROJECT_KEY_PATTERN_IN_REPORT);
            Pattern p_url = Pattern.compile(URL_PATTERN_IN_REPORT);
            Pattern p_task = Pattern.compile(TASK_ID_PATTERN_IN_REPORT);
            Pattern p_url_task = Pattern.compile(TASK_URL_PATTERN_IN_REPORT);
            while ((line = br.readLine()) != null) {

                Matcher match = p_proj_name.matcher(line);
                if (match.matches()) {
                    projName = match.group(1);
                    continue;
                } 

                match = p_url.matcher(line);
                if (match.matches()) {
                    url = match.group(1);
                    continue;
                } 
                
                match = p_task.matcher(line);
                if (match.matches()) {
                    taskId = match.group(1);
                    continue;
                } 

                match = p_url_task.matcher(line);
                if (match.matches()) {
                    taskUrl = match.group(1);
                }
            }
        }

        return new String[]{projName, url, taskId, taskUrl};
    }

    public List<Path> findReportByFileName(String workspacePath)
            throws IOException, UncheckedIOException {

        Path path = Paths.get(workspacePath);                
        String reportName = env.get("SONARQUBE_BUILD_REPORT_NAME", 
                                    SONARQUBE_DEFAULT_BUILD_REPORT_NAME);

        try (Stream<Path> pathStream = Files.find(path,
                Integer.MAX_VALUE,
                (p, basicFileAttributes) ->
                    basicFileAttributes.isRegularFile() && 
                    p.endsWith(reportName));
        ) {
            return pathStream.collect(Collectors.toList());
        }
    }

    public String getSonarMetricStr(String url, String metric) throws IOException {
        return getSonarMetricValue(url, metric);
    }

    public Float getSonarMetric(String url, String metric) throws IOException {
        Float value = null;
        try {
            value = Float.parseFloat(getSonarMetricValue(url, metric));
        } catch (NumberFormatException ignored) {}

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
        } catch (IndexOutOfBoundsException ignored) {}

        return value;
    }

    private int getSonarIssues(String url, String severity) throws IOException {
        String output = getResult(url + severity);
        return JSONObject.fromObject(output).getInt("total");
    }
}
