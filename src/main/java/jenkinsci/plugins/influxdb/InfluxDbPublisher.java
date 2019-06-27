package jenkinsci.plugins.influxdb;

import com.google.common.base.Preconditions;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import jenkinsci.plugins.influxdb.models.Target;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep {

    /** The logger. **/
    private static final Logger logger = Logger.getLogger(InfluxDbPublisher.class.getName());

    public static final String DEFAULT_MEASUREMENT_NAME = "jenkins_data";

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

    /**
     * custom measurement name used for all measurement types
     * Overrides the default measurement names.
     * Default value is "jenkins_data"
     * 
     * For custom data, prepends "custom_", i.e. "some_measurement"
     * becomes "custom_some_measurement".
     * Default custom name remains "jenkins_custom_data"
     */
    private String measurementName;


    /**
     * Whether or not replace dashes with underscores in tags.
     * i.e. "my-custom-tag" --> "my_custom_tag"
     */
    private boolean replaceDashWithUnderscore;

    @DataBoundConstructor
    public InfluxDbPublisher() {
    }

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

    @DataBoundSetter
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

    @DataBoundSetter
    public void setMeasurementName(String measurementName) {
        this.measurementName = measurementName;
    }

    public boolean getReplaceDashWithUnderscore() {
        return replaceDashWithUnderscore;
    }

    @DataBoundSetter
    public void setReplaceDashWithUnderscore(boolean replaceDashWithUnderscore) {
        this.replaceDashWithUnderscore = replaceDashWithUnderscore;
    }

    public String getMeasurementName() {
        return measurementName;
    }

    private String getMeasurementNameIfNotBlankOrDefault() {
        return measurementName != null ? measurementName : DEFAULT_MEASUREMENT_NAME;
    }

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
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // Gets the target from the job's config
        Target target = getTarget();
        if (target == null) {
            throw new RuntimeException("Target was null!");
        }

        // Get the current time for timestamping all point generation and convert to nanoseconds
        long currTime = resolveTimestampForPointGenerationInNanoseconds(build);

        measurementName = getMeasurementNameIfNotBlankOrDefault();
        final EnvVars env = build.getEnvironment(listener);
        String expandedCustomPrefix = env.expand(customPrefix);
        String expandedCustomProjectName = env.expand(customProjectName);

        // Preparing the service
        InfluxDbPublicationService publicationService = new InfluxDbPublicationService(
                Collections.singletonList(target),
                expandedCustomProjectName,
                expandedCustomPrefix,
                customData,
                customDataTags, customDataMapTags, customDataMap,
                currTime,
                jenkinsEnvParameterField,
                jenkinsEnvParameterTag, measurementName,
                replaceDashWithUnderscore);

        // Publishes the metrics
        publicationService.perform(build, listener);
    }

    private long resolveTimestampForPointGenerationInNanoseconds(final Run<?, ?> build) {
        long timestamp = System.currentTimeMillis();
        if (getTarget().isJobScheduledTimeAsPointsTimestamp()) {
            timestamp = build.getTimeInMillis();
        }
        return timestamp * 1000000;
    }
}
