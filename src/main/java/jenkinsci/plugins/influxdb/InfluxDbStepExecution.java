package jenkinsci.plugins.influxdb;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

public class InfluxDbStepExecution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;

    private transient final InfluxDbStep step;

    InfluxDbStepExecution(InfluxDbStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        FilePath workspace = getContext().get(FilePath.class);
        InfluxDbPublisher publisher = new InfluxDbPublisher(step.getSelectedTarget());
        publisher.setCustomData(step.getCustomData());
        publisher.setCustomDataMap(step.getCustomDataMap());
        publisher.setCustomDataMapTags(step.getCustomDataMapTags());
        publisher.setCustomDataTags(step.getCustomDataTags());
        publisher.setCustomPrefix(step.getCustomPrefix());
        publisher.setCustomProjectName(step.getCustomProjectName());
        publisher.setJenkinsEnvParameterField(step.getJenkinsEnvParameterField());
        publisher.setJenkinsEnvParameterTag(step.getJenkinsEnvParameterTag());
        publisher.setMeasurementName(step.getMeasurementName());
        publisher.setEnv(getContext().get(EnvVars.class));

        publisher.perform(getContext().get(Run.class), workspace, getContext().get(EnvVars.class), getContext().get(Launcher.class), getContext().get(TaskListener.class));
        return null;
    }
}
