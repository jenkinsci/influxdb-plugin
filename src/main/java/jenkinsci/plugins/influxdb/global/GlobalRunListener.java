package jenkinsci.plugins.influxdb.global;

import hudson.EnvVars;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Listens to all builds being completed and publishes their metrics to InfluxDB.
 */
@Extension
public class GlobalRunListener extends RunListener<Run<?, ?>> {

    private static final String VARIABLE_PREFIX = "INFLUXDB_PLUGIN_";

    @Override
    public void onCompleted(Run<?, ?> build, @Nonnull TaskListener listener) {
        // Gets the full path of the build's project
        String path = build.getParent().getRelativeNameFrom(Jenkins.getInstanceOrNull());
        // Gets the list of targets from the configuration
        List<Target> targets = Jenkins.getInstanceOrNull().getDescriptorByType(InfluxDbPublisher.DescriptorImpl.class).getTargets();
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

            EnvVars env;
            try {
                env = build.getEnvironment(listener);
            } catch (IOException | InterruptedException e) {
                env = new EnvVars();
            }

            // Creates the publication service
            InfluxDbPublicationService publicationService = new InfluxDbPublicationService(
                    selectedTargets,
                    env.get(VARIABLE_PREFIX + "CUSTOM_PROJECT_NAME"),
                    env.get(VARIABLE_PREFIX + "CUSTOM_PREFIX"),
                    null,
                    null,
                    null,
                    null,
                    System.currentTimeMillis() * 1000000,
                    env.expand(env.get(VARIABLE_PREFIX + "CUSTOM_FIELDS")),
                    env.expand(env.get(VARIABLE_PREFIX + "CUSTOM_TAGS")),
                    "jenkins_data"
            );

            // Publication
            publicationService.perform(build, listener, env);
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
