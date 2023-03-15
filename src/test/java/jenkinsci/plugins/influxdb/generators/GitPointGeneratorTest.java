package jenkinsci.plugins.influxdb.generators;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
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

    private static final String CUSTOM_PREFIX = "test_prefix";
    private static final String JOB_NAME = "job_name";
    private static final String GIT_REPOSITORY = "repository";
    private static final String GIT_REFERENCE = "reference";
    private static final String GIT_REVISION = "revision";

    private Run<?, ?> build;
    private List<BuildData> gitActions;
    private BuildData gitAction1;
    private BuildData gitAction2;
    private TaskListener listener;
    private long currTime;
    private ProjectNameRenderer measurementRenderer;
    private Revision revision1;
    private Revision revision2;
    private Branch branch1;
    private Branch branch2;

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
        gitAction1 = Mockito.mock(BuildData.class);
        gitAction2 = Mockito.mock(BuildData.class);
        gitActions = new ArrayList<>();
        gitActions.add(gitAction1);
        gitActions.add(gitAction2);
        branch1 = Mockito.mock(Branch.class);
        branch2 = Mockito.mock(Branch.class);
        revision1 = Mockito.mock(Revision.class);
        revision2 = Mockito.mock(Revision.class);
        Mockito.when(build.getActions(BuildData.class)).thenReturn(gitActions);
        Mockito.when(gitAction1.getLastBuiltRevision()).thenReturn(revision1);
        Mockito.when(gitAction2.getLastBuiltRevision()).thenReturn(revision2);
        List<Branch> branches1 = Arrays.asList(branch1);
        List<Branch> branches2 = Arrays.asList(branch2);
        Mockito.when(revision1.getBranches()).thenReturn(branches1);
        Mockito.when(revision2.getBranches()).thenReturn(branches2);
        Mockito.when(branch1.getName()).thenReturn(GIT_REFERENCE);
        Mockito.when(branch2.getName()).thenReturn(GIT_REFERENCE + "2");
        Mockito.when(revision1.getSha1String()).thenReturn(GIT_REVISION);
        Mockito.when(revision2.getSha1String()).thenReturn(GIT_REVISION + "2");
        Set<String> remoteUrls1 = new HashSet<>();
        remoteUrls1.add(GIT_REPOSITORY);
        Set<String> remoteUrls2 = new HashSet<>();
        remoteUrls2.add(GIT_REPOSITORY + "2");
        Mockito.when(gitAction1.getRemoteUrls()).thenReturn(remoteUrls1);
        Mockito.when(gitAction2.getRemoteUrls()).thenReturn(remoteUrls2);
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
        String lineProtocol2 = points[1].toLineProtocol();
        assertTrue(lineProtocol2.contains("git_repository=\"repository2\""));
        assertTrue(lineProtocol2.contains("git_revision=\"revision2\""));
        assertTrue(lineProtocol2.contains("git_reference=\"reference2\""));
    }

}
