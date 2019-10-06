package jenkinsci.plugins.influxdb;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.GlobalConfiguration;
import jenkinsci.plugins.influxdb.models.Target;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Extension
public class InfluxDbGlobalConfig extends GlobalConfiguration {
    private List<Target> targets = new CopyOnWriteArrayList<>();
    private boolean targetsMigrated = false;

    public InfluxDbGlobalConfig() {
        load();
    }

    public static InfluxDbGlobalConfig getInstance() {
        return GlobalConfiguration.all().get(InfluxDbGlobalConfig.class);
    }

    public List<Target> getTargets() {
        return Collections.unmodifiableList(targets);
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
        save();
    }

    @SuppressWarnings("deprecation")
    @Initializer(after = InitMilestone.JOB_LOADED)
    public void migrateTargets() {
        if (targetsMigrated) {
            return;
        }
        Optional<InfluxDbPublisher.DescriptorImpl> optionalDescriptor = ExtensionList.lookup(InfluxDbPublisher.DescriptorImpl.class).stream().findFirst();

        optionalDescriptor.ifPresent(publisher -> {
            if (publisher.getDeprecatedTargets().length > 0) {
                targets = Arrays.asList(publisher.getDeprecatedTargets());
                save();
            }
            publisher.removeDeprecatedTargets();
        });
        targetsMigrated = true;
        save();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) {
        targets = new CopyOnWriteArrayList<>();
        targets.addAll(req.bindJSONToList(Target.class, formData.get("targets")));
        save();
        return true;
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
}
