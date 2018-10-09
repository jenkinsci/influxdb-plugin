package jenkinsci.plugins.influxdb;
 
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;

import hudson.util.PersistedList;
import org.kohsuke.stapler.StaplerRequest;
 
import jenkinsci.plugins.influxdb.models.Target;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
 
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject, java.io.Serializable {
 
    public static final String DISPLAY_NAME = "Publish build data to InfluxDb target";
    public final PersistedList<Target> targets = new PersistedList<>(this);
 
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
        Iterator<Target> it = targets.iterator();
        while (it.hasNext()) {
            Target t = it.next();
            String description = t.getDescription();
            if (description.equals(targetDescription)) {
                targetToRemove = t;
                break;
            }
        }
        if (targetToRemove != null) {
            targets.remove(targetToRemove);
        }
    }

    public Target[] getTargets() {
        return targets.toArray(new Target[targets.size()]);
    }

    public void setTargets(List<Target> newTargets) throws IOException {
        targets.replaceBy(newTargets);
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
        req.bindJSON(this, formData);
        return true;
    }
}
