package jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import jenkinsci.plugins.influxdb.models.AbstractPoint;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Collection;
import java.util.List;

public class ChangeLogPointGenerator extends AbstractPointGenerator {

    private static final String BUILD_DISPLAY_NAME = "display_name";

    private final String customPrefix;

    private StringBuilder affectedPaths;

    private StringBuilder messages;

    private StringBuilder culprits;

    private int commitCount = 0;

    public ChangeLogPointGenerator(Run<?, ?> build, TaskListener listener,
                                   ProjectNameRenderer projectNameRenderer,
                                   long timestamp, String jenkinsEnvParameterTag,
                                   String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        this.affectedPaths = new StringBuilder();
        this.messages = new StringBuilder();
        this.culprits = new StringBuilder();
    }

    public boolean hasReport() {
        if (build instanceof AbstractBuild) {   // freestyle job
            getChangeLogFromAbstractBuild(build);
        } else if (build instanceof WorkflowRun) {     // pipeline
            getChangeLogFromPipeline(build);
        }
        return this.getCommitCount() > 0;
    }

    public AbstractPoint[] generate() {
        AbstractPoint point = buildPoint("changelog_data", customPrefix, build);

        point.addField(BUILD_DISPLAY_NAME, build.getDisplayName())
                .addField("commit_messages", this.getMessages())
                .addField("culprits", this.getCulprits())
                .addField("affected_paths", this.getAffectedPaths())
                .addField("commit_count", this.getCommitCount());

        return new AbstractPoint[]{point};
    }

    private void getChangeLogFromAbstractBuild(Run<?, ?> run) {
        AbstractBuild<?, ?> abstractBuild = (AbstractBuild<?, ?>) run;
        ChangeLogSet<? extends ChangeLogSet.Entry> changeset = abstractBuild.getChangeSet();
        addChangeLogData(changeset);
    }

    private void getChangeLogFromPipeline(Run<?, ?> run) {
        WorkflowRun workflowRun = (WorkflowRun) run;
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeLogsSets = workflowRun.getChangeSets();
        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet : changeLogsSets) {
            addChangeLogData(changeLogSet);
        }
    }

    private void addChangeLogData(ChangeLogSet<? extends ChangeLogSet.Entry> changeLogSet) {
        for (ChangeLogSet.Entry str : changeLogSet) {
            Collection<? extends ChangeLogSet.AffectedFile> affectedFiles = str.getAffectedFiles();
            for (ChangeLogSet.AffectedFile affectedFile : affectedFiles) {
                this.affectedPaths.append(affectedFile.getPath());
                this.affectedPaths.append(", ");
            }
            this.messages.append(str.getMsg());
            this.messages.append(", ");

            this.culprits.append(str.getAuthor().getFullName());
            this.culprits.append(", ");

            this.commitCount += 1;
        }
    }

    private String getMessages() {
        return this.messages.length() > 0 ? this.messages.substring(0, this.messages.length() - 2) : "";
    }

    private String getCulprits() {
        return this.culprits.length() > 0 ? this.culprits.substring(0, this.culprits.length() - 2) : "";
    }

    private String getAffectedPaths() {
        return this.affectedPaths.length() > 0 ? this.affectedPaths.substring(0, this.affectedPaths.length() - 2) : "";
    }

    private int getCommitCount() {
        return this.commitCount;
    }
}
