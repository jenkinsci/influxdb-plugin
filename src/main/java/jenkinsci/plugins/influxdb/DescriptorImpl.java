package jenkinsci.plugins.influxdb;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import jenkinsci.plugins.influxdb.models.Target;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

@Symbol("influxDbPublisher")
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject, java.io.Serializable {

    private static final String DISPLAY_NAME = "Publish build data to InfluxDB.";
    private List<Target> targets = new CopyOnWriteArrayList<>();

    public DescriptorImpl() {
        super(InfluxDbPublisher.class);
        load();
    }

    /**
     * Add target to list of targets
     *
     * @param target Target to add
     */
    public void addTarget(Target target) {
        targets.add(target);
    }

    /**
     * Remove target from list of targets
     *
     * @param targetDescription Target description of target to remove.
     */
    public void removeTarget(String targetDescription) {
        targets.removeIf(target -> target.getDescription().equals(targetDescription));
    }

    @Nonnull
    public Target[] getTargets() {
        return targets.toArray(new Target[0]);
    }

    @DataBoundSetter
    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
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
