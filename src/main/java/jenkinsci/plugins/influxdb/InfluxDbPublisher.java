package jenkinsci.plugins.influxdb;

import com.google.common.base.Preconditions;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
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
import java.util.Collections;
import java.util.Map;

public class InfluxDbPublisher extends Notifier implements SimpleBuildStep {

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
     * custom data, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB
     * This can easily be done by calling
     * <p>
     * def myDataMap = [:]
     * myDataMap['myKey'] = 'myValue'
     * step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customData: myDataMap])
     * <p>
     * inside a pipeline script
     */
    private Map<String, Object> customData;

    /**
     * custom data maps, especially in pipelines, where additional information is calculated
     * or retrieved by Groovy functions which should be sent to InfluxDB.
     * <p>
     * This goes beyond customData since it allows to define multiple customData measurements
     * where the name of the measurement is defined as the key of the customDataMap.
     * <p>
     * Example for a pipeline script:
     * <p>
     * def myDataMap1 = [:]
     * def myDataMap2 = [:]
     * def myCustomDataMap = [:]
     * myDataMap1["myMap1Key1"] = 11 //first value of first map
     * myDataMap1["myMap1Key2"] = 12 //second value of first map
     * myDataMap2["myMap2Key1"] = 21 //first value of second map
     * myDataMap2["myMap2Key2"] = 22 //second value of second map
     * myCustomDataMap["series1"] = myDataMap1
     * myCustomDataMap["series2"] = myDataMap2
     * step([$class: 'InfluxDbPublisher', target: myTarget, customPrefix: 'myPrefix', customDataMap: myCustomDataMap])
     */
    private Map<String, Map<String, Object>> customDataMap;

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

    @DataBoundSetter
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    @DataBoundSetter
    public void setCustomDataMap(Map<String, Map<String, Object>> customDataMap) {
        this.customDataMap = customDataMap;
    }

    public Map<String, Map<String, Object>> getCustomDataMap() {
        return customDataMap;
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

        // Preparing the service
        InfluxDbPublicationService publicationService = new InfluxDbPublicationService(
                Collections.singletonList(target),
                customProjectName,
                customPrefix,
                customData,
                customDataMap
        );

        // Publishes the metrics
        publicationService.perform(build, listener);
    }
}
