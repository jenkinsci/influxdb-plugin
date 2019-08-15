package jenkinsci.plugins.influxdb;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.Serializable;

public class InfluxDbStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient final InfluxDbStep step;

    InfluxDbStepExecution(InfluxDbStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override protected Void run() throws Exception {
        FilePath workspace = getContext().get(FilePath.class);
//        InfluxDbPublisher publisher = new InfluxDbPublisher(step.getTarget().getDescription(), step.getCustomProjectName(), step.getCustomPrefix(),
//                step.getJenkinsEnvParameterField(), step.getJenkinsEnvParameterTag(),
//                step.getCustomData(), step.getCustomDataTags(), step.getCustomDataMap(),
//                step.getCustomDataMapTags(), step.getMeasurementName(), step.getReplaceDashWithUnderscore());

        InfluxDbPublisher publisher = new InfluxDbPublisher(step.getSelectedTarget());
        publisher.perform(getContext().get(Run.class), workspace, getContext().get(Launcher.class), getContext().get(TaskListener.class));
        return null;
    }
}
