package jenkinsci.plugins.influxdb;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import jenkinsci.plugins.influxdb.generators.*;
import jenkinsci.plugins.influxdb.models.Target;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import okhttp3.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep{

    /** The logger. **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublisher.class.getName());

    @Extension(optional = true)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String selectedTarget;

    /**
     * custom project name, overrides the project name with the specified value
     */
    private String customProjectName;

    /**
     * custom prefix, for example in multi branch pipelines, where every build is named
     * after the branch built and thus you have different builds called 'master' that report
     * different metrics.
     */
    private String customPrefix;

    /**
     * Jenkins parameter/s which will be added as FieldSet to measurement 'jenkins_data'.
     * If parameter-value has a $-prefix, it will be resolved from current jenkins-job environment-properties.
     */
    private String jenkinsEnvParameterField;

    /**
     * Jenkins parameter/s which will be added as TagSet to  measurement 'jenkins_data'.
     * If parameter-value has a $-prefix, it will be resolved from current jenkins-job environment-properties.
     */
    private String jenkinsEnvParameterTag;

    /**
     * custom data, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB
     * This can easily be done by calling
     *
     *   def myDataMap = [:]
     *   myDataMap['myKey'] = 'myValue'
     *   step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customData: myDataMap])
     *
     * inside a pipeline script
     */
    private Map<String, Object> customData;


    /**
     * custom data tags, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB
     * This can easily be done by calling
     *
     *   def myDataMapTags = [:]
     *   myDataMapTags['myKey'] = 'myValue'
     *   step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customData: myDataMap, customDataTags: myDataMapTags])
     *
     * inside a pipeline script
     */
    private Map<String, String> customDataTags;

    /**
     * custom data maps, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     *
     * This goes beyond customData since it allows to define multiple customData measurements
     * where the name of the measurement is defined as the key of the customDataMap.
     *
     * Example for a pipeline script:
     *
     *   def myDataMap1 = [:]
     *   def myDataMap2 = [:]
     *   def myCustomDataMap = [:]
     *   myDataMap1["myMap1Key1"] = 11 //first value of first map
     *   myDataMap1["myMap1Key2"] = 12 //second value of first map
     *   myDataMap2["myMap2Key1"] = 21 //first value of second map
     *   myDataMap2["myMap2Key2"] = 22 //second value of second map
     *   myCustomDataMap["series1"] = myDataMap1
     *   myCustomDataMap["series2"] = myDataMap2
     *   step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customDataMap: myCustomDataMap])
     *
     */
    private Map<String, Map<String, Object>> customDataMap;

    /**
     * custom tags that are sent to all measurements defined in customDataMaps.
     *
     * Example for a pipeline script:
     *
     * def myCustomDataMapTags = [:]
     * def myCustomTags = [:]
     * myCustomTags["buildResult"] = currentBuild.result
     * myCustomTags["NODE_LABELS"] = env.NODE_LABELS
     * myCustomDataMapTags["series1"] = myCustomTags
     * step([$class: 'InfluxDbPublisher',
     *       target: myTarget,
     *       customPrefix: 'myPrefix',
     *       customDataMap: myCustomDataMap,
     *       customDataMapTags: myCustomDataMapTags])
     */

    private Map<String, Map<String, String>> customDataMapTags;

    public InfluxDbPublisher() {
    }

    @DataBoundConstructor
    public InfluxDbPublisher(String target) {
        this.selectedTarget = target;
    }

    public String getSelectedTarget() {
        String ipTemp = selectedTarget;
        if (ipTemp == null) {
            Target[] targets = DESCRIPTOR.getTargets();
            if (targets.length > 0) {
                //ipTemp = targets[0].getUrl() + "," + targets[0].getDatabase();
                ipTemp = targets[0].getDescription();
            }
        }
        return ipTemp;
    }

    public void setSelectedTarget(String target) {
        Preconditions.checkNotNull(target);
        this.selectedTarget = target;
    }

    public String getCustomProjectName() {
        return customProjectName;
    }

    @DataBoundSetter
    public void setCustomProjectName(String customProjectName) {
        this.customProjectName = customProjectName;
    }

    public String getCustomPrefix() {
        return customPrefix;
    }

    @DataBoundSetter
    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
    }

    public String getJenkinsEnvParameterField() {
        return jenkinsEnvParameterField;
    }

    @DataBoundSetter
    public void setJenkinsEnvParameterField(String jenkinsEnvParameterField) {
        this.jenkinsEnvParameterField = jenkinsEnvParameterField;
    }

    public String getJenkinsEnvParameterTag() {
        return jenkinsEnvParameterTag;
    }

    @DataBoundSetter
    public void setJenkinsEnvParameterTag(String jenkinsEnvParameterTag) {
        this.jenkinsEnvParameterTag = jenkinsEnvParameterTag;
    }

    @DataBoundSetter
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    @DataBoundSetter
    public void setCustomDataTags(Map<String, String> customDataTags) {
        this.customDataTags = customDataTags;
    }

    public Map<String, String> getCustomDataTags() {
        return customDataTags;
    }

    @DataBoundSetter
    public void setCustomDataMap(Map<String, Map<String, Object>> customDataMap) {
        this.customDataMap = customDataMap;
    }

    public Map<String, Map<String, Object>> getCustomDataMap() {
        return customDataMap;
    }

    @DataBoundSetter
    public void setCustomDataMapTags(Map<String, Map<String, String>> customDataMapTags) {
        this.customDataMapTags = customDataMapTags;
    }

    public Map<String, Map<String, String>> getCustomDataMapTags() { return customDataMapTags; }

    public Target getTarget() {
        Target[] targets = DESCRIPTOR.getTargets();
        if (selectedTarget == null && targets.length > 0) {
            return targets[0];
        }
        for (Target target : targets) {
            String targetInfo = target.getDescription();
            if (targetInfo.equals(selectedTarget)) {
                return target;
            }
        }
        return null;
    }

    //@Override
    public boolean prebuild(Run<?, ?> build, TaskListener listener) {
        return true;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        MeasurementRenderer<Run<?, ?>> measurementRenderer = new ProjectNameRenderer(customPrefix, customProjectName);

        // get the target from the job's config
        Target target = getTarget();
        if (target==null) {
            throw new RuntimeException("Target was null!");
        }

        // prepare a meaningful logmessage
        String logMessage = "[InfluxDB Plugin] Publishing data to: " + target.toString();

        // write to jenkins logger
        logger.log(Level.INFO, logMessage);
        // write to jenkins console
        listener.getLogger().println(logMessage);

        // use proxy if checked
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (target.isUsingJenkinsProxy()) {
            builder.proxy(Jenkins.getInstance().proxy.createProxy(target.getUrl()));
            if (Jenkins.getInstance().proxy.getUserName() != null) {
                builder.proxyAuthenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        if (response.request().header("Proxy-Authorization") != null) {
                            return null; // Give up, we've already failed to authenticate.
                        }

                        String credential = Credentials.basic(Jenkins.getInstance().proxy.getUserName(), Jenkins.getInstance().proxy.getPassword());
                        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                    }
                });
            }
            builder.build();
        }

        // connect to InfluxDB
        InfluxDB influxDB = Strings.isNullOrEmpty(target.getUsername()) ? InfluxDBFactory.connect(target.getUrl(), builder) : InfluxDBFactory.connect(target.getUrl(), target.getUsername(), target.getPassword(), builder);
        List<Point> pointsToWrite = new ArrayList<Point>();

        // finally write to InfluxDB
        JenkinsBasePointGenerator jGen = new JenkinsBasePointGenerator(measurementRenderer, customPrefix, build, listener, jenkinsEnvParameterField, jenkinsEnvParameterTag);
        addPoints(pointsToWrite, jGen, listener);

        CustomDataPointGenerator cdGen = new CustomDataPointGenerator(measurementRenderer, customPrefix, build, customData, customDataTags);
        if (cdGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Custom data found. Writing to InfluxDB...");
            addPoints(pointsToWrite, cdGen, listener);
        }

        CustomDataMapPointGenerator cdmGen = new CustomDataMapPointGenerator(measurementRenderer, customPrefix, build, customDataMap, customDataMapTags);
        if (cdmGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Custom data map found. Writing to InfluxDB...");
            addPoints(pointsToWrite, cdmGen, listener);
        }

        try {
            CoberturaPointGenerator cGen = new CoberturaPointGenerator(measurementRenderer, customPrefix, build);
            if (cGen.hasReport()) {
                listener.getLogger().println("[InfluxDB Plugin] Cobertura data found. Writing to InfluxDB...");
                addPoints(pointsToWrite, cGen, listener);
            }
        } catch (NoClassDefFoundError ignore) {
            logger.log(Level.INFO, "Plugin skipped: Cobertura");
        }

        try {
            RobotFrameworkPointGenerator rfGen = new RobotFrameworkPointGenerator(measurementRenderer, customPrefix, build);
            if (rfGen.hasReport()) {
                listener.getLogger().println("[InfluxDB Plugin] Robot Framework data found. Writing to InfluxDB...");
                addPoints(pointsToWrite, rfGen, listener);
            }
        } catch (NoClassDefFoundError ignore) {
            logger.log(Level.INFO, "Plugin skipped: Robot Framework");
        }

        try {
            JacocoPointGenerator jacoGen = new JacocoPointGenerator(measurementRenderer, customPrefix, build);
            if (jacoGen.hasReport()) {
                listener.getLogger().println("[InfluxDB Plugin] Jacoco data found. Writing to InfluxDB...");
                addPoints(pointsToWrite, jacoGen, listener);
            }
        } catch (NoClassDefFoundError ignore) {
            logger.log(Level.INFO, "Plugin skipped: JaCoCo");
        }

        try {
            PerformancePointGenerator perfGen = new PerformancePointGenerator(measurementRenderer, customPrefix, build);
            if (perfGen.hasReport()) {
                listener.getLogger().println("[InfluxDB Plugin] Performance data found. Writing to InfluxDB...");
                addPoints(pointsToWrite, perfGen, listener);
            }
        } catch (NoClassDefFoundError ignore) {
            logger.log(Level.INFO, "Plugin skipped: Performance");
        }

        SonarQubePointGenerator sonarGen = new SonarQubePointGenerator(measurementRenderer, customPrefix, build, listener);
        if (sonarGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] SonarQube data found. Writing to InfluxDB...");
            addPoints(pointsToWrite, sonarGen, listener);
        }


        ChangeLogPointGenerator changeLogGen = new ChangeLogPointGenerator(measurementRenderer, customPrefix, build);
        if (changeLogGen.hasReport()) {
            listener.getLogger().println("[InfluxDB Plugin] Git ChangeLog data found. Writing to InfluxDB...");
            addPoints(pointsToWrite, changeLogGen, listener);
        }

        try {
            PerfPublisherPointGenerator perfPublisherGen = new PerfPublisherPointGenerator(measurementRenderer, customPrefix, build);
            if (perfPublisherGen.hasReport()) {
                listener.getLogger().println("[InfluxDB Plugin] PerfPublisher data found. Writing to InfluxDB...");
                addPoints(pointsToWrite, perfPublisherGen, listener);
            }
        } catch (NoClassDefFoundError ignore) {
            logger.log(Level.INFO, "Plugin skipped: Performance Publisher");
        }

        writeToInflux(target, influxDB, pointsToWrite);
        listener.getLogger().println("[InfluxDB Plugin] Completed.");
    }

    private void addPoints(List<Point> pointsToWrite, PointGenerator generator, TaskListener listener) {
        try {
            pointsToWrite.addAll(Arrays.asList(generator.generate()));
        } catch (Exception e) {
            listener.getLogger().println("[InfluxDB Plugin] Failed to collect data. Ignoring Exception:" + e);
        }
    }

    private void writeToInflux(Target target, InfluxDB influxDB, List<Point> pointsToWrite) {
        /**
         * build batchpoints for a single write.
         */
        try {
            BatchPoints batchPoints = BatchPoints
                    .database(target.getDatabase())
                    .points(pointsToWrite.toArray(new Point[0]))
                    .retentionPolicy(target.getRetentionPolicy())
                    .consistency(ConsistencyLevel.ANY)
                    .build();
            influxDB.write(batchPoints);
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
