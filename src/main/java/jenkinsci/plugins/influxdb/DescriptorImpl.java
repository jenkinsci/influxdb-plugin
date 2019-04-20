package jenkinsci.plugins.influxdb;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;

import org.kohsuke.stapler.StaplerRequest;

import jenkinsci.plugins.influxdb.models.Target;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject, java.io.Serializable {

    private static final String DISPLAY_NAME = "Publish build data to InfluxDb target";
    private List<Target> targets = new CopyOnWriteArrayList<>();

    public DescriptorImpl() {
        super(InfluxDbPublisher.class);
        load();
    }

    /** Add target to list of targets
     *
     * @param target Target to add
     */
    public void addTarget(Target target) {
        targets.add(target);
    }

    /** Remove target from list of targets
     *
     * @param targetDescription Target description of target to remove.
     */
    public void removeTarget(String targetDescription) {
        Target targetToRemove = null;
        for (Target target : targets) {
            String description = target.getDescription();
            if (description.equals(targetDescription)) {
                targetToRemove = target;
                break;
            }
        }
        if (targetToRemove != null) {
            targets.remove(targetToRemove);
        }
    }

    public Target[] getTargets() {
        return targets.toArray(new Target[0]);
    }

    public void setTargets(List newTargets) {
        targets = newTargets;
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
    public Publisher newInstance(@CheckForNull StaplerRequest req, @Nonnull JSONObject formData) {
        InfluxDbPublisher publisher = new InfluxDbPublisher();
        req.bindParameters(publisher, "publisherBinding.");
        return publisher;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
        targets = req.bindJSONToList(Target.class, formData.get("currentTarget"));
        save();
        return true;
    }
}
