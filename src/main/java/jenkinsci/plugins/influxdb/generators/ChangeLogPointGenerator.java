package jenkinsci.plugins.influxdb.generators;

import java.util.Collection;
import java.util.Iterator;

import org.influxdb.dto.Point;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import jenkinsci.plugins.influxdb.renderer.MeasurementRenderer;

public class ChangeLogPointGenerator extends AbstractPointGenerator {
    
	public static final String BUILD_DISPLAY_NAME = "display_name";

	private final Run<?, ?> build;
	private final String customPrefix;
	
	private StringBuilder affectedPaths;

	private StringBuilder messages;

	private StringBuilder culprits;

	private int commitCount = 0;

	public ChangeLogPointGenerator(MeasurementRenderer<Run<?, ?>> projectNameRenderer, String customPrefix,
			Run<?, ?> build) {
		super(projectNameRenderer);
		this.build = build;
		this.customPrefix = customPrefix;
	}

	public boolean hasReport() {
		if (build instanceof AbstractBuild) {
			getChangeLog(build);
			return true;
		}
		return false;
	}

	public Point[] generate() {
		
		Point.Builder point = buildPoint(measurementName("changelog_data"), customPrefix, build);

		point.field(BUILD_DISPLAY_NAME, build.getDisplayName())
				.field("commit_messages", this.getMessages())
				.field("culprits", this.getCulprits())
				.field("affected_paths", this.getAffectedPaths())
				.field("commit_count", this.getCommitCount());

		return new Point[] { point.build() };
	}

	public void getChangeLog(Run<?, ?> run) {
		this.affectedPaths = new StringBuilder();

		this.messages = new StringBuilder();

		this.culprits = new StringBuilder();

		AbstractBuild<?,?> abstractBuild = (AbstractBuild<?,?>) run;
		ChangeLogSet<? extends ChangeLogSet.Entry> changeset = abstractBuild.getChangeSet();
		Iterator<? extends ChangeLogSet.Entry> itrChangeSet = changeset.iterator();
		while (itrChangeSet.hasNext()) {
			ChangeLogSet.Entry str = itrChangeSet.next();
			Collection<? extends ChangeLogSet.AffectedFile> affectedFiles = str.getAffectedFiles();
			Iterator<? extends ChangeLogSet.AffectedFile> affectedFilesItr = affectedFiles.iterator();
			while (affectedFilesItr.hasNext()) {
				this.affectedPaths.append(affectedFilesItr.next().getPath());
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
	    return this.messages.length() > 0 ? this.messages.substring(0, this.messages.length() - 2).toString(): "";
	}

	private String getCulprits() {
		return this.culprits.length() > 0 ? this.culprits.substring(0, this.culprits.length() - 2).toString(): "";
	}
	
	private String getAffectedPaths() {
		return this.affectedPaths.length() > 0 ? this.affectedPaths.substring(0, this.affectedPaths.length() - 2).toString(): "";
	}

	private int getCommitCount() {
        return this.commitCount;
	}
}
