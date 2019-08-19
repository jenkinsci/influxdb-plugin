package jenkinsci.plugins.influxdb;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkinsci.plugins.influxdb.models.Target;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Arrays;
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

    public Target[] getTargets() {
        return this.targets.toArray(new Target[0]);
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
        save();
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public void migrateTargets() {
        if (targetsMigrated) {
            return;
        }
        Optional<DescriptorImpl> optionalDescriptor = ExtensionList.lookup(DescriptorImpl.class).stream().findFirst();

        optionalDescriptor.ifPresent(publisher -> {
            if (publisher.getTargets().length > 0) {
                this.targets = Arrays.asList(publisher.getTargets());
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

    public ListBoxModel doFillSelectedTargetItems() {
        ListBoxModel model = new ListBoxModel();
        for (Target target : targets) {
            model.add(target.getDescription());
        }
        return model;
    }
}
