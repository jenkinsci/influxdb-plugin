package jenkinsci.plugins.influxdb;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.write.Point;
import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.generators.*;
import jenkinsci.plugins.influxdb.generators.serenity.SerenityJsonSummaryFile;
import jenkinsci.plugins.influxdb.generators.serenity.SerenityPointGenerator;
import jenkinsci.plugins.influxdb.models.Target;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class InfluxDbPublicationService {

    /**
     * The logger.
     **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublicationService.class.getName());

    /**
     * Shared HTTP client which can make use of connection and thread pooling.
     */
    private static final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Targets to write to.
     */
    private final List<Target> selectedTargets;

    /**
     * Custom project name, overrides the project name with the specified value.
     */
    private final String customProjectName;

    /**
     * Custom prefix, for example in multi-branch pipelines, where every build is named
     * after the branch built and thus you have different builds called 'master' that report
     * different metrics.
     */
    private final String customPrefix;

    /**
     * Custom data fields, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myFields = [:]
     * myFields['field_a'] = 11
     * myFields['field_b'] = 12
     * influxDbPublisher(selectedTarget: 'my-target', customData: myFields)
     * }</pre>
     */
    private final Map<String, Object> customData;

    /**
     * Custom data tags, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myTags = [:]
     * myTags['tag_1'] = 'foo'
     * myTags['tag_2'] = 'bar'
     * influxDbPublisher(selectedTarget: 'my-target', customData: ..., customDataTags: myTags)
     * }</pre>
     */
    private final Map<String, String> customDataTags;

    /**
     * Custom data maps for fields, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * This goes beyond {@code customData} since it allows to define multiple {@code customData} measurements
     * where the name of the measurement is defined as the key of the {@code customDataMap}.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myFields1 = [:]
     * def myFields2 = [:]
     * def myCustomMeasurementFields = [:]
     * myFields1['field_a'] = 11
     * myFields1['field_b'] = 12
     * myFields2['field_c'] = 21
     * myFields2['field_d'] = 22
     * myCustomMeasurementFields['series_1'] = myFields1
     * myCustomMeasurementFields['series_2'] = myFields2
     * influxDbPublisher(selectedTarget: 'my-target', customDataMap: myCustomMeasurementFields)
     * }</pre>
     */
    private final Map<String, Map<String, Object>> customDataMap;

    /**
     * Custom data maps for tags, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * Custom tags that are sent to respective measurements defined in {@code customDataMap}.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myTags = [:]
     * def myCustomMeasurementTags = [:]
     * myTags['buildResult'] = currentBuild.result
     * myTags['NODE_LABELS'] = env.NODE_LABELS
     * myCustomMeasurementTags['series_1'] = myTags
     * influxDbPublisher(selectedTarget: 'my-target', customDataMap: ..., customDataMapTags: myCustomMeasurementTags)
     * }</pre>
     */
    private final Map<String, Map<String, String>> customDataMapTags;

    /**
     * Jenkins parameter(s) which will be added as field set to measurement 'jenkins_data'.
     * If parameter value has a $-prefix, it will be resolved from current Jenkins job environment properties.
     */
    private final String jenkinsEnvParameterField;

    /**
     * Jenkins parameter(s) which will be added as tag set to all measurements.
     * If parameter value has a $-prefix, it will be resolved from current Jenkins job environment properties.
     */
    private final String jenkinsEnvParameterTag;

    /**
     * Custom measurement name used for all measurement types,
     * overrides the default measurement names.
     * Default value is "jenkins_data"
     * <p>
     * For custom data, prepends "custom_", i.e. "some_measurement"
     * becomes "custom_some_measurement".
     * Default custom name remains "jenkins_custom_data".
     */
    private final String measurementName;

    private final long timestamp;

    public InfluxDbPublicationService(List<Target> selectedTargets, String customProjectName, String customPrefix, Map<String, Object> customData, Map<String, String> customDataTags, Map<String, Map<String, String>> customDataMapTags, Map<String, Map<String, Object>> customDataMap, long timestamp, String jenkinsEnvParameterField, String jenkinsEnvParameterTag, String measurementName) {
        this.selectedTargets = selectedTargets;
        this.customProjectName = customProjectName;
        this.customPrefix = customPrefix;
        this.customData = customData;
        this.customDataTags = customDataTags;
        this.customDataMap = customDataMap;
        this.customDataMapTags = customDataMapTags;
        this.timestamp = timestamp;
        this.jenkinsEnvParameterField = jenkinsEnvParameterField;
        this.jenkinsEnvParameterTag = jenkinsEnvParameterTag;
        this.measurementName = measurementName;
    }

    public void perform(Run<?, ?> build, TaskListener listener, EnvVars env) {
        // Logging
        listener.getLogger().println("[InfluxDB Plugin] Collecting data...");

        // Renderer to use for the metrics
        ProjectNameRenderer measurementRenderer = new ProjectNameRenderer(customPrefix, customProjectName);

        // Points to write
        List<Point> pointsToWrite = new ArrayList<>();

        // Basic metrics
        JenkinsBasePointGenerator jGen = new JenkinsBasePointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, jenkinsEnvParameterField, customPrefix, measurementName, env);
        addPoints(pointsToWrite, jGen, listener);

        AgentPointGenerator agentGen = new AgentPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
        addPointsFromPlugin(pointsToWrite, agentGen, listener, "Custom Data");

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix, customData, customDataTags, measurementName);
        addPointsFromPlugin(pointsToWrite, cdGen, listener, "Custom Data");

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix, customDataMap, customDataMapTags);
        addPointsFromPlugin(pointsToWrite, cdmGen, listener, "Custom Data Map");

        try {
            CoberturaPointGenerator cGen = new CoberturaPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, cGen, listener, "Cobertura");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Cobertura");
        }

        try {
            RobotFrameworkPointGenerator rfGen = new RobotFrameworkPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, rfGen, listener, "Robot Framework");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Robot Framework");
        }

        try {
            JacocoPointGenerator jacoGen = new JacocoPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, jacoGen, listener, "JaCoCo");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: JaCoCo");
        }

        try {
            PerformancePointGenerator perfGen = new PerformancePointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, perfGen, listener, "Performance");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Performance");
        }

        try {
            GitPointGenerator gitGen = new GitPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, gitGen, listener, "Git");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Git");
        }

        JUnitPointGenerator junitGen = new JUnitPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix, env);
        addPointsFromPlugin(pointsToWrite, junitGen, listener, "JUnit");

        SonarQubePointGenerator sonarGen = new SonarQubePointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix, env);
        addPointsFromPlugin(pointsToWrite, sonarGen, listener, "SonarQube");

        SerenityJsonSummaryFile serenityJsonSummaryFile = new SerenityJsonSummaryFile(env.get("WORKSPACE"));
        SerenityPointGenerator serenityGen = new SerenityPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix, serenityJsonSummaryFile);
        addPointsFromPlugin(pointsToWrite, serenityGen, listener, "Serenity");

        ChangeLogPointGenerator changeLogGen = new ChangeLogPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
        addPointsFromPlugin(pointsToWrite, changeLogGen, listener, "Change log");

        try {
            PerfPublisherPointGenerator perfPublisherGen = new PerfPublisherPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, perfPublisherGen, listener, "Performance Publisher");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Performance Publisher");
        }

        try {
            MetricsPointGenerator metricsGen = new MetricsPointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, metricsGen, listener, "Metrics");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Metrics");
        }

        try {
            CoveragePointGenerator coverageGen = new CoveragePointGenerator(build, listener, measurementRenderer, timestamp, jenkinsEnvParameterTag, customPrefix);
            addPointsFromPlugin(pointsToWrite, coverageGen, listener, "Coverage");
        } catch (NoClassDefFoundError ignore) {
            logger.fine("Plugin skipped: Coverage");
        }

        for (Target target : selectedTargets) {
            URL url;
            try {
                url = new URL(target.getUrl());
            } catch (MalformedURLException e) {
                String logMessage = String.format("[InfluxDB Plugin] Skipping target '%s' due to invalid URL '%s'",
                        target.getDescription(),
                        target.getUrl());
                logger.warning(logMessage);
                listener.getLogger().println(logMessage);
                continue;
            }

            String logMessage = String.format("[InfluxDB Plugin] Publishing data to target '%s' (url='%s', database='%s')",
                    target.getDescription(),
                    target.getUrl(),
                    target.getDatabase());
            logger.fine(logMessage);
            listener.getLogger().println(logMessage);

            try (InfluxDBClient influxDB = getInfluxDBClient(build, target, url)) {
                writeToInflux(target, influxDB, pointsToWrite);
            }

        }

        listener.getLogger().println("[InfluxDB Plugin] Completed.");
    }

    private void addPointsFromPlugin(List<Point> pointsToWrite, PointGenerator generator, TaskListener listener, String plugin) {
        if (generator.hasReport()) {
            listener.getLogger().println("[InfluxDB plugin] " + plugin + " plugin data found. Writing to InfluxDB...");
            addPoints(pointsToWrite, generator, listener);
        } else {
            logger.fine("Data not found: " + plugin);
        }
    }

    private InfluxDBClient getInfluxDBClient(Run<?, ?> build, Target target, URL url) {
        StandardUsernamePasswordCredentials credentials = CredentialsProvider.findCredentialById(target.getCredentialsId(), StandardUsernamePasswordCredentials.class, build);
        InfluxDBClient influxDB;

        if (target.getOrganization() != null && !target.getOrganization().trim().isEmpty()) {
            InfluxDBClientOptions.Builder options = InfluxDBClientOptions.builder()
                    .url(target.getUrl())
                    .org(target.getOrganization())
                    .bucket(target.getDatabase())
                    .okHttpClient(createHttpClient(url, target.isUsingJenkinsProxy()));
            if (credentials != null) {  // basic auth
                options.authenticate(credentials.getUsername(), credentials.getPassword().getPlainText().toCharArray());
            } else {    // token auth
                StringCredentials c = CredentialsProvider.findCredentialById(target.getCredentialsId(), StringCredentials.class, build);
                assert c != null;
                options.authenticateToken(c.getSecret().getPlainText().toCharArray());
            }
            influxDB = InfluxDBClientFactory.create(options.build());
        } else {
            influxDB = credentials == null ?
                InfluxDBClientFactory.createV1(target.getUrl(), "", "".toCharArray(), target.getDatabase(), target.getRetentionPolicy()) :
                InfluxDBClientFactory.createV1(target.getUrl(), credentials.getUsername(), credentials.getPassword().getPlainText().toCharArray(), target.getDatabase(), target.getRetentionPolicy());
        }
        return influxDB;
    }

    private void addPoints(List<Point> pointsToWrite, PointGenerator generator, TaskListener listener) {
        try {
            pointsToWrite.addAll(Arrays.stream(generator.generate()).filter(Objects::nonNull).collect(Collectors.toList()));
        } catch (Exception e) {
            listener.getLogger().println("[InfluxDB Plugin] Failed to collect data. Ignoring Exception:" + e);
        }
    }

    private OkHttpClient.Builder createHttpClient(URL url, boolean useProxy) {
        OkHttpClient.Builder builder = httpClient.newBuilder();
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        ProxyConfiguration proxyConfig = jenkins == null ? null : jenkins.proxy;
        if (useProxy && proxyConfig != null) {
            builder.proxy(proxyConfig.createProxy(url.getHost()));
            if (proxyConfig.getUserName() != null) {
                builder.proxyAuthenticator((route, response) -> {
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null; // Give up, we've already failed to authenticate.
                    }

                    String credential = Credentials.basic(proxyConfig.getUserName(), proxyConfig.getSecretPassword().getPlainText());
                    return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                });
            }
        }
        return builder;
    }

    private void writeToInflux(Target target, InfluxDBClient influxDB, List<Point> pointsToWrite) {
        /*
         * build batchpoints for a single write.
         */
        try {
            influxDB.getWriteApiBlocking().writePoints(pointsToWrite);
        } catch (Exception e) {
            if (target.isExposeExceptions()) {
                throw new InfluxReportException(e);
            } else {
                //Exceptions not exposed by configuration. Just log and ignore.
                logger.log(Level.WARNING, "Could not report to InfluxDB. Ignoring Exception.", e);
            }
        }
    }
}
