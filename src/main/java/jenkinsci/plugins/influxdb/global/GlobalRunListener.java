package jenkinsci.plugins.influxdb.global;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.InfluxDbPublicationService;
import jenkinsci.plugins.influxdb.InfluxDbPublisher;
import jenkinsci.plugins.influxdb.models.Target;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Listens to call builds being completed, and publishes their metrics
 * in InfluxDB.
 */
@Extension
public class GlobalRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        // Gets the full path of the build's project
        String path = build.getParent().getRelativeNameFrom(Jenkins.getInstance());
        // Gets the list of targets from the configuration
        Target[] targets = InfluxDbPublisher.DESCRIPTOR.getTargets();
        if (targets != null) {
            // Selects the targets eligible as global listeners and which match the build path
            List<Target> selectedTargets = new ArrayList<>();
            for (Target target : targets) {
                // Checks if the target matches the path to the project
                // Skip build if it already publishes information on this target
                if (isTargetMatchingPath(target, path) && !isPublicationInBuild(target, build)) {
                    selectedTargets.add(target);
                }
            }
            // If some targets are selected
            if (!selectedTargets.isEmpty()) {
                // Creates the publication service
                InfluxDbPublicationService publicationService = new InfluxDbPublicationService(
                        selectedTargets,
                        null,
                        null,
                        null,
                        null
                );
                // Publication
                publicationService.perform(build, listener);
            }
        }
    }

    private boolean isPublicationInBuild(Target target, Run<?, ?> build) {
        Job<?, ?> parent = build.getParent();
        if (parent instanceof AbstractProject) {
            InfluxDbPublisher publisher = (InfluxDbPublisher) ((AbstractProject) parent).getPublishersList().get(InfluxDbPublisher.class);
            if (publisher != null) {
                String buildTarget = publisher.getSelectedTarget();
                return buildTarget != null && StringUtils.equals(buildTarget, target.getDescription());
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isTargetMatchingPath(@Nonnull Target target, @Nonnull String path) {
        if (target.isGlobalListener()) {
            String pattern = target.getGlobalListenerFilter();
            return StringUtils.isBlank(pattern) || Pattern.matches(pattern, path);
        } else {
            return false;
        }
    }
}
