package jenkinsci.plugins.influxdb.generators;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.influxdb.client.write.Point;

import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;

/**
 * @author Mathieu Delrocq
 */
public class GitPointGeneratorTest {

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String JOB_NAME = "job_name";
    private static final String GIT_REPOSITORY = "repository";
    private static final String GIT_REFERENCE = "reference";
    private static final String GIT_REVISION = "revision";

    private Run<?, ?> build;
    private List<BuildData> gitActions;
    private BuildData gitAction;
    private TaskListener listener;
    private long currTime;
    private ProjectNameRenderer measurementRenderer;
    private Revision revision;
    private Branch branch;

    @Before
    public void before() throws Exception {
        // Global Mocks
        listener = Mockito.mock(TaskListener.class);
        currTime = System.currentTimeMillis();
        measurementRenderer = new ProjectNameRenderer(CUSTOM_PREFIX, null);
        Job<?, ?> job = Mockito.mock(Job.class);
        Mockito.when(job.getName()).thenReturn(JOB_NAME);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + JOB_NAME);
        Mockito.when(job.getBuildHealth()).thenReturn(new HealthReport());

        build = Mockito.mock(Run.class);
        Mockito.doReturn(job).when(build).getParent();
        gitAction = Mockito.mock(BuildData.class);
        gitActions = new ArrayList<>();
        gitActions.add(gitAction);
        branch = Mockito.mock(Branch.class);
        revision = Mockito.mock(Revision.class);
        Mockito.when(build.getActions(BuildData.class)).thenReturn(gitActions);
        Mockito.when(gitAction.getLastBuiltRevision()).thenReturn(revision);
        List<Branch> branches = Arrays.asList(branch);
        Mockito.when(revision.getBranches()).thenReturn(branches);
        Mockito.when(branch.getName()).thenReturn(GIT_REFERENCE);
        Mockito.when(revision.getSha1String()).thenReturn(GIT_REVISION);
        Mockito.when(revision.getSha1String()).thenReturn(GIT_REVISION);
        Set<String> remoteUrls = new HashSet<>();
        remoteUrls.add(GIT_REPOSITORY);
        Mockito.when(gitAction.getRemoteUrls()).thenReturn(remoteUrls);
    }

    @Test
    public void test_with_datas() {
        GitPointGenerator gen = new GitPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY,
                CUSTOM_PREFIX);
        assertTrue(gen.hasReport());
        Point[] points = gen.generate();
        assertTrue(points != null && points.length != 0);
        assertTrue(points[0].hasFields());
        String lineProtocol = points[0].toLineProtocol();
        assertTrue(lineProtocol.contains("git_repository=\"repository\""));
        assertTrue(lineProtocol.contains("git_revision=\"revision\""));
        assertTrue(lineProtocol.contains("git_reference=\"reference\""));
    }

}
