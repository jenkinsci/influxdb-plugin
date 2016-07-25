package jenkinsci.plugins.influxdb;
 
import java.util.Iterator;
 
import org.kohsuke.stapler.StaplerRequest;
 
import jenkinsci.plugins.influxdb.models.Target;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
 
public final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements ModelObject {
 
    public static final String DISPLAY_NAME = "Publish build data to InfluxDb target";
    private final CopyOnWriteList<Target> targets = new CopyOnWriteList<Target>();
 
    public DescriptorImpl() {
        super(InfluxDbPublisher.class);
        load();
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
 
    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }
 
    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }
 
    @Override
    public Publisher newInstance(@Nonnull StaplerRequest req, JSONObject formData) {
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
