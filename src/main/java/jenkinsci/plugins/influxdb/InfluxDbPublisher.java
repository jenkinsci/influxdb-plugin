package jenkinsci.plugins.influxdb;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import jenkinsci.plugins.influxdb.models.Target;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep {

    public static final String DEFAULT_MEASUREMENT_NAME = "jenkins_data";

    @Extension(optional = true)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Target to write to.
     */
    private String selectedTarget;

    /**
     * Custom project name, overrides the project name with the specified value.
     */
    private String customProjectName;

    /**
     * Custom prefix, for example in multi-branch pipelines, where every build is named
     * after the branch built and thus you have different builds called 'master' that report
     * different metrics.
     */
    private String customPrefix;

    /**
     * Custom data, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * Inside a pipeline script this can easily be done by calling:
     * <pre>{@code
     * def myDataMap = [:]
     * myDataMap['myKey'] = 'myValue'
     * step([$class: 'InfluxDbPublisher',
     *       target: myTarget,
     *       customPrefix: 'myPrefix',
     *       customData: myDataMap])
     * }</pre>
     */
    private Map<String, Object> customData;

    /**
     * Custom data tags, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * Inside a pipeline script this can easily be done by calling:
     * <pre>{@code
     * def myDataMapTags = [:]
     * myDataMapTags['myKey'] = 'myValue'
     * step([$class: 'InfluxDbPublisher',
     *       target: myTarget,
     *       customPrefix: 'myPrefix',
     *       customData: myDataMap,
     *       customDataTags: myDataMapTags])
     * }</pre>
     */
    private Map<String, String> customDataTags;

    /**
     * Custom tags that are sent to all measurements defined in customDataMaps.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myCustomDataMapTags = [:]
     * def myCustomTags = [:]
     * myCustomTags['buildResult'] = currentBuild.result
     * myCustomTags['NODE_LABELS'] = env.NODE_LABELS
     * myCustomDataMapTags['series1'] = myCustomTags
     * step([$class: 'InfluxDbPublisher',
     *       target: myTarget,
     *       customPrefix: 'myPrefix',
     *       customDataMap: myCustomDataMap,
     *       customDataMapTags: myCustomDataMapTags])
     * }</pre>
     */
    private Map<String, Map<String, String>> customDataMapTags;

    /**
     * Custom data maps, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * This goes beyond {@code customData} since it allows to define multiple {@code customData} measurements
     * where the name of the measurement is defined as the key of the {@code customDataMap}.
     * <p>
     * Example for a pipeline script:
     * <pre>{@code
     * def myDataMap1 = [:]
     * def myDataMap2 = [:]
     * def myCustomDataMap = [:]
     * myDataMap1['myMap1Key1'] = 11 // first value of first map
     * myDataMap1['myMap1Key2'] = 12 // second value of first map
     * myDataMap2['myMap2Key1'] = 21 // first value of second map
     * myDataMap2['myMap2Key2'] = 22 // second value of second map
     * myCustomDataMap['series1'] = myDataMap1
     * myCustomDataMap['series2'] = myDataMap2
     * step([$class: 'InfluxDbPublisher',
     *       target: myTarget,
     *       customPrefix: 'myPrefix',
     *       customDataMap: myCustomDataMap])
     * }</pre>
     */
    private Map<String, Map<String, Object>> customDataMap;

    /**
     * Jenkins parameter(s) which will be added as field set to measurement 'jenkins_data'.
     * If parameter value has a $-prefix, it will be resolved from current Jenkins job environment properties.
     */
    private String jenkinsEnvParameterField;

    /**
     * Jenkins parameter(s) which will be added as tag set to  measurement 'jenkins_data'.
     * If parameter value has a $-prefix, it will be resolved from current Jenkins job environment properties.
     */
    private String jenkinsEnvParameterTag;

    /**
     * Custom measurement name used for all measurement types,
     * overrides the default measurement names.
     * Default value is "jenkins_data"
     * <p>
     * For custom data, prepends "custom_", i.e. "some_measurement"
     * becomes "custom_some_measurement".
     * Default custom name remains "jenkins_custom_data".
     */
    private String measurementName;

    /**
     * Whether to replace dashes with underscores in tags.
     * i.e. "my-custom-tag" --> "my_custom_tag"
     */
    private boolean replaceDashWithUnderscore;

    private EnvVars env;

    public InfluxDbPublisher() {
    }

    @DataBoundConstructor
    public InfluxDbPublisher(String target) {
        this.selectedTarget = target;
    }

    public String getSelectedTarget() {
        String target = selectedTarget;
        if (target == null) {
            List<Target> targets = DESCRIPTOR.getTargets();
            if (!targets.isEmpty()) {
                target = targets.get(0).getDescription();
            }
        }
        return target;
    }

    @DataBoundSetter
    public void setSelectedTarget(String target) {
        Objects.requireNonNull(target);
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

    public Map<String, Object> getCustomData() {
        return customData;
    }

    @DataBoundSetter
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;
    }

    public Map<String, String> getCustomDataTags() {
        return customDataTags;
    }

    @DataBoundSetter
    public void setCustomDataTags(Map<String, String> customDataTags) {
        this.customDataTags = customDataTags;
    }

    public Map<String, Map<String, Object>> getCustomDataMap() {
        return customDataMap;
    }

    @DataBoundSetter
    public void setCustomDataMap(Map<String, Map<String, Object>> customDataMap) {
        this.customDataMap = customDataMap;
    }

    public Map<String, Map<String, String>> getCustomDataMapTags() {
        return customDataMapTags;
    }

    @DataBoundSetter
    public void setCustomDataMapTags(Map<String, Map<String, String>> customDataMapTags) {
        this.customDataMapTags = customDataMapTags;
    }

    public String getMeasurementName() {
        return measurementName;
    }

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

    private String getMeasurementNameIfNotBlankOrDefault() {
        return measurementName != null ? measurementName : DEFAULT_MEASUREMENT_NAME;
    }

    public Target getTarget() {
        List<Target> targets = DESCRIPTOR.getTargets();
        if (selectedTarget == null && !targets.isEmpty()) {
            return targets.get(0);
        }
        for (Target target : targets) {
            String targetInfo = target.getDescription();
            if (targetInfo.equals(selectedTarget)) {
                return target;
            }
        }
        return null;
    }

    public void setEnv(EnvVars env) {
        this.env = env;
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
        if (env == null) {
            env = build.getEnvironment(listener);
        }

        String expandedCustomPrefix = env.expand(customPrefix);
        String expandedCustomProjectName = env.expand(customProjectName);

        // Preparing the service
        InfluxDbPublicationService publicationService = new InfluxDbPublicationService(
                Collections.singletonList(target),
                expandedCustomProjectName,
                expandedCustomPrefix,
                customData,
                customDataTags,
                customDataMapTags,
                customDataMap,
                currTime,
                jenkinsEnvParameterField,
                jenkinsEnvParameterTag,
                measurementName,
                replaceDashWithUnderscore);

        // Publishes the metrics
        publicationService.perform(build, listener, env);
    }

    private long resolveTimestampForPointGenerationInNanoseconds(Run<?, ?> build) {
        long timestamp = getTarget().isJobScheduledTimeAsPointsTimestamp() ? build.getTimeInMillis() : System.currentTimeMillis();
        return timestamp * 1000000;
    }

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject {

        private List<Target> targets = new CopyOnWriteArrayList<>();

        public DescriptorImpl() {
            load();
        }

        @Nonnull
        @Deprecated
        public Target[] getDeprecatedTargets() {
            return targets.toArray(new Target[0]);
        }

        @DataBoundSetter
        @Deprecated
        public void setDeprecatedTargets(List<Target> targets) {
            this.targets = targets;
        }

        public List<Target> getTargets() {
            return InfluxDbGlobalConfig.getInstance().getTargets();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Publish build data to InfluxDB";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public ListBoxModel doFillSelectedTargetItems() {
            ListBoxModel model = new ListBoxModel();
            for (Target target : getTargets()) {
                model.add(target.getDescription());
            }
            return model;
        }

        void removeDeprecatedTargets() {
            this.targets = new CopyOnWriteArrayList<>();
        }
    }
}
