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
import jenkins.model.Jenkins;
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

    private String selectedTarget;
    private String customProjectName;
    private String customPrefix;
    private Map<String, Object> customData;
    private Map<String, String> customDataTags;
    private Map<String, Map<String, Object>> customDataMap;
    private Map<String, Map<String, String>> customDataMapTags;
    private String jenkinsEnvParameterField;
    private String jenkinsEnvParameterTag;
    private String measurementName;
    private EnvVars env;

    @DataBoundConstructor
    public InfluxDbPublisher(String selectedTarget) {
        this.selectedTarget = selectedTarget;
    }

    public String getSelectedTarget() {
        String target = selectedTarget;
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (target == null && jenkins != null) {
            List<Target> targets = jenkins.getDescriptorByType(DescriptorImpl.class).getTargets();
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

    public String getMeasurementName() {
        return measurementName;
    }

    @DataBoundSetter
    public void setMeasurementName(String measurementName) {
        this.measurementName = measurementName;
    }

    private String getMeasurementNameIfNotBlankOrDefault() {
        return measurementName != null ? measurementName : DEFAULT_MEASUREMENT_NAME;
    }

    public Target getTarget() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            List<Target> targets = jenkins.getDescriptorByType(DescriptorImpl.class).getTargets();
            if (selectedTarget == null && !targets.isEmpty()) {
                return targets.get(0);
            }
            for (Target target : targets) {
                String targetInfo = target.getDescription();
                if (targetInfo.equals(selectedTarget)) {
                    return target;
                }
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
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull EnvVars envVars, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
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
//            env = build.getEnvironment(listener);
            env = envVars;
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
                measurementName);

        // Publishes the metrics
        publicationService.perform(build, listener, env);
    }

    private long resolveTimestampForPointGenerationInNanoseconds(Run<?, ?> build) {
        long timestamp = getTarget().isJobScheduledTimeAsPointsTimestamp() ? build.getTimeInMillis() : System.currentTimeMillis();
        return timestamp * 1000000;
    }

    @Extension(optional = true)
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
