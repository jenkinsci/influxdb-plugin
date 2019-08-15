package jenkinsci.plugins.influxdb;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkinsci.plugins.influxdb.models.Target;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class InfluxDbStep extends Step {

    private String selectedTarget;
    private String customProjectName;
    private String customPrefix;
    private String jenkinsEnvParameterField;
    private String jenkinsEnvParameterTag;
    private Map<String, Object> customData;
    private Map<String, String> customDataTags;
    private Map<String, Map<String, Object>> customDataMap;
    private Map<String, Map<String, String>> customDataMapTags;
    private String measurementName;
    private boolean replaceDashWithUnderscore;

    private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();


    @DataBoundConstructor
    public InfluxDbStep(String selectedTarget, Map<String, Object> customData, Map<String, Map<String, Object>> customDataMap, String customPrefix) {
        this.selectedTarget = selectedTarget;
        this.customData = customData;
        this.customDataMap = customDataMap;
        this.customPrefix = customPrefix;
    }

    public String getSelectedTarget() {
        String ipTemp = selectedTarget;
        if (ipTemp == null) {
            Target[] targets = DESCRIPTOR.getTargets();
            if (targets.length > 0) {
                ipTemp = targets[0].getDescription();
            }
        }
        return ipTemp;
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

    public String getMeasurementName() {
        return measurementName;
    }

    public boolean getReplaceDashWithUnderscore() {
        return replaceDashWithUnderscore;
    }

    @DataBoundSetter
    public void setReplaceDashWithUnderscore(boolean replaceDashWithUnderscore) {
        this.replaceDashWithUnderscore = replaceDashWithUnderscore;
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

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new InfluxDbStepExecution(this, context);
    }


    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        private List<Target> targets = new CopyOnWriteArrayList<>();

        public DescriptorImpl() {
            load();
        }

        @Nonnull
        public Target[] getTargets() {
            return targets.toArray(new Target[0]);
        }

        @DataBoundSetter
        public void setTargets(List<Target> targets) {
            this.targets = targets;
        }

        public void addTarget(Target target) {
            targets.add(target);
        }
        public void removeTarget(String targetDescription) {
            targets.removeIf(target -> target.getDescription().equals(targetDescription));
        }

        @Override
        public String getFunctionName() {
            return "influxdb";
        }

        @Override
        public String getDisplayName() {
            return "Publish build data to InfluxDB.";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, Launcher.class, TaskListener.class);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            targets.clear();
            targets.addAll(req.bindJSONToList(Target.class, formData.get("targets")));
            save();
            return true;
        }

        public ListBoxModel doFillSelectedTargetItems() {
            ListBoxModel model = new ListBoxModel();
            for (Target target : targets) {
                model.add(target.getDescription());
            }
            return model;
        }
    }
}
