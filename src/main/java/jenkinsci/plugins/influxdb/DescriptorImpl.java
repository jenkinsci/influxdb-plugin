package jenkinsci.plugins.influxdb;
 
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.CheckForNull;
 
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
    private transient CopyOnWriteList<Target> targets = new CopyOnWriteList<Target>();
 
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
        Iterator<Target> it = targets.iterator();
        int size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        return targets.toArray(new Target[size]);
    }

    public void setTargets(CopyOnWriteList newTargets) {
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
        targets.replaceBy(req.bindParametersToList(Target.class, "targetBinding."));
        save();
        return true;
    }
}
