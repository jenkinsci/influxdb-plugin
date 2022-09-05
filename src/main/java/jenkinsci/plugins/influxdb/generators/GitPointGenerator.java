package jenkinsci.plugins.influxdb.generators;

import java.util.ArrayList;
import java.util.Collection;
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
    protected static final String UNIQUE_ID = "unique_id";

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
        String sha1String = null;
        String branchName = null;
        BuildData gitAction = null;
        for (int i = 0; i < gitActions.size(); i++) {
            gitAction = gitActions.get(i);
            Revision revision = gitAction.getLastBuiltRevision();
            if(revision != null) {
                sha1String = revision.getSha1String();
                Collection<Branch> branches = revision.getBranches();
                if (CollectionUtils.isNotEmpty(branches)) {
                    branchName = branches.iterator().next().getName();
                }
            }
            Point point = buildPoint("git_data", customPrefix, build)
                    .addTag(UNIQUE_ID, String.valueOf(i+1))
                    .addField(GIT_REPOSITORY, !CollectionUtils.isEmpty(gitAction.getRemoteUrls()) ? gitAction.getRemoteUrls().iterator().next() : "")//
                    .addField(GIT_REFERENCE, branchName)
                    .addField(GIT_REVISION, sha1String);
            points.add(point);
        }
        return points.toArray(new Point[0]);
    }

}
