package jenkinsci.plugins.influxdb;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundSetter;

import jenkinsci.plugins.influxdb.models.Target;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject, java.io.Serializable {

    private static final String DISPLAY_NAME = "Publish build data to InfluxDB.";
    private List<Target> targets = new CopyOnWriteArrayList<>();

    public DescriptorImpl() {
        super(InfluxDbPublisher.class);
        load();
    }

    public void addTarget(Target target) {
        InfluxDbGlobalConfig.getInstance().addTarget(target);
    }

    public void removeTarget(String targetDescription) {
        InfluxDbGlobalConfig.getInstance().removeTarget(targetDescription);
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

    public Target[] getTargets() {
        return InfluxDbGlobalConfig.getInstance().getTargets();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
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
