package jenkinsci.plugins.influxdb.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.influxdb.client.write.Point;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

/**
 * @author Mathieu Delrocq
 */
public class GitPointGenerator extends AbstractPointGenerator {

    // Point fields names
    protected static final String GIT_REPOSITORY = "git_repository";
    protected static final String GIT_REVISION = "git_revision";
    protected static final String GIT_REFERENCE = "git_reference";

    private String customPrefix;
    private List<BuildData> gitActions;

    public GitPointGenerator(Run<?, ?> build, TaskListener listener, ProjectNameRenderer projectNameRenderer,
            long timestamp, String jenkinsEnvParameterTag, String customPrefix) {
        super(build, listener, projectNameRenderer, timestamp, jenkinsEnvParameterTag);
        this.customPrefix = customPrefix;
        gitActions = build.getActions(BuildData.class);
    }

    /**
     * Check if git infos are presents in the build
     * 
     * @return true if present
     */
    @Override
    public boolean hasReport() {
        return CollectionUtils.isNotEmpty(gitActions);
    }

    /**
     * Generates Git Points with datas in Git plugins
     * 
     * return Array of Point
     */
    @Override
    public Point[] generate() {
        List<Point> points = new ArrayList<>();
        Revision revision = null;
        Branch branch = null;
        for (BuildData gitAction : gitActions) {
            revision = gitAction.getLastBuiltRevision();
            branch = revision.getBranches().iterator().next();
            Point point = buildPoint("git_data", customPrefix, build)//
                    .addField(GIT_REPOSITORY, !CollectionUtils.isEmpty(gitAction.getRemoteUrls()) ? gitAction.getRemoteUrls().iterator().next() : "")//
                    .addField(GIT_REFERENCE, branch.getName())//
                    .addField(GIT_REVISION, revision.getSha1String());//
            points.add(point);
        }
        return points.toArray(new Point[0]);
    }

}
